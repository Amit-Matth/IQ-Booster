package com.amitmatth.iqbooster.fragments;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.amitmatth.iqbooster.R;
import com.amitmatth.iqbooster.activities.MainActivity;
import com.amitmatth.iqbooster.databinding.FragmentQuizResultsBinding;

public class QuizResultsFragment extends Fragment {

    private static final String ARG_TOTAL_QUESTIONS = "totalQuestions";
    private static final String ARG_CORRECT_ANSWERS = "correctAnswers";
    private static final String ARG_WRONG_ANSWERS = "wrongAnswers";
    private static final String ARG_SKIPPED_ANSWERS = "skippedAnswers";
    private static final String ARG_FINAL_SCORE = "finalScore";
    private static final String ARG_CATEGORY = "category";
    private static final String ARG_MARKS_PER_CORRECT = "marksPerCorrect";
    private static final String ARG_MARKS_PER_WRONG = "marksPerWrong";

    private FragmentQuizResultsBinding binding;
    private ObjectAnimator progressBarAnimator;

    public static QuizResultsFragment newInstance(int totalQuestions, int correctAnswers, int wrongAnswers, int skippedAnswers, int finalScore, String category, int marksPerCorrect, int marksPerWrong) {
        QuizResultsFragment fragment = new QuizResultsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_TOTAL_QUESTIONS, totalQuestions);
        args.putInt(ARG_CORRECT_ANSWERS, correctAnswers);
        args.putInt(ARG_WRONG_ANSWERS, wrongAnswers);
        args.putInt(ARG_SKIPPED_ANSWERS, skippedAnswers);
        args.putInt(ARG_FINAL_SCORE, finalScore);
        args.putString(ARG_CATEGORY, category);
        args.putInt(ARG_MARKS_PER_CORRECT, marksPerCorrect);
        args.putInt(ARG_MARKS_PER_WRONG, marksPerWrong);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setFullScreenMode(true);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setFullScreenMode(false);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentQuizResultsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (binding != null && binding.quizResultsProgressBar.getVisibility() == View.VISIBLE) {
            startLoadingAnimation();
        } else if (binding != null) {
            binding.quizResultsProgressBar.setVisibility(View.GONE);
        }

        if (getArguments() != null) {
            int totalQuestions = getArguments().getInt(ARG_TOTAL_QUESTIONS);
            int correctAnswers = getArguments().getInt(ARG_CORRECT_ANSWERS);
            int wrongAnswers = getArguments().getInt(ARG_WRONG_ANSWERS);
            int skippedAnswers = getArguments().getInt(ARG_SKIPPED_ANSWERS);
            int finalScore = getArguments().getInt(ARG_FINAL_SCORE);
            int marksPerCorrect = getArguments().getInt(ARG_MARKS_PER_CORRECT);
            int marksPerWrong = getArguments().getInt(ARG_MARKS_PER_WRONG);

            binding.totalQuestionsSummaryTextView.setText("Total Data Points Analyzed: " + totalQuestions);
            binding.correctQuestionsSummaryTextView.setText("Successful Computations: " + correctAnswers);
            binding.wrongQuestionsSummaryTextView.setText("System Errors: " + wrongAnswers);
            binding.skippedQuestionsSummaryTextView.setText("Bypassed Nodes: " + skippedAnswers);

            int scoreFromCorrectAnswers = correctAnswers * marksPerCorrect;
            int penaltyFromWrongAnswers = wrongAnswers * marksPerWrong;

            binding.scoreFromCorrectTextView.setText(String.format("Correct Score: %d x %d = %d", correctAnswers, marksPerCorrect, scoreFromCorrectAnswers));
            binding.penaltyFromWrongTextView.setText(String.format("Wrong Penalty: %d x %d = %d", wrongAnswers, marksPerWrong, penaltyFromWrongAnswers));
            binding.finalScoreSummaryTextView.setText(String.valueOf(finalScore));
        }

        binding.mainMenuButton.setOnClickListener(v -> {
            FragmentManager fm = getParentFragmentManager();
            fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        });
    }

    private void startLoadingAnimation() {
        if (binding.quizResultsProgressBar != null) {
            binding.quizResultsProgressBar.setVisibility(View.VISIBLE);
            progressBarAnimator = ObjectAnimator.ofFloat(binding.quizResultsProgressBar, "rotation", 0f, 360f);
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
        if (binding != null && binding.quizResultsProgressBar != null) {
            binding.quizResultsProgressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopLoadingAnimation();
        binding = null;
    }
}