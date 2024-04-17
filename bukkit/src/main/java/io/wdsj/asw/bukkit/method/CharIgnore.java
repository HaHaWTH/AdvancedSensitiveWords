package io.wdsj.asw.bukkit.method;

import com.github.houbb.heaven.util.lang.StringUtil;
import com.github.houbb.sensitive.word.api.ISensitiveWordCharIgnore;
import com.github.houbb.sensitive.word.api.context.InnerSensitiveWordContext;
import io.wdsj.asw.bukkit.setting.PluginSettings;

import java.util.Set;

import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.settingsManager;

public class CharIgnore implements ISensitiveWordCharIgnore {
    @Override
    public boolean ignore(int i, char[] chars, InnerSensitiveWordContext innerSensitiveWordContext) {
        Set<Character> SET = StringUtil.toCharSet(settingsManager.getProperty(PluginSettings.IGNORE_CHAR));
        char c = chars[i];
        return SET.contains(c);
    }
}
