package io.wdsj.asw.bukkit.method;

import com.github.houbb.sensitive.word.api.IWordAllow;
import io.wdsj.asw.bukkit.AdvancedSensitiveWords;
import io.wdsj.asw.bukkit.impl.list.AdvancedList;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ExternalWordAllow implements IWordAllow {
    private final File dataFolder = Paths.get(AdvancedSensitiveWords.getInstance().getDataFolder().getPath(),"external","allow").toFile();

    @Override
    public List<String> allow() {
        List<String> totalList = new AdvancedList<>();

        if (Files.notExists(dataFolder.toPath())) {
            try {
                Files.createDirectories(dataFolder.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try (Stream<Path> paths = Files.walk(dataFolder.toPath())) {
            List<File> files = paths
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .collect(Collectors.toList());

            for (File file : files) {
                List<String> lines = Files.readAllLines(file.toPath());
                totalList.addAll(lines);
            }
            if (files.size() > 0) AdvancedSensitiveWords.getInstance().getLogger().info("Loaded " + files.size() + " external allow file(s).");
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
        return totalList;
    }
}
