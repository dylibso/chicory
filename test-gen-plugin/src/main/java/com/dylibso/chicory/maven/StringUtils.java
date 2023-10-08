package com.dylibso.chicory.maven;

import com.github.javaparser.utils.StringEscapeUtils;

public class StringUtils {

    public static String capitalize(String in) {
        return in.substring(0, 1).toUpperCase() + in.substring(1);
    }

    public static String escapedCamelCase(String in) {
        var escaped = StringEscapeUtils.escapeJava(in);
        var sb = new StringBuffer();
        var capitalize = false;
        for (var i = 0; i < escaped.length(); i++) {
            var character = escaped.charAt(i);

            if (Character.isDigit(character)) {
                sb.append(character);
            } else if (Character.isAlphabetic(character)) {
                if (capitalize) {
                    sb.append(Character.toUpperCase(character));
                    capitalize = false;
                } else {
                    sb.append(character);
                }
            } else {
                capitalize = true;
            }
        }

        return sb.toString();
    }

}
