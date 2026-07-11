package com.mhlotto.snoozereviews.ui.launch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class LaunchCoordinatorTest {
    @Test
    public void lookupFinishesBeforeMinimumDuration() {
        RecordingListener listener = new RecordingListener();
        LaunchCoordinator coordinator = new LaunchCoordinator(listener);

        coordinator.onLookupSuccess(LaunchRoute.createLastNightLog("2026-07-10"));
        assertNull(listener.route);

        coordinator.onMinimumDurationElapsed();
        assertEquals(LaunchDestination.CREATE_LAST_NIGHT_LOG, listener.route.getDestination());
        assertEquals(1, listener.routeCount);
    }

    @Test
    public void minimumDurationFinishesBeforeLookup() {
        RecordingListener listener = new RecordingListener();
        LaunchCoordinator coordinator = new LaunchCoordinator(listener);

        coordinator.onMinimumDurationElapsed();
        assertNull(listener.route);

        coordinator.onLookupSuccess(LaunchRoute.showLastNightLog(7L, "2026-07-10"));
        assertEquals(LaunchDestination.SHOW_LAST_NIGHT_LOG, listener.route.getDestination());
        assertEquals(1, listener.routeCount);
    }

    @Test
    public void duplicateCompletionsRouteOnce() {
        RecordingListener listener = new RecordingListener();
        LaunchCoordinator coordinator = new LaunchCoordinator(listener);

        coordinator.onMinimumDurationElapsed();
        coordinator.onLookupSuccess(LaunchRoute.createLastNightLog("2026-07-10"));
        coordinator.onLookupSuccess(LaunchRoute.showLastNightLog(7L, "2026-07-10"));
        coordinator.onMinimumDurationElapsed();

        assertEquals(LaunchDestination.CREATE_LAST_NIGHT_LOG, listener.route.getDestination());
        assertEquals(1, listener.routeCount);
    }

    @Test
    public void lookupErrorProducesErrorState() {
        RecordingListener listener = new RecordingListener();
        LaunchCoordinator coordinator = new LaunchCoordinator(listener);
        RuntimeException error = new RuntimeException("boom");

        coordinator.onLookupError(error);
        assertNull(listener.error);

        coordinator.onMinimumDurationElapsed();
        assertEquals(error, listener.error);
        assertEquals(1, listener.errorCount);
    }

    @Test
    public void successfulRetryProducesOneRoute() {
        RecordingListener listener = new RecordingListener();
        LaunchCoordinator coordinator = new LaunchCoordinator(listener);

        coordinator.onMinimumDurationElapsed();
        coordinator.onLookupError(new RuntimeException("boom"));
        assertEquals(1, listener.errorCount);

        coordinator.startRetry();
        coordinator.onLookupSuccess(LaunchRoute.createLastNightLog("2026-07-10"));
        coordinator.onLookupSuccess(LaunchRoute.showLastNightLog(7L, "2026-07-10"));

        assertNotNull(listener.route);
        assertEquals(LaunchDestination.CREATE_LAST_NIGHT_LOG, listener.route.getDestination());
        assertEquals(1, listener.routeCount);
        assertEquals(1, listener.errorCount);
    }

    private static class RecordingListener implements LaunchCoordinator.Listener {
        LaunchRoute route;
        Throwable error;
        int routeCount;
        int errorCount;

        @Override
        public void onRouteReady(LaunchRoute route) {
            this.route = route;
            routeCount++;
        }

        @Override
        public void onLaunchError(Throwable error) {
            this.error = error;
            errorCount++;
        }
    }
}
