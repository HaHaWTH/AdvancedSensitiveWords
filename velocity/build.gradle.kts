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
    implementation("org.bstats:bstats-velocity:3.2.1")
    implementation("com.github.thatsmusic99:ConfigurationMaster-API:v2.0.0-rc.3")
    implementation("org.java-websocket:Java-WebSocket:${property("javaWebSocketVersion")}")

    compileOnly("com.velocitypowered:velocity-api:3.5.0-SNAPSHOT")
    compileOnly("com.google.guava:guava:33.4.0-jre")
    compileOnly("com.google.code.gson:gson:2.12.1")
    annotationProcessor("com.velocitypowered:velocity-api:3.5.0-SNAPSHOT")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
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
    relocate("org.java_websocket", "io.wdsj.asw.velocity.libs.websocket")

    minimize {
        exclude(dependency("org.java-websocket:Java-WebSocket:.*"))
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
