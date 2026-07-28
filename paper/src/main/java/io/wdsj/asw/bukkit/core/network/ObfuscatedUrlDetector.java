package io.wdsj.asw.bukkit.core.network;

import com.github.houbb.sensitive.word.api.IWordResult;
import com.github.houbb.sensitive.word.bs.SensitiveWordBs;
import com.github.houbb.sensitive.word.support.check.WordChecks;
import com.github.houbb.sensitive.word.support.result.WordResultHandlers;
import com.github.houbb.sensitive.word.support.resultcondition.WordResultConditions;
import com.github.houbb.sensitive.word.support.tag.WordTags;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ObfuscatedUrlDetector implements AutoCloseable {
    private static final int MINIMUM_NORMALIZED_LENGTH = 5;
    private static final List<String> DOT_PHRASES = List.of(
            "小數點", "小数点",
            "句號", "句号",
            "句點", "句点",
            "圓點", "圆点",
            "中點", "中点",
            "頓號", "顿号",
            "點兒", "点儿",
            "點", "点"
    );
    private static final Int2ObjectMap<String> SYMBOL_REPLACEMENTS = symbolReplacements();

    private final SensitiveWordBs urlMatcher;

    public ObfuscatedUrlDetector(boolean allowUrlWithoutPrefix) {
        this.urlMatcher = SensitiveWordBs.newInstance()
                .ignoreCase(true)
                .ignoreWidth(true)
                .ignoreNumStyle(true)
                .ignoreChineseStyle(false)
                .ignoreEnglishStyle(true)
                .ignoreRepeat(false)
                .enableNumCheck(false)
                .enableEmailCheck(false)
                .enableUrlCheck(true)
                .enableWordCheck(false)
                .enableIpv4Check(false)
                .wordResultCondition(WordResultConditions.alwaysTrue())
                .wordCheckUrl(allowUrlWithoutPrefix ? WordChecks.urlNoPrefix() : WordChecks.url())
                .wordTag(WordTags.none())
                .wordFailFast(false)
                .init();
    }

    public List<IWordResult> findAll(String source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }

        NormalizedText normalized = normalize(source);
        if (normalized.text().length() < MINIMUM_NORMALIZED_LENGTH) {
            return List.of();
        }

        List<IWordResult> normalizedResults = urlMatcher.findAll(normalized.text(), WordResultHandlers.raw());
        if (normalizedResults.isEmpty()) {
            return List.of();
        }

        Map<SourceRange, IWordResult> mappedResults = new LinkedHashMap<>();
        for (IWordResult result : normalizedResults) {
            int normalizedStart = result.startIndex();
            int normalizedEnd = result.endIndex();
            if (normalizedStart < 0 || normalizedEnd <= normalizedStart || normalizedEnd > normalized.text().length()) {
                continue;
            }

            int sourceStart = normalized.sourceStarts()[normalizedStart];
            int sourceEnd = normalized.sourceEnds()[normalizedEnd - 1];
            if (sourceStart < 0 || sourceEnd <= sourceStart || sourceEnd > source.length()) {
                continue;
            }

            SourceRange range = new SourceRange(sourceStart, sourceEnd);
            mappedResults.putIfAbsent(
                    range,
                    new MappedWordResult(sourceStart, sourceEnd, result.type(), source.substring(sourceStart, sourceEnd))
            );
        }

        return mappedResults.values().stream()
                .sorted(Comparator.comparingInt(IWordResult::startIndex)
                        .thenComparing(Comparator.comparingInt(IWordResult::endIndex).reversed()))
                .toList();
    }

    @Override
    public void close() {
        urlMatcher.destroy();
    }

    static NormalizedText normalize(String source) {
        MappedTextBuilder normalized = new MappedTextBuilder(source.length());
        for (int sourceIndex = 0; sourceIndex < source.length(); ) {
            String dotPhrase = dotPhraseAt(source, sourceIndex);
            if (dotPhrase != null) {
                int sourceEnd = sourceIndex + dotPhrase.length();
                normalized.append(".", sourceIndex, sourceEnd);
                sourceIndex = sourceEnd;
                continue;
            }

            int codePoint = source.codePointAt(sourceIndex);
            int sourceEnd = sourceIndex + Character.charCount(codePoint);
            while (sourceEnd < source.length()) {
                int trailing = source.codePointAt(sourceEnd);
                int type = Character.getType(trailing);
                if (trailing != 0xFE0E && trailing != 0xFE0F && trailing != 0x20E3
                        && type != Character.NON_SPACING_MARK
                        && type != Character.COMBINING_SPACING_MARK
                        && type != Character.ENCLOSING_MARK) {
                    break;
                }
                sourceEnd += Character.charCount(trailing);
            }

            String replacement = mapSymbol(codePoint);
            if (replacement == null) {
                String cluster = source.substring(sourceIndex, sourceEnd);
                replacement = normalizeCluster(cluster);
            }
            normalized.append(replacement, sourceIndex, sourceEnd);
            sourceIndex = sourceEnd;
        }
        return normalized.removeObfuscatingColons();
    }

    private static String normalizeCluster(String cluster) {
        String nfkc = Normalizer.normalize(cluster, Normalizer.Form.NFKC);
        StringBuilder result = new StringBuilder(nfkc.length());
        for (int index = 0; index < nfkc.length(); ) {
            int codePoint = nfkc.codePointAt(index);
            String replacement = mapSymbol(codePoint);
            if (replacement != null) {
                result.append(replacement);
            } else if (isDotAlias(codePoint)) {
                result.append('.');
            } else if (codePoint != 0xFE0E && codePoint != 0xFE0F && codePoint != 0x20E3) {
                result.appendCodePoint(codePoint);
            }
            index += Character.charCount(codePoint);
        }
        return result.toString();
    }

    private static String mapSymbol(int codePoint) {
        if (codePoint >= 0x1F1E6 && codePoint <= 0x1F1FF) {
            return Character.toString('a' + codePoint - 0x1F1E6);
        }
        if (codePoint >= 0x1F130 && codePoint <= 0x1F149) {
            return Character.toString('a' + codePoint - 0x1F130);
        }
        if (codePoint >= 0x1F7E0 && codePoint <= 0x1F7EB) {
            return ".";
        }
        return SYMBOL_REPLACEMENTS.get(codePoint);
    }

    private static String dotPhraseAt(String source, int sourceIndex) {
        for (String phrase : DOT_PHRASES) {
            if (source.startsWith(phrase, sourceIndex)) {
                return phrase;
            }
        }
        return null;
    }

    private static boolean isDotAlias(int codePoint) {
        return switch (codePoint) {
            case '.',
                    0x3002, 0xFF61, 0xFF0E, 0xFE52, 0xFE12, 0x30FB, 0xFF65,
                    0x4E36, 0x3001, 0xFF64, 0xFE51,
                    0x2024, 0x2025, 0x2026, 0x0701, 0x0702, 0xA60E, 0x10A50,
                    0xA4F8, 0xA4FA, 0xA4FB, 0x1D16D,
                    0x06D4, 0x1362, 0x166E, 0x1803, 0x1809,
                    0x00B7, 0x0387, 0x16EB, 0x2E31, 0x10101, 0x2022, 0x2027,
                    0x2219, 0x22C5, 0x25CF, 0x25E6, 0x2981, 0x2E30, 0xFE45, 0xFE46,
                    0x00B0, 0x02DA, 0x2218, 0x25CB, 0x25C9, 0x25CC, 0x25D8,
                    0x25D9, 0x26AC -> true;
            default -> false;
        };
    }

    private static boolean isUrlCharacter(char character) {
        return character >= 'a' && character <= 'z'
                || character >= 'A' && character <= 'Z'
                || character >= '0' && character <= '9'
                || ".-/:?&=#".indexOf(character) >= 0;
    }

    private static Int2ObjectMap<String> symbolReplacements() {
        Int2ObjectMap<String> replacements = new Int2ObjectLinkedOpenHashMap<>();
        replacements.put(0x1F170, "a");
        replacements.put(0x1F171, "b");
        replacements.put(0x1F17E, "o");
        replacements.put(0x1F17F, "p");
        replacements.put(0x1F18E, "ab");
        replacements.put(0x1F191, "cl");
        replacements.put(0x1F192, "cool");
        replacements.put(0x1F193, "free");
        replacements.put(0x1F194, "id");
        replacements.put(0x1F195, "new");
        replacements.put(0x1F196, "ng");
        replacements.put(0x1F197, "ok");
        replacements.put(0x1F198, "sos");
        replacements.put(0x1F199, "up");
        replacements.put(0x1F19A, "vs");
        replacements.put(0x1F51F, "10");
        replacements.put(0x2795, "+");
        replacements.put(0x2796, "-");
        replacements.put(0x2797, "/");
        replacements.put(0x2716, "x");
        replacements.put(0x274C, "x");
        replacements.put(0x274E, "x");
        replacements.put(0x2B55, "o");
        replacements.put(0x1F534, ".");
        replacements.put(0x1F535, ".");
        replacements.put(0x26AA, ".");
        replacements.put(0x26AB, ".");
        return replacements;
    }

    record NormalizedText(String text, int[] sourceStarts, int[] sourceEnds) {
    }

    private record SourceRange(int start, int end) {
    }

    private record MappedWordResult(int startIndex, int endIndex, String type, String word)
            implements IWordResult {
    }

    private static final class MappedTextBuilder {
        private final StringBuilder text;
        private final List<Integer> sourceStarts;
        private final List<Integer> sourceEnds;

        private MappedTextBuilder(int expectedLength) {
            this.text = new StringBuilder(expectedLength);
            this.sourceStarts = new ArrayList<>(expectedLength);
            this.sourceEnds = new ArrayList<>(expectedLength);
        }

        private void append(String value, int sourceStart, int sourceEnd) {
            for (int index = 0; index < value.length(); index++) {
                char character = value.charAt(index);
                if (!isUrlCharacter(character)) {
                    continue;
                }
                if (character == '.' && !text.isEmpty() && text.charAt(text.length() - 1) == '.') {
                    sourceEnds.set(sourceEnds.size() - 1, sourceEnd);
                    continue;
                }
                text.append(character);
                sourceStarts.add(sourceStart);
                sourceEnds.add(sourceEnd);
            }
        }

        private NormalizedText removeObfuscatingColons() {
            int lastColon = text.lastIndexOf(":");
            if (lastColon < 0) {
                return build();
            }

            MappedTextBuilder cleaned = new MappedTextBuilder(text.length());
            for (int index = 0; index < text.length(); index++) {
                char character = text.charAt(index);
                if (character == ':' && !isSchemeColon(index) && !isPortColon(index)) {
                    continue;
                }
                cleaned.append(
                        Character.toString(character),
                        sourceStarts.get(index),
                        sourceEnds.get(index)
                );
            }
            return cleaned.build();
        }

        private boolean isSchemeColon(int index) {
            return index + 2 < text.length()
                    && text.charAt(index + 1) == '/'
                    && text.charAt(index + 2) == '/';
        }

        private boolean isPortColon(int index) {
            int cursor = index + 1;
            int digits = 0;
            while (cursor < text.length() && Character.isDigit(text.charAt(cursor)) && digits < 6) {
                cursor++;
                digits++;
            }
            return digits >= 1 && digits <= 5
                    && (cursor == text.length() || text.charAt(cursor) == '/' || text.charAt(cursor) == '?'
                    || text.charAt(cursor) == '#');
        }

        private NormalizedText build() {
            int[] starts = new int[sourceStarts.size()];
            int[] ends = new int[sourceEnds.size()];
            for (int index = 0; index < sourceStarts.size(); index++) {
                starts[index] = sourceStarts.get(index);
                ends[index] = sourceEnds.get(index);
            }
            return new NormalizedText(text.toString(), starts, ends);
        }
    }
}
