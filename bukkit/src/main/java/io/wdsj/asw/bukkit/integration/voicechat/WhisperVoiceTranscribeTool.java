package io.wdsj.asw.bukkit.integration.voicechat;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.github.givimad.whisperjni.WhisperContext;
import io.github.givimad.whisperjni.WhisperFullParams;
import io.github.givimad.whisperjni.WhisperJNI;
import io.wdsj.asw.bukkit.AdvancedSensitiveWords;
import io.wdsj.asw.bukkit.setting.PluginSettings;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.*;

import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.LOGGER;
import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.settingsManager;

public class WhisperVoiceTranscribeTool {
    private final WhisperJNI whisper;
    private final WhisperContext whisperCtx;
    private final ThreadPoolExecutor threadPool;

    public WhisperVoiceTranscribeTool() {
        try {
            WhisperJNI.loadLibrary();
            if (settingsManager.getProperty(PluginSettings.VOICE_DEBUG)) {
                WhisperJNI.LibraryLogger logger = log -> LOGGER.info("[WhisperJNI] " + log);
                WhisperJNI.setLibraryLogger(logger);
                LOGGER.info("WhisperJNI debug logger enabled");
            } else {
                WhisperJNI.setLibraryLogger(null);
            }
            whisper = new WhisperJNI();
            File dataFolder = Paths.get(AdvancedSensitiveWords.getInstance().getDataFolder().getPath(), "whisper", "model").toFile();
            if (Files.notExists(dataFolder.toPath())) {
                Files.createDirectories(dataFolder.toPath());
            }
            int coreCount = Runtime.getRuntime().availableProcessors();
            int maxThread = settingsManager.getProperty(PluginSettings.VOICE_REALTIME_TRANSCRIBING_MAX_THREAD);
            if (maxThread <= -1) {
                maxThread = coreCount;
            } else if (maxThread == 0) {
                maxThread = coreCount * 2;
            }
            LOGGER.info("Using " + maxThread + " thread(s) for voice transcription");
            whisperCtx = whisper.init(Paths.get(dataFolder.getPath(), settingsManager.getProperty(PluginSettings.VOICE_MODEL_NAME)));
            RejectedExecutionHandler handler = (r, executor) -> LOGGER.info("Rejected execution of transcription task, thread pool is full");
            threadPool = new ThreadPoolExecutor(Math.min(coreCount, maxThread),
                    maxThread,
                    settingsManager.getProperty(PluginSettings.VOICE_REALTIME_TRANSCRIBING_THREAD_KEEP_ALIVE),
                    TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(100),
                    new ThreadFactoryBuilder()
                            .setNameFormat("ASW Whisper Transcribe Thread-%d")
                            .setDaemon(true)
                            .build(),
                    handler);
            threadPool.allowCoreThreadTimeOut(true);
            threadPool.prestartAllCoreThreads();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public CompletableFuture<String> transcribe(float[] data) {
        return CompletableFuture.supplyAsync(() -> {
            WhisperFullParams params = new WhisperFullParams();
            int result = whisper.full(whisperCtx, params, data, data.length);
            if (result != 0) {
                throw new RuntimeException("Transcription failed with code " + result);
            }
            whisper.fullNSegments(whisperCtx);
            return whisper.fullGetSegmentText(whisperCtx,0);
        }, threadPool);
    }

    public void shutdown() {
        threadPool.shutdownNow();
        whisperCtx.close();
    }
}
