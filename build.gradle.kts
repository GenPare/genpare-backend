plugins {
    application
    kotlin("jvm") version "1.5.31"
}

group = "de.genpare"
version = "0.0.1"
application {
    mainClass.set("de.genpare.ApplicationKt")
}

repositories {
    mavenCentral()
}

val ktorVersion: String by project
val kotlinVersion: String by project
val logbackVersion: String by project
val klaxonVersion: String by project
val exposedVersion: String by project
val mysqlConnectorVersion: String by project

dependencies {
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-gson:$ktorVersion")
    implementation("io.ktor:ktor-auth-jwt:$ktorVersion")

    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("mysql:mysql-connector-java:$mysqlConnectorVersion")

    testImplementation("io.ktor:ktor-server-tests:$ktorVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
}