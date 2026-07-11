package com.mhlotto.snoozereviews.ui.form;

public final class FormInputParser {
    private FormInputParser() {
    }

    public static Integer parseAwakeningCount(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        try {
            int value = Integer.parseInt(text.trim());
            if (value < 0) {
                throw new IllegalArgumentException("awakening count must be zero or greater");
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("awakening count must be a whole number", exception);
        }
    }
}
