package com.mhlotto.snoozereviews.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.mhlotto.snoozereviews.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
    }
}
