package io.wdsj.asw.method;

import com.github.houbb.sensitive.word.api.IWordDeny;
import io.wdsj.asw.setting.PluginSettings;

import java.util.List;

import static io.wdsj.asw.AdvancedSensitiveWords.settingsManager;

public class WordDeny implements IWordDeny {
    @Override
    public List<String> deny() {
        return settingsManager.getProperty(PluginSettings.BLACK_LIST);
    }
}
