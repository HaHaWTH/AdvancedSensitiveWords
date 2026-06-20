package io.wdsj.asw.bukkit.method;

import com.github.houbb.sensitive.word.api.IWordAllow;
import io.wdsj.asw.bukkit.setting.PluginSettings;

import java.util.List;

import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.setting;

public class WordAllow implements IWordAllow {
    @Override
    public List<String> allow() {
        return setting(PluginSettings.WHITE_LIST);
    }
}
