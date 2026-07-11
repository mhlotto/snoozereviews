package com.mhlotto.snoozereviews.ui.backup;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

public class BackupFilenameFactoryTest {
    @Test
    public void defaultBackupFilenameUsesLocalCalendarDate() {
        BackupFilenameFactory factory = new BackupFilenameFactory(
                Clock.fixed(Instant.parse("2026-07-11T03:00:00Z"), ZoneId.of("America/New_York"))
        );

        assertEquals("snooze-reviews-backup-2026-07-10.json", factory.defaultBackupFilename());
    }

    @Test
    public void sameInstantCanProduceDifferentFilenameDatesByZone() {
        Instant instant = Instant.parse("2026-07-11T23:30:00Z");

        assertEquals(
                "snooze-reviews-backup-2026-07-11.json",
                new BackupFilenameFactory(Clock.fixed(instant, ZoneId.of("America/New_York"))).defaultBackupFilename()
        );
        assertEquals(
                "snooze-reviews-backup-2026-07-12.json",
                new BackupFilenameFactory(Clock.fixed(instant, ZoneId.of("Asia/Tokyo"))).defaultBackupFilename()
        );
    }
}
