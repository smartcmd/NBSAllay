plugins {
    id("java-library")
    id("org.allaymc.gradle.plugin") version "0.2.1"
}

group = "me.daoge.nbsallay"
description = "Note block song player for Allay server"
version = "0.1.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

allay {
    api = "0.18.0-SNAPSHOT"

    plugin {
        entrance = ".NBSAllay"
        authors += "daoge_cmd"
        website = "https://github.com/smartcmd/NBSAllay"
    }
}

dependencies {
    implementation(group = "net.raphimc", name = "NoteBlockLib", version = "3.1.1")
    compileOnly(group = "org.projectlombok", name = "lombok", version = "1.18.34")
    annotationProcessor(group = "org.projectlombok", name = "lombok", version = "1.18.34")
}
