package com.amitmatth.iqbooster.fragments;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.amitmatth.iqbooster.R;
import com.amitmatth.iqbooster.activities.FullscreenImageActivity;
import com.amitmatth.iqbooster.adapter.BadgeAdapter;
import com.amitmatth.iqbooster.adapter.LevelAdapter;
import com.amitmatth.iqbooster.databinding.DialogItemDisplayBinding;
import com.amitmatth.iqbooster.databinding.FragmentProgressBinding;
import com.amitmatth.iqbooster.model.Badge;
import com.amitmatth.iqbooster.model.Level;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProgressFragment extends Fragment implements LevelAdapter.OnLevelItemClickListener, BadgeAdapter.OnBadgeItemClickListener {

    private FragmentProgressBinding binding;
    private FirebaseAuth mAuth;
    private DatabaseReference database;

    private BadgeAdapter badgeAdapter;
    private List<Badge> allBadgesList;
    private List<Badge> badgeDisplayList;

    private LevelAdapter levelAdapter;
    private List<Level> allLevelsList;
    private List<Level> levelDisplayList;

    private ObjectAnimator progressBarAnimator;
    private ObjectAnimator dialogOuterRingAlphaAnimator;
    private ObjectAnimator dialogOuterRingRotationAnimator;
    private ObjectAnimator dialogInnerRingAlphaAnimator;

    private static final int MAX_LEVEL = 20;
    private static final int[] LEVEL_THRESHOLDS = {
            0, 100, 250, 500, 800, 1200, 1700, 2300, 3000, 4000,
            5200, 6600, 8200, 10000, 12000, 14500, 17500, 21000, 25000, 30000
    };

    private ActivityResultLauncher<String> requestPermissionLauncher;
    private int pendingDownloadImageResId;
    private String pendingDownloadImageName;

    private ProgressBar currentDialogProgressBar;
    private MaterialButton currentDialogDownloadButton;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                if (pendingDownloadImageResId != 0 && pendingDownloadImageName != null) {
                    saveImageToGallery(pendingDownloadImageResId, pendingDownloadImageName);
                } else {
                    resetDialogDownloadUI();
                }
            } else {
                Toast.makeText(getContext(), "Storage permission denied. Cannot save image.", Toast.LENGTH_SHORT).show();
                resetDialogDownloadUI();
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProgressBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference();

        initializeAllBadgesList();
        initializeAllLevelsList();

        setupBadgesRecyclerView();
        setupLevelsRecyclerView();

        if (binding != null) {
            binding.progressFragmentProgressBar.setVisibility(View.VISIBLE);
            binding.progressFragmentNestedScroll.setVisibility(View.GONE);
            binding.levelsRecyclerView.setNestedScrollingEnabled(false);
            binding.badgesRecyclerView.setNestedScrollingEnabled(false);
        }

        fetchProgressData();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (binding != null && binding.progressFragmentProgressBar.getVisibility() == View.VISIBLE) {
            startLoadingAnimation();
        }
    }

    private void startLoadingAnimation() {
        if (binding.progressFragmentProgressBar != null) {
            progressBarAnimator = ObjectAnimator.ofFloat(binding.progressFragmentProgressBar, "rotation", 0f, 360f);
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

    private void initializeAllBadgesList() {
        allBadgesList = new ArrayList<>();
        allBadgesList.add(new Badge("Quiz Novice", "Answer 10 questions correctly", R.drawable.ques_10, R.drawable.ic_lock, 10));
        allBadgesList.add(new Badge("Quiz Adept", "Answer 30 questions correctly", R.drawable.ques_30, R.drawable.ic_lock, 30));
        allBadgesList.add(new Badge("Quiz Expert", "Answer 60 questions correctly", R.drawable.ques_60, R.drawable.ic_lock, 60));
        allBadgesList.add(new Badge("Quiz Master", "Answer 100 questions correctly", R.drawable.ques_100, R.drawable.ic_lock, 100));
        allBadgesList.add(new Badge("Quiz Champion", "Answer 200 questions correctly", R.drawable.ques_200, R.drawable.ic_lock, 200));
        allBadgesList.add(new Badge("Quiz Legend", "Answer 300 questions correctly", R.drawable.ques_300, R.drawable.ic_lock, 300));
        allBadgesList.add(new Badge("Quiz Grandmaster", "Answer 500 questions correctly", R.drawable.ques_500, R.drawable.ic_lock, 500));
        allBadgesList.add(new Badge("Quiz God", "Answer 1000 questions correctly", R.drawable.ques_1000, R.drawable.ic_lock, 1000));
    }

    private void initializeAllLevelsList() {
        allLevelsList = new ArrayList<>();
        if (getContext() == null) return;
        Resources resources = getResources();
        String packageName = requireContext().getPackageName();

        for (int i = 0; i < MAX_LEVEL; i++) {
            String levelName = "Level " + (i + 1);
            int requiredScore = LEVEL_THRESHOLDS[i];
            @SuppressLint("DiscouragedApi") int earnedDrawableResId = resources.getIdentifier("lvl_" + (i + 1), "drawable", packageName);
            if (earnedDrawableResId == 0) {
                earnedDrawableResId = R.drawable.lvl_0;
            }
            allLevelsList.add(new Level(i + 1, levelName, requiredScore, earnedDrawableResId, R.drawable.ic_lock));
        }
    }

    private void setupBadgesRecyclerView() {
        if (binding == null || getContext() == null) return;
        badgeDisplayList = new ArrayList<>();
        badgeAdapter = new BadgeAdapter(requireContext(), badgeDisplayList);
        badgeAdapter.setOnItemClickListener(this);
        binding.badgesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.badgesRecyclerView.setAdapter(badgeAdapter);
        binding.badgesRecyclerView.setNestedScrollingEnabled(false);
    }

    private void setupLevelsRecyclerView() {
        if (binding == null || getContext() == null) return;
        levelDisplayList = new ArrayList<>();
        levelAdapter = new LevelAdapter(requireContext(), levelDisplayList);
        levelAdapter.setOnItemClickListener(this);
        binding.levelsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.levelsRecyclerView.setAdapter(levelAdapter);
        binding.levelsRecyclerView.setNestedScrollingEnabled(false);
    }

    private int calculateUserLevel(int score) {
        for (int i = LEVEL_THRESHOLDS.length - 1; i >= 0; i--) {
            if (score >= LEVEL_THRESHOLDS[i]) {
                return i + 1;
            }
        }
        return 1;
    }

    @SuppressLint("NotifyDataSetChanged")
    private void updateBadgesUI(int correctAnswers) {
        if (binding == null || allBadgesList == null || badgeDisplayList == null || badgeAdapter == null)
            return;
        badgeDisplayList.clear();

        for (Badge badge : allBadgesList) {
            badge.setCurrentProgress(Math.min(correctAnswers, badge.getRequiredCorrectAnswers()));
            badge.setUnlocked(correctAnswers >= badge.getRequiredCorrectAnswers());
            badgeDisplayList.add(badge);
        }

        badgeAdapter.updateBadgeDisplayList(badgeDisplayList);
        binding.achievementsCardView.setVisibility(badgeDisplayList.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void updateLevelsUI(int score, int currentLevel) {
        if (binding == null || allLevelsList == null || levelDisplayList == null || levelAdapter == null)
            return;
        levelDisplayList.clear();

        for (Level level : allLevelsList) {
            level.setUnlocked(score >= level.getRequiredScore());
            level.setUserScoreForThisLevel(score);
            if (level.getLevelNumber() == 1) {
                level.setPreviousLevelScoreRequirement(0);
            } else {
                if (level.getLevelNumber() - 2 >= 0 && level.getLevelNumber() - 2 < LEVEL_THRESHOLDS.length) {
                    level.setPreviousLevelScoreRequirement(LEVEL_THRESHOLDS[level.getLevelNumber() - 2]);
                } else {
                    level.setPreviousLevelScoreRequirement(0);
                }
            }
            levelDisplayList.add(level);
        }

        levelAdapter.updateLevelDisplayList(levelDisplayList);

        if (currentLevel < MAX_LEVEL) {
            binding.nextLevelProgressTitleTextView.setVisibility(View.VISIBLE);
            binding.nextLevelProgressBar.setVisibility(View.VISIBLE);
            binding.nextLevelScoreTextView.setVisibility(View.VISIBLE);
            int currentLevelScoreThreshold = (currentLevel > 0 && currentLevel - 1 < LEVEL_THRESHOLDS.length) ? LEVEL_THRESHOLDS[currentLevel - 1] : 0;
            int nextLevelScoreThreshold = (currentLevel < LEVEL_THRESHOLDS.length) ? LEVEL_THRESHOLDS[currentLevel] : currentLevelScoreThreshold;

            int progress = 0;
            if (nextLevelScoreThreshold > currentLevelScoreThreshold) {
                progress = (int) (((double) (score - currentLevelScoreThreshold) / (nextLevelScoreThreshold - currentLevelScoreThreshold)) * 100);
            }
            binding.nextLevelProgressBar.setProgress(Math.max(0, Math.min(progress, 100)));
            binding.nextLevelScoreTextView.setText(score + " / " + nextLevelScoreThreshold + " points");
        } else {
            binding.nextLevelProgressTitleTextView.setVisibility(View.VISIBLE);
            binding.nextLevelProgressBar.setVisibility(View.VISIBLE);
            binding.nextLevelScoreTextView.setVisibility(View.VISIBLE);
            binding.nextLevelProgressBar.setProgress(100);
            binding.nextLevelScoreTextView.setText("Max Level Reached!");
        }
        binding.levelProgressionCardView.setVisibility(levelDisplayList.isEmpty() && currentLevel >= MAX_LEVEL ? View.GONE : View.VISIBLE);
    }

    private void fetchProgressData() {
        if (binding != null && binding.progressFragmentProgressBar.getVisibility() == View.VISIBLE) {
            startLoadingAnimation();
        }

        if (mAuth.getCurrentUser() == null) {
            Log.e("ProgressFragment", "User not logged in.");
            if (binding != null && isAdded() && getContext() != null) {
                binding.progressFragmentProgressBar.setVisibility(View.GONE);
                stopLoadingAnimation();
                binding.progressFragmentNestedScroll.setVisibility(View.VISIBLE);
                updateBadgesUI(0);
                updateLevelsUI(0, 1);
                binding.nextLevelProgressTitleTextView.setText("Complete a quiz to start earning points!");
                binding.nextLevelProgressBar.setProgress(0);
                binding.nextLevelScoreTextView.setText("0 / " + (LEVEL_THRESHOLDS.length > 0 ? LEVEL_THRESHOLDS[0] : 0) + " points");
            }
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        database.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || getContext() == null || binding == null) return;

                binding.progressFragmentProgressBar.setVisibility(View.GONE);
                stopLoadingAnimation();
                binding.progressFragmentNestedScroll.setVisibility(View.VISIBLE);

                int score = 0;
                if (snapshot.child("score").exists() && snapshot.child("score").getValue(Integer.class) != null) {
                    score = snapshot.child("score").getValue(Integer.class);
                }

                int correctAnswers = 0;
                DataSnapshot quizResultsSnapshot = snapshot.child("quizResults");
                if (quizResultsSnapshot.exists() && quizResultsSnapshot.child("correct").getValue(Integer.class) != null) {
                    correctAnswers = quizResultsSnapshot.child("correct").getValue(Integer.class);
                }

                int userLevel = calculateUserLevel(score);
                updateBadgesUI(correctAnswers);
                updateLevelsUI(score, userLevel);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded() || getContext() == null || binding == null) return;
                Log.e("ProgressFragment", "Failed to load user progress", error.toException());
                binding.progressFragmentProgressBar.setVisibility(View.GONE);
                stopLoadingAnimation();
                binding.progressFragmentNestedScroll.setVisibility(View.VISIBLE);
                Toast.makeText(getContext(), "Failed to load progress.", Toast.LENGTH_SHORT).show();
                updateBadgesUI(0);
                updateLevelsUI(0, 1);
                binding.nextLevelProgressTitleTextView.setText("Could not load data.");
                binding.nextLevelProgressBar.setProgress(0);
                binding.nextLevelScoreTextView.setText("");
            }
        });
    }

    @Override
    public void onLevelItemClicked(Level level) {
        if (!isAdded() || getContext() == null) return;
        showItemDialog(level.getName(), "Required Total Score: " + level.getRequiredScore(), level.isUnlocked() ? level.getEarnedDrawableResId() : level.getLockedDrawableResId(), level.isUnlocked());
    }

    @Override
    public void onBadgeItemClicked(Badge badge) {
        if (!isAdded() || getContext() == null) return;
        showItemDialog(badge.getName(), badge.getDescription(), badge.isUnlocked() ? badge.getEarnedDrawableResId() : badge.getLockedDrawableResId(), badge.isUnlocked());
    }

    private void startDialogRingAnimations(DialogItemDisplayBinding dialogBinding) {
        if (dialogBinding.dialogOuterGlowRing != null) {
            dialogOuterRingAlphaAnimator = ObjectAnimator.ofFloat(dialogBinding.dialogOuterGlowRing, "alpha", 0.3f, 1.0f);
            dialogOuterRingAlphaAnimator.setDuration(2000);
            dialogOuterRingAlphaAnimator.setRepeatMode(ValueAnimator.REVERSE);
            dialogOuterRingAlphaAnimator.setRepeatCount(ValueAnimator.INFINITE);
            dialogOuterRingAlphaAnimator.start();

            dialogOuterRingRotationAnimator = ObjectAnimator.ofFloat(dialogBinding.dialogOuterGlowRing, "rotation", 0f, 360f);
            dialogOuterRingRotationAnimator.setDuration(7000);
            dialogOuterRingRotationAnimator.setRepeatCount(ValueAnimator.INFINITE);
            dialogOuterRingRotationAnimator.setRepeatMode(ValueAnimator.RESTART);
            dialogOuterRingRotationAnimator.setInterpolator(new LinearInterpolator());
            dialogOuterRingRotationAnimator.start();
        }

        if (dialogBinding.dialogInnerGlowRing != null) {
            dialogInnerRingAlphaAnimator = ObjectAnimator.ofFloat(dialogBinding.dialogInnerGlowRing, "alpha", 0.3f, 1.0f);
            dialogInnerRingAlphaAnimator.setDuration(1700);
            dialogInnerRingAlphaAnimator.setStartDelay(300);
            dialogInnerRingAlphaAnimator.setRepeatMode(ValueAnimator.REVERSE);
            dialogInnerRingAlphaAnimator.setRepeatCount(ValueAnimator.INFINITE);
            dialogInnerRingAlphaAnimator.start();
        }
    }

    private void stopDialogRingAnimations() {
        if (dialogOuterRingAlphaAnimator != null) {
            dialogOuterRingAlphaAnimator.cancel();
        }
        if (dialogOuterRingRotationAnimator != null) {
            dialogOuterRingRotationAnimator.cancel();
        }
        if (dialogInnerRingAlphaAnimator != null) {
            dialogInnerRingAlphaAnimator.cancel();
        }
    }

    private void showItemDialog(String name, String description, int imageResId, boolean isUnlocked) {
        if (getContext() == null || getActivity() == null) return;

        DialogItemDisplayBinding dialogBinding = DialogItemDisplayBinding.inflate(LayoutInflater.from(getContext()));
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(dialogBinding.getRoot());

        dialogBinding.dialogItemImageView.setImageResource(imageResId);
        dialogBinding.dialogItemNameTextView.setText(name);
        dialogBinding.dialogItemDescriptionTextView.setText(description);

        currentDialogProgressBar = dialogBinding.dialogDownloadProgressBar;
        currentDialogDownloadButton = dialogBinding.dialogDownloadButton;

        if (isUnlocked) {
            dialogBinding.dialogFullscreenButton.setVisibility(View.VISIBLE);
            dialogBinding.dialogDownloadButton.setVisibility(View.VISIBLE);
        } else {
            dialogBinding.dialogFullscreenButton.setVisibility(View.GONE);
            dialogBinding.dialogDownloadButton.setVisibility(View.GONE);
        }

        final AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        startDialogRingAnimations(dialogBinding);

        dialogBinding.dialogFullscreenButton.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), FullscreenImageActivity.class);
            intent.putExtra(FullscreenImageActivity.EXTRA_IMAGE_RES_ID, imageResId);
            startActivity(intent);
        });

        dialogBinding.dialogDownloadButton.setOnClickListener(v -> {
            pendingDownloadImageResId = imageResId;
            pendingDownloadImageName = name.replaceAll("\\s+", "_") + ".png";

            if (currentDialogProgressBar != null)
                currentDialogProgressBar.setVisibility(View.VISIBLE);
            if (currentDialogDownloadButton != null) {
                currentDialogDownloadButton.setText("");
                currentDialogDownloadButton.setEnabled(false);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveImageToGallery(pendingDownloadImageResId, pendingDownloadImageName);
            } else {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    saveImageToGallery(pendingDownloadImageResId, pendingDownloadImageName);
                } else {
                    requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                }
            }
        });

        dialogBinding.dialogCloseButton.setOnClickListener(v -> dialog.dismiss());

        dialog.setOnDismissListener(d -> {
            resetDialogDownloadUI();
            stopDialogRingAnimations();
        });
        dialog.show();
    }

    private void saveImageToGallery(int imageResId, String imageName) {
        Context appContext = getContext() != null ? getContext().getApplicationContext() : null;
        if (appContext == null || getActivity() == null) {
            mainThreadHandler.post(this::resetDialogDownloadUI);
            return;
        }

        mainThreadHandler.post(() -> {
            if (currentDialogProgressBar != null)
                currentDialogProgressBar.setVisibility(View.VISIBLE);
            if (currentDialogDownloadButton != null) {
                currentDialogDownloadButton.setText("");
                currentDialogDownloadButton.setEnabled(false);
            }
        });

        executorService.execute(() -> {
            Bitmap bitmap = BitmapFactory.decodeResource(appContext.getResources(), imageResId);
            if (bitmap == null) {
                Log.e("ProgressFragment", "Failed to decode bitmap resource.");
                mainThreadHandler.post(() -> Toast.makeText(appContext, "Failed to load image resource.", Toast.LENGTH_SHORT).show());
                mainThreadHandler.post(this::resetDialogDownloadUI);
                return;
            }

            OutputStream fos = null;
            boolean success = false;
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Images.Media.DISPLAY_NAME, imageName);
                    values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                    values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "IQBooster");
                    Uri imageUri = appContext.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                    if (imageUri != null) {
                        fos = appContext.getContentResolver().openOutputStream(imageUri);
                    }
                } else {
                    String imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + File.separator + "IQBooster";
                    File dir = new File(imagesDir);
                    if (!dir.exists() && !dir.mkdirs()) {
                        Log.e("ProgressFragment", "Failed to create directory for saving image.");
                        mainThreadHandler.post(() -> Toast.makeText(appContext, "Error: Could not create directory.", Toast.LENGTH_SHORT).show());
                        mainThreadHandler.post(this::resetDialogDownloadUI);
                        return;
                    }
                    File imageFile = new File(imagesDir, imageName);
                    fos = new FileOutputStream(imageFile);
                }

                if (fos != null) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    success = true;
                }
            } catch (IOException e) {
                Log.e("ProgressFragment", "Error saving image: " + e.getMessage(), e);
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        Log.e("ProgressFragment", "Error closing stream: " + e.getMessage(), e);
                    }
                }
            }

            final boolean finalSuccess = success;
            mainThreadHandler.post(() -> {
                if (finalSuccess) {
                    Toast.makeText(appContext, imageName + " saved to Gallery!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(appContext, "Failed to save image.", Toast.LENGTH_SHORT).show();
                }
                resetDialogDownloadUI();
            });
        });
    }

    private void resetDialogDownloadUI() {
        if (currentDialogProgressBar != null) {
            currentDialogProgressBar.setVisibility(View.GONE);
        }
        if (currentDialogDownloadButton != null) {
            currentDialogDownloadButton.setText("EXTRACT");
            currentDialogDownloadButton.setEnabled(true);
        }
        pendingDownloadImageResId = 0;
        pendingDownloadImageName = null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopLoadingAnimation();
        stopDialogRingAnimations();
        binding = null;
    }
}