package com.amitmatth.iqbooster.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.amitmatth.iqbooster.R;
import com.amitmatth.iqbooster.databinding.ItemLevelBinding;
import com.amitmatth.iqbooster.model.Level;

import java.util.List;

public class LevelAdapter extends RecyclerView.Adapter<LevelAdapter.LevelItemViewHolder> {

    private List<Level> itemList;
    private OnLevelItemClickListener listener;
    private Context context;

    public interface OnLevelItemClickListener {
        void onLevelItemClicked(Level level);
    }

    public void setOnItemClickListener(OnLevelItemClickListener listener) {
        this.listener = listener;
    }

    public LevelAdapter(Context context, List<Level> itemList) {
        this.context = context;
        this.itemList = itemList;
    }

    @NonNull
    @Override
    public LevelItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemLevelBinding levelBinding = ItemLevelBinding.inflate(inflater, parent, false);
        return new LevelItemViewHolder(levelBinding, listener, itemList);
    }

    @Override
    public void onBindViewHolder(@NonNull LevelItemViewHolder holder, int position) {
        Level level = itemList.get(position);
        holder.bind(level, context);
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public void updateLevelDisplayList(List<Level> newItemList) {
        this.itemList = newItemList;
        notifyDataSetChanged();
    }

    static class LevelItemViewHolder extends RecyclerView.ViewHolder {
        private ItemLevelBinding binding;

        LevelItemViewHolder(ItemLevelBinding binding, final OnLevelItemClickListener listener, final List<Level> itemList) {
            super(binding.getRoot());
            this.binding = binding;

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION && position < itemList.size()) {
                    Level item = itemList.get(position);
                    listener.onLevelItemClicked(item);
                }
            });
        }

        void bind(final Level level, Context context) {
            binding.levelNameTextView.setText(level.getName());

            if (level.isUnlocked()) {
                binding.levelIconImageView.setImageResource(level.getEarnedDrawableResId());
                binding.levelStatusIndicatorImageView.setImageResource(R.drawable.ic_success_matrix);
                binding.levelStatusIndicatorImageView.setColorFilter(ContextCompat.getColor(context, R.color.neon_green));
                binding.levelProgressLayout.setVisibility(View.GONE);
            } else {
                binding.levelIconImageView.setImageResource(level.getLockedDrawableResId());
                binding.levelStatusIndicatorImageView.setImageResource(R.drawable.ic_error_matrix);
                binding.levelStatusIndicatorImageView.setColorFilter(ContextCompat.getColor(context, R.color.error_red));

                int userScore = level.getUserScoreForThisLevel();
                int prevLevelReq = level.getPreviousLevelScoreRequirement();
                int currentLevelReq = level.getRequiredScore();

                int pointsEarnedForThisLevel = userScore - prevLevelReq;
                int pointsNeededForThisLevel = currentLevelReq - prevLevelReq;

                pointsEarnedForThisLevel = Math.max(0, Math.min(pointsEarnedForThisLevel, pointsNeededForThisLevel));

                if (pointsNeededForThisLevel > 0) {
                    binding.levelItemProgressBar.setMax(pointsNeededForThisLevel);
                    binding.levelItemProgressBar.setProgress(pointsEarnedForThisLevel);
                    binding.levelItemProgressText.setText(pointsEarnedForThisLevel + " / " + pointsNeededForThisLevel);
                    binding.levelProgressLayout.setVisibility(View.VISIBLE);
                } else {
                    binding.levelProgressLayout.setVisibility(View.GONE);
                }
            }
            binding.levelIconImageView.setAlpha(1.0f);
        }
    }
}