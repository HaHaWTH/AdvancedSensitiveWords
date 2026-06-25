plugins {
    `java-library`
    id("com.github.gmazzo.buildconfig")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    compileOnly("org.jetbrains:annotations:26.1.0")
    compileOnly("com.google.code.gson:gson:2.12.1")
    compileOnly("com.google.guava:guava:33.4.0-jre")
}

buildConfig {
    packageName("io.wdsj.asw.common.template")
    className("PluginVersionTemplate")
    useJavaOutput()

    buildConfigField("VERSION", project.version.toString())
    buildConfigField("VERSION_CHANNEL", rootProject.extra["versionChannel"].toString())
    buildConfigField("COMMIT_HASH_SHORT", rootProject.extra["gitCommitShort"].toString())
    buildConfigField("COMMIT_HASH", rootProject.extra["gitCommitFull"].toString())
    buildConfigField("COMMIT_BRANCH", rootProject.extra["gitBranch"].toString())
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(17)
}
