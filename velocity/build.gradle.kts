import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    id("com.gradleup.shadow")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    implementation(project(":common"))
    implementation("org.bstats:bstats-velocity:3.1.0")
    implementation("com.github.thatsmusic99:ConfigurationMaster-API:v2.0.0-rc.3")

    compileOnly("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    compileOnly("com.google.guava:guava:33.4.0-jre")
    annotationProcessor("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(17)
}

tasks.named<Jar>("jar") {
    enabled = false
}

tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set("AdvancedSensitiveWords-velocity")
    archiveClassifier.set("")
    archiveVersion.set("")

    relocate("org.bstats", "io.wdsj.asw.velocity.libs.bstats")
    relocate("io.github.thatsmusic99", "io.wdsj.asw.velocity.libs.thatsmusic99")

    minimize()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
