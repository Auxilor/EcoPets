group = "com.willfp"
version = rootProject.version

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.7-R0.1-SNAPSHOT")
    compileOnly("com.github.ben-manes.caffeine:caffeine:3.0.2")

    implementation("com.willfp:ecomponent:1.4.1")
    implementation("com.willfp:ModelEngineBridge:1.3.0")
}

tasks {
    build {
        dependsOn(publishToMavenLocal)
    }
}


publishing {
    publications {
        create<MavenPublication>("shadow") {
            from(components["java"])
            artifactId = "EcoPets"
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/Auxilor/eco")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }

    publishing {
        repositories {
            maven {
                name = "Auxilor"
                url = uri("https://repo.auxilor.io/repository/maven-releases/")
                credentials {
                    username = System.getenv("MAVEN_USERNAME")
                    password = System.getenv("MAVEN_PASSWORD")
                }
            }
        }
    }
}