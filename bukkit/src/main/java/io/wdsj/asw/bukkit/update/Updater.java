package io.wdsj.asw.bukkit.update;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.wdsj.asw.common.template.PluginVersionTemplate;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.PLUGIN_VERSION;

public class Updater {
    private static String currentVersion = PLUGIN_VERSION;
    private static String latestVersion;
    private static boolean isUpdateAvailable = false;
    private static final String RELEASE_URL = "https://api.github.com/repos/HaHaWTH/AdvancedSensitiveWords/releases/latest";
    private static final String COMMITS_URL = "https://api.github.com/repos/HaHaWTH/AdvancedSensitiveWords/commits/" + PluginVersionTemplate.COMMIT_BRANCH;
    @SuppressWarnings("all")
    private static final boolean isDevChannel = PluginVersionTemplate.VERSION_CHANNEL.equalsIgnoreCase("dev");

    public Updater() {
    }

    /**
     * Check if there is an update available
     * Note: This method will perform a network request!
     * @return true if there is an update available, false otherwise
     */
    public boolean isUpdateAvailable() {
        if (isDevChannel) {
            return isDevUpdateAvailable();
        }
        URI uri = URI.create(RELEASE_URL);
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
                return isUpdateAvailable;
            }
        } catch (Exception e) {
            latestVersion = null;
            isUpdateAvailable = false;
            return false;
        }
    }

    protected boolean isDevUpdateAvailable() {
        URI uri = URI.create(COMMITS_URL);
        try {
            URL url = uri.toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(5000);
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStreamReader reader = new InputStreamReader(connection.getInputStream());
                JsonObject jsonObject = new JsonParser().parse(reader).getAsJsonObject();
                String latestHash = jsonObject.get("sha").getAsString();
                latestVersion = latestHash.substring(0, 7);
                currentVersion = PluginVersionTemplate.COMMIT_HASH_SHORT;
                isUpdateAvailable = !PluginVersionTemplate.COMMIT_HASH.equals(latestHash);
                return isUpdateAvailable;
            }
        } catch (Exception ignored) {
        }
        return false;
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

    public static boolean isDevChannel() {
        return isDevChannel;
    }
}