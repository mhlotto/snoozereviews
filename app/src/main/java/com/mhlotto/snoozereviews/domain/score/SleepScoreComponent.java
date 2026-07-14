package com.mhlotto.snoozereviews.domain.score;

import java.util.Objects;

public final class SleepScoreComponent {
    private final String componentKey;
    private final boolean included;
    private final Double normalizedValue;
    private final Double weight;
    private final Double contribution;
    private final String reasonCode;

    public SleepScoreComponent(
            String componentKey,
            boolean included,
            Double normalizedValue,
            Double weight,
            Double contribution,
            String reasonCode
    ) {
        if (componentKey == null || componentKey.trim().isEmpty()) {
            throw new IllegalArgumentException("componentKey is required");
        }
        this.componentKey = componentKey;
        this.included = included;
        this.normalizedValue = normalizedValue;
        this.weight = weight;
        this.contribution = contribution;
        this.reasonCode = reasonCode;
    }

    public static SleepScoreComponent included(
            String componentKey,
            Double normalizedValue,
            Double weight,
            Double contribution
    ) {
        return new SleepScoreComponent(componentKey, true, normalizedValue, weight, contribution, null);
    }

    public static SleepScoreComponent omitted(String componentKey, String reasonCode) {
        return new SleepScoreComponent(componentKey, false, null, null, null, reasonCode);
    }

    public String getComponentKey() {
        return componentKey;
    }

    public boolean isIncluded() {
        return included;
    }

    public Double getNormalizedValue() {
        return normalizedValue;
    }

    public Double getWeight() {
        return weight;
    }

    public Double getContribution() {
        return contribution;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SleepScoreComponent)) {
            return false;
        }
        SleepScoreComponent that = (SleepScoreComponent) o;
        return included == that.included
                && Objects.equals(componentKey, that.componentKey)
                && Objects.equals(normalizedValue, that.normalizedValue)
                && Objects.equals(weight, that.weight)
                && Objects.equals(contribution, that.contribution)
                && Objects.equals(reasonCode, that.reasonCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(componentKey, included, normalizedValue, weight, contribution, reasonCode);
    }
}
