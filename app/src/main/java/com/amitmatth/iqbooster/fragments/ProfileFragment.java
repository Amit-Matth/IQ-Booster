package com.amitmatth.iqbooster.fragments;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.amitmatth.iqbooster.R;
import com.amitmatth.iqbooster.databinding.FragmentProfileBinding;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private FirebaseAuth mAuth;
    private DatabaseReference database;

    private ObjectAnimator outerRingAnimator;
    private ObjectAnimator innerRingAnimator;
    private ObjectAnimator progressBarAnimator;
    private ObjectAnimator outerRingRotationAnimator;

    private static final int[] LEVEL_THRESHOLDS = {
            0,    // Level 1
            100,  // Level 2
            250,  // Level 3
            500,  // Level 4
            800,  // Level 5
            1200, // Level 6
            1700, // Level 7
            2300, // Level 8
            3000, // Level 9
            4000, // Level 10
            5200, // Level 11
            6600, // Level 12
            8200, // Level 13
            10000,// Level 14
            12000,// Level 15
            14500,// Level 16
            17500,// Level 17
            21000,// Level 18
            25000,// Level 19
            30000 // Level 20
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference();

        if (binding != null) {
            binding.profileProgressBar.setVisibility(View.VISIBLE);
            binding.profileView.setVisibility(View.GONE);
        }

        fetchUserDetails();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (binding != null) {
            startFragmentAnimations();
        }
    }

    private void startFragmentAnimations() {
        if (binding.avatarOuterRing != null) {
            outerRingAnimator = ObjectAnimator.ofFloat(binding.avatarOuterRing, "alpha", 0.3f, 1.0f);
            outerRingAnimator.setDuration(2000);
            outerRingAnimator.setRepeatMode(ValueAnimator.REVERSE);
            outerRingAnimator.setRepeatCount(ValueAnimator.INFINITE);
            outerRingAnimator.start();

            outerRingRotationAnimator = ObjectAnimator.ofFloat(binding.avatarOuterRing, "rotation", 0f, 360f);
            outerRingRotationAnimator.setDuration(7000);
            outerRingRotationAnimator.setRepeatCount(ValueAnimator.INFINITE);
            outerRingRotationAnimator.setRepeatMode(ValueAnimator.RESTART);
            outerRingRotationAnimator.setInterpolator(new LinearInterpolator());
            outerRingRotationAnimator.start();
        }

        if (binding.avatarInnerRing != null) {
            innerRingAnimator = ObjectAnimator.ofFloat(binding.avatarInnerRing, "alpha", 0.3f, 1.0f);
            innerRingAnimator.setDuration(1700);
            innerRingAnimator.setStartDelay(300);
            innerRingAnimator.setRepeatMode(ValueAnimator.REVERSE);
            innerRingAnimator.setRepeatCount(ValueAnimator.INFINITE);
            innerRingAnimator.start();
        }

        if (binding.profileProgressBar != null && binding.profileProgressBar.getVisibility() == View.VISIBLE) {
            progressBarAnimator = ObjectAnimator.ofFloat(binding.profileProgressBar, "rotation", 0f, 360f);
            progressBarAnimator.setDuration(1000);
            progressBarAnimator.setRepeatCount(ValueAnimator.INFINITE);
            progressBarAnimator.setRepeatMode(ValueAnimator.RESTART);
            progressBarAnimator.setInterpolator(new LinearInterpolator());
            progressBarAnimator.start();
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

    private void fetchUserDetails() {
        if (binding != null && binding.profileProgressBar != null && binding.profileProgressBar.getVisibility() == View.VISIBLE) {
            if (progressBarAnimator == null || !progressBarAnimator.isRunning()) {
                startFragmentAnimations();
            }
        }

        if (mAuth.getCurrentUser() == null) {
            Log.e("ProfileFragment", "User not logged in.");
            if (binding != null) {
                binding.profileProgressBar.setVisibility(View.GONE);
                if (progressBarAnimator != null) progressBarAnimator.cancel();
                binding.profileView.setVisibility(View.VISIBLE);
                if (isAdded() && getContext() != null) {
                    binding.nameTextView.setText("Not Logged In");
                    binding.emailTextView.setText("");
                    binding.userLevelTextView.setText("Level: N/A");
                    binding.totalScoreTextView.setText("0");
                    binding.totalQuestionsTextView.setText("0");
                    binding.totalCorrectQuestionsTextView.setText("0");
                    binding.totalWrongQuestionsTextView.setText("0");
                    binding.totalQuizzesTextView.setText("0");
                    binding.totalSkippedQuestionsTextView.setText("0");
                    Glide.with(this).load(R.mipmap.ic_launcher_round).circleCrop().into(binding.userProfileImage);
                }
            }
            return;
        }
        String userId = mAuth.getCurrentUser().getUid();

        database.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || getContext() == null || binding == null) {
                    return;
                }

                binding.profileProgressBar.setVisibility(View.GONE);
                if (progressBarAnimator != null) progressBarAnimator.cancel();
                binding.profileView.setVisibility(View.VISIBLE);

                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);
                    String photoUrl = snapshot.child("photoUrl").getValue(String.class);
                    Integer storedUserLevel = snapshot.child("userLevel").getValue(Integer.class);

                    int score = snapshot.child("score").getValue(Integer.class) != null
                            ? snapshot.child("score").getValue(Integer.class) : 0;
                    int correct = snapshot.child("quizResults").child("correct").getValue(Integer.class) != null
                            ? snapshot.child("quizResults").child("correct").getValue(Integer.class) : 0;
                    int wrong = snapshot.child("quizResults").child("wrong").getValue(Integer.class) != null
                            ? snapshot.child("quizResults").child("wrong").getValue(Integer.class) : 0;
                    int totalQuizzes = snapshot.child("quizResults").child("totalQuizzes").getValue(Integer.class) != null
                            ? snapshot.child("quizResults").child("totalQuizzes").getValue(Integer.class) : 0;
                    int skipped = snapshot.child("quizResults").child("skipped").getValue(Integer.class) != null
                            ? snapshot.child("quizResults").child("skipped").getValue(Integer.class) : 0;
                    int totalQuestions = correct + wrong + skipped;

                    binding.nameTextView.setText(name != null ? name : "N/A");
                    binding.emailTextView.setText(email != null ? email : "No Email");
                    binding.totalScoreTextView.setText(String.valueOf(score));
                    binding.totalQuestionsTextView.setText(String.valueOf(totalQuestions));
                    binding.totalCorrectQuestionsTextView.setText(String.valueOf(correct));
                    binding.totalWrongQuestionsTextView.setText(String.valueOf(wrong));
                    binding.totalQuizzesTextView.setText(String.valueOf(totalQuizzes));
                    binding.totalSkippedQuestionsTextView.setText(String.valueOf(skipped));

                    int calculatedUserLevel = calculateUserLevel(score);
                    binding.userLevelTextView.setText("Level: " + calculatedUserLevel);

                    if (storedUserLevel == null || calculatedUserLevel > storedUserLevel) {
                        database.child("users").child(userId).child("userLevel").setValue(calculatedUserLevel)
                                .addOnSuccessListener(aVoid -> Log.d("ProfileFragment", "User level updated successfully."))
                                .addOnFailureListener(e -> Log.e("ProfileFragment", "Failed to update user level.", e));
                    }

                    if (getActivity() != null) {
                        Glide.with(getActivity())
                                .load(photoUrl != null ? photoUrl : R.mipmap.ic_launcher_round)
                                .circleCrop()
                                .placeholder(R.mipmap.ic_launcher_round)
                                .error(R.mipmap.ic_launcher_round)
                                .into(binding.userProfileImage);
                    }
                } else {
                    binding.nameTextView.setText("User Data Not Found");
                    binding.emailTextView.setText("");
                    binding.userLevelTextView.setText("Level: N/A");
                    if (getActivity() != null) {
                        Glide.with(getActivity()).load(R.mipmap.ic_launcher_round).circleCrop().into(binding.userProfileImage);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded() && binding != null) {
                    Log.e("ProfileFragment", "Failed to fetch user data: " + error.getMessage());
                    binding.profileProgressBar.setVisibility(View.GONE);
                    if (progressBarAnimator != null) progressBarAnimator.cancel();
                    binding.profileView.setVisibility(View.VISIBLE);
                    binding.nameTextView.setText("Error loading data");
                    if (getActivity() != null) {
                        Glide.with(getActivity()).load(R.mipmap.ic_launcher_round).circleCrop().into(binding.userProfileImage);
                    }
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (outerRingAnimator != null) {
            outerRingAnimator.cancel();
        }
        if (innerRingAnimator != null) {
            innerRingAnimator.cancel();
        }
        if (progressBarAnimator != null) {
            progressBarAnimator.cancel();
        }
        if (outerRingRotationAnimator != null) {
            outerRingRotationAnimator.cancel();
        }
        binding = null;
    }
}