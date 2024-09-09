package io.wdsj.asw.bukkit.service;

import com.alessiodp.libby.BukkitLibraryManager;
import com.alessiodp.libby.Library;
import io.wdsj.asw.bukkit.AdvancedSensitiveWords;

import java.util.Locale;

public class BukkitLibraryService {
    private final BukkitLibraryManager libraryManager;
    private static final Library ollama4j = Library.builder()
            .groupId("com{}github{}HaHaWTH")
            .artifactId("ollama4j-j8")
            .resolveTransitiveDependencies(true)
            .version("8ce2ad8840")
            .build();

    private static final Library openai4j = Library.builder()
            .groupId("dev{}ai4j")
            .artifactId("openai4j")
            .resolveTransitiveDependencies(true)
            .version("0.20.0")
            .build();

    public BukkitLibraryService(AdvancedSensitiveWords plugin) {
        libraryManager = new BukkitLibraryManager(plugin);
        libraryManager.addJitPack();
        if (Locale.getDefault().getCountry().toUpperCase(Locale.ROOT).equals("CN")) {
            libraryManager.addRepository("https://maven.aliyun.com/repository/public");
        } else {
            libraryManager.addSonatype();
            libraryManager.addMavenCentral();
        }
    }

    public void load() {
        libraryManager.loadLibraries(ollama4j, openai4j);
    }
}
