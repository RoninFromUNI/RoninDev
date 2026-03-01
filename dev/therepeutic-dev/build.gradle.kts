plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("org.jetbrains.intellij.platform") version "2.3.0"
}

group = "com.ronin.therapeuticdev"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        create("IC", "2024.2.5")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
        bundledPlugin("com.intellij.java")
    }
    implementation("org.xerial:sqlite-jdbc:3.44.1.0")
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "242"
            untilBuild= "253.*"
        }

        changeNotes = """
      Initial version
    """.trimIndent()
    }
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "21"
    }

    buildSearchableOptions {
        enabled = false
    }

    // Seed the local metrics DB with simulated session data.
    // Usage: ./gradlew seedDb
    register<JavaExec>("seedDb") {
        group = "therapeutic-dev"
        description = "Seeds metrics.db with simulated coding session data for testing"
        classpath = sourceSets["test"].runtimeClasspath
        mainClass.set("com.ronin.therapeuticdev.seeder.DbSeeder")
    }
}