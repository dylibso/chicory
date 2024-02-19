package com.dylibso.chicory.wasm;

public class ParserUtil {

    private ParserUtil() {}

    public static boolean isValidIdentifier(String s) {
        if (s == null) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            if (!isValidIdentifierChar(s.codePointAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidIdentifierChar(int ch) {
        return Character.isUnicodeIdentifierPart(ch)
                || ch == '-'
                || ch == '>'
                || ch == ' '
                || ch == '.'
                || ch == '⌣';
    }
}
