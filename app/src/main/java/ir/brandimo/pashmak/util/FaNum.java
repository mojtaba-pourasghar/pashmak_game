package ir.brandimo.pashmak.util;

/** Persian-Indic digit shaping. Mirrors the prototype's FA() helper. */
public final class FaNum {

    private static final char[] FA_DIGITS = {'۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹'};
    public static final char PERCENT = '٪';

    private FaNum() {
    }

    public static String of(long value) {
        return of(Long.toString(value));
    }

    public static String of(String source) {
        if (source == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(source.length());
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c >= '0' && c <= '9') {
                out.append(FA_DIGITS[c - '0']);
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    /** Persian digits back to ASCII, so parsed input stays locale-independent. */
    public static String toAscii(String source) {
        if (source == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(source.length());
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            int fa = indexOfFaDigit(c);
            if (fa >= 0) {
                out.append((char) ('0' + fa));
            } else if (c >= '٠' && c <= '٩') {
                out.append((char) ('0' + (c - '٠')));
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    public static String percent(int value) {
        return of(value) + PERCENT;
    }

    private static int indexOfFaDigit(char c) {
        for (int i = 0; i < FA_DIGITS.length; i++) {
            if (FA_DIGITS[i] == c) {
                return i;
            }
        }
        return -1;
    }
}
