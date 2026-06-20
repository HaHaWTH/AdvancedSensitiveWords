package io.wdsj.asw.bukkit.method;

import java.util.*;

public class WildCardLineResolver {
    public List<String> resolveWildCardLine(String line) {
        String[] parts = line.split("\\|");
        if (parts.length == 1) {
            return Collections.singletonList(line);
        }

        List<List<String>> options = new ArrayList<>();
        for (String part : parts) {
            String[] alternatives = part.split("\\*");
            options.add(Arrays.asList(alternatives));
        }

        List<String> result = new ArrayList<>();
        result.add("");

        for (List<String> optionList : options) {
            List<String> temp = new ArrayList<>();
            for (String prefix : result) {
                for (String option : optionList) {
                    temp.add(prefix + option);
                }
            }
            result = temp;
        }

        return result;
    }
}