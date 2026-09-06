plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

val paperVersion = "26.2.build.121-stable"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperVersion")
}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(25)
    }
    withType<Jar> {
        archiveBaseName.set("Garbage-Collector")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}
