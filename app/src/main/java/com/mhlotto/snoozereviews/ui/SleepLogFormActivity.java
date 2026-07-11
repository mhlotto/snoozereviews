package com.mhlotto.snoozereviews.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.mhlotto.snoozereviews.R;
import com.mhlotto.snoozereviews.databinding.ActivitySleepLogFormBinding;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class SleepLogFormActivity extends AppCompatActivity {
    public static final String EXTRA_NIGHT_DATE = "com.mhlotto.snoozereviews.extra.NIGHT_DATE";

    private static final String TAG = "SleepLogFormActivity";

    public static Intent newCreateIntent(Context context, String nightDate) {
        Intent intent = new Intent(context, SleepLogFormActivity.class);
        intent.putExtra(EXTRA_NIGHT_DATE, nightDate);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivitySleepLogFormBinding binding = ActivitySleepLogFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        String nightDate = getIntent().getStringExtra(EXTRA_NIGHT_DATE);
        if (!isValidNightDate(nightDate)) {
            Log.e(TAG, "Invalid or missing night date extra");
            binding.toolbar.setTitle(R.string.invalid_launch_data_title);
            binding.nightDate.setText("");
            binding.message.setText(R.string.invalid_launch_data_message);
            return;
        }

        binding.nightDate.setText(nightDate);
    }

    public static boolean isValidNightDate(String nightDate) {
        if (nightDate == null) {
            return false;
        }
        try {
            return LocalDate.parse(nightDate).toString().equals(nightDate);
        } catch (DateTimeParseException exception) {
            return false;
        }
    }
}
