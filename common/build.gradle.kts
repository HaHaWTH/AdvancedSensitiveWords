plugins {
    `java-library`
}

val generatedVersionDir = layout.buildDirectory.dir("generated/sources/version-template/java")

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

sourceSets {
    named("main") {
        java {
            srcDir(generatedVersionDir)
        }
    }
}

dependencies {
    compileOnly("org.jetbrains:annotations:26.0.2")
    compileOnly("com.google.code.gson:gson:2.12.1")
    compileOnly("com.google.guava:guava:33.4.0-jre")
}

val generateVersionTemplate by tasks.registering(Copy::class) {
    from("src/main/java/io/wdsj/asw/common/template/PluginVersionTemplate.java")
    into(generatedVersionDir)
    filteringCharset = "UTF-8"
    expand(mapOf<String, String>(
        "pluginVersion" to project.version.toString(),
        "versionChannel" to rootProject.extra["versionChannel"].toString(),
        "gitCommitShort" to rootProject.extra["gitCommitShort"].toString(),
        "gitCommitFull" to rootProject.extra["gitCommitFull"].toString(),
        "gitBranch" to rootProject.extra["gitBranch"].toString(),
    ))
}

tasks.withType<JavaCompile>().configureEach {
    dependsOn(generateVersionTemplate)
    exclude("io/wdsj/asw/common/template/PluginVersionTemplate.java")
    options.encoding = "UTF-8"
    options.release.set(17)
}
