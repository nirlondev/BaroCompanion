package baro.model;

import java.util.Collections;
import java.util.List;

/** Разбор предмета в разборщике. */
public final class Deconstruction {
    private final double seconds;
    private final boolean randomSingle;
    private final boolean geneticStationOnly;
    private final List<DeconOutput> outputs;
    private final int picks;

    public Deconstruction(double seconds, boolean randomSingle, boolean geneticStationOnly, List<DeconOutput> outputs) {
        this(seconds, randomSingle, geneticStationOnly, outputs, 1);
    }

    /** @param picks сколько раз выбирается случайный предмет (для разбора «выпадает случайное») */
    public Deconstruction(double seconds, boolean randomSingle, boolean geneticStationOnly, List<DeconOutput> outputs, int picks) {
        this.picks = picks;
        this.seconds = seconds;
        this.randomSingle = randomSingle;
        this.geneticStationOnly = geneticStationOnly;
        this.outputs = Collections.unmodifiableList(outputs);
    }

    public double seconds() { return seconds; }
    public boolean isRandomSingle() { return randomSingle; }
    public boolean isGeneticStationOnly() { return geneticStationOnly; }
    public List<DeconOutput> outputs() { return outputs; }
    /** Сколько случайных предметов выпадает за один разбор. */
    public int picks() { return picks; }

    /** Ожидаемое количество предмета с одного разбора (с учётом шансов). */
    public double expectedCount(DeconOutput o) {
        return o.probability().map(p -> p * picks * o.count()).orElse((double) o.count());
    }

    public DeconOutput outputOf(Item item) {
        for (DeconOutput o : outputs) if (o.item() == item) return o;
        return null;
    }
}
