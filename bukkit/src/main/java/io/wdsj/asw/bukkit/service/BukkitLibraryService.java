package io.wdsj.asw.bukkit.service;

import com.alessiodp.libby.BukkitLibraryManager;
import com.alessiodp.libby.Library;
import io.wdsj.asw.bukkit.AdvancedSensitiveWords;

import java.util.Locale;

public class BukkitLibraryService {
    private final BukkitLibraryManager libraryManager;
    /* Ollama4j */
    private static final Library ollama4j = Library.builder()
            .groupId("com{}github{}HaHaWTH")
            .artifactId("ollama4j-j8")
            .resolveTransitiveDependencies(false)
            .version("8ce2ad8840")
            .build();
    private static final Library lombok = Library.builder()
            .groupId("org{}projectlombok")
            .artifactId("lombok")
            .resolveTransitiveDependencies(false)
            .version("1.18.30")
            .build();
    private static final Library slf4j = Library.builder()
            .groupId("org{}slf4j")
            .artifactId("slf4j-api")
            .resolveTransitiveDependencies(false)
            .version("2.0.9")
            .build();
    private static final Library jackson = Library.builder()
            .groupId("com{}fasterxml{}jackson{}core")
            .artifactId("jackson-databind")
            .resolveTransitiveDependencies(false)
            .version("2.17.1")
            .build();
    private static final Library jackson_jsr310 = Library.builder()
            .groupId("com{}fasterxml{}jackson{}datatype")
            .artifactId("jackson-datatype-jsr310")
            .resolveTransitiveDependencies(false)
            .version("2.17.1")
            .build();
    private static final Library logback = Library.builder()
            .groupId("ch{}qos{}logback")
            .artifactId("logback-classic")
            .resolveTransitiveDependencies(false)
            .version("1.4.12")
            .build();

    private static final Library openai4j = Library.builder()
            .groupId("dev{}ai4j")
            .artifactId("openai4j")
            .resolveTransitiveDependencies(true) // TODO fix
            .version("0.27.0")
            .build();

    private static final Library caffeine = Library.builder()
            .groupId("com{}github{}ben-manes{}caffeine")
            .artifactId("caffeine")
            .resolveTransitiveDependencies(false)
            .version("2.9.3")
            .build();

    public BukkitLibraryService(AdvancedSensitiveWords plugin) {
        libraryManager = new BukkitLibraryManager(plugin);
        if (Locale.getDefault().getCountry().toUpperCase(Locale.ROOT).equals("CN")) {
            libraryManager.addRepository("https://maven.aliyun.com/repository/public/");
            libraryManager.addRepository("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/");
            libraryManager.addRepository("https://repo.huaweicloud.com/repository/maven/");
        } else {
            libraryManager.addSonatype();
            libraryManager.addMavenCentral();
        }
        libraryManager.addJitPack();
    }

    public void loadRequired() {
        libraryManager.loadLibraries(caffeine);
    }
    public void loadOllamaOptional() {
        libraryManager.loadLibraries(ollama4j, lombok, slf4j, jackson, jackson_jsr310, logback);
    }

    public void loadOpenAiOptional() {
        libraryManager.loadLibrary(openai4j);
    }

    public void loadWhisperJniOptional() {
        libraryManager.loadLibraries(Library.builder()
                .groupId("io{}github{}givimad")
                .artifactId("whisper-jni")
                .version("1.6.1")
                .build());
    }
}
