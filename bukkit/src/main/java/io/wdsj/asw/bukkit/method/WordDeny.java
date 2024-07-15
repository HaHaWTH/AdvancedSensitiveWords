package io.wdsj.asw.bukkit.method;

import com.github.houbb.sensitive.word.api.IWordDeny;
import io.wdsj.asw.bukkit.setting.PluginSettings;

import java.util.List;

import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.settingsManager;

public class WordDeny implements IWordDeny {
    @Override
    public List<String> deny() {
        return settingsManager.getProperty(PluginSettings.BLACK_LIST);
    }
}
