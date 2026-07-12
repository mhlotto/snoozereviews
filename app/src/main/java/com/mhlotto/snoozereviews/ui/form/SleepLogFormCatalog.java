package com.mhlotto.snoozereviews.ui.form;

import com.mhlotto.snoozereviews.R;
import com.mhlotto.snoozereviews.data.SleepLocationKeys;
import com.mhlotto.snoozereviews.data.SleepTagKeys;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class SleepLogFormCatalog {
    public static final List<FormOption> LOCATION_OPTIONS = Collections.unmodifiableList(Arrays.asList(
            new FormOption(SleepLocationKeys.BED, R.string.location_bed),
            new FormOption(SleepLocationKeys.COUCH, R.string.location_couch),
            new FormOption(SleepLocationKeys.BED_AND_COUCH, R.string.location_bed_and_couch),
            new FormOption(SleepLocationKeys.RECLINER, R.string.location_recliner),
            new FormOption(SleepLocationKeys.HOTEL_OR_GUEST_BED, R.string.location_hotel_or_guest_bed),
            new FormOption(SleepLocationKeys.FLOOR, R.string.location_floor),
            new FormOption(SleepLocationKeys.VEHICLE, R.string.location_vehicle),
            new FormOption(SleepLocationKeys.OTHER, R.string.location_other)
    ));

    public static final List<TagCategory> TAG_CATEGORIES = Collections.unmodifiableList(Arrays.asList(
            new TagCategory(com.mhlotto.snoozereviews.data.tag.SleepTagCategoryKeys.ENVIRONMENT, R.string.tag_category_temperature_environment, Arrays.asList(
                    new FormOption(SleepTagKeys.TOO_HOT, R.string.tag_too_hot),
                    new FormOption(SleepTagKeys.TOO_COLD, R.string.tag_too_cold),
                    new FormOption(SleepTagKeys.SWEATY, R.string.tag_sweaty),
                    new FormOption(SleepTagKeys.NOISY, R.string.tag_noisy),
                    new FormOption(SleepTagKeys.TOO_BRIGHT, R.string.tag_too_bright),
                    new FormOption(SleepTagKeys.UNCOMFORTABLE, R.string.tag_uncomfortable),
                    new FormOption(SleepTagKeys.INTERRUPTED, R.string.tag_interrupted)
            )),
            new TagCategory(com.mhlotto.snoozereviews.data.tag.SleepTagCategoryKeys.PHYSICAL, R.string.tag_category_physical_condition, Arrays.asList(
                    new FormOption(SleepTagKeys.SICK, R.string.tag_sick),
                    new FormOption(SleepTagKeys.CONGESTED, R.string.tag_congested),
                    new FormOption(SleepTagKeys.PAIN, R.string.tag_pain),
                    new FormOption(SleepTagKeys.HEADACHE, R.string.tag_headache),
                    new FormOption(SleepTagKeys.RESTLESS, R.string.tag_restless),
                    new FormOption(SleepTagKeys.ITCHY, R.string.tag_itchy),
                    new FormOption(SleepTagKeys.HUNGRY, R.string.tag_hungry),
                    new FormOption(SleepTagKeys.OVERFULL, R.string.tag_overfull)
            )),
            new TagCategory(com.mhlotto.snoozereviews.data.tag.SleepTagCategoryKeys.SLEEP_PATTERN, R.string.tag_category_sleep_pattern, Arrays.asList(
                    new FormOption(SleepTagKeys.FELL_ASLEEP_QUICKLY, R.string.tag_fell_asleep_quickly),
                    new FormOption(SleepTagKeys.TROUBLE_FALLING_ASLEEP, R.string.tag_trouble_falling_asleep),
                    new FormOption(SleepTagKeys.WOKE_EARLY, R.string.tag_woke_early),
                    new FormOption(SleepTagKeys.SLEPT_LATE, R.string.tag_slept_late),
                    new FormOption(SleepTagKeys.WOKE_REPEATEDLY, R.string.tag_woke_repeatedly),
                    new FormOption(SleepTagKeys.LIGHT_SLEEP, R.string.tag_light_sleep),
                    new FormOption(SleepTagKeys.DEEP_SLEEP, R.string.tag_deep_sleep),
                    new FormOption(SleepTagKeys.TOSSED_AND_TURNED, R.string.tag_tossed_and_turned)
            )),
            new TagCategory(com.mhlotto.snoozereviews.data.tag.SleepTagCategoryKeys.MIND_AND_DREAMS, R.string.tag_category_mind_dreams, Arrays.asList(
                    new FormOption(SleepTagKeys.CALM, R.string.tag_calm),
                    new FormOption(SleepTagKeys.ANXIOUS, R.string.tag_anxious),
                    new FormOption(SleepTagKeys.RACING_THOUGHTS, R.string.tag_racing_thoughts),
                    new FormOption(SleepTagKeys.VIVID_DREAMS, R.string.tag_vivid_dreams),
                    new FormOption(SleepTagKeys.PLEASANT_DREAMS, R.string.tag_pleasant_dreams),
                    new FormOption(SleepTagKeys.NIGHTMARES, R.string.tag_nightmares),
                    new FormOption(SleepTagKeys.STRESSFUL_DREAMS, R.string.tag_stressful_dreams),
                    new FormOption(SleepTagKeys.NO_DREAM_RECALL, R.string.tag_no_dream_recall)
            )),
            new TagCategory(com.mhlotto.snoozereviews.data.tag.SleepTagCategoryKeys.MORNING_RESULT, R.string.tag_category_morning_result, Arrays.asList(
                    new FormOption(SleepTagKeys.REFRESHED, R.string.tag_refreshed),
                    new FormOption(SleepTagKeys.GROGGY, R.string.tag_groggy),
                    new FormOption(SleepTagKeys.EXHAUSTED, R.string.tag_exhausted),
                    new FormOption(SleepTagKeys.ENERGETIC, R.string.tag_energetic),
                    new FormOption(SleepTagKeys.NEEDED_A_NAP, R.string.tag_needed_a_nap)
            ))
    ));

    private SleepLogFormCatalog() {
    }
}
