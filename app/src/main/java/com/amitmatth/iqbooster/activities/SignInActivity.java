package com.amitmatth.iqbooster.activities;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.amitmatth.iqbooster.R;
import com.amitmatth.iqbooster.databinding.ActivitySignInBinding;
import com.amitmatth.iqbooster.model.QuizResults;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class SignInActivity extends AppCompatActivity {

    private ActivitySignInBinding binding;
    private static final int RC_SIGN_IN = 100;
    private static final String TAG = "GoogleSignIn";
    private FirebaseAuth firebaseAuth;
    private GoogleSignInClient googleSignInClient;
    private ObjectAnimator progressBarAnimator;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        binding.signInWithGoogleBtn.setOnClickListener(v -> signIn());
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            if (binding != null) {
                binding.signInActivity.setVisibility(View.GONE);
                binding.progressBar.setVisibility(View.VISIBLE);
                startLoadingAnimation();
            }
            updateUI(currentUser);
        } else {
            if (binding != null) {
                binding.signInActivity.setVisibility(View.VISIBLE);
                binding.progressBar.setVisibility(View.GONE);
                stopLoadingAnimation();
            }
        }
    }

    private void startLoadingAnimation() {
        if (binding != null && binding.progressBar != null) {
            progressBarAnimator = ObjectAnimator.ofFloat(binding.progressBar, "rotation", 0f, 360f);
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
            progressBarAnimator = null;
        }
    }

    private void signIn() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d(TAG, "firebaseAuthWithGoogle:" + account.getId());
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Log.w(TAG, "Google sign in failed", e);
                Toast.makeText(this, "Sign-in failed. Please try again.", Toast.LENGTH_SHORT).show();
                if (binding != null) {
                    binding.signInActivity.setVisibility(View.VISIBLE);
                    binding.progressBar.setVisibility(View.GONE);
                    stopLoadingAnimation();
                }
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        if (binding != null) {
            binding.progressBar.setVisibility(View.VISIBLE);
            startLoadingAnimation();
            binding.signInActivity.setVisibility(View.GONE);
        }

        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = firebaseAuth.getCurrentUser();
                            updateUI(user);
                        } else {
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            updateUI(null);
                        }
                    }
                });
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("users").child(user.getUid());

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("name", user.getDisplayName());
                    updates.put("email", user.getEmail());
                    if (user.getPhotoUrl() != null) {
                        updates.put("photoUrl", user.getPhotoUrl().toString());
                    } else {
                        updates.put("photoUrl", null);
                    }

                    if (!snapshot.child("score").exists()) {
                        updates.put("score", 0);
                    }

                    if (!snapshot.child("quizResults").exists()) {
                        updates.put("quizResults", new QuizResults(0, 0, 0, 0, 0));
                    }

                    if (!updates.isEmpty()) {
                        userRef.updateChildren(updates).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "User profile updated/initialized.");
                            } else {
                                Log.w(TAG, "Failed to update user profile.", task.getException());
                            }
                            proceedToMainActivity();
                        });
                    } else {
                        proceedToMainActivity();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.w(TAG, "Failed to read user data for initialization check.", error.toException());
                    proceedToMainActivity();
                }
            });
        } else {
            if (binding != null) {
                binding.signInActivity.setVisibility(View.VISIBLE);
                binding.progressBar.setVisibility(View.GONE);
                stopLoadingAnimation();
            }
        }
    }

    private void proceedToMainActivity() {
        stopLoadingAnimation();
        if (binding != null) {
            binding.progressBar.setVisibility(View.GONE);
        }

        SharedPreferences prefs = getSharedPreferences("login", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("flag", true);
        editor.apply();

        Intent mainActivityIntent = new Intent(SignInActivity.this, MainActivity.class);
        mainActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainActivityIntent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLoadingAnimation();
        binding = null;
    }
}