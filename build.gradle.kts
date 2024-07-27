plugins {
    java
    `java-library`
    `maven-publish`
    kotlin("jvm") version "1.9.20"
    id("io.github.goooler.shadow") version "8.1.7"
    id("com.willfp.libreforge-gradle-plugin") version "1.0.0"
}

group = "com.willfp"
version = findProperty("version")!!
val libreforgeVersion = findProperty("libreforge-version")

base {
    archivesName.set(project.name)
}

dependencies {
    project(":eco-core").dependencyProject.subprojects {
        implementation(this)
    }
}

allprojects {
    apply(plugin = "java")
    apply(plugin = "kotlin")
    apply(plugin = "maven-publish")
    apply(plugin = "io.github.goooler.shadow")

    repositories {
        mavenLocal()
        mavenCentral()

        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.auxilor.io/repository/maven-public/")
        maven("https://jitpack.io")
        maven("https://mvn.lumine.io/repository/maven-public/")
    }

    dependencies {
        compileOnly("com.willfp:eco:6.55.0")
        compileOnly("org.jetbrains:annotations:23.0.0")
        compileOnly("org.jetbrains.kotlin:kotlin-stdlib:1.9.20")
    }

    java {
        withSourcesJar()
        toolchain.languageVersion.set(JavaLanguageVersion.of(17))
    }

    tasks {
        shadowJar {
            relocate("com.willfp.libreforge.loader", "com.willfp.ecopets.libreforge.loader")
            relocate("com.willfp.ecomponent", "com.willfp.ecopets.ecomponent")
            relocate("com.willfp.modelenginebridge", "com.willfp.ecopets.modelenginebridge")
        }

        compileKotlin {
            kotlinOptions {
                jvmTarget = "17"
            }
        }

        compileJava {
            options.isDeprecation = true
            options.encoding = "UTF-8"

            dependsOn(clean)
        }

        processResources {
            filesMatching(listOf("**plugin.yml", "**eco.yml")) {
                expand(
                    "version" to project.version,
                    "libreforgeVersion" to libreforgeVersion,
                    "pluginName" to rootProject.name
                )
            }
        }

        build {
            dependsOn(shadowJar)
        }
    }
}
