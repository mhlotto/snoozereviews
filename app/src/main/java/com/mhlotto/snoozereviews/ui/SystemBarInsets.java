package com.mhlotto.snoozereviews.ui;

import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

final class SystemBarInsets {
    private SystemBarInsets() {
    }

    static void applyToRoot(View view) {
        apply(view, true, true);
    }

    static void applyToView(View view) {
        apply(view, true, true);
    }

    private static void apply(View view, boolean applyTop, boolean applyBottom) {
        int left = view.getPaddingLeft();
        int top = view.getPaddingTop();
        int right = view.getPaddingRight();
        int bottom = view.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(view, (target, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            target.setPadding(
                    left + insets.left,
                    top + (applyTop ? insets.top : 0),
                    right + insets.right,
                    bottom + (applyBottom ? insets.bottom : 0)
            );
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(view);
    }
}
