package com.mhlotto.snoozereviews.ui.tag;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.mhlotto.snoozereviews.R;
import com.mhlotto.snoozereviews.data.SleepTagKeys;
import com.mhlotto.snoozereviews.data.entity.CustomSleepTagEntity;
import com.mhlotto.snoozereviews.data.tag.CustomSleepTagKey;
import com.mhlotto.snoozereviews.data.tag.SleepTagCategoryKeys;
import com.mhlotto.snoozereviews.ui.detail.SleepLogDetailFormatter;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class SleepTagCategoryGrouperTest {
    private final SleepTagCategoryGrouper grouper = new SleepTagCategoryGrouper(new Labels(), Locale.US);

    @Test
    public void emptySelectionReturnsNoGroups() {
        assertTrue(grouper.group(Collections.emptyList(), Collections.emptyList()).isEmpty());
    }

    @Test
    public void builtInTagsUseCatalogCategoriesAndOrder() {
        List<SleepTagCategoryGroup> groups = grouper.group(Arrays.asList(
                SleepTagKeys.RESTLESS,
                SleepTagKeys.TOO_HOT,
                SleepTagKeys.NOISY,
                SleepTagKeys.VIVID_DREAMS
        ), Collections.emptyList());

        assertEquals(3, groups.size());
        assertEquals(SleepTagCategoryKeys.ENVIRONMENT, groups.get(0).getCategoryKey());
        assertEquals(Arrays.asList("Too hot", "Noisy"), labels(groups.get(0)));
        assertEquals(SleepTagCategoryKeys.PHYSICAL, groups.get(1).getCategoryKey());
        assertEquals(Collections.singletonList("Restless"), labels(groups.get(1)));
        assertEquals(SleepTagCategoryKeys.MIND_AND_DREAMS, groups.get(2).getCategoryKey());
        assertEquals(Collections.singletonList("Vivid dreams"), labels(groups.get(2)));
    }

    @Test
    public void customTagsUseCurrentCategorySortAfterBuiltInsAndDeduplicate() {
        String fanNoise = CustomSleepTagKey.encode("Fan noise");
        String airFilter = CustomSleepTagKey.encode("Air filter");
        List<String> selected = new ArrayList<>(Arrays.asList(
                fanNoise,
                SleepTagKeys.TOO_HOT,
                airFilter,
                fanNoise
        ));
        List<CustomSleepTagEntity> customTags = Arrays.asList(
                custom(fanNoise, "Fan noise", SleepTagCategoryKeys.ENVIRONMENT, false),
                custom(airFilter, "Air filter", SleepTagCategoryKeys.ENVIRONMENT, true)
        );

        List<SleepTagCategoryGroup> groups = grouper.group(selected, customTags);

        assertEquals(1, groups.size());
        assertEquals(Arrays.asList("Too hot", "Air filter", "Fan noise"), labels(groups.get(0)));
        assertEquals(4, selected.size());
    }

    @Test
    public void categoryChangesAffectGroupingWithoutChangingKey() {
        String key = CustomSleepTagKey.encode("Weighted blanket");

        List<SleepTagCategoryGroup> environment = grouper.group(
                Collections.singletonList(key),
                Collections.singletonList(custom(key, "Weighted blanket", SleepTagCategoryKeys.ENVIRONMENT, true))
        );
        List<SleepTagCategoryGroup> physical = grouper.group(
                Collections.singletonList(key),
                Collections.singletonList(custom(key, "Weighted blanket", SleepTagCategoryKeys.PHYSICAL, true))
        );

        assertEquals(SleepTagCategoryKeys.ENVIRONMENT, environment.get(0).getCategoryKey());
        assertEquals(SleepTagCategoryKeys.PHYSICAL, physical.get(0).getCategoryKey());
        assertEquals(key, physical.get(0).getTags().get(0).getKey());
    }

    @Test
    public void orphanMalformedAndUnknownKeysGoToOther() {
        String orphan = CustomSleepTagKey.encode("Weighted blanket");
        List<SleepTagCategoryGroup> groups = grouper.group(Arrays.asList(
                orphan,
                CustomSleepTagKey.PREFIX + "%%%BAD",
                "FUTURE_TAG"
        ), Collections.emptyList());

        assertEquals(1, groups.size());
        assertEquals(SleepTagCategoryKeys.OTHER, groups.get(0).getCategoryKey());
        assertEquals(Arrays.asList("Unknown tag (CUSTOM_TAG_B64:%%%BAD)", "Unknown tag (FUTURE_TAG)", "Weighted blanket"), labels(groups.get(0)));
    }

    private CustomSleepTagEntity custom(String key, String displayName, String category, boolean active) {
        return new CustomSleepTagEntity(key, displayName, displayName.toLowerCase(Locale.ROOT), category, active, 1L, 2L);
    }

    private List<String> labels(SleepTagCategoryGroup group) {
        List<String> labels = new ArrayList<>();
        for (com.mhlotto.snoozereviews.ui.detail.TagDisplayItem item : group.getTags()) {
            labels.add(item.getLabel());
        }
        return labels;
    }

    private static class Labels implements SleepLogDetailFormatter.LabelResolver {
        @Override
        public String getString(int resId) {
            if (resId == R.string.tag_category_temperature_environment) return "Temperature and environment";
            if (resId == R.string.tag_category_physical_condition) return "Physical condition";
            if (resId == R.string.tag_category_sleep_pattern) return "Sleep pattern";
            if (resId == R.string.tag_category_mind_dreams) return "Mind and dreams";
            if (resId == R.string.tag_category_morning_result) return "Morning result";
            if (resId == R.string.tag_category_other) return "Other";
            if (resId == R.string.tag_too_hot) return "Too hot";
            if (resId == R.string.tag_noisy) return "Noisy";
            if (resId == R.string.tag_restless) return "Restless";
            if (resId == R.string.tag_vivid_dreams) return "Vivid dreams";
            return "res-" + resId;
        }

        @Override
        public String getString(int resId, Object... args) {
            if (resId == R.string.unknown_tag_detail_format) {
                return "Unknown tag (" + args[0] + ")";
            }
            return String.format(getString(resId), args);
        }
    }
}
