package com.mhlotto.snoozereviews.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.mhlotto.snoozereviews.R;
import com.mhlotto.snoozereviews.databinding.ActivitySleepLogDetailBinding;

public class SleepLogDetailActivity extends AppCompatActivity {
    public static final String EXTRA_SLEEP_LOG_ID = "com.mhlotto.snoozereviews.extra.SLEEP_LOG_ID";
    public static final String EXTRA_NIGHT_DATE = "com.mhlotto.snoozereviews.extra.NIGHT_DATE";

    private static final String TAG = "SleepLogDetailActivity";

    public static Intent newIntent(Context context, long sleepLogId, String nightDate) {
        Intent intent = new Intent(context, SleepLogDetailActivity.class);
        intent.putExtra(EXTRA_SLEEP_LOG_ID, sleepLogId);
        intent.putExtra(EXTRA_NIGHT_DATE, nightDate);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivitySleepLogDetailBinding binding = ActivitySleepLogDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        long sleepLogId = getIntent().getLongExtra(EXTRA_SLEEP_LOG_ID, 0L);
        String nightDate = getIntent().getStringExtra(EXTRA_NIGHT_DATE);
        if (sleepLogId <= 0L || !SleepLogFormActivity.isValidNightDate(nightDate)) {
            Log.e(TAG, "Invalid or missing sleep log destination extras");
            binding.toolbar.setTitle(R.string.invalid_launch_data_title);
            binding.nightDate.setText("");
            binding.message.setText(R.string.invalid_launch_data_message);
            return;
        }

        binding.nightDate.setText(nightDate);
    }
}
