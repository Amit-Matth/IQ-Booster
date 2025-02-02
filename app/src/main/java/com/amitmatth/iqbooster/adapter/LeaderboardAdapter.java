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

import java.util.Collections;
import java.util.List;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.UserViewHolder> {

    private final List<Users> userList;
    private final Context context;

    // Constructor
    public LeaderboardAdapter(List<Users> userList, Context context) {
        this.context = context;
        this.userList = userList;

        // Sort the list by score in descending order before displaying
        this.userList.sort((u1, u2) -> Integer.compare(u2.getScore(), u1.getScore()));
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.users_list_layout, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        Users user = userList.get(position);

        // Set user details
        holder.userLeaderboardName.setText(user.getName());
        holder.userLeaderboardScore.setText("Score: " + user.getScore());

        // Set the rank dynamically (calculated after sorting)
        holder.userLeaderboardRank.setText("Rank: " + user.getRank()); // Use the rank from the user object

        Glide.with(context)
                .load(user.getProfilePicUrl() != null ? user.getProfilePicUrl() : R.drawable.user)
                .circleCrop()
                .into(holder.userLeaderboardImg);
    }




    @Override
    public int getItemCount() {
        return userList.size();
    }

    // ViewHolder class to hold the views
    static class UserViewHolder extends RecyclerView.ViewHolder {
        private final TextView userLeaderboardName;
        private final TextView userLeaderboardScore;
        private final TextView userLeaderboardRank;
        private final ImageView userLeaderboardImg;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            userLeaderboardImg = itemView.findViewById(R.id.userLeaderboardImg);
            userLeaderboardName = itemView.findViewById(R.id.userLeaderboardName);
            userLeaderboardScore = itemView.findViewById(R.id.userLeaderboardScore);
            userLeaderboardRank = itemView.findViewById(R.id.userLeaderboardRank);
        }
    }
}
