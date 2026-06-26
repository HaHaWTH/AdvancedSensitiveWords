package io.wdsj.asw.bukkit.ai;

public enum LlmApiMode {
    /**
     * Sends requests to the OpenAI-compatible {@code /chat/completions} endpoint.
     */
    CHAT_COMPLETIONS,
    /**
     * Sends requests to the OpenAI-compatible {@code /responses} endpoint.
     */
    RESPONSES,
    /**
     * Sends requests to the Anthropic-compatible {@code /messages} endpoint.
     */
    ANTHROPIC_MESSAGES
}
