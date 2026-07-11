package com.mhlotto.snoozereviews.data.backup;

public class SleepBackupValidationException extends Exception {
    public SleepBackupValidationException(String message) {
        super(message);
    }

    public SleepBackupValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
