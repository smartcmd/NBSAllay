plugins {
    id("java-library")
    id("org.allaymc.gradle.plugin") version "0.2.0"
}

// TODO: Update the group to yours (should be same to the package of the plugin main class)
group = "org.allaymc.javaplugintemplate"
// TODO: Update the description to yours
description = "Java plugin template for allay server"
version = "0.1.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

// See also https://github.com/AllayMC/AllayGradle
allay {
    // TODO: Update the api version to the latest
    // You can find the latest version here: https://central.sonatype.com/artifact/org.allaymc.allay/api
    api = "0.17.0"

    plugin {
        // TODO: Update the entrance when you change your plugin main class
        // Same to `org.allaymc.javaplugintemplate.JavaPluginTemplate`
        entrance = ".JavaPluginTemplate"
        // TODO: Use your handsome name here
        authors += "YourNameHere"
        // TODO: Update the website to yours
        website = "https://github.com/AllayMC/JavaPluginTemplate"
    }
}

dependencies {
    compileOnly(group = "org.projectlombok", name = "lombok", version = "1.18.34")
    annotationProcessor(group = "org.projectlombok", name = "lombok", version = "1.18.34")
}
