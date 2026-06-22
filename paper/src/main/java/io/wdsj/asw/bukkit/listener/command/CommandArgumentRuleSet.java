package io.wdsj.asw.bukkit.listener.command;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class CommandArgumentRuleSet {
    private static final Pattern DIRECTIVE_PATTERN = Pattern.compile("\\[(default|include|ignore):([^]]+)]", Pattern.CASE_INSENSITIVE);

    private final List<Rule> rules;

    private CommandArgumentRuleSet(List<Rule> rules) {
        this.rules = List.copyOf(rules);
    }

    public static CommandArgumentRuleSet compile(List<String> rawRules) {
        if (rawRules == null) {
            throw new IllegalArgumentException("chat.command-white-list cannot be null");
        }

        List<Rule> parsedRules = new ObjectArrayList<>(rawRules.size());
        Set<String> paths = new ObjectLinkedOpenHashSet<>();
        for (String rawRule : rawRules) {
            Rule rule = parseRule(rawRule);
            if (!paths.add(rule.normalizedPath())) {
                throw new IllegalArgumentException("Duplicate command argument rule path: " + rule.path());
            }
            parsedRules.add(rule);
        }
        parsedRules.sort(Comparator.comparingInt((Rule rule) -> rule.pathTokens().size()).reversed());
        return new CommandArgumentRuleSet(parsedRules);
    }

    public CommandSelection select(String command) {
        List<Token> tokens = tokenize(command);
        if (tokens.isEmpty() || !tokens.getFirst().value().startsWith("/")) {
            return CommandSelection.empty(command);
        }

        Rule matchedRule = rules.stream()
                .filter(rule -> rule.matches(tokens))
                .findFirst()
                .orElse(null);
        int commandPathLength = matchedRule == null ? 1 : matchedRule.pathTokens().size();
        List<Token> arguments = commandPathLength >= tokens.size()
                ? List.of()
                : List.copyOf(tokens.subList(commandPathLength, tokens.size()));

        boolean[] selected = new boolean[arguments.size()];
        if (matchedRule == null || matchedRule.defaultInclude()) {
            Arrays.fill(selected, true);
        }
        if (matchedRule != null) {
            matchedRule.applyDirectives(selected);
        }

        List<SelectedSegment> segments = selectedSegments(command, arguments, selected);
        return new CommandSelection(command, matchedRule != null, segments);
    }

    private static Rule parseRule(String rawRule) {
        if (rawRule == null || rawRule.isBlank()) {
            throw new IllegalArgumentException("Command argument rule cannot be blank");
        }
        String trimmedRule = rawRule.trim();
        Matcher matcher = DIRECTIVE_PATTERN.matcher(trimmedRule);
        StringBuilder commandPathBuilder = new StringBuilder(trimmedRule);
        List<Directive> directives = new ObjectArrayList<>();
        Boolean defaultInclude = null;
        while (matcher.find()) {
            for (int index = matcher.start(); index < matcher.end(); index++) {
                commandPathBuilder.setCharAt(index, ' ');
            }
            String directiveName = matcher.group(1).toLowerCase(Locale.ROOT);
            String value = matcher.group(2).trim();
            switch (directiveName) {
                case "default" -> {
                    if (defaultInclude != null) {
                        throw new IllegalArgumentException("Command argument rule declares default more than once: " + rawRule);
                    }
                    if ("include".equalsIgnoreCase(value)) {
                        defaultInclude = true;
                    } else if ("ignore".equalsIgnoreCase(value)) {
                        defaultInclude = false;
                    } else {
                        throw new IllegalArgumentException("Rule default must be include or ignore: " + rawRule);
                    }
                }
                case "include", "ignore" -> directives.add(new Directive(
                        "include".equals(directiveName),
                        parseSelectors(value, rawRule)
                ));
                default -> throw new IllegalArgumentException("Unknown command argument directive: " + rawRule);
            }
        }

        String commandPath = commandPathBuilder.toString().trim();
        if (commandPath.contains("[") || commandPath.contains("]")) {
            throw new IllegalArgumentException("Malformed command argument directive: " + rawRule);
        }
        List<Token> pathTokens = tokenize(commandPath);
        if (pathTokens.isEmpty() || !pathTokens.getFirst().value().startsWith("/")) {
            throw new IllegalArgumentException("Command argument rule must start with '/': " + rawRule);
        }
        if (pathTokens.stream().anyMatch(token -> token.value().isBlank())) {
            throw new IllegalArgumentException("Command argument path contains a blank token: " + rawRule);
        }
        List<String> normalizedPathTokens = pathTokens.stream()
                .map(token -> token.value().toLowerCase(Locale.ROOT))
                .toList();
        String normalizedPath = String.join(" ", normalizedPathTokens);
        return new Rule(commandPath, normalizedPathTokens, normalizedPath, defaultInclude == null || defaultInclude, directives);
    }

    private static List<IndexSelector> parseSelectors(String rawSelectors, String rawRule) {
        if (rawSelectors.isBlank()) {
            throw new IllegalArgumentException("Argument selector cannot be blank: " + rawRule);
        }
        List<IndexSelector> selectors = new ObjectArrayList<>();
        for (String rawSelector : rawSelectors.split(",", -1)) {
            String selector = rawSelector.trim();
            if (selector.isEmpty()) {
                throw new IllegalArgumentException("Argument selector cannot be blank: " + rawRule);
            }
            int rangeDelimiter = selector.indexOf("..");
            if (rangeDelimiter < 0) {
                selectors.add(new IndexSelector(parseIndex(selector, rawRule), parseIndex(selector, rawRule)));
                continue;
            }
            if (rangeDelimiter != selector.lastIndexOf("..")) {
                throw new IllegalArgumentException("Argument selector has multiple ranges: " + rawRule);
            }
            String start = selector.substring(0, rangeDelimiter).trim();
            String end = selector.substring(rangeDelimiter + 2).trim();
            int startIndex = start.isEmpty() ? 1 : parseIndex(start, rawRule);
            int endIndex = end.isEmpty() ? Integer.MAX_VALUE : parseIndex(end, rawRule);
            if (sameDirection(startIndex, endIndex) && startIndex > endIndex) {
                throw new IllegalArgumentException("Argument selector range is descending: " + rawRule);
            }
            selectors.add(new IndexSelector(startIndex, endIndex));
        }
        return List.copyOf(selectors);
    }

    private static boolean sameDirection(int first, int second) {
        return (first > 0 && second > 0) || (first < 0 && second < 0);
    }

    private static int parseIndex(String value, String rawRule) {
        try {
            int index = Integer.parseInt(value);
            if (index == 0) {
                throw new IllegalArgumentException("Argument selector cannot use zero: " + rawRule);
            }
            return index;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid command argument selector: " + rawRule, exception);
        }
    }

    private static List<SelectedSegment> selectedSegments(String command, List<Token> arguments, boolean[] selected) {
        List<SelectedSegment> segments = new ObjectArrayList<>();
        int runStartIndex = -1;

        for (int index = 0; index < selected.length; index++) {
            if (!selected[index]) {
                if (runStartIndex >= 0) {
                    addSelectedSegment(command, arguments, segments, runStartIndex, index - 1);
                    runStartIndex = -1;
                }
                continue;
            }

            if (runStartIndex < 0) {
                runStartIndex = index;
            }
        }

        if (runStartIndex >= 0) {
            addSelectedSegment(command, arguments, segments, runStartIndex, selected.length - 1);
        }

        return List.copyOf(segments);
    }

    private static void addSelectedSegment(
            String command,
            List<Token> arguments,
            List<SelectedSegment> segments,
            int runStartIndex,
            int runEndIndex
    ) {
        Token first = arguments.get(runStartIndex);
        Token last = arguments.get(runEndIndex);

        boolean singleTokenRun = runStartIndex == runEndIndex;
        int segmentStart = singleTokenRun ? first.contentStart() : first.start();
        int segmentEnd = singleTokenRun ? first.contentEnd() : last.end();

        segments.add(new SelectedSegment(
                segmentStart,
                segmentEnd,
                command.substring(segmentStart, segmentEnd)
        ));
    }

    private static List<Token> tokenize(String input) {
        Objects.requireNonNull(input, "input");
        List<Token> tokens = new ObjectArrayList<>();
        int index = 0;
        while (index < input.length()) {
            while (index < input.length() && Character.isWhitespace(input.charAt(index))) {
                index++;
            }
            if (index >= input.length()) {
                break;
            }
            int tokenStart = index;
            char quote = input.charAt(index);
            if (quote == '\'' || quote == '"') {
                int contentStart = ++index;
                boolean escaped = false;
                while (index < input.length()) {
                    char current = input.charAt(index);
                    if (!escaped && current == quote) {
                        break;
                    }
                    escaped = !escaped && current == '\\';
                    if (current != '\\') {
                        escaped = false;
                    }
                    index++;
                }
                int contentEnd = index;
                if (index < input.length()) {
                    index++;
                }
                tokens.add(new Token(tokenStart, index, contentStart, contentEnd, input.substring(contentStart, contentEnd)));
                continue;
            }

            int contentStart = index;
            while (index < input.length() && !Character.isWhitespace(input.charAt(index))) {
                index++;
            }
            tokens.add(new Token(contentStart, index, contentStart, index, input.substring(contentStart, index)));
        }
        return List.copyOf(tokens);
    }

    public record CommandSelection(String command, boolean listed, List<SelectedSegment> segments) {
        private static CommandSelection empty(String command) {
            return new CommandSelection(command, false, List.of());
        }

        public String scannedContent() {
            return segments.stream().map(SelectedSegment::content).collect(Collectors.joining("\n"));
        }

        public String replaceSelected(UnaryOperator<String> replacement) {
            StringBuilder result = new StringBuilder(command);
            for (int index = segments.size() - 1; index >= 0; index--) {
                SelectedSegment segment = segments.get(index);
                result.replace(segment.startInclusive(), segment.endExclusive(), replacement.apply(segment.content()));
            }
            return result.toString();
        }
    }

    public record SelectedSegment(int startInclusive, int endExclusive, String content) {
    }

    private record Rule(
            String path,
            List<String> pathTokens,
            String normalizedPath,
            boolean defaultInclude,
            List<Directive> directives
    ) {
        private boolean matches(List<Token> tokens) {
            if (tokens.size() < pathTokens.size()) {
                return false;
            }
            for (int index = 0; index < pathTokens.size(); index++) {
                if (!pathTokens.get(index).equals(tokens.get(index).value().toLowerCase(Locale.ROOT))) {
                    return false;
                }
            }
            return true;
        }

        private void applyDirectives(boolean[] selected) {
            for (Directive directive : directives) {
                for (IndexSelector selector : directive.selectors()) {
                    selector.apply(selected, directive.include());
                }
            }
        }
    }

    private record Directive(boolean include, List<IndexSelector> selectors) {
    }

    private record IndexSelector(int start, int end) {
        private void apply(boolean[] selected, boolean include) {
            int resolvedStart = resolve(start, selected.length);
            int resolvedEnd = resolve(end, selected.length);
            if (resolvedStart > resolvedEnd) {
                return;
            }
            for (int index = Math.max(1, resolvedStart); index <= Math.min(selected.length, resolvedEnd); index++) {
                selected[index - 1] = include;
            }
        }

        private static int resolve(int index, int size) {
            return index > 0 ? index : size + index + 1;
        }
    }

    private record Token(int start, int end, int contentStart, int contentEnd, String value) {
    }
}
