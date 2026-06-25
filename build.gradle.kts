plugins {
    `java-base`
    kotlin("jvm") version "2.2.0" apply false
    id("com.gradleup.shadow") version "9.4.2" apply false
    id("com.github.gmazzo.buildconfig") version "6.0.10" apply false
}

group = "io.wdsj"
val pluginVersion = providers.gradleProperty("pluginVersion").get()
val versionChannel = providers.gradleProperty("versionChannel").get()
version = pluginVersion

fun git(vararg args: String): String {
    return try {
        providers.exec {
            commandLine("git", *args)
            isIgnoreExitValue = true
        }.standardOutput.asText.get().trim().ifBlank { "unknown" }
    } catch (_: Exception) {
        "unknown"
    }
}

extra["versionChannel"] = versionChannel
extra["gitCommitShort"] = git("rev-parse", "--short", "HEAD")
extra["gitCommitFull"] = git("rev-parse", "HEAD")
extra["gitBranch"] = git("rev-parse", "--abbrev-ref", "HEAD")

subprojects {
    group = rootProject.group
    version = rootProject.version
}
