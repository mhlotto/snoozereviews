package com.mhlotto.snoozereviews.ui.backup;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class BackupFilenameFactory {
    private final Clock clock;

    public BackupFilenameFactory(Clock clock) {
        this.clock = clock;
    }

    public String defaultBackupFilename() {
        String date = LocalDate.now(clock).format(DateTimeFormatter.ISO_LOCAL_DATE);
        return "snooze-reviews-backup-" + date + ".json";
    }
}
