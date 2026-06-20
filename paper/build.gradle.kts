import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.JavaExec
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

val jmh: SourceSet by sourceSets.creating {
    java.srcDir("src/jmh/java")
    resources.srcDir("src/jmh/resources")
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}

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
    implementation("org.incendo:cloud-paper:${property("cloudVersion")}")
    implementation("org.incendo:cloud-minecraft-extras:${property("cloudVersion")}")

    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("org.geysermc.floodgate:api:2.2.2-SNAPSHOT")
    compileOnly("fr.xephi:authme:5.7.0-SNAPSHOT")
    compileOnly("com.github.retrooper:packetevents-spigot:2.12.2")
    compileOnly("org.jetbrains:annotations:26.0.2")
    compileOnly("com.google.guava:guava:33.4.0-jre")
    compileOnly("dev.langchain4j:langchain4j-open-ai:${property("langchain4jVersion")}")
    compileOnly("dev.langchain4j:langchain4j-http-client-jdk:${property("langchain4jVersion")}")

    implementation(kotlin("stdlib"))
    implementation("com.github.houbb:sensitive-word:0.29.5")
    implementation("com.github.Anon8281:UniversalScheduler:0.1.7")
    implementation("org.bstats:bstats-bukkit:3.1.0")
    implementation("de.exlll:configlib-yaml:4.8.1")
    runtimeOnly("org.snakeyaml:snakeyaml-engine:2.7")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    testImplementation("dev.langchain4j:langchain4j-open-ai:${property("langchain4jVersion")}")
    testImplementation("dev.langchain4j:langchain4j-http-client-jdk:${property("langchain4jVersion")}")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")

    add(jmh.implementationConfigurationName, "org.openjdk.jmh:jmh-core:${property("jmhVersion")}")
    add(jmh.annotationProcessorConfigurationName, "org.openjdk.jmh:jmh-generator-annprocess:${property("jmhVersion")}")
    add(jmh.runtimeOnlyConfigurationName, "com.google.guava:guava:33.4.0-jre")
}

configurations.named(jmh.implementationConfigurationName) {
    extendsFrom(configurations.implementation.get())
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

tasks.test {
    useJUnitPlatform()
}

tasks.register<JavaExec>("jmh") {
    group = "verification"
    description = "Runs the sensitive-word benchmark suite with JMH."
    dependsOn(tasks.named(jmh.classesTaskName))
    classpath = jmh.runtimeClasspath
    mainClass.set("org.openjdk.jmh.Main")
    args("SensitiveWordBookBenchmark")
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
        "langchain4jVersion" to project.property("langchain4jVersion"),
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
    relocate("de.exlll.configlib", "io.wdsj.asw.bukkit.libs.configlib")
    relocate("org.snakeyaml.engine.v2", "io.wdsj.asw.bukkit.libs.snakeyaml.engine.v2")
    relocate("org.snakeyaml.engine.external", "io.wdsj.asw.bukkit.libs.snakeyaml.engine.external")
    relocate("org.incendo", "io.wdsj.asw.bukkit.libs.incendo")
    relocate("io.leangen.geantyref", "io.wdsj.asw.bukkit.libs.geantyref")

    exclude("org/slf4j/**")
    exclude("net/kyori/**")
    exclude("*.md")
    exclude("META-INF/maven/**")
    exclude("com/google/gson/**")
    exclude("sample-db-prompt-template.txt")
    exclude("LICENSE")

    minimize {
        exclude(dependency("org.incendo:.*:.*"))
        exclude(dependency("de.exlll:.*:.*"))
        exclude(dependency("org.snakeyaml:snakeyaml-engine:.*"))
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
