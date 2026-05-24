plugins {
    kotlin("jvm") version "1.9.22"
    application
}

group = "soko.ekibun"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.openpnp:opencv:4.9.0-0")
    implementation("com.formdev:flatlaf:3.4")
    implementation("com.formdev:flatlaf-intellij-themes:3.4")
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
