package com.amitmatth.iqbooster.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.amitmatth.iqbooster.R;
import com.amitmatth.iqbooster.data.Users;
import com.bumptech.glide.Glide;

import java.util.List;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.UserViewHolder> {

    private final List<Users> userList;
    private final Context context;

    public LeaderboardAdapter(List<Users> userList, Context context) {
        this.context = context;
        this.userList = userList;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_leaderboard_entry, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        Users user = userList.get(position);

        holder.nameTextView.setText(user.getName());
        holder.scoreTextView.setText("Score: " + user.getScore());
        holder.rankTextView.setText("#" + user.getRank());
        holder.levelTextView.setText("LVL " + user.getLevel());

        Glide.with(context)
                .load(user.getProfilePicUrl() != null ? user.getProfilePicUrl() : R.drawable.user)
                .circleCrop()
                .into(holder.avatarImageView);
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameTextView;
        private final TextView scoreTextView;
        private final TextView rankTextView;
        private final ImageView avatarImageView;
        private final TextView levelTextView;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarImageView = itemView.findViewById(R.id.avatarImageView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            scoreTextView = itemView.findViewById(R.id.scoreTextView);
            rankTextView = itemView.findViewById(R.id.rankTextView);
            levelTextView = itemView.findViewById(R.id.levelTextView);
        }
    }
}