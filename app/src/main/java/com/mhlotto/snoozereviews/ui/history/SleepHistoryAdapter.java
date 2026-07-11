package com.mhlotto.snoozereviews.ui.history;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.mhlotto.snoozereviews.R;
import com.mhlotto.snoozereviews.databinding.ItemSleepHistoryBinding;

public class SleepHistoryAdapter extends ListAdapter<SleepHistoryItem, SleepHistoryAdapter.ViewHolder> {
    public interface OnItemClickListener {
        void onItemClicked(SleepHistoryItem item);
    }

    private final OnItemClickListener listener;

    public SleepHistoryAdapter(OnItemClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getSleepLogId();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSleepHistoryBinding binding = ItemSleepHistoryBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    public static final DiffUtil.ItemCallback<SleepHistoryItem> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(@NonNull SleepHistoryItem oldItem, @NonNull SleepHistoryItem newItem) {
                    return oldItem.getSleepLogId() == newItem.getSleepLogId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull SleepHistoryItem oldItem, @NonNull SleepHistoryItem newItem) {
                    return oldItem.equals(newItem);
                }
            };

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemSleepHistoryBinding binding;

        ViewHolder(ItemSleepHistoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(SleepHistoryItem item, OnItemClickListener listener) {
            binding.date.setText(item.getDisplayDate());
            binding.duration.setText(item.getDuration());
            binding.ratings.setText(item.getRatingSummary());
            binding.location.setText(item.getLocationLabel());
            binding.getRoot().setContentDescription(item.getAccessibilitySummary());
            binding.getRoot().setOnClickListener(view -> listener.onItemClicked(item));
            bindTags(item);
        }

        private void bindTags(SleepHistoryItem item) {
            binding.tagPreview.removeAllViews();
            for (String label : item.getTagLabels()) {
                Chip chip = new Chip(binding.tagPreview.getContext());
                chip.setText(label);
                chip.setCheckable(false);
                chip.setClickable(false);
                chip.setFocusable(false);
                binding.tagPreview.addView(chip);
            }
            if (item.getRemainingTagCount() > 0) {
                Chip chip = new Chip(binding.tagPreview.getContext());
                chip.setText(binding.tagPreview.getResources().getString(
                        R.string.history_tag_overflow_format,
                        item.getRemainingTagCount()
                ));
                chip.setCheckable(false);
                chip.setClickable(false);
                chip.setFocusable(false);
                binding.tagPreview.addView(chip);
            }
            binding.tagPreview.setVisibility(
                    binding.tagPreview.getChildCount() == 0 ? View.GONE : View.VISIBLE
            );
        }
    }
}
