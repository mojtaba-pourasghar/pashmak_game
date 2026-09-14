package ir.brandimo.pashmak.util;

/** Spells small integers in Persian words, for the parent gate challenge. */
public final class PersianWords {

    private static final String[] ONES = {
            "صفر", "یک", "دو", "سه", "چهار", "پنج", "شش", "هفت", "هشت", "نه"
    };
    private static final String[] TEENS = {
            "ده", "یازده", "دوازده", "سیزده", "چهارده", "پانزده",
            "شانزده", "هفده", "هجده", "نوزده"
    };
    private static final String[] TENS = {
            "", "", "بیست", "سی", "چهل", "پنجاه", "شصت", "هفتاد", "هشتاد", "نود"
    };

    private PersianWords() {
    }

    public static String of(int value) {
        if (value < 0) {
            return "منفی " + of(-value);
        }
        if (value < 10) {
            return ONES[value];
        }
        if (value < 20) {
            return TEENS[value - 10];
        }
        if (value < 100) {
            int tens = value / 10;
            int ones = value % 10;
            return ones == 0 ? TENS[tens] : TENS[tens] + " و " + ONES[ones];
        }
        if (value < 1000) {
            int hundreds = value / 100;
            int rest = value % 100;
            String head = hundredWord(hundreds);
            return rest == 0 ? head : head + " و " + of(rest);
        }
        return FaNum.of(value);
    }

    private static String hundredWord(int hundreds) {
        switch (hundreds) {
            case 1: return "صد";
            case 2: return "دویست";
            case 3: return "سیصد";
            case 4: return "چهارصد";
            case 5: return "پانصد";
            case 6: return "ششصد";
            case 7: return "هفتصد";
            case 8: return "هشتصد";
            default: return "نهصد";
        }
    }
}
