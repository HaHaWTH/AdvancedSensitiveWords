package io.wdsj.asw.bukkit.api;

/**
 * Public API entry point for other plugins.
 */
@SuppressWarnings("unused")
public final class AdvancedSensitiveWordsApi {
    private static final ShadowBanApi SHADOW_BAN_API = new ShadowBanApi();
    private static final CoreApi CORE_API = new CoreApi();

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

    /**
     * Get the core API.
     *
     * @return core API
     */
    public static CoreApi core() {
        return CORE_API;
    }
}
