import java.io.File
import java.time.Duration

plugins {
    kotlin("jvm") version "1.9.22"
    application
}

group = "soko.ekibun"
version = "2.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.openpnp:opencv:4.9.0-0")
    implementation("com.formdev:flatlaf:3.7.2")
    implementation("com.formdev:flatlaf-intellij-themes:3.7.2")
}

application {
    mainClass.set("soko.ekibun.stitch.AppKt")
}

kotlin {
    jvmToolchain(17)
}

// ──────────────────────────────────────────────
// 剥离 OpenCV 非 Windows 平台原生库，减小发行包体积
// ──────────────────────────────────────────────
val stripOpenCvJar by tasks.registering(Jar::class) {
    group = "build"
    description = "剥离 OpenCV 非 Windows 平台原生库，减小发行包体积"
    val opencvFile = configurations.runtimeClasspath.get().files.single {
        it.name.startsWith("opencv-") && it.name.endsWith(".jar") && !it.name.contains("sources")
    }
    from(zipTree(opencvFile)) {
        exclude("nu/pattern/opencv/linux/**")
        exclude("nu/pattern/opencv/osx/**")
        exclude("nu/pattern/opencv/windows/x86_32/**")
    }
    archiveFileName.set("opencv-4.9.0-0-windows-only.jar")
    destinationDirectory.set(layout.buildDirectory.dir("stripped-libs"))
}

// 在 installDist 完成后替换为剥离后的 opencv jar
tasks.named("installDist") {
    dependsOn(stripOpenCvJar)
    doLast {
        val libDir = file("${layout.buildDirectory.get()}/install/${rootProject.name}/lib")
        libDir.listFiles()?.filter {
            it.name.startsWith("opencv-") && !it.name.contains("windows-only") && !it.name.contains("sources")
        }?.forEach { it.delete() }
        copy {
            from(layout.buildDirectory.dir("stripped-libs"))
            into(libDir)
        }
    }
}

// ──────────────────────────────────────────────
// jpackage 打包：便携版 ZIP / 单文件安装包（Windows）
// 用法：
//   .\gradlew packagePortable   # build/distributions/Stitch-windows-1.0.0-portable.zip
//   .\gradlew packageInstaller  # build/distributions/Stitch-windows-1.0.0.exe（需 WiX 3.x）
//   .\gradlew packageAll        # 以上两者一次构建
// jpackage 路径优先级：-PjpackageBin="..." > 环境变量 JPACKAGE_BIN > 当前 JVM 自带
// ──────────────────────────────────────────────
val appMainJar = "${rootProject.name}-${project.version}.jar"
val jpackageBin = (findProperty("jpackageBin") as? String)
    ?: System.getenv("JPACKAGE_BIN")
    ?: "${System.getProperty("java.home")}${File.separator}bin${File.separator}jpackage.exe"

fun requireWindowsJpackage() {
    val os = System.getProperty("os.name") ?: ""
    require(os.contains("Windows", ignoreCase = true)) { "jpackage 打包仅支持在 Windows 上执行，当前系统：$os" }
    val exe = File(jpackageBin)
    require(exe.exists()) { "找不到 jpackage：$jpackageBin，请安装 JDK 17+ 或用 -PjpackageBin=... / JPACKAGE_BIN 指定路径" }
}

val jpackageAppImage by tasks.registering(Exec::class) {
    group = "distribution"
    description = "jpackage 生成 app-image（便携版中间产物）"
    dependsOn("installDist")
    timeout.set(Duration.ofMinutes(5))
    doFirst { requireWindowsJpackage(); project.delete(layout.buildDirectory.dir("jpackage")) }
    val inputDir = layout.buildDirectory.dir("install/${rootProject.name}/lib")
    val destDir = layout.buildDirectory.dir("jpackage")
    commandLine(
        jpackageBin,
        "--type", "app-image",
        "--name", rootProject.name,
        "--input", inputDir.get().asFile.absolutePath,
        "--main-jar", appMainJar,
        "--main-class", "soko.ekibun.stitch.AppKt",
        "--dest", destDir.get().asFile.absolutePath
    )
}

val portableZip by tasks.registering(Zip::class) {
    group = "distribution"
    description = "打包便携版 ZIP（含 EXE + 内置 JRE）"
    dependsOn(jpackageAppImage)
    archiveFileName.set("${rootProject.name}-${project.version}-portable.zip")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
    from(layout.buildDirectory.dir("jpackage/${rootProject.name}"))
}

tasks.register("packagePortable") {
    group = "distribution"
    description = "一键构建便携版 ZIP"
    dependsOn(portableZip)
}

tasks.register<Exec>("packageInstaller") {
    group = "distribution"
    description = "生成单文件安装包 .exe（需 WiX Toolset 3.x）"
    dependsOn("installDist")
    timeout.set(Duration.ofMinutes(5))
    doFirst {
        requireWindowsJpackage()
        val path = System.getenv("PATH") ?: ""
        val hasWix = listOf("candle.exe", "light.exe").all { tool ->
            path.split(";").any { dir -> dir.isNotBlank() && File(dir, tool).exists() }
        }
        require(hasWix) { "生成安装包需要 WiX Toolset 3.x（candle.exe/light.exe 需在 PATH 中），详见 README" }
    }
    val inputDir = layout.buildDirectory.dir("install/${rootProject.name}/lib")
    val destDir = layout.buildDirectory.dir("distributions")
    commandLine(
        jpackageBin,
        "--type", "exe",
        "--name", rootProject.name,
        "--app-version", project.version.toString(),
        "--input", inputDir.get().asFile.absolutePath,
        "--main-jar", appMainJar,
        "--main-class", "soko.ekibun.stitch.AppKt",
        "--dest", destDir.get().asFile.absolutePath,
        "--win-dir-chooser", "--win-menu", "--win-shortcut"
    )
}

tasks.register("packageAll") {
    group = "distribution"
    description = "一键构建便携版 ZIP + 单文件安装包"
    dependsOn(portableZip, "packageInstaller")
}
