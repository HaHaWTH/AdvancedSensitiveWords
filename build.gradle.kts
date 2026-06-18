plugins {
    `java-base`
    kotlin("jvm") version "2.2.0" apply false
    id("com.gradleup.shadow") version "9.2.1" apply false
}

group = "io.wdsj"
version = "1.3"

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

val versionChannel = providers.gradleProperty("versionChannel").orElse("dev").get()

extra["versionChannel"] = versionChannel
extra["gitCommitShort"] = git("rev-parse", "--short", "HEAD")
extra["gitCommitFull"] = git("rev-parse", "HEAD")
extra["gitBranch"] = git("rev-parse", "--abbrev-ref", "HEAD")

subprojects {
    group = rootProject.group
    version = rootProject.version
}
