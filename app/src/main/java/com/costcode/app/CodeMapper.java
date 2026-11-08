package com.costcode.app;

/**
 * Pure logic for the supplier cost code: letters <-> digits.
 * Each digit 0-9 can own several letters. No Android classes here so it can be unit tested anywhere.
 */
public class CodeMapper {

    /** Starting table. Digits 5-9 are left empty for the user to fill in. */
    public static final String[] DEFAULTS = {"ix", "r", "e", "s", "u", "", "", "", "", ""};

    public static class Result {
        public final String text;
        /** Human readable note about anything that could not be converted, or empty. */
        public final String problems;

        Result(String text, String problems) {
            this.text = text;
            this.problems = problems;
        }
    }

    private final String[] letters = new String[10];

    public CodeMapper(String[] digitLetters) {
        for (int d = 0; d < 10; d++) {
            String s = (digitLetters != null && d < digitLetters.length) ? digitLetters[d] : "";
            letters[d] = clean(s);
        }
    }

    /** Keeps only a-z, lowercases, removes duplicates, keeps order. */
    public static String clean(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : s.toLowerCase().toCharArray()) {
            if (c >= 'a' && c <= 'z' && sb.indexOf(String.valueOf(c)) < 0) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public String lettersFor(int digit) {
        return letters[digit];
    }

    /** Returns a message if one letter is assigned to two digits, otherwise null. */
    public String findConflict() {
        for (int a = 0; a < 10; a++) {
            for (int i = 0; i < letters[a].length(); i++) {
                char c = letters[a].charAt(i);
                for (int b = a + 1; b < 10; b++) {
                    if (letters[b].indexOf(c) >= 0) {
                        return "Letter " + Character.toUpperCase(c) + " is used for both "
                                + a + " and " + b + ". Each letter can only mean one number.";
                    }
                }
            }
        }
        return null;
    }

    private int digitFor(char c) {
        for (int d = 0; d < 10; d++) {
            if (letters[d].indexOf(c) >= 0) return d;
        }
        return -1;
    }

    /** Letters -> number. Digits and '.' typed in are kept as they are; other characters are skipped. */
    public Result decode(String input) {
        StringBuilder out = new StringBuilder();
        StringBuilder unknown = new StringBuilder();
        if (input != null) {
            for (char raw : input.toCharArray()) {
                char c = Character.toLowerCase(raw);
                if (c >= 'a' && c <= 'z') {
                    int d = digitFor(c);
                    if (d >= 0) {
                        out.append(d);
                    } else {
                        out.append('?');
                        String up = String.valueOf(Character.toUpperCase(c));
                        if (unknown.indexOf(up) < 0) {
                            if (unknown.length() > 0) unknown.append(", ");
                            unknown.append(up);
                        }
                    }
                } else if ((c >= '0' && c <= '9') || c == '.') {
                    out.append(c);
                }
            }
        }
        String problems = unknown.length() == 0 ? "" : "Not in your table: " + unknown;
        return new Result(out.toString(), problems);
    }

    /** Number -> letters, using the first letter listed for each digit. */
    public Result encode(String number) {
        StringBuilder out = new StringBuilder();
        StringBuilder missing = new StringBuilder();
        if (number != null) {
            for (char c : number.toCharArray()) {
                if (c >= '0' && c <= '9') {
                    int d = c - '0';
                    if (letters[d].isEmpty()) {
                        out.append('?');
                        if (missing.indexOf(String.valueOf(d)) < 0) {
                            if (missing.length() > 0) missing.append(", ");
                            missing.append(d);
                        }
                    } else {
                        out.append(Character.toUpperCase(letters[d].charAt(0)));
                    }
                } else if (c == '.') {
                    out.append('.');
                }
            }
        }
        String problems = missing.length() == 0 ? "" : "No letter set for digit: " + missing;
        return new Result(out.toString(), problems);
    }
}
