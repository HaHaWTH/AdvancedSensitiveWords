package io.wdsj.asw.method;

import com.github.houbb.sensitive.word.api.IWordAllow;
import io.wdsj.asw.setting.PluginSettings;

import java.util.List;

import static io.wdsj.asw.AdvancedSensitiveWords.settingsManager;

public class WordAllow implements IWordAllow {
    @Override
    public List<String> allow() {
        return settingsManager.getProperty(PluginSettings.WHITE_LIST);
    }
}
