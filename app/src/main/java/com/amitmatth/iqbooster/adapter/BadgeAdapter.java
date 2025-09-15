package com.amitmatth.iqbooster.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.amitmatth.iqbooster.R;
import com.amitmatth.iqbooster.databinding.ItemBadgeBinding;
import com.amitmatth.iqbooster.model.Badge;

import java.util.List;

public class BadgeAdapter extends RecyclerView.Adapter<BadgeAdapter.BadgeViewHolder> {

    private List<Badge> itemList;
    private OnBadgeItemClickListener listener;
    private Context context;

    public interface OnBadgeItemClickListener {
        void onBadgeItemClicked(Badge badge);
    }

    public void setOnItemClickListener(OnBadgeItemClickListener listener) {
        this.listener = listener;
    }

    public BadgeAdapter(Context context, List<Badge> itemList) {
        this.context = context;
        this.itemList = itemList;
    }

    @NonNull
    @Override
    public BadgeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemBadgeBinding badgeBinding = ItemBadgeBinding.inflate(inflater, parent, false);
        return new BadgeViewHolder(badgeBinding, listener, itemList);
    }

    @Override
    public void onBindViewHolder(@NonNull BadgeViewHolder holder, int position) {
        Badge badge = itemList.get(position);
        holder.bind(badge, context);
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public void updateBadgeDisplayList(List<Badge> newItemList) {
        this.itemList = newItemList;
        notifyDataSetChanged();
    }

    static class BadgeViewHolder extends RecyclerView.ViewHolder {
        private ItemBadgeBinding binding;

        BadgeViewHolder(ItemBadgeBinding binding, final OnBadgeItemClickListener listener, final List<Badge> itemList) {
            super(binding.getRoot());
            this.binding = binding;

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION && position < itemList.size()) {
                    Badge item = itemList.get(position);
                    listener.onBadgeItemClicked(item);
                }
            });
        }

        void bind(final Badge badge, Context context) {
            binding.badgeNameTextView.setText(badge.getName());
            binding.badgeDescriptionTextView.setText(badge.getDescription());

            if (badge.isUnlocked()) {
                binding.badgeIconImageView.setImageResource(badge.getEarnedDrawableResId());
                binding.badgeStatusIconImageView.setImageResource(R.drawable.ic_success_matrix);
                binding.badgeStatusIconImageView.setColorFilter(ContextCompat.getColor(context, R.color.neon_green));
                binding.badgeProgressLayout.setVisibility(View.GONE);
            } else {
                binding.badgeIconImageView.setImageResource(badge.getLockedDrawableResId());
                binding.badgeStatusIconImageView.setImageResource(R.drawable.ic_error_matrix);
                binding.badgeStatusIconImageView.setColorFilter(ContextCompat.getColor(context, R.color.error_red));

                if (badge.getCurrentProgress() > 0 && badge.getRequiredCorrectAnswers() > 0) {
                    binding.badgeProgressLayout.setVisibility(View.VISIBLE);
                    binding.badgeProgressBar.setMax(badge.getRequiredCorrectAnswers());
                    binding.badgeProgressBar.setProgress(badge.getCurrentProgress());
                    binding.badgeProgressText.setText(badge.getCurrentProgress() + "/" + badge.getRequiredCorrectAnswers());
                } else {
                    binding.badgeProgressLayout.setVisibility(View.GONE);
                }
            }
            binding.badgeIconImageView.setAlpha(1.0f);
        }
    }
}