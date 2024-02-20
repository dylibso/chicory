package com.dylibso.chicory.wasm;

public class ParserUtil {

    private ParserUtil() {}

    public static boolean isValidIdentifier(String s) {
        if (s == null) {
            return false;
        }

        for (int i = 0; i < s.length(); i++) {
            if (!isValidIdentifierChar(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidIdentifierChar(char ch) {
        return (ch >= '\u0000' && ch <= '\ud7ff') || (ch >= '\ue000' && ch <= '\ufffc');
    }
}
