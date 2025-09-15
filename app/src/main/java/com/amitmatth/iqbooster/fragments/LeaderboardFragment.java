package com.amitmatth.iqbooster.fragments;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;

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
import java.util.Objects;

public class LeaderboardFragment extends Fragment {

    private FragmentLeaderboardBinding binding;
    private final List<Users> usersList = new ArrayList<>();
    private LeaderboardAdapter adapter;
    private DatabaseReference database;
    private ObjectAnimator progressBarAnimator;

    private static final String TAG = "LeaderboardFragment";

    private static final int[] LEVEL_THRESHOLDS = {
            0, 100, 250, 500, 800, 1200, 1700, 2300, 3000, 4000,
            5200, 6600, 8200, 10000, 12000, 14500, 17500, 21000, 25000, 30000
    };


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLeaderboardBinding.inflate(inflater, container, false);

        database = FirebaseDatabase.getInstance().getReference("users");
        database.keepSynced(true);

        binding.leaderboardRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new LeaderboardAdapter(usersList, getContext());
        binding.leaderboardRecyclerView.setAdapter(adapter);

        binding.loadingUserProgressBar.setVisibility(View.VISIBLE);
        startLoadingAnimation();
        binding.leaderboardRecyclerView.setVisibility(View.GONE);
        binding.userDetailsCard.setVisibility(View.GONE);

        loadLeaderboard();

        return binding.getRoot();
    }

    private void startLoadingAnimation() {
        if (binding.loadingUserProgressBar != null) {
            progressBarAnimator = ObjectAnimator.ofFloat(binding.loadingUserProgressBar, "rotation", 0f, 360f);
            progressBarAnimator.setDuration(1000);
            progressBarAnimator.setRepeatCount(ValueAnimator.INFINITE);
            progressBarAnimator.setRepeatMode(ValueAnimator.RESTART);
            progressBarAnimator.setInterpolator(new LinearInterpolator());
            progressBarAnimator.start();
        }
    }

    private void stopLoadingAnimation() {
        if (progressBarAnimator != null) {
            progressBarAnimator.cancel();
        }
    }

    private int calculateUserLevel(int score) {
        for (int i = LEVEL_THRESHOLDS.length - 1; i >= 0; i--) {
            if (score >= LEVEL_THRESHOLDS[i]) {
                return i + 1;
            }
        }
        return 1;
    }

    private void loadLeaderboard() {
        database.orderByChild("score").addValueEventListener(new ValueEventListener() {
            @SuppressLint({"SetTextI18n", "NotifyDataSetChanged"})
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || getContext() == null || binding == null) {
                    return;
                }

                usersList.clear();
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    Users user = userSnapshot.getValue(Users.class);

                    if (user != null) {
                        int score = 0;
                        if (userSnapshot.child("score").exists() && userSnapshot.child("score").getValue(Integer.class) != null) {
                            score = userSnapshot.child("score").getValue(Integer.class);
                        }
                        user.setScore(score);
                        user.setLevel(calculateUserLevel(score));

                        DataSnapshot quizResultsSnapshot = userSnapshot.child("quizResults");
                        int totalQuestions = 0;
                        if (quizResultsSnapshot.exists() && quizResultsSnapshot.child("totalQuestions").getValue(Integer.class) != null) {
                            totalQuestions = quizResultsSnapshot.child("totalQuestions").getValue(Integer.class);
                        }
                        user.setTotalQuestions(totalQuestions);
                        usersList.add(user);
                    } else {
                        Log.e(TAG, "User data is null for snapshot: " + userSnapshot.getKey());
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    usersList.sort((u1, u2) -> {
                        int scoreComparison = Integer.compare(u2.getScore(), u1.getScore());
                        if (scoreComparison != 0) {
                            return scoreComparison;
                        }
                        return Integer.compare(u1.getTotalQuestions(), u2.getTotalQuestions());
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

                adapter.notifyDataSetChanged();

                binding.loadingUserProgressBar.setVisibility(View.GONE);
                stopLoadingAnimation();
                binding.leaderboardRecyclerView.setVisibility(View.VISIBLE);
                binding.userDetailsCard.setVisibility(View.VISIBLE);

                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser != null) {
                    String currentUserId = currentUser.getUid();
                    Users loggedInUser = null;
                    for (Users user : usersList) {
                        if (user.getId() != 0 && String.valueOf(user.getId()).equals(currentUserId)) {
                            loggedInUser = user;
                            break;
                        }
                    }
                    if (loggedInUser == null) {
                        for (int i = 0; i < usersList.size(); i++) {
                            DataSnapshot userSnapshot = null;
                            if (usersList.get(i).getName().equals(currentUser.getDisplayName())) {
                                loggedInUser = usersList.get(i);
                                break;
                            }
                        }
                    }

                    if (loggedInUser != null) {
                        if (isAdded() && getContext() != null) {
                            Glide.with(requireContext())
                                    .load(currentUser.getPhotoUrl() != null ? currentUser.getPhotoUrl().toString() : R.drawable.user)
                                    .circleCrop()
                                    .into(binding.userBottomImg);
                            binding.userBottomName.setText(Objects.requireNonNullElse(currentUser.getDisplayName(), "User"));
                            binding.userBottomScore.setText("Score: " + loggedInUser.getScore());
                            binding.userBottomRank.setText("#" + loggedInUser.getRank());
                            binding.userBottomLevel.setText("LVL " + loggedInUser.getLevel());
                        }
                    } else {
                        fetchCurrentUserDetails(currentUser);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error: " + error.getMessage());
                if (isAdded() && getContext() != null && binding != null) {
                    binding.loadingUserProgressBar.setVisibility(View.GONE);
                    stopLoadingAnimation();
                    binding.userDetailsCard.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void fetchCurrentUserDetails(FirebaseUser currentUser) {
        if (currentUser == null || !isAdded() || getContext() == null) return;
        String userId = currentUser.getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || getContext() == null || binding == null) return;

                if (snapshot.exists()) {
                    Integer score = snapshot.child("score").getValue(Integer.class);
                    int userScore = (score != null) ? score : 0;
                    int userLevel = calculateUserLevel(userScore);

                    Glide.with(requireContext())
                            .load(currentUser.getPhotoUrl() != null ? currentUser.getPhotoUrl().toString() : R.drawable.user)
                            .circleCrop()
                            .into(binding.userBottomImg);
                    binding.userBottomName.setText(Objects.requireNonNullElse(currentUser.getDisplayName(), "User"));
                    binding.userBottomScore.setText("Score: " + userScore);
                    binding.userBottomLevel.setText("LVL " + userLevel);
                    binding.userBottomRank.setText("Rank: N/A");
                } else {
                    Log.d(TAG, "Current user's data not found for separate fetch.");
                    binding.userBottomName.setText(Objects.requireNonNullElse(currentUser.getDisplayName(), "User"));
                    binding.userBottomScore.setText("Score: 0");
                    binding.userBottomLevel.setText("LVL 1");
                    binding.userBottomRank.setText("Rank: N/A");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error fetching current user details: " + error.getMessage());
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopLoadingAnimation();
        binding = null;
    }
}