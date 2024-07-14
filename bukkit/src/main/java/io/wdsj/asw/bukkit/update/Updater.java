package io.wdsj.asw.bukkit.update;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Scanner;

public class Updater {
    private static String currentVersion;
    private static String latestVersion;
    private static boolean isUpdateAvailable = false;
    private static final String UPDATE_URL = "https://api.github.com/repos/HaHaWTH/AdvancedSensitiveWords/releases/latest";

    public Updater(String current) {
        currentVersion = current;
    }


    /**
     * Check if there is an update available
     * Note: This method will perform a network request!
     * @return true if there is an update available, false otherwise
     */
    public boolean isUpdateAvailable() {
        URI uri = URI.create(UPDATE_URL);
        try {
            URL url = uri.toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            Scanner scanner = new Scanner(conn.getInputStream());
            String response = scanner.useDelimiter("\\Z").next();
            scanner.close();
            String latest = response.substring(response.indexOf("tag_name") + 11);
            latest = latest.substring(0, latest.indexOf("\""));
            latestVersion = latest;
            isUpdateAvailable = !currentVersion.equals(latest);
            return isUpdateAvailable;
        } catch (IOException ignored) {
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
