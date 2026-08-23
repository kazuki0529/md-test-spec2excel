plugins {
    kotlin("jvm") version "1.9.23"
    id("com.gradleup.shadow") version "8.3.6"
    application
}

group = "io.github.kazuki0529"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jxls:jxls-poi:2.12.0")
    implementation("com.vladsch.flexmark:flexmark-all:0.62.2")
    implementation("org.slf4j:slf4j-api:2.0.13")
    runtimeOnly("org.slf4j:slf4j-simple:2.0.13")

    testImplementation("org.junit.jupiter:junit-jupiter:5.9.3")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.9.3")
}

tasks.test {
    useJUnitPlatform()
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
    mainClass.set("io.github.kazuki0529.mdspec2excel.MainKt")
}

tasks.shadowJar {
    archiveBaseName.set("md-test-spec2excel")
    archiveClassifier.set("")
    archiveVersion.set(version.toString())
    manifest {
        attributes["Main-Class"] = "io.github.kazuki0529.mdspec2excel.MainKt"
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
