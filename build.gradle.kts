import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  kotlin("jvm") version "1.9.0"
  id("com.github.johnrengelman.shadow") version "8.1.1"
  application
}

tasks {
  named<ShadowJar>("shadowJar") {
    archiveBaseName.set("mistral")
    archiveClassifier.set("")
    archiveVersion.set("")
  }
}

group = "com.moni"
version = "1.0-SNAPSHOT"

repositories {
  mavenCentral()
  jcenter()
  maven("https://jitpack.io/")
}

dependencies {
  implementation("net.dv8tion", "JDA", "5.0.0-beta.13")
  implementation("com.github.minndevelopment", "jda-ktx", "9370cb1")
  implementation("com.jagrosh", "JagTag", "0.5")
  implementation("ch.qos.logback", "logback-classic", "1.4.11")
  implementation("org.scilab.forge", "jlatexmath", "1.0.7")
  implementation("org.jetbrains.exposed", "exposed-core", "0.42.1")
  implementation("org.jetbrains.exposed", "exposed-jdbc", "0.42.1")
  implementation("org.xerial", "sqlite-jdbc", "3.43.0.0")
}

kotlin {
  jvmToolchain(17)
}

application {
  mainClass.set("MistralKt")
}