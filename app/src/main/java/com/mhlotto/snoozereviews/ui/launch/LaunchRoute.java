package com.mhlotto.snoozereviews.ui.launch;

public class LaunchRoute {
    private final LaunchDestination destination;
    private final String nightDate;
    private final long sleepLogId;

    private LaunchRoute(LaunchDestination destination, String nightDate, long sleepLogId) {
        this.destination = destination;
        this.nightDate = nightDate;
        this.sleepLogId = sleepLogId;
    }

    public static LaunchRoute createLastNightLog(String nightDate) {
        return new LaunchRoute(LaunchDestination.CREATE_LAST_NIGHT_LOG, nightDate, 0L);
    }

    public static LaunchRoute showLastNightLog(long sleepLogId, String nightDate) {
        return new LaunchRoute(LaunchDestination.SHOW_LAST_NIGHT_LOG, nightDate, sleepLogId);
    }

    public LaunchDestination getDestination() {
        return destination;
    }

    public String getNightDate() {
        return nightDate;
    }

    public long getSleepLogId() {
        return sleepLogId;
    }
}
