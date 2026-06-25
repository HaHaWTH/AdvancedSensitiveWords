package io.wdsj.asw.common.update;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.wdsj.asw.common.environment.PluginBuildInfo;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Checks GitHub releases for stable builds and compares commits for development builds.
 */
public final class Updater {
    private static final String GITHUB_USERNAME = "HaHaWTH";
    private static final String GITHUB_REPOSITORY = "AdvancedSensitiveWords";
    private static final String API_ROOT = "https://api.github.com/repos/"
            + GITHUB_USERNAME + "/" + GITHUB_REPOSITORY;
    private static final String RELEASE_URL = API_ROOT + "/releases/latest";
    private static final String COMMITS_URL = API_ROOT + "/commits/";
    private static final String COMPARE_URL = API_ROOT + "/compare/";
    private static final boolean DEV_CHANNEL = "dev".equalsIgnoreCase(PluginBuildInfo.VERSION_CHANNEL);

    private Updater() {
    }

    public static final class UpdateResult {
        private final boolean updateAvailable;
        private final String latestVersion;
        private final boolean error;
        private final int commitsBehind;
        private final String latestReleaseVersion;
        private final boolean releaseUpdateAvailable;

        public UpdateResult(
                boolean updateAvailable,
                String latestVersion,
                boolean error,
                int commitsBehind,
                String latestReleaseVersion,
                boolean releaseUpdateAvailable
        ) {
            this.updateAvailable = updateAvailable;
            this.latestVersion = latestVersion;
            this.error = error;
            this.commitsBehind = commitsBehind;
            this.latestReleaseVersion = latestReleaseVersion;
            this.releaseUpdateAvailable = releaseUpdateAvailable;
        }

        public static UpdateResult noUpdate() {
            return new UpdateResult(false, null, false, 0, null, false);
        }

        public static UpdateResult error() {
            return new UpdateResult(false, null, true, 0, null, false);
        }

        public boolean isUpdateAvailable() {
            return updateAvailable;
        }

        public String getLatestVersion() {
            return latestVersion;
        }

        public boolean isError() {
            return error;
        }

        /**
         * Returns the number of commits the current development build is behind its source branch.
         */
        public int getCommitsBehind() {
            return commitsBehind;
        }

        public String getLatestReleaseVersion() {
            return latestReleaseVersion;
        }

        public boolean isReleaseUpdateAvailable() {
            return releaseUpdateAvailable;
        }
    }

    /**
     * Performs a synchronous GitHub API request. Call this from an asynchronous task.
     */
    public static UpdateResult checkNow() {
        try {
            return DEV_CHANNEL ? checkDevelopmentUpdate() : checkReleaseUpdate();
        } catch (Exception ignored) {
            return UpdateResult.error();
        }
    }

    public static boolean isDevChannel() {
        return DEV_CHANNEL;
    }

    private static UpdateResult checkReleaseUpdate() throws IOException {
        JsonObject release = requestJson(RELEASE_URL);
        String latestVersion = release.get("tag_name").getAsString();
        int comparison = compareVersions(parseSemanticVersion(latestVersion), parseSemanticVersion(PluginBuildInfo.VERSION));
        return new UpdateResult(comparison > 0, latestVersion, false, 0, latestVersion, comparison > 0);
    }

    private static UpdateResult checkDevelopmentUpdate() throws IOException {
        UpdateResult releaseResult = checkReleaseUpdate();
        String localCommit = PluginBuildInfo.COMMIT_HASH;
        String branch = PluginBuildInfo.COMMIT_BRANCH;
        final UpdateResult errorResult = new UpdateResult(
                releaseResult.isReleaseUpdateAvailable(),
                branch,
                true,
                0,
                releaseResult.getLatestReleaseVersion(),
                releaseResult.isReleaseUpdateAvailable()
        );
        if (isUnknown(localCommit) || isUnknown(branch)) {
            return errorResult;
        }

        try {
            JsonObject latestCommit = requestJson(COMMITS_URL + encodeReference(branch));
            String compareReference = encodeReference(localCommit) + "..." + encodeReference(branch);
            JsonObject comparison = requestJson(COMPARE_URL + compareReference);
            int commitsBehind = comparison.get("ahead_by").getAsInt();
            return new UpdateResult(
                    releaseResult.isReleaseUpdateAvailable() || commitsBehind > 0,
                    shortCommit(latestCommit.get("sha").getAsString()),
                    false,
                    commitsBehind,
                    releaseResult.getLatestReleaseVersion(),
                    releaseResult.isReleaseUpdateAvailable()
            );
        } catch (Exception ignored) {
            return errorResult;
        }
    }

    private static JsonObject requestJson(String endpoint) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(endpoint).toURL().openConnection();
        try {
            connection.setRequestProperty("User-Agent", GITHUB_REPOSITORY + "-Updater");
            connection.setRequestProperty("Accept", "application/vnd.github+json");
            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(7_000);
            connection.setRequestMethod("GET");

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException("GitHub API returned HTTP " + connection.getResponseCode());
            }

            try (InputStreamReader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } finally {
            connection.disconnect();
        }
    }

    private static String shortCommit(String sha) {
        return sha.substring(0, Math.min(7, sha.length()));
    }

    private static String encodeReference(String reference) {
        return URLEncoder.encode(reference, StandardCharsets.UTF_8);
    }

    private static boolean isUnknown(String value) {
        return value == null || value.isBlank() || "unknown".equalsIgnoreCase(value);
    }

    private static int compareVersions(int[] remote, int[] local) {
        int length = Math.max(remote.length, local.length);
        for (int index = 0; index < length; index++) {
            int remotePart = index < remote.length ? remote[index] : 0;
            int localPart = index < local.length ? local[index] : 0;
            if (remotePart != localPart) {
                return Integer.compare(remotePart, localPart);
            }
        }
        return 0;
    }

    private static int[] parseSemanticVersion(String version) {
        String normalized = version.startsWith("v") || version.startsWith("V") ? version.substring(1) : version;
        int suffixIndex = normalized.indexOf('-');
        if (suffixIndex >= 0) {
            normalized = normalized.substring(0, suffixIndex);
        }

        String[] parts = normalized.split("\\.");
        int[] parsed = new int[parts.length];
        for (int index = 0; index < parts.length; index++) {
            try {
                parsed[index] = Integer.parseInt(parts[index]);
            } catch (NumberFormatException ignored) {
                parsed[index] = 0;
            }
        }
        return parsed;
    }
}
