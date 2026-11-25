package baro.model;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** Медикамент: предмет + эффекты при успешном и неудачном применении. */
public final class Medicine {
    private final Item item;
    private final MedCategory category;
    private final Integer useSkill;
    private final List<Effect> onSuccess;
    private final List<Effect> onFailure;
    private final String bestReason;

    public Medicine(Item item, MedCategory category, Integer useSkill, List<Effect> onSuccess, List<Effect> onFailure, String bestReason) {
        this.item = item; this.category = category; this.useSkill = useSkill;
        this.onSuccess = Collections.unmodifiableList(onSuccess);
        this.onFailure = Collections.unmodifiableList(onFailure);
        this.bestReason = bestReason;
    }

    public Item item() { return item; }
    public MedCategory category() { return category; }
    /** Навык «Медицина», ниже которого возможен провал. */
    public Optional<Integer> useSkill() { return Optional.ofNullable(useSkill); }
    public List<Effect> onSuccess() { return onSuccess; }
    public List<Effect> onFailure() { return onFailure; }
    /** Причина, если препарат в списке лучших. */
    public Optional<String> bestReason() { return Optional.ofNullable(bestReason); }
    public boolean isBest() { return bestReason != null; }
}
