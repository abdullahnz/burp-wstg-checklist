plugins {
    kotlin("jvm") version "2.2.20"
    id("com.gradleup.shadow") version "8.3.8"
    application
}

group = "io.github.d0ublew.bapp.starter"
version = "1.0.0"

repositories {
    mavenCentral()
    mavenLocal()
}

application {
    mainClass = "$group.MainKt"
}

val bappCommon = "io.github.d0ublew.bapp.common"
val bappCommonVersion = "1.2.0"

dependencies {
    testImplementation(kotlin("test"))
    compileOnly("net.portswigger.burp.extensions:montoya-api:2025.8")
    implementation("${bappCommon}:config:${bappCommonVersion}")
    implementation("${bappCommon}:log:${bappCommonVersion}")
    implementation("${bappCommon}:ui:${bappCommonVersion}")
    implementation("com.google.code.gson:gson:2.10.1")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveBaseName = "WSTG Checklist"
    minimize()
    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
        )
    }
    dependencies {
        exclude("${application.mainClass.get().replace(".", "/")}.class")
        exclude("logback.xml")
        exclude("DebugProbesKt.bin")
    }
}
