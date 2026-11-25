package baro.talents;


import static baro.Tr.tr;
import baro.model.Talent;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Таблица эффектов выбранных талантов: одинаковые эффекты объединяются, числа складываются.
 * Эффекты берутся из описаний талантов (тексты игры), разные формулировки одного и того же
 * («Вы получаете дополнительно 15 к навыку Медицина» и «+15 Бонусный навык медика») сводятся к одной строке.
 * Условия («При уровне здоровья ниже 50%…», «Относится ко всей команде») сохраняются — такие эффекты
 * складываются только с эффектами при том же условии.
 */
public final class EffectTotals {

    /** Строка таблицы. */
    public static final class Row {
        private final String condition, effect;
        private final boolean numeric;
        private final List<Num> totals;
        private final List<Talent> sources = new ArrayList<>();
        private boolean nonStacking;

        Row(String condition, String effect, List<Num> nums) {
            this.condition = condition; this.effect = effect;
            this.numeric = !nums.isEmpty();
            this.totals = new ArrayList<>();
            for (Num n : nums) totals.add(n.copy());
        }

        /** Условие, при котором действует эффект (пусто — всегда). */
        public String condition() { return condition; }
        /** Название эффекта; N — место, где стоит число. */
        public String effect() { return effect; }
        public boolean numeric() { return numeric; }
        /** Таланты, которые дают этот эффект. */
        public List<Talent> sources() { return sources; }
        /** У одного из талантов указано «эффект не суммируется» — берётся наибольшее значение. */
        public boolean nonStacking() { return nonStacking; }

        /** Итог: «+20%», или «N₁ = 100%, N₂ = 75%», или «есть» для эффектов без чисел. */
        public String total() {
            if (!numeric) return sources.size() > 1 ? tr("есть ×") + sources.size() : tr("есть");
            return String.join(" · ", totals.stream().map(Num::format).toList());
        }

        /** Текст эффекта с итоговыми числами на месте N. */
        public String text() {
            if (!effect.contains("N")) return effect;
            StringBuilder s = new StringBuilder();
            int k = 0;
            for (int i = 0; i < effect.length(); i++) {
                char ch = effect.charAt(i);
                if (ch == 'N' && k < totals.size()) {
                    String v = totals.get(k++).format();
                    if (i + 1 < effect.length() && effect.charAt(i + 1) == '%' && v.endsWith("%")) v = v.substring(0, v.length() - 1);
                    s.append(v);
                } else s.append(ch);
            }
            return s.toString();
        }

        /** Числовое значение первого числа (для сортировки). */
        public double value() { return numeric ? totals.get(0).value : 0; }

        void add(Talent t, List<Num> nums, boolean single) {
            if (!sources.contains(t)) sources.add(t);
            if (sources.size() == 1) return; // первое значение уже записано в конструкторе
            nonStacking |= single;
            for (int i = 0; i < totals.size() && i < nums.size(); i++) {
                Num cur = totals.get(i), add = nums.get(i);
                cur.value = nonStacking ? Math.max(cur.value, add.value) : cur.value + add.value;
                cur.signed |= add.signed;
            }
        }
    }

    private static final Pattern NUM = Pattern.compile("([-+]?)(\\d+(?:[.,]\\d+)?)(%?)");
    private static final Map<String, String> SKILL = Map.of(
            tr("оружия"), tr("Оружие"), tr("медика"), tr("Медицина"), tr("электрика"), tr("Электрика"), tr("механика"), tr("Механика"), tr("управления"), tr("Управление"));
    private static final Pattern RESIST = Pattern.compile(tr("^Вы получаете дополнительно ([-+]?\\d+(?:[.,]\\d+)?%?) устойчивости \\((.+)\\)\\.?$"));
    private static final Pattern EXTRA = Pattern.compile(tr("^Вы получаете дополнительно ([-+]?\\d+(?:[.,]\\d+)?%?) \\((.+)\\)\\.?$"));
    private static final Pattern TO_SKILL = Pattern.compile(tr("^(?:Вы получаете дополнительно|Дает) ([-+]?\\d+(?:[.,]\\d+)?) к навыку (Бонусный навык [^\\s.,]+|\"?[^\\s.,\"]+\"?|\"[^\"]+\")\\.?$"));
    private static final Pattern LEAD = Pattern.compile("^([-+]\\d+(?:[.,]\\d+)?%?) (.+?)\\.?$");
    private static final Pattern TAIL = Pattern.compile("^(.+?) ([-+]\\d+(?:[.,]\\d+)?%?)\\.?$");

    private EffectTotals() {}

    public static List<Row> of(Collection<Talent> talents) {
        Map<String, Row> rows = new LinkedHashMap<>();
        for (Talent t : talents) {
            boolean single = t.description().toLowerCase(Locale.ROOT).contains(tr("не суммируется"));
            String condition = "";
            for (String raw : t.description().split("\n")) {
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith(tr("Доступен рецепт")) || line.startsWith(tr("Доступны рецепты"))
                        || line.startsWith(tr("Если таких талантов несколько")) || line.startsWith(tr("Поддерживаемая фракция:"))) continue;
                if (line.endsWith(":")) { condition = line.substring(0, line.length() - 1); continue; }
                String cond = condition;
                int colon = line.lastIndexOf(": ");
                if (colon > 0 && NUM.matcher(line.substring(colon + 2)).lookingAt() && line.substring(colon + 2).matches("^[-+]\\d.*")) {
                    cond = (cond.isEmpty() ? "" : cond + "; ") + line.substring(0, colon);
                    line = line.substring(colon + 2);
                }
                if (line.startsWith("- ")) line = line.substring(2);
                Effect eff = normalize(line);
                List<Num> nums = numbers(eff.text);
                String c = cond;
                rows.computeIfAbsent(cond + "|" + eff.name, k -> new Row(c, eff.name, nums)).add(t, nums, single);
            }
        }
        List<Row> list = new ArrayList<>(rows.values());
        list.sort(Comparator.comparing((Row r) -> !r.numeric()).thenComparing(r -> !r.condition().isEmpty())
                .thenComparing(r -> -r.sources().size()).thenComparing(Row::effect));
        return list;
    }

    /** Название эффекта (числа заменены на N) и текст, откуда берутся числа. */
    private record Effect(String name, String text) {}

    private static Effect normalize(String line) {
        Matcher m;
        if ((m = RESIST.matcher(line)).matches()) return new Effect(tr("Устойчивость: ") + m.group(2), plus(m.group(1)));
        if ((m = EXTRA.matcher(line)).matches()) return new Effect(m.group(2), plus(m.group(1)));
        if ((m = TO_SKILL.matcher(line)).matches()) return new Effect(skill(m.group(2).replace("\"", "").trim()), plus(m.group(1)));
        if ((m = LEAD.matcher(line)).matches() && !NUM.matcher(m.group(2)).find()) return new Effect(skill(m.group(2)), m.group(1));
        if ((m = TAIL.matcher(line)).matches() && !NUM.matcher(m.group(1)).find()) return new Effect(skill(m.group(1).replaceAll(":$", "")), m.group(2));
        // Общий случай: формулировка из игры, числа заменены на N.
        return new Effect(NUM.matcher(line).replaceAll("$1N$3").replace("+N", "N").replace("-N", "N"), line);
    }

    private static String plus(String v) { return v.startsWith("-") || v.startsWith("+") ? v : "+" + v; }

    private static final Set<String> SKILL_NAMES = Set.of(tr("Оружие"), tr("Медицина"), tr("Электрика"), tr("Механика"), tr("Управление"));

    private static String skill(String s) {
        String w = s.startsWith(tr("Бонусный навык ")) ? s.substring(tr("Бонусный навык ").length()).trim() : s;
        String k = SKILL.getOrDefault(w.toLowerCase(Locale.ROOT), SKILL_NAMES.contains(w) ? w : null);
        return k != null ? tr("Навык «") + k + "»" : s;
    }

    private static List<Num> numbers(String text) {
        List<Num> l = new ArrayList<>();
        Matcher m = NUM.matcher(text);
        while (m.find()) {
            double v = Double.parseDouble(m.group(2).replace(',', '.'));
            if ("-".equals(m.group(1))) v = -v;
            l.add(new Num(v, !m.group(1).isEmpty(), !m.group(3).isEmpty()));
        }
        return l;
    }

    static final class Num {
        double value;
        boolean signed;
        final boolean percent;

        Num(double value, boolean signed, boolean percent) { this.value = value; this.signed = signed; this.percent = percent; }

        Num copy() { return new Num(value, signed, percent); }

        String format() {
            String n = Math.abs(value - Math.rint(value)) < 1e-9 ? String.valueOf((long) Math.rint(value))
                    : String.format(Locale.ROOT, "%.1f", value).replace('.', baro.Tr.english() ? '.' : ',');
            if (signed && value > 0) n = "+" + n;
            return n + (percent ? "%" : "");
        }
    }
}
