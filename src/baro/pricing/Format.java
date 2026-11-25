package baro.pricing;


import static baro.Tr.tr;
import java.util.Locale;

/** Форматирование чисел для интерфейса. */
public final class Format {
    private Format() {}

    /** Десятичный разделитель: запятая по-русски, точка по-английски. */
    private static char dec() { return baro.Tr.english() ? '.' : ','; }

    public static String money(long v) {
        String s = String.format(Locale.ROOT, "%,d", v);
        return (baro.Tr.english() ? s : s.replace(',', ' ')) + " mk"; // по-русски — неразрывный пробел
    }

    public static String seconds(double t) {
        double r = Math.round(t * 100) / 100.0; // суммы дробных секунд без хвоста 0,30000000000000004
        return (r == Math.rint(r) ? String.valueOf((long) r) : String.valueOf(r).replace('.', dec())) + tr(" с");
    }

    /** «+400», «−22,5» */
    public static String signed(double v) {
        double a = Math.abs(v), r = Math.round(a * 10) / 10.0;
        if (r == 0) return "0";
        String s = r == Math.rint(r) ? String.valueOf((long) r) : String.valueOf(r).replace('.', dec());
        return (v > 0 ? "+" : "−") + s;
    }

    public static String percent(double fraction) { return Math.round(fraction * 100) + "%"; }

    /** Шанс: «22%», «8,3%», «0,5%». */
    public static String chance(double fraction) {
        double p = fraction * 100;
        if (p >= 10 || p == Math.rint(p)) return Math.round(p) + "%";
        double r = Math.round(p * 10) / 10.0;
        if (r == Math.rint(r)) return (long) r + "%"; // 9,97% → «10%», а не «10,0%»
        return String.valueOf(r).replace('.', dec()) + "%";
    }

    /** Число с одним знаком после запятой без лишнего нуля. */
    public static String num(double v) {
        double r = Math.round(v * 10) / 10.0;
        return r == Math.rint(r) ? String.valueOf((long) r) : String.valueOf(r).replace('.', dec());
    }

    /** Примерное время баффа: сила спадает на 1 в секунду. */
    public static String buffTime(double strength) {
        return strength >= 90 ? "≈ " + Math.round(strength / 60) + tr(" мин") : "≈ " + Math.round(strength) + tr(" с");
    }
}
