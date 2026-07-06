package io.wdsj.asw.bukkit.method;

import com.github.houbb.heaven.util.lang.StringUtil;
import com.github.houbb.sensitive.word.api.ISensitiveWordCharIgnore;
import com.github.houbb.sensitive.word.api.context.InnerSensitiveWordContext;

import java.util.HashSet;
import java.util.Set;

public final class CharIgnore implements ISensitiveWordCharIgnore {
    public static final String NETWORK_SYNTAX_CHARS = ".．。｡:/?#[]@!$&'()*+,;=%-_~";

    private final Set<Character> ignoredCharacters;

    public CharIgnore(String ignoredCharacters) {
        this(ignoredCharacters, "");
    }

    public CharIgnore(String ignoredCharacters, String protectedCharacters) {
        Set<Character> ignored = new HashSet<>(StringUtil.toCharSet(ignoredCharacters == null ? "" : ignoredCharacters));
        ignored.removeAll(StringUtil.toCharSet(protectedCharacters == null ? "" : protectedCharacters));
        this.ignoredCharacters = Set.copyOf(ignored);
    }

    @Override
    public boolean ignore(int i, String string, InnerSensitiveWordContext innerSensitiveWordContext) {
        char c = string.charAt(i);
        return ignoredCharacters.contains(c);
    }
}
