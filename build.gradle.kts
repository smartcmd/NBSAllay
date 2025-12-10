plugins {
    id("java-library")
    id("org.allaymc.gradle.plugin") version "0.2.1"
}

group = "me.daoge.nbsallay"
description = "Note block song player for Allay server"
version = "0.2.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

allay {
    api = "0.18.0"

    plugin {
        entrance = ".NBSAllay"
        authors += "daoge_cmd"
        website = "https://github.com/smartcmd/NBSAllay"
    }
}

dependencies {
    implementation(group = "net.raphimc", name = "NoteBlockLib", version = "3.1.1")
    compileOnly(group = "org.projectlombok", name = "lombok", version = "1.18.34")
    compileOnly(group = "org.cloudburstmc.protocol", name = "bedrock-connection", version = "3.0.0.Beta11-20251209.183710-13")
    annotationProcessor(group = "org.projectlombok", name = "lombok", version = "1.18.34")
}
