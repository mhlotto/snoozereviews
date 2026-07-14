package com.mhlotto.snoozereviews.domain.score;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SleepScoreResult {
    public enum Status {
        SCORED,
        INSUFFICIENT_DATA
    }

    private final String algorithmId;
    private final Status status;
    private final Integer score;
    private final List<SleepScoreComponent> components;
    private final List<String> limitationCodes;

    private SleepScoreResult(
            String algorithmId,
            Status status,
            Integer score,
            List<SleepScoreComponent> components,
            List<String> limitationCodes
    ) {
        if (algorithmId == null || algorithmId.trim().isEmpty()) {
            throw new IllegalArgumentException("algorithmId is required");
        }
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        if (status == Status.SCORED && score == null) {
            throw new IllegalArgumentException("scored results require a score");
        }
        if (status == Status.INSUFFICIENT_DATA && score != null) {
            throw new IllegalArgumentException("insufficient-data results must not include a score");
        }
        if (score != null && (score < 0 || score > 100)) {
            throw new IllegalArgumentException("score must be between 0 and 100");
        }
        this.algorithmId = algorithmId;
        this.status = status;
        this.score = score;
        this.components = Collections.unmodifiableList(new ArrayList<>(
                components == null ? Collections.emptyList() : components
        ));
        this.limitationCodes = Collections.unmodifiableList(new ArrayList<>(
                limitationCodes == null ? Collections.emptyList() : limitationCodes
        ));
    }

    public static SleepScoreResult scored(
            String algorithmId,
            int score,
            List<SleepScoreComponent> components,
            List<String> limitationCodes
    ) {
        return new SleepScoreResult(algorithmId, Status.SCORED, score, components, limitationCodes);
    }

    public static SleepScoreResult insufficientData(
            String algorithmId,
            List<SleepScoreComponent> components,
            List<String> limitationCodes
    ) {
        return new SleepScoreResult(algorithmId, Status.INSUFFICIENT_DATA, null, components, limitationCodes);
    }

    public String getAlgorithmId() {
        return algorithmId;
    }

    public Status getStatus() {
        return status;
    }

    public Integer getScore() {
        return score;
    }

    public List<SleepScoreComponent> getComponents() {
        return components;
    }

    public List<String> getLimitationCodes() {
        return limitationCodes;
    }
}
