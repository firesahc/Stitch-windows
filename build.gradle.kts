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
    implementation("com.github.kwhat:jnativehook:2.2.2")
    implementation("com.formdev:flatlaf:3.4")
    implementation("com.formdev:flatlaf-intellij-themes:3.4")
}

application {
    mainClass.set("soko.ekibun.stitch.AppKt")
}

kotlin {
    jvmToolchain(17)
}
