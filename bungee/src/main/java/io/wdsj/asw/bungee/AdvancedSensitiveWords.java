package io.wdsj.asw.bungee;

import io.wdsj.asw.bungee.config.Config;
import io.wdsj.asw.bungee.listener.PluginMessageListener;
import io.wdsj.asw.common.update.Updater;
import net.md_5.bungee.api.plugin.Plugin;
import org.bstats.bungeecord.Metrics;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.util.logging.Logger;

public final class AdvancedSensitiveWords extends Plugin {
    public static final String BUNGEE_CHANNEL = "BungeeCord";
    public static final String SUB_CHANNEL = "asw";
    public static Logger LOGGER;

    private static AdvancedSensitiveWords instance;
    private static Config config;
    public static AdvancedSensitiveWords getInstance() {
        return instance;
    }
    private File dataFolder;

    @Override
    public void onEnable() {
        LOGGER = getLogger();
        dataFolder = getDataFolder();
        reloadConfiguration();
        instance = this;
        getProxy().getPluginManager().registerListener(this, new PluginMessageListener());
        Metrics metrics = new Metrics(this, 21636);
        if (config.check_for_update) {
            getProxy().getScheduler().runAsync(this, () -> {
                LOGGER.info("Checking for update...");
                if (Updater.isUpdateAvailable()) {
                    if (Updater.isDevChannel()) {
                        LOGGER.warning("There is a new development version available: " + Updater.getLatestVersion() +
                                ", you're on: " + Updater.getCurrentVersion());
                    } else {
                        LOGGER.warning("There is a new version available: " + Updater.getLatestVersion() +
                                ", you're on: " + Updater.getCurrentVersion());
                    }
                } else {
                    if (!Updater.isErred()) {
                        LOGGER.info("You are running the latest version.");
                    } else {
                        LOGGER.info("Unable to fetch version info.");
                    }
                }
            });
        }
    }

    @Override
    public void onDisable() {
        getProxy().getPluginManager().unregisterListener(new PluginMessageListener());
    }

    public static Config config() {
        return config;
    }

    public void createDirectory(File dir) throws IOException {
        try {
            Files.createDirectories(dir.toPath());
        } catch (FileAlreadyExistsException e) { // Thrown if dir exists but is not a directory
            if (dir.delete()) createDirectory(dir);
        }
    }

    private void reloadConfiguration() {
        try {
            createDirectory(dataFolder);
            config = new Config(this, dataFolder);
            config.saveConfig();
        } catch (Throwable t) {
            LOGGER.severe("Failed while loading config!");
        }
    }
}
