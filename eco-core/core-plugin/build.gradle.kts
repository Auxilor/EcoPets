group = "com.willfp"
version = rootProject.version

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.7-R0.1-SNAPSHOT")
    compileOnly("com.github.ben-manes.caffeine:caffeine:3.0.2")
    // Pinned to the last release built with Kotlin metadata 2.1, which is all this branch's
    // kotlinc 2.1.0 can read. Newer EcoSkills (4.9.0+) carry metadata 2.3 and 2026.x artifacts
    // are shaded with eco's relocated Kotlin; both are unreadable here.
    compileOnly("com.willfp:EcoSkills:3.67.0")

    implementation("com.willfp:ecomponent:1.3.0")
    implementation("com.willfp:ModelEngineBridge:1.2.0")
}

publishing {
    publications {
        register<MavenPublication>("maven") {
            groupId = project.group.toString()
            version = project.version.toString()
            artifactId = rootProject.name

            artifact(rootProject.tasks.shadowJar.get().archiveFile)
        }
    }

    publishing {
        repositories {
            maven {
                name = "auxilor"
                url = uri("https://repo.auxilor.io/repository/maven-releases/")
                credentials {
                    username = System.getenv("MAVEN_USERNAME")
                    password = System.getenv("MAVEN_PASSWORD")
                }
            }
        }
    }
}

tasks {
    build {
        dependsOn(publishToMavenLocal)
    }
}
