package com.mhlotto.snoozereviews.domain.score;

import com.mhlotto.snoozereviews.domain.sleep.SleepObservation;

public interface SleepScoreCalculator {
    SleepScoreResult calculate(SleepObservation observation);
}
