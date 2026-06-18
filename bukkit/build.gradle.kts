import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm")
    id("com.gradleup.shadow")
}

val pluginMain = "io.wdsj.asw.bukkit.AdvancedSensitiveWords"
val pluginWebsite = "https://github.com/HaHaWTH/AdvancedSensitiveWords"
val pluginDescription = "Ultimate chat moderation solution for Minecraft"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":common"))

    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("org.geysermc.floodgate:api:2.2.2-SNAPSHOT")
    compileOnly("fr.xephi:authme:5.7.0-SNAPSHOT")
    compileOnly("org.jetbrains:annotations:26.0.2")
    compileOnly("com.google.guava:guava:33.4.0-jre")

    implementation(kotlin("stdlib"))
    implementation("com.github.houbb:sensitive-word:0.26.2")
    implementation("com.github.Anon8281:UniversalScheduler:0.1.7")
    implementation("org.bstats:bstats-bukkit:3.1.0")
    implementation("ch.jalu:configme:1.3.1") {
        exclude(group = "org.yaml", module = "snakeyaml")
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        javaParameters.set(true)
    }
}

tasks.processResources {
    filteringCharset = "UTF-8"
    val properties = mapOf(
        "pluginVersion" to project.version.toString(),
        "versionChannel" to rootProject.extra["versionChannel"],
        "pluginMain" to pluginMain,
        "pluginWebsite" to pluginWebsite,
        "pluginDescription" to pluginDescription,
        "gitCommitShort" to rootProject.extra["gitCommitShort"],
    )
    inputs.properties(properties)
    filesMatching("plugin.yml") {
        expand(properties)
    }
}

tasks.named<Jar>("jar") {
    enabled = false
}

tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set("AdvancedSensitiveWords-paper")
    archiveClassifier.set("")
    archiveVersion.set("")

    manifest {
        attributes("paperweight-mappings-namespace" to "mojang")
    }

    relocate("kotlin", "io.wdsj.asw.bukkit.libs.kt.stdlib")
    relocate("com.github.houbb", "io.wdsj.asw.bukkit.libs.lib")
    relocate("java.util.internal", "io.wdsj.asw.bukkit.libs.java.util.internal")
    relocate("com.github.Anon8281.universalScheduler", "io.wdsj.asw.bukkit.libs.universalScheduler")
    relocate("org.bstats", "io.wdsj.asw.bukkit.libs.bstats")
    relocate("org.jetbrains.annotations", "io.wdsj.asw.bukkit.libs.org.jetbrains.annotations")
    relocate("org.intellij.lang.annotations", "io.wdsj.asw.bukkit.libs.org.intellij.lang.annotations")
    relocate("org.apiguardian.api", "io.wdsj.asw.bukkit.libs.api")
    relocate("ch.jalu.configme", "io.wdsj.asw.bukkit.libs.config")

    exclude("org/yaml/snakeyaml/**")
    exclude("org/slf4j/**")
    exclude("*.md")
    exclude("META-INF/maven/**")
    exclude("com/google/gson/**")
    exclude("sample-db-prompt-template.txt")
    exclude("LICENSE")

    minimize()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
