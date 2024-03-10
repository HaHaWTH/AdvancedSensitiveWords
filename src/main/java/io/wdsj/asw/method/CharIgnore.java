package io.wdsj.asw.method;

import com.github.houbb.heaven.util.lang.StringUtil;
import com.github.houbb.sensitive.word.api.ISensitiveWordCharIgnore;
import com.github.houbb.sensitive.word.api.context.InnerSensitiveWordContext;
import io.wdsj.asw.setting.PluginSettings;
import io.wdsj.asw.util.Utils;

import java.util.Set;

import static io.wdsj.asw.AdvancedSensitiveWords.settingsManager;

public class CharIgnore implements ISensitiveWordCharIgnore {
    @Override
    public boolean ignore(int i, char[] chars, InnerSensitiveWordContext innerSensitiveWordContext) {
        if (settingsManager.getProperty(PluginSettings.IGNORE_FORMAT_CODE)) {
            String regex = Utils.getIgnoreFormatCodeRegex();
            if (i + 1 < chars.length) {
                String nextTwoChars = String.valueOf(chars[i]) +
                        chars[i + 1];
                if (nextTwoChars.matches(regex)) {
                    return true;
                }
            }
            if (i - 1 >= 0) {
                String nextTwoChars = String.valueOf(chars[i - 1]) +
                        chars[i];
                if (nextTwoChars.matches(regex)) {
                    return true;
                }
            }
        }
        Set<Character> SET = StringUtil.toCharSet(settingsManager.getProperty(PluginSettings.IGNORE_CHAR));
        char c = chars[i];
        return SET.contains(c);
    }
}
