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
import com.mhlotto.snoozereviews.databinding.ActivitySplashBinding;
import com.mhlotto.snoozereviews.ui.launch.LaunchCoordinator;
import com.mhlotto.snoozereviews.ui.launch.LaunchDestination;
import com.mhlotto.snoozereviews.ui.launch.LaunchRoute;

public class SplashActivity extends AppCompatActivity implements LaunchCoordinator.Listener {
    private static final String TAG = "SplashActivity";
    private static final long MINIMUM_SPLASH_DURATION_MILLIS = 2500L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean destroyed;
    private long startupElapsedMillis;
    private LaunchCoordinator coordinator;
    private LastNightDateCalculator lastNightDateCalculator;
    private SleepLogRepository sleepLogRepository;
    private ActivitySplashBinding binding;
    private final Runnable minimumDurationRunnable = () -> {
        if (!destroyed) {
            coordinator.onMinimumDurationElapsed();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        SystemBarInsets.applyToView(binding.errorContent);

        startupElapsedMillis = SystemClock.elapsedRealtime();
        coordinator = new LaunchCoordinator(this);
        lastNightDateCalculator = new LastNightDateCalculator();
        sleepLogRepository = new SleepLogRepository(this);
        binding.retryButton.setOnClickListener(view -> startRetry());

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
        showErrorState();
    }

    private void startInitialLookup() {
        long elapsed = SystemClock.elapsedRealtime() - startupElapsedMillis;
        long remainingMillis = Math.max(0L, MINIMUM_SPLASH_DURATION_MILLIS - elapsed);
        handler.postDelayed(minimumDurationRunnable, remainingMillis);
        startLookup();
    }

    private void startRetry() {
        coordinator.startRetry();
        binding.retryButton.setEnabled(false);
        binding.errorContent.setVisibility(android.view.View.GONE);
        binding.splashImage.setVisibility(android.view.View.VISIBLE);
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
        binding.splashImage.setVisibility(android.view.View.GONE);
        binding.errorContent.setVisibility(android.view.View.VISIBLE);
        binding.retryButton.setEnabled(true);
    }
}
