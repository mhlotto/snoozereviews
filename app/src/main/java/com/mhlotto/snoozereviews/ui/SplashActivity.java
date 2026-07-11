package com.mhlotto.snoozereviews.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.mhlotto.snoozereviews.data.LastNightDateCalculator;
import com.mhlotto.snoozereviews.data.SleepLogRepository;
import com.mhlotto.snoozereviews.data.SleepLogWithTags;
import com.mhlotto.snoozereviews.databinding.ActivitySplashErrorBinding;
import com.mhlotto.snoozereviews.ui.launch.LaunchCoordinator;
import com.mhlotto.snoozereviews.ui.launch.LaunchDestination;
import com.mhlotto.snoozereviews.ui.launch.LaunchRoute;

public class SplashActivity extends AppCompatActivity implements LaunchCoordinator.Listener {
    private static final String TAG = "SplashActivity";
    private static final long MINIMUM_SPLASH_DURATION_MILLIS = 2500L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean keepSplashOnScreen = true;
    private boolean destroyed;
    private boolean errorContentShown;
    private long startupElapsedMillis;
    private LaunchCoordinator coordinator;
    private LastNightDateCalculator lastNightDateCalculator;
    private SleepLogRepository sleepLogRepository;
    private ActivitySplashErrorBinding errorBinding;
    private final Runnable minimumDurationRunnable = () -> {
        if (!destroyed) {
            coordinator.onMinimumDurationElapsed();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        splashScreen.setKeepOnScreenCondition(() -> keepSplashOnScreen);

        super.onCreate(savedInstanceState);

        startupElapsedMillis = SystemClock.elapsedRealtime();
        coordinator = new LaunchCoordinator(this);
        lastNightDateCalculator = new LastNightDateCalculator();
        sleepLogRepository = new SleepLogRepository(this);

        startInitialLookup();
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        handler.removeCallbacks(minimumDurationRunnable);
        if (sleepLogRepository != null) {
            sleepLogRepository.shutdownBackgroundExecutor();
        }
        super.onDestroy();
    }

    @Override
    public void onRouteReady(LaunchRoute route) {
        if (destroyed || isFinishing()) {
            return;
        }

        keepSplashOnScreen = false;
        Intent intent;
        if (route.getDestination() == LaunchDestination.CREATE_LAST_NIGHT_LOG) {
            intent = SleepLogFormActivity.newCreateIntent(this, route.getNightDate());
        } else {
            intent = SleepLogDetailActivity.newIntent(this, route.getSleepLogId(), route.getNightDate());
        }
        startActivity(intent);
        finish();
    }

    @Override
    public void onLaunchError(Throwable error) {
        if (destroyed || isFinishing()) {
            return;
        }

        Log.e(TAG, "Sleep log lookup failed during launch", error);
        keepSplashOnScreen = false;
        showErrorState();
    }

    private void startInitialLookup() {
        keepSplashOnScreen = true;
        long elapsed = SystemClock.elapsedRealtime() - startupElapsedMillis;
        long remainingMillis = Math.max(0L, MINIMUM_SPLASH_DURATION_MILLIS - elapsed);
        handler.postDelayed(minimumDurationRunnable, remainingMillis);
        startLookup();
    }

    private void startRetry() {
        coordinator.startRetry();
        if (errorBinding != null) {
            errorBinding.retryButton.setEnabled(false);
        }
        startLookup();
    }

    private void startLookup() {
        String nightDate = lastNightDateCalculator.calculateLastNightDate();
        sleepLogRepository.findSleepLogByNightDate(nightDate, new SleepLogRepository.Callback<>() {
            @Override
            public void onSuccess(SleepLogWithTags result) {
                if (destroyed) {
                    return;
                }
                if (result == null) {
                    coordinator.onLookupSuccess(LaunchRoute.createLastNightLog(nightDate));
                } else {
                    coordinator.onLookupSuccess(LaunchRoute.showLastNightLog(
                            result.getSleepLog().getId(),
                            result.getSleepLog().getNightDate()
                    ));
                }
            }

            @Override
            public void onError(Throwable error) {
                if (!destroyed) {
                    coordinator.onLookupError(error);
                }
            }
        });
    }

    private void showErrorState() {
        if (!errorContentShown) {
            errorBinding = ActivitySplashErrorBinding.inflate(getLayoutInflater());
            setContentView(errorBinding.getRoot());
            errorContentShown = true;
            errorBinding.retryButton.setOnClickListener(view -> startRetry());
        }
        errorBinding.retryButton.setEnabled(true);
    }
}
