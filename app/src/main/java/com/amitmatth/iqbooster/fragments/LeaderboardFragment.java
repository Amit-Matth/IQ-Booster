package com.amitmatth.iqbooster.fragments;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.amitmatth.iqbooster.R;
import com.amitmatth.iqbooster.adapter.LeaderboardAdapter;
import com.amitmatth.iqbooster.data.Users;
import com.amitmatth.iqbooster.databinding.FragmentLeaderboardBinding;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardFragment extends Fragment {

    private FragmentLeaderboardBinding binding;
    private final List<Users> usersList = new ArrayList<>();
    private LeaderboardAdapter adapter;
    private DatabaseReference database;

    private static final String TAG = "LeaderboardFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLeaderboardBinding.inflate(inflater, container, false);

        // Firebase reference
        database = FirebaseDatabase.getInstance().getReference("users");

        database.keepSynced(true);

        // Initialize RecyclerView
        binding.leaderboardRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new LeaderboardAdapter(usersList, getContext());
        binding.leaderboardRecyclerView.setAdapter(adapter);

        binding.loadingUserProgressBar.setVisibility(View.VISIBLE);
        binding.leaderboardRecyclerView.setVisibility(View.GONE);
        binding.userDetails.setVisibility(View.GONE);

        // Load data from Firebase
        loadLeaderboard();

        return binding.getRoot();
    }

    private void loadLeaderboard() {
        database.orderByChild("score").addValueEventListener(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || getContext() == null) {
                    return;
                }

                usersList.clear();
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    Users user = userSnapshot.getValue(Users.class);

                    if (user != null) {
                        DataSnapshot quizResultsSnapshot = userSnapshot.child("quizResults");
                        int totalQuestions = 0;

                        if (quizResultsSnapshot.exists() && quizResultsSnapshot.child("totalQuestions").exists()) {
                            totalQuestions = quizResultsSnapshot.child("totalQuestions").getValue(Integer.class);
                        }

                        user.setTotalQuestions(totalQuestions);
                        usersList.add(user);
                    } else {
                        Log.e("leaderboard", "User data is null!");
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    usersList.sort((u1, u2) -> {
                        int scoreComparison = Integer.compare(u2.getScore(), u1.getScore());
                        if (scoreComparison != 0) {
                            return scoreComparison;
                        } else {
                            return Integer.compare(u1.getTotalQuestions(), u2.getTotalQuestions());
                        }
                    });
                }

                for (int i = 0; i < usersList.size(); i++) {
                    Users user = usersList.get(i);
                    int rank = i + 1;

                    if (i > 0 && user.getScore() == usersList.get(i - 1).getScore() &&
                            user.getTotalQuestions() == usersList.get(i - 1).getTotalQuestions()) {
                        rank = usersList.get(i - 1).getRank();
                    }

                    user.setRank(rank);
                }

                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }

                if (isAdded() && getContext() != null) {
                    binding.loadingUserProgressBar.setVisibility(View.GONE);
                    binding.leaderboardRecyclerView.setVisibility(View.VISIBLE);
                    binding.userDetails.setVisibility(View.VISIBLE);
                }

                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

                if (currentUser != null) {
                    DatabaseReference database = FirebaseDatabase.getInstance().getReference();
                    String userId = currentUser.getUid();

                    if (isAdded() && getContext() != null) {
                        Glide.with(requireContext())
                                .load(usersList.get(0).photoUrl != null ? currentUser.getPhotoUrl() : R.drawable.user)
                                .circleCrop()
                                .into(binding.userBottomImg);
                    }

                    database.child("users").child(userId).child("score")
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    if (!isAdded() || getContext() == null) {
                                        return;
                                    }

                                    if (snapshot.exists()) {
                                        int score = snapshot.getValue(Integer.class);
                                        binding.userBottomScore.setText("Your score is: " + score);
                                        binding.userBottomName.setText(currentUser.getDisplayName());

                                        int rank = 1;
                                        for (Users user : usersList) {
                                            if (user.getScore() == score) {
                                                break;
                                            }
                                            rank++;
                                        }
                                        binding.userBottomRank.setText("Your rank: " + rank);
                                    } else {
                                        Log.d("currentUser", "Score not found");
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Log.e("currentUser", "Error fetching score: " + error.getMessage());
                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("leaderboard error", "Database error: " + error.getMessage());
                if (isAdded() && getContext() != null) {
                    binding.loadingUserProgressBar.setVisibility(View.VISIBLE);
                    binding.userDetails.setVisibility(View.VISIBLE);
                }
            }
        });
    }

}
