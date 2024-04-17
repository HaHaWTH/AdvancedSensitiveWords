package io.wdsj.asw.bukkit.method;

import com.github.houbb.sensitive.word.api.IWordDeny;
import io.wdsj.asw.bukkit.AdvancedSensitiveWords;
import io.wdsj.asw.bukkit.impl.list.AdvancedList;
import io.wdsj.asw.bukkit.setting.PluginSettings;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.settingsManager;

/**
 * OnlineWordDeny for ASW.
 * @since Dragon
 */
public class OnlineWordDeny implements IWordDeny {

    @Override
    public List<String> deny() {
        String link = settingsManager.getProperty(PluginSettings.ONLINE_WORDS_URL);
        String charset = settingsManager.getProperty(PluginSettings.ONLINE_WORDS_ENCODING);
        List<String> lines = new AdvancedList<>();
        try {
            URL url = new URL(link);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(4000);
            connection.setReadTimeout(5000);
            try (Scanner scanner = new Scanner(connection.getInputStream(), charset)) {
                while (scanner.hasNextLine()) {
                    lines.add(scanner.nextLine());
                }
            } finally {
                connection.disconnect();
            }
        } catch (Exception e) {
            AdvancedSensitiveWords.getInstance().getLogger().warning("Failed to load online words list.");
            return Collections.emptyList();
        }
        AdvancedSensitiveWords.getInstance().getLogger().info("Loaded " + lines.size() + " word(s) online.");
        return lines;
    }
}
