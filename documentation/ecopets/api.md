---
title: "API"
sidebar_position: 6
---

This page is for developers who want to hook into EcoPets from their own plugin, e.g. to read or modify a player's pets and levels. EcoPets is open-source, so you can browse the full API in the repo.

## Source code

The source code is on [GitHub](https://github.com/Auxilor/EcoPets).

## Adding the dependency

1. Add the Auxilor repository to your `build.gradle.kts`.
2. Add EcoPets as a `compileOnly` dependency.

```kotlin
repositories {
    maven("https://repo.auxilor.io/repository/maven-public/")
}

dependencies {
    compileOnly("com.willfp:EcoPets:<version>")
}
```

The latest version available on the repo can be found [here](https://github.com/Auxilor/EcoPets/tags).

<hr/>

## Where to go next

- **Shared APIs:** the [eco framework](https://github.com/Auxilor/eco), where the shared APIs live.
- **Config side:** [How to Make a Pet](how-to-make-a-custom-pet) for building pets in config.