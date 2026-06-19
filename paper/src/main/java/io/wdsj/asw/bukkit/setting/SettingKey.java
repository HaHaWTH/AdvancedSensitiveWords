package io.wdsj.asw.bukkit.setting;

import java.util.function.Function;

public record SettingKey<T>(Function<SettingsConfiguration, T> accessor) {
    public T get(SettingsConfiguration settings) {
        return accessor.apply(settings);
    }
}
