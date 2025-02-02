package com.amitmatth.iqbooster.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

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

import java.util.Objects;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;

    private FirebaseAuth mAuth;
    private DatabaseReference database;

    private ImageView profileImage;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference();
        database.keepSynced(true);

        binding.profileProgressBar.setVisibility(View.VISIBLE);
        binding.profileView.setVisibility(View.GONE);

        fetchUserDetails();

        return binding.getRoot();
    }



    private void fetchUserDetails() {
        String userId = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();

        database.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                // Ensure the fragment is still added and the context is valid
                if (!isAdded() || getContext() == null) {
                    return;
                }

                if (snapshot.exists()) {
                    // Retrieve user details with null safety
                    String name = snapshot.child("name").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);
                    String photoUrl = snapshot.child("photoUrl").getValue(String.class);

                    int score = snapshot.child("score").getValue(Integer.class) != null
                            ? snapshot.child("score").getValue(Integer.class) : 0;

                    int totalQuestions = snapshot.child("quizResults").child("totalQuestions").getValue(Integer.class) != null
                            ? snapshot.child("quizResults").child("totalQuestions").getValue(Integer.class) : 0;

                    int correct = snapshot.child("quizResults").child("correct").getValue(Integer.class) != null
                            ? snapshot.child("quizResults").child("correct").getValue(Integer.class) : 0;

                    int wrong = snapshot.child("quizResults").child("wrong").getValue(Integer.class) != null
                            ? snapshot.child("quizResults").child("wrong").getValue(Integer.class) : 0;

                    int totalQuizzes = snapshot.child("quizResults").child("totalQuizzes").getValue(Integer.class) != null
                            ? snapshot.child("quizResults").child("totalQuizzes").getValue(Integer.class) : 0;

                    int skipped = snapshot.child("quizResults").child("skipped").getValue(Integer.class) != null
                            ? snapshot.child("quizResults").child("skipped").getValue(Integer.class) : 0;

                    // Update UI based on the fetched data
                    if (name.isEmpty()){
                        binding.profileProgressBar.setVisibility(View.VISIBLE);
                        binding.profileView.setVisibility(View.GONE);
                    } else {
                        binding.profileProgressBar.setVisibility(View.GONE);
                        binding.profileView.setVisibility(View.VISIBLE);
                    }

                    // Safely update UI elements
                    if (isAdded()) {
                        binding.name.setText(name);
                        binding.email.setText(email != null ? email : "No Email");
                        binding.totalScore.setText("Your score is: " + score);
                        binding.totalQuestions.setText(String.valueOf(totalQuestions));
                        binding.totalCorrectQuestions.setText(String.valueOf(correct));
                        binding.totalWrongQuestions.setText(String.valueOf(wrong));
                        binding.totalQuizzes.setText(String.valueOf(totalQuizzes));
                        binding.totalSkippedQuestions.setText(String.valueOf(skipped));

                        Glide.with(requireActivity())
                                .load(photoUrl != null ? photoUrl : R.drawable.user)
                                .circleCrop()
                                .into(binding.userprofile);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Ensure fragment is still attached before logging the error
                if (isAdded()) {
                    Log.e("ProfileFragment", "Failed to fetch user data: " + error.getMessage());
                }
            }
        });
    }


}
