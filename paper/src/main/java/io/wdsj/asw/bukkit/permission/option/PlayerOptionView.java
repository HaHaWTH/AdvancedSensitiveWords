package io.wdsj.asw.bukkit.permission.option;

import io.wdsj.asw.bukkit.setting.PaperConfigurationService;
import io.wdsj.asw.bukkit.setting.SettingKey;
import io.wdsj.asw.bukkit.type.ProcessMethod;

import java.util.List;
import java.util.Objects;

public final class PlayerOptionView {
    private final PaperConfigurationService configuration;
    private final List<String> permissions;

    PlayerOptionView(PaperConfigurationService configuration, List<String> permissions) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.permissions = List.copyOf(permissions);
    }

    public boolean bool(String optionPath, SettingKey<Boolean> defaultKey) {
        return PlayerOptionResolver.resolveBoolean(optionPath, configuration.get(defaultKey), permissions);
    }

    public ProcessMethod method(String optionPath, SettingKey<ProcessMethod> defaultKey) {
        return PlayerOptionResolver.resolveMethod(optionPath, configuration.get(defaultKey), permissions);
    }

    public int integer(String optionPath, SettingKey<Integer> defaultKey) {
        return PlayerOptionResolver.resolveInteger(optionPath, configuration.get(defaultKey), permissions);
    }

    public double decimal(String optionPath, SettingKey<Double> defaultKey) {
        return PlayerOptionResolver.resolveDouble(optionPath, configuration.get(defaultKey), permissions);
    }
}
