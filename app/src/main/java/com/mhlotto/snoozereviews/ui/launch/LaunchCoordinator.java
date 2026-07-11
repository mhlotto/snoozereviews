package com.mhlotto.snoozereviews.ui.launch;

public class LaunchCoordinator {
    public interface Listener {
        void onRouteReady(LaunchRoute route);

        void onLaunchError(Throwable error);
    }

    private final Listener listener;
    private boolean minimumDurationElapsed;
    private boolean completed;
    private LaunchRoute pendingRoute;
    private Throwable pendingError;

    public LaunchCoordinator(Listener listener) {
        this.listener = listener;
    }

    public void onMinimumDurationElapsed() {
        if (completed) {
            return;
        }
        minimumDurationElapsed = true;
        maybeComplete();
    }

    public void onLookupSuccess(LaunchRoute route) {
        if (completed) {
            return;
        }
        pendingRoute = route;
        pendingError = null;
        maybeComplete();
    }

    public void onLookupError(Throwable error) {
        if (completed) {
            return;
        }
        pendingError = error;
        pendingRoute = null;
        maybeComplete();
    }

    public void startRetry() {
        minimumDurationElapsed = true;
        completed = false;
        pendingRoute = null;
        pendingError = null;
    }

    private void maybeComplete() {
        if (!minimumDurationElapsed || completed) {
            return;
        }
        if (pendingRoute != null) {
            completed = true;
            listener.onRouteReady(pendingRoute);
        } else if (pendingError != null) {
            completed = true;
            listener.onLaunchError(pendingError);
        }
    }
}
