package io.wdsj.asw.bukkit.method;

import com.github.houbb.sensitive.word.api.IWordContext;
import com.github.houbb.sensitive.word.api.IWordReplace;
import com.github.houbb.sensitive.word.api.IWordResult;
import com.github.houbb.sensitive.word.utils.InnerWordCharUtils;
import io.wdsj.asw.bukkit.setting.PluginSettings;

import static io.wdsj.asw.bukkit.AdvancedSensitiveWords.setting;

public class WordReplace implements IWordReplace {
    @Override
    public void replace(StringBuilder stringBuilder, String rawString, IWordResult wordResult, IWordContext wordContext) {
        String sensitiveWord = InnerWordCharUtils.getString(rawString, wordResult);
        for (String word : setting(PluginSettings.DEFINED_REPLACEMENT)) {
            String[] parts = word.split("\\|");
            if (parts.length == 2) {
                int l = parts[1].length();
                String definedSensitiveWord = parts[0];
                String definedReplacement = parts[1].substring(0, l);
                if (definedSensitiveWord.equals(sensitiveWord)) {
                    stringBuilder.append(definedReplacement);
                    return;
                }
            }
        }
        int wordLength = wordResult.endIndex() - wordResult.startIndex();

        for (int i = 0; i < wordLength; ++i) {
            stringBuilder.append(setting(PluginSettings.REPLACEMENT));
        }

    }
}
