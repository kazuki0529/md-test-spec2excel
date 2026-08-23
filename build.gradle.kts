plugins {
    kotlin("jvm") version "1.9.23"
    id("com.gradleup.shadow") version "8.3.6"
    application
}

group = "com.example"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jxls:jxls-poi:2.12.0")
    implementation("com.vladsch.flexmark:flexmark-all:0.64.0")
}

kotlin {
    jvmToolchain(8)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

application {
    mainClass.set("com.example.md2excel.MainKt")
}

tasks.shadowJar {
    archiveBaseName.set("md-test-spec2excel")
    archiveClassifier.set("")
    archiveVersion.set(version.toString())
    manifest {
        attributes["Main-Class"] = "com.example.md2excel.MainKt"
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
