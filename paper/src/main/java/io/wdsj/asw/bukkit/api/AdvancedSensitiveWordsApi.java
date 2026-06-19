package io.wdsj.asw.bukkit.api;

/**
 * Public API entry point for other plugins.
 */
@SuppressWarnings("unused")
public final class AdvancedSensitiveWordsApi {
    private static final ShadowBanApi SHADOW_BAN_API = new ShadowBanApi();

    private AdvancedSensitiveWordsApi() {
    }

    /**
     * Get the shadowban API.
     *
     * @return shadowban API
     */
    public static ShadowBanApi shadowBan() {
        return SHADOW_BAN_API;
    }
}
