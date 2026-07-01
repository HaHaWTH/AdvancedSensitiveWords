package io.wdsj.asw.bukkit.api;

import io.wdsj.asw.bukkit.AdvancedSensitiveWords;

import java.util.List;

@SuppressWarnings("unused")
public final class CoreApi {
    CoreApi() {
    }

    /**
     * Returns whether the plugin is initialized.
     *
     * @return true if the plugin is initialized, false otherwise
     */
    public boolean isInitialized() {
        return AdvancedSensitiveWords.isInitialized;
    }

    /**
     * Finds all sensitive words in the given text.
     * <p>
     * <b>Warning:</b> This method should be called after checking initialization state.
     *
     * @param text the text to search for sensitive words
     * @return a list of sensitive words found in the text
     * @see #isInitialized()
     */
    public List<String> findSensitiveWords(String text) {
        return AdvancedSensitiveWords.sensitiveWordBs.findAll(text);
    }

    /**
     * Replaces all sensitive words in the given text.
     * <p>
     * <b>Warning:</b> This method should be called after checking initialization state.
     *
     * @param text the text to replace sensitive words in
     * @return the text with sensitive words replaced
     * @see #isInitialized()
     */
    public String replaceSensitiveWords(String text) {
        return AdvancedSensitiveWords.sensitiveWordBs.replace(text);
    }
}
