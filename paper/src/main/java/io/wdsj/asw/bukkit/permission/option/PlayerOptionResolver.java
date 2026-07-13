package io.wdsj.asw.bukkit.permission.option;

import io.wdsj.asw.bukkit.permission.cache.CachingPermTool;
import io.wdsj.asw.bukkit.setting.PaperConfigurationService;
import io.wdsj.asw.bukkit.type.ProcessMethod;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class PlayerOptionResolver {
    static final String PREFIX = "advancedsensitivewords.option.";
    private static final String PRIORITY_MARKER = ".priority.";
    private static final int DEFAULT_PRIORITY = 0;

    private PlayerOptionResolver() {
    }

    public static PlayerOptionView resolve(PaperConfigurationService configuration, Player player) {
        return new PlayerOptionView(configuration, CachingPermTool.effectivePermissions(player));
    }

    static boolean resolveBoolean(String optionPath, boolean defaultValue, Collection<String> permissions) {
        Optional<ResolvedAction> action = resolveAction(optionPath, permissions, ActionType.BOOLEAN);
        if (action.isEmpty() || action.get().kind() == ActionKind.DEFAULT) {
            return defaultValue;
        }
        return action.get().kind() == ActionKind.ENABLE;
    }

    static ProcessMethod resolveMethod(String optionPath, ProcessMethod defaultValue, Collection<String> permissions) {
        Optional<ResolvedAction> action = resolveAction(optionPath, permissions, ActionType.METHOD);
        if (action.isEmpty() || action.get().kind() == ActionKind.DEFAULT) {
            return defaultValue;
        }
        return action.get().kind() == ActionKind.CANCEL ? ProcessMethod.CANCEL : ProcessMethod.REPLACE;
    }

    static int resolveInteger(String optionPath, int defaultValue, Collection<String> permissions) {
        Optional<ResolvedNumber> value = resolveNumber(optionPath, permissions);
        return value.map(number -> (int) Math.floor(number.value())).orElse(defaultValue);
    }

    static double resolveDouble(String optionPath, double defaultValue, Collection<String> permissions) {
        Optional<ResolvedNumber> value = resolveNumber(optionPath, permissions);
        return value.map(ResolvedNumber::value).orElse(defaultValue);
    }

    private static Optional<ResolvedAction> resolveAction(
            String optionPath,
            Collection<String> permissions,
            ActionType actionType
    ) {
        String optionPrefix = PREFIX + optionPath.toLowerCase(Locale.ROOT) + ".";
        ResolvedAction selected = null;
        for (String rawPermission : permissions) {
            String permission = rawPermission.toLowerCase(Locale.ROOT);
            if (!permission.startsWith(optionPrefix)) {
                continue;
            }
            ParsedSuffix parsed = parseSuffix(permission.substring(optionPrefix.length()));
            ActionKind kind = switch (parsed.action()) {
                case "default" -> ActionKind.DEFAULT;
                case "enable" -> actionType == ActionType.BOOLEAN ? ActionKind.ENABLE : null;
                case "disable" -> actionType == ActionType.BOOLEAN ? ActionKind.DISABLE : null;
                case "cancel" -> actionType == ActionType.METHOD ? ActionKind.CANCEL : null;
                case "replace" -> actionType == ActionType.METHOD ? ActionKind.REPLACE : null;
                default -> null;
            };
            if (kind == null) {
                continue;
            }
            ResolvedAction candidate = new ResolvedAction(parsed.priority(), kind);
            if (selected == null || candidate.compareTo(selected) > 0) {
                selected = candidate;
            }
        }
        return Optional.ofNullable(selected);
    }

    private static Optional<ResolvedNumber> resolveNumber(String optionPath, Collection<String> permissions) {
        String optionPrefix = PREFIX + optionPath.toLowerCase(Locale.ROOT) + ".";
        ResolvedNumber selected = null;
        ResolvedDefault selectedDefault = null;
        for (String rawPermission : permissions) {
            String permission = rawPermission.toLowerCase(Locale.ROOT);
            if (!permission.startsWith(optionPrefix)) {
                continue;
            }
            ParsedSuffix parsed = parseSuffix(permission.substring(optionPrefix.length()));
            if ("default".equals(parsed.action())) {
                ResolvedDefault candidate = new ResolvedDefault(parsed.priority());
                if (selectedDefault == null || candidate.priority() > selectedDefault.priority()) {
                    selectedDefault = candidate;
                }
                continue;
            }
            if (!parsed.action().startsWith("value.")) {
                continue;
            }
            String valueText = parsed.action().substring("value.".length());
            double value;
            try {
                value = Double.parseDouble(valueText);
            } catch (NumberFormatException exception) {
                continue;
            }
            if (!Double.isFinite(value) || value < 0.0D) {
                continue;
            }
            ResolvedNumber candidate = new ResolvedNumber(parsed.priority(), value);
            if (selected == null || candidate.compareTo(selected) > 0) {
                selected = candidate;
            }
        }
        if (selected == null) {
            return Optional.empty();
        }
        if (selectedDefault != null && selectedDefault.priority() > selected.priority()) {
            return Optional.empty();
        }
        return Optional.of(selected);
    }

    private static ParsedSuffix parseSuffix(String suffix) {
        int priorityIndex = suffix.lastIndexOf(PRIORITY_MARKER);
        if (priorityIndex < 0) {
            return new ParsedSuffix(suffix, DEFAULT_PRIORITY);
        }
        String action = suffix.substring(0, priorityIndex);
        String priorityText = suffix.substring(priorityIndex + PRIORITY_MARKER.length());
        try {
            return new ParsedSuffix(action, Integer.parseInt(priorityText));
        } catch (NumberFormatException exception) {
            return new ParsedSuffix(action, DEFAULT_PRIORITY);
        }
    }

    private enum ActionType {
        BOOLEAN,
        METHOD
    }

    private enum ActionKind {
        DEFAULT(0),
        ENABLE(1),
        DISABLE(2),
        REPLACE(1),
        CANCEL(2);

        private final int strictness;

        ActionKind(int strictness) {
            this.strictness = strictness;
        }
    }

    private record ParsedSuffix(String action, int priority) {
    }

    private record ResolvedDefault(int priority) {
    }

    private record ResolvedAction(int priority, ActionKind kind) implements Comparable<ResolvedAction> {
        @Override
        public int compareTo(ResolvedAction other) {
            int priorityCompare = Integer.compare(priority, other.priority);
            if (priorityCompare != 0) {
                return priorityCompare;
            }
            return Integer.compare(kind.strictness, other.kind.strictness);
        }
    }

    private record ResolvedNumber(int priority, double value) implements Comparable<ResolvedNumber> {
        @Override
        public int compareTo(ResolvedNumber other) {
            int priorityCompare = Integer.compare(priority, other.priority);
            if (priorityCompare != 0) {
                return priorityCompare;
            }
            return Double.compare(value, other.value);
        }
    }
}
