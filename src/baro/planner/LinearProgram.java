package baro.planner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Небольшой решатель целочисленного линейного программирования: симплекс-метод (две фазы)
 * и метод ветвей и границ. Максимизирует c·x при ограничениях и x ≥ 0, все переменные целые.
 */
final class LinearProgram {
    enum Sense { LE, GE, EQ }

    private static final double EPS = 1e-9;
    /** Сколько памяти могут занимать таблицы, ждущие своей ветки «вверх» на пути от корня; дальше узлы решаются заново. */
    private static final long WARM_BYTES = 96L << 20;

    private final int n;
    private final double[] objective;
    private final List<double[]> rows = new ArrayList<>();
    private final List<Sense> senses = new ArrayList<>();
    private final List<Double> rhs = new ArrayList<>();

    LinearProgram(int variables) { this.n = variables; this.objective = new double[variables]; }

    void objective(int var, double coef) { objective[var] += coef; }

    void constraint(double[] coefs, Sense s, double b) { rows.add(coefs); senses.add(s); rhs.add(b); }

    double[] newRow() { return new double[n]; }

    /** Значения переменных; optimal = false — поиск остановлен по времени или числу узлов. */
    record Result(double[] x, boolean optimal) {}

    // ===================== ветви и границы =====================

    private double best;
    private double[] bestX, root;
    private int nodes;
    private boolean exhausted;
    private long deadline;

    /** @return null, если решения нет; если целое не найдено вовремя — решение без целочисленности */
    Result solveInteger(int nodeLimit, long millis) {
        deadline = System.currentTimeMillis() + millis;
        best = Double.NEGATIVE_INFINITY; bestX = null; root = null; nodes = 0; exhausted = false;
        double[] lo = new double[n], hi = new double[n];
        Arrays.fill(hi, Double.POSITIVE_INFINITY);
        branch(lo, hi, nodeLimit, null, 0);
        if (bestX != null) return new Result(bestX, !exhausted);
        return root == null ? null : new Result(root, false);
    }

    /**
     * @param warm таблица родителя с добавленной границей этого узла: она дорешивается двойственным симплексом
     *             за несколько шагов вместо полного решения с нуля; null — решить заново
     * @param held сколько байт занимают таблицы предков, ждущие своей второй ветки
     */
    private void branch(double[] lo, double[] hi, int limit, Tableau warm, long held) {
        if (++nodes > limit || System.currentTimeMillis() > deadline) { exhausted = true; return; }
        Tableau tab = null;
        if (warm != null) {
            int r = warm.reoptimize();
            if (r == 0) return; // недопустимо
            if (r > 0) tab = warm;
        }
        if (tab == null) tab = solveRelaxation(lo, hi);
        if (tab == null) return;
        double[] x = tab.solution();
        if (root == null) root = x;
        if (dot(objective, x) <= best + 1e-6) return;
        int frac = -1; double worst = 0;
        for (int j = 0; j < n; j++) {
            double f = x[j] - Math.floor(x[j] + 1e-7);
            double d = Math.min(f, 1 - f);
            if (d > 1e-6 && d > worst) { worst = d; frac = j; }
        }
        if (frac < 0) {
            double[] r = new double[n];
            for (int j = 0; j < n; j++) r[j] = Math.round(x[j]);
            // после многих шагов дорешивания могла накопиться погрешность — тогда этот узел решаем заново
            if (tab == warm && !feasible(r, lo, hi)) { nodes--; branch(lo, hi, limit, null, held); return; }
            best = dot(objective, r); bestX = r;
            return;
        }
        double fl = Math.floor(x[frac]);
        // таблицу держим, пока обходится поддерево «вниз», только если хватает памяти
        Tableau parent = held + tab.bytes() <= WARM_BYTES ? tab : null;
        long downHeld = parent == null ? held : held + parent.bytes();
        tab = null; warm = null;
        // сначала ветка «вниз» — она чаще даёт допустимое решение
        double oldHi = hi[frac];
        hi[frac] = fl;
        branch(lo, hi, limit, parent == null ? null : parent.withBound(frac, fl, false), downHeld);
        hi[frac] = oldHi;
        double oldLo = lo[frac];
        lo[frac] = fl + 1;
        Tableau up = parent == null ? null : parent.withBound(frac, fl + 1, true);
        parent = null;
        branch(lo, hi, limit, up, held);
        lo[frac] = oldLo;
    }

    /** Целое решение удовлетворяет всем ограничениям и границам (с небольшим допуском). */
    private boolean feasible(double[] x, double[] lo, double[] hi) {
        for (int j = 0; j < n; j++) if (x[j] < lo[j] - 1e-6 || x[j] > hi[j] + 1e-6) return false;
        for (int r = 0; r < rows.size(); r++) {
            double s = dot(rows.get(r), x), b = rhs.get(r);
            if (senses.get(r) != Sense.GE && s > b + 1e-5) return false;
            if (senses.get(r) != Sense.LE && s < b - 1e-5) return false;
        }
        return true;
    }

    private static double dot(double[] a, double[] b) { double s = 0; for (int i = 0; i < a.length; i++) s += a[i] * b[i]; return s; }

    // ===================== симплекс =====================

    /** Решение без требования целочисленности с границами lo ≤ x ≤ hi; null — решения нет. */
    private Tableau solveRelaxation(double[] lo, double[] hi) {
        // подстановка x = lo + y, y ≥ 0; конечные верхние границы — отдельными строками
        List<double[]> a = new ArrayList<>(rows);
        List<Sense> sense = new ArrayList<>(senses);
        List<Double> b = new ArrayList<>();
        for (int r = 0; r < rows.size(); r++) {
            double v = rhs.get(r);
            for (int j = 0; j < n; j++) if (lo[j] != 0) v -= rows.get(r)[j] * lo[j];
            b.add(v);
        }
        for (int j = 0; j < n; j++) {
            if (hi[j] == Double.POSITIVE_INFINITY) continue;
            if (hi[j] < lo[j]) return null;
            double[] row = new double[n];
            row[j] = 1;
            a.add(row); sense.add(Sense.LE); b.add(hi[j] - lo[j]);
        }
        return simplex(a, sense, b, lo.clone());
    }

    private Tableau simplex(List<double[]> A, List<Sense> S, List<Double> B, double[] shift) {
        int m = A.size();
        // правые части делаем неотрицательными (строка с b < 0 умножается на −1, знак неравенства меняется)
        Sense[] sense = new Sense[m];
        int slack = 0, art = 0;
        for (int i = 0; i < m; i++) {
            Sense s = S.get(i);
            if (B.get(i) < 0) s = s == Sense.LE ? Sense.GE : s == Sense.GE ? Sense.LE : Sense.EQ;
            sense[i] = s;
            if (s != Sense.EQ) slack++;
            if (s != Sense.LE) art++;
        }
        int cols = n + slack + art, real = n + slack;
        double[][] t = new double[m + 1][cols + 1];
        int[] basis = new int[m];
        for (int i = 0, sc = n, ac = real; i < m; i++) {
            double k = B.get(i) < 0 ? -1 : 1;
            for (int j = 0; j < n; j++) t[i][j] = k * A.get(i)[j];
            t[i][cols] = k * B.get(i);
            if (sense[i] == Sense.LE) { t[i][sc] = 1; basis[i] = sc++; }
            else if (sense[i] == Sense.GE) { t[i][sc++] = -1; t[i][ac] = 1; basis[i] = ac++; }
            else { t[i][ac] = 1; basis[i] = ac++; }
        }
        double[] z = t[m];
        // фаза 1: минимизировать сумму искусственных переменных
        if (art > 0) {
            for (int i = 0; i < m; i++) if (basis[i] >= real) for (int j = 0; j <= cols; j++) z[j] -= t[i][j];
            Arrays.fill(z, real, cols, 0);
            if (!pivotLoop(t, basis, m, cols, cols)) return null;
            if (-z[cols] > 1e-7) return null; // недопустимо
            // вывести искусственные из базиса
            for (int i = 0; i < m; i++) {
                if (basis[i] < real) continue;
                for (int j = 0; j < real; j++) if (Math.abs(t[i][j]) > 1e-9) { pivot(t, m, cols, i, j, basis); break; }
            }
        }
        // фаза 2: максимизация c·x  ⇒ строка цели содержит −c
        Arrays.fill(z, 0);
        for (int j = 0; j < n; j++) z[j] = -objective[j];
        for (int i = 0; i < m; i++) {
            double c = basis[i] < n ? objective[basis[i]] : 0;
            if (c != 0) for (int j = 0; j <= cols; j++) z[j] += c * t[i][j];
        }
        if (!pivotLoop(t, basis, m, cols, real)) return null;
        if (art == 0) return new Tableau(t, basis, m, cols, shift);
        // искусственные столбцы больше не нужны; строки, где искусственная осталась в базисе, — лишние (нулевые)
        int mm = 0;
        for (int i = 0; i < m; i++) if (basis[i] < real) mm++;
        double[][] ct = new double[mm + 1][real + 1];
        int[] cb = new int[mm];
        for (int i = 0, k = 0; i <= m; i++) {
            if (i < m && basis[i] >= real) continue;
            System.arraycopy(t[i], 0, ct[k], 0, real);
            ct[k][real] = t[i][cols];
            if (i < m) cb[k] = basis[i];
            k++;
        }
        return new Tableau(ct, cb, mm, real, shift);
    }

    /**
     * Оптимальная симплекс-таблица узла: m строк ограничений и строка цели, столбец cols — правые части.
     * Переменные сдвинуты: x = shift + y.
     */
    private record Tableau(double[][] t, int[] basis, int m, int cols, double[] shift) {
        long bytes() { return 8L * (m + 1) * (cols + 1); }

        double[] solution() {
            double[] x = shift.clone();
            for (int i = 0; i < m; i++) if (basis[i] < x.length) x[basis[i]] += t[i][cols];
            return x;
        }

        /**
         * Копия таблицы с новой границей x[var] ≥ bound (up) или x[var] ≤ bound: строка базисной переменной var,
         * выраженная через небазисные, плюс новая слабая переменная. Правая часть отрицательна — дорешивает reoptimize().
         */
        Tableau withBound(int var, double bound, boolean up) {
            int r = 0;
            while (r < m && basis[r] != var) r++;
            if (r == m) return null;
            int nc = cols + 1;
            double[][] nt = new double[m + 2][nc + 1];
            for (int i = 0; i <= m; i++) {
                int to = i < m ? i : m + 1; // строка цели остаётся последней
                System.arraycopy(t[i], 0, nt[to], 0, cols);
                nt[to][nc] = t[i][cols];
            }
            double[] src = t[r], row = nt[m];
            double sign = up ? 1 : -1, b = bound - shift[var];
            for (int k = 0; k < cols; k++) row[k] = sign * src[k];
            row[var] = 0;
            row[cols] = 1;
            row[nc] = sign * (src[cols] - b);
            int[] nb = Arrays.copyOf(basis, m + 1);
            nb[m] = cols;
            return new Tableau(nt, nb, m + 1, nc, shift);
        }

        /** Двойственный симплекс, затем прямой для чистоты. 1 — решено, 0 — недопустимо, −1 — не сошлось. */
        int reoptimize() {
            for (int iter = 0; iter <= 5000; iter++) {
                int pr = -1; double minb = -EPS;
                for (int i = 0; i < m; i++) if (t[i][cols] < minb) { minb = t[i][cols]; pr = i; }
                if (pr < 0) return pivotLoop(t, basis, m, cols, cols) ? 1 : -1;
                double[] row = t[pr], z = t[m];
                int pc = -1; double ratio = Double.POSITIVE_INFINITY;
                for (int j = 0; j < cols; j++)
                    if (row[j] < -EPS) { double q = Math.max(z[j], 0) / -row[j]; if (q < ratio) { ratio = q; pc = j; } }
                if (pc < 0) return 0;
                pivot(t, m, cols, pr, pc, basis);
            }
            return -1;
        }
    }

    /** Итерации симплекса; столбцы ≥ limit не входят в базис. false — задача неограничена или зациклилась. */
    private static boolean pivotLoop(double[][] t, int[] basis, int m, int cols, int limit) {
        for (int iter = 0; iter < 20000; iter++) {
            boolean bland = iter > 2000; // против зацикливания — правило Бленда
            int pc = -1; double minv = -EPS;
            for (int j = 0; j < limit; j++) {
                if (t[m][j] < minv) { pc = j; if (bland) break; minv = t[m][j]; }
            }
            if (pc < 0) return true;
            int pr = -1; double ratio = Double.POSITIVE_INFINITY;
            for (int i = 0; i < m; i++) {
                if (t[i][pc] > EPS) {
                    double r = t[i][cols] / t[i][pc];
                    if (r < ratio - 1e-12 || (Math.abs(r - ratio) <= 1e-12 && pr >= 0 && basis[i] < basis[pr])) { ratio = r; pr = i; }
                }
            }
            if (pr < 0) return false;
            pivot(t, m, cols, pr, pc, basis);
        }
        return false;
    }

    private static void pivot(double[][] t, int m, int cols, int pr, int pc, int[] basis) {
        double[] prow = t[pr];
        double pv = prow[pc];
        int[] nz = new int[cols + 1];
        int k = 0;
        for (int j = 0; j <= cols; j++) if (prow[j] != 0) { prow[j] /= pv; nz[k++] = j; }
        // в больших задачах ведущая строка почти пустая — тогда обновляем только её ненулевые столбцы,
        // иначе сплошной цикл (его JIT векторизует)
        boolean sparse = k * 8 < cols;
        for (int i = 0; i <= m; i++) {
            double f = t[i][pc];
            if (i == pr || f == 0) continue;
            double[] row = t[i];
            if (sparse) for (int q = 0; q < k; q++) row[nz[q]] -= f * prow[nz[q]];
            else for (int j = 0; j <= cols; j++) row[j] -= f * prow[j];
        }
        basis[pr] = pc;
    }
}
