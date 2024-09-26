package io.wdsj.asw.bukkit.update;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.wdsj.asw.common.template.PluginVersionTemplate;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.LOGGER;

public class Updater {
    private static String currentVersion;
    private static String latestVersion;
    private static boolean isUpdateAvailable = false;
    private static final String UPDATE_URL = "https://api.github.com/repos/HaHaWTH/AdvancedSensitiveWords/releases/latest";
    private static final String currentVersionChannel = PluginVersionTemplate.VERSION_CHANNEL;

    public Updater(String current) {
        currentVersion = current;
    }

    /**
     * Check if there is an update available
     * Note: This method will perform a network request!
     * @return true if there is an update available, false otherwise
     */
    @SuppressWarnings("all")
    public boolean isUpdateAvailable() {
        boolean isDevChannel = currentVersionChannel.equalsIgnoreCase("dev");
        if (isDevChannel) {
            LOGGER.info("You are running an development version of AdvancedSensitiveWords!");
        }
        URI uri = URI.create(UPDATE_URL);
        try {
            URL url = uri.toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            try (InputStreamReader reader = new InputStreamReader(conn.getInputStream())) {
                JsonObject jsonObject = new JsonParser().parse(reader).getAsJsonObject();
                String latest = jsonObject.get("tag_name").getAsString();
                latestVersion = latest;
                isUpdateAvailable = !currentVersion.equals(latest);
                reader.close();
                if (isDevChannel) {
                    LOGGER.info("Current running: " + currentVersion + "-" + currentVersionChannel + ", latest release: " + latest);
                    return false;
                }
                return isUpdateAvailable;
            }
        } catch (Exception e) {
            latestVersion = null;
            isUpdateAvailable = false;
            return false;
        }
    }

    public static String getLatestVersion() {
        return latestVersion;
    }

    public static String getCurrentVersion() {
        return currentVersion;
    }

    /**
     * Returns true if there is an update available, false otherwise
     * Must be called after {@link Updater#isUpdateAvailable()}
     * @return A boolean indicating whether there is an update available
     */
    public static boolean hasUpdate() {
        return isUpdateAvailable;
    }
}