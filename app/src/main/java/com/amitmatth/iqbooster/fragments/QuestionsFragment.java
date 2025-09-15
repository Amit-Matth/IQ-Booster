package com.amitmatth.iqbooster.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.amitmatth.iqbooster.R;
import com.amitmatth.iqbooster.activities.MainActivity;
import com.amitmatth.iqbooster.data.Question;
import com.amitmatth.iqbooster.databinding.FragmentQuestionsBinding;
import com.amitmatth.iqbooster.model.QuizResults;
import com.amitmatth.iqbooster.util.QuestionGenerator;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class QuestionsFragment extends Fragment implements View.OnClickListener {

    private static final String ARG_CATEGORY = "category";
    private static final String ARG_NUM_QUESTIONS = "num_questions";
    private static final String ARG_DIFFICULTY = "difficulty";
    private static final String ARG_TIME_PER_QUESTION = "time_per_question";
    private static final String ARG_MARKS_PER_CORRECT = "marks_per_correct";
    private static final String ARG_MARKS_PER_WRONG = "marks_per_wrong";

    private FragmentQuestionsBinding binding;
    private int currentIndex = 0;
    private int score = 0;

    private CountDownTimer timer;
    private int timePerQuestionInSeconds;
    private int marksPerCorrectAnswer;
    private int marksPerWrongAnswer;
    private List<Question> questions;
    private String currentCategory;
    private int numberOfQuestionsToLoad;
    private String currentDifficulty;

    private FirebaseAuth mAuth;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    private enum AnswerStatus {NOT_ANSWERED, CORRECT, WRONG, SKIPPED}

    private static class QuestionState {
        AnswerStatus status = AnswerStatus.NOT_ANSWERED;
        String selectedOptionText = null;
        int selectedRadioButtonId = -1;
    }

    private Map<Integer, QuestionState> questionStates;

    public static QuestionsFragment newInstance(String category, int numQuestions, String difficulty, int timePerQuestion, int marksCorrect, int marksWrong) {
        QuestionsFragment fragment = new QuestionsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CATEGORY, category);
        args.putInt(ARG_NUM_QUESTIONS, numQuestions);
        args.putString(ARG_DIFFICULTY, difficulty);
        args.putInt(ARG_TIME_PER_QUESTION, timePerQuestion);
        args.putInt(ARG_MARKS_PER_CORRECT, marksCorrect);
        args.putInt(ARG_MARKS_PER_WRONG, marksWrong);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentCategory = getArguments().getString(ARG_CATEGORY, "arithmetic");
            numberOfQuestionsToLoad = getArguments().getInt(ARG_NUM_QUESTIONS, 10);
            currentDifficulty = getArguments().getString(ARG_DIFFICULTY, "Medium");
            timePerQuestionInSeconds = getArguments().getInt(ARG_TIME_PER_QUESTION, 10);
            marksPerCorrectAnswer = getArguments().getInt(ARG_MARKS_PER_CORRECT, 3);
            marksPerWrongAnswer = getArguments().getInt(ARG_MARKS_PER_WRONG, -3);
        } else {
            currentCategory = "arithmetic";
            numberOfQuestionsToLoad = 10;
            currentDifficulty = "Medium";
            timePerQuestionInSeconds = 10;
            marksPerCorrectAnswer = 3;
            marksPerWrongAnswer = -3;
            Log.e("QuestionsFragment", "Arguments not found, using default quiz parameters including scoring.");
        }
        mAuth = FirebaseAuth.getInstance();
        questions = new ArrayList<>();
        questionStates = new HashMap<>();
        loadQuestionsBasedOnCategoryAndDifficulty(currentCategory, numberOfQuestionsToLoad, currentDifficulty);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            mainActivity.setFullScreenMode(true);
        }
        if (!questions.isEmpty() && currentIndex < questions.size()) {
            QuestionState state = questionStates.get(currentIndex);
            if (state != null && state.status != AnswerStatus.NOT_ANSWERED) {
                restoreAnsweredState(state, questions.get(currentIndex));
            } else if (state != null) {
                startTimer();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopTimer();
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            mainActivity.setFullScreenMode(false);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentQuestionsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showExitDialog();
            }
        });

        initComponents();
    }

    private void loadQuestionsBasedOnCategoryAndDifficulty(String category, int numQuestionsToSelect, String difficulty) {
        this.questions.clear();
        this.questionStates.clear();

        List<Question> generatedQuestions = QuestionGenerator.generateQuestions(category, numQuestionsToSelect, difficulty);

        if (generatedQuestions != null && !generatedQuestions.isEmpty()) {
            for (int i = 0; i < generatedQuestions.size(); i++) {
                this.questions.add(generatedQuestions.get(i));
                this.questionStates.put(i, new QuestionState());
            }
            Log.i("QuestionLoading", "Loaded " + generatedQuestions.size() + " questions from generator for category: " + category);
        } else {
            Log.e("QuestionLoading", "QuestionGenerator returned null or empty list for category: " + category);
            if (getContext() != null) {
                Toast.makeText(getContext(), "Could not generate questions for: " + category, Toast.LENGTH_LONG).show();
            }
        }

        if (this.questions.isEmpty() && numQuestionsToSelect > 0) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "No questions available for this category at the moment.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void showExitDialog() {
        if (getContext() == null || !isAdded()) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_custom_layout, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        MaterialButton btnExit = dialogView.findViewById(R.id.btnExit);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnExit.setOnClickListener(v -> {
            dialog.dismiss();
            FragmentManager fm = getParentFragmentManager();
            fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        });
        dialog.show();
    }

    private void initComponents() {
        binding.option1.setOnClickListener(this);
        binding.option2.setOnClickListener(this);
        binding.option3.setOnClickListener(this);
        binding.option4.setOnClickListener(this);

        binding.nextQuestionButton.setOnClickListener(v -> handleNextQuestion());
        binding.previousQuestionButton.setOnClickListener(v -> handlePreviousQuestion());
        binding.leaveQuizButton.setOnClickListener(v -> showExitDialog());
        binding.explainAnswerButton.setOnClickListener(v -> showAnswerExplanation());

        if (!questions.isEmpty()) {
            displayQuestion(currentIndex);
        } else {
            Toast.makeText(getContext(), "No questions loaded. Please try setup again or select another category.", Toast.LENGTH_LONG).show();
            if (isAdded() && getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        }
    }

    private void showAnswerExplanation() {
        if (currentIndex >= 0 && currentIndex < questions.size()) {
            Question currentQuestion = questions.get(currentIndex);
            QuestionState currentQuestionState = questionStates.get(currentIndex);

            if (currentQuestion != null && currentQuestionState != null) {
                String questionText = currentQuestion.getQuestionText();
                String correctAnswer = currentQuestion.getCorrectAnswer();
                String userAnswer = currentQuestionState.selectedOptionText != null ? currentQuestionState.selectedOptionText : "Not Answered";

                AnswerExplanationFragment explanationFragment = AnswerExplanationFragment.newInstance(questionText, correctAnswer, userAnswer);
                FragmentManager fragmentManager = getParentFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.container, explanationFragment);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
            } else {
                Log.e("ShowExplanation", "currentQuestion or currentQuestionState is null. Index: " + currentIndex);
                Toast.makeText(getContext(), "Error: Could not retrieve question details for explanation.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getContext(), "Error: No question selected for explanation.", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleNextQuestion() {
        if (currentIndex < questions.size() - 1) {
            currentIndex++;
            displayQuestion(currentIndex);
        } else {
            submitQuiz();
        }
    }

    private void handlePreviousQuestion() {
        if (currentIndex > 0) {
            currentIndex--;
            displayQuestion(currentIndex);
        }
    }

    @SuppressLint("SetTextI18n")
    private void displayQuestion(int index) {
        if (binding == null || questions == null || index < 0 || index >= questions.size()) {
            Log.e("DisplayQuestionError", "Binding null or invalid index/questions list. Index: " + index);
            if (questions != null)
                Log.e("DisplayQuestionError", "Questions size: " + questions.size());
            return;
        }
        Question question = questions.get(index);
        QuestionState state = questionStates.get(index);

        if (question == null || state == null) {
            Log.e("DisplayQuestionError", "Question or its state is null at index: " + index);
            return;
        }

        binding.questionTextView.setText(String.format("Q%d. %s", index + 1, question.getQuestionText()));
        binding.option1.setText(question.getOption1());
        binding.option2.setText(question.getOption2());
        binding.option3.setText(question.getOption3());
        binding.option4.setText(question.getOption4());

        resetOptionViews();
        binding.answerFeedbackTextView.setVisibility(View.GONE);
        binding.explainAnswerButton.setVisibility(View.GONE);

        if (state.status != AnswerStatus.NOT_ANSWERED) {
            restoreAnsweredState(state, question);
            stopTimer();
        } else {
            enableOptions();
            startTimer();
        }

        binding.previousQuestionButton.setVisibility(index > 0 ? View.VISIBLE : View.INVISIBLE);
        binding.nextQuestionButton.setText(index == questions.size() - 1 ? "SUBMIT QUIZ" : "NEXT");
    }

    private void restoreAnsweredState(QuestionState state, Question question) {
        if (binding == null || getView() == null || getContext() == null || !isAdded()) return;
        disableOptions();
        binding.explainAnswerButton.setVisibility(View.VISIBLE);

        if (state.selectedRadioButtonId != -1) {
            RadioButton selectedButton = getView().findViewById(state.selectedRadioButtonId);
            if (selectedButton != null) {
                selectedButton.setChecked(true);
            }
        }

        int correctBgResource = R.drawable.option_correct_bg;
        int wrongBgResource = R.drawable.option_wrong_bg;

        binding.option1.setBackgroundResource(R.drawable.option_background_selector);
        binding.option2.setBackgroundResource(R.drawable.option_background_selector);
        binding.option3.setBackgroundResource(R.drawable.option_background_selector);
        binding.option4.setBackgroundResource(R.drawable.option_background_selector);

        RadioButton selectedRB = (state.selectedRadioButtonId != -1) ? getView().findViewById(state.selectedRadioButtonId) : null;

        switch (state.status) {
            case CORRECT:
                binding.answerFeedbackTextView.setText("You marked the correct option!");
                binding.answerFeedbackTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.neon_green));
                if (selectedRB != null) {
                    selectedRB.setBackgroundResource(correctBgResource);
                }
                break;
            case WRONG:
                binding.answerFeedbackTextView.setText("You marked the wrong option! Correct: " + question.getCorrectAnswer());
                binding.answerFeedbackTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.error_red));
                if (selectedRB != null) {
                    selectedRB.setBackgroundResource(wrongBgResource);
                }
                highlightCorrectOptionWithActualDrawable(question.getCorrectAnswer(), correctBgResource);
                break;
            case SKIPPED:
                binding.answerFeedbackTextView.setText("You not marked anything! Correct: " + question.getCorrectAnswer());
                binding.answerFeedbackTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.warning_yellow));
                highlightCorrectOptionWithActualDrawable(question.getCorrectAnswer(), correctBgResource);
                break;
            case NOT_ANSWERED:
                binding.answerFeedbackTextView.setVisibility(View.GONE);
                break;
        }
        binding.answerFeedbackTextView.setVisibility(View.VISIBLE);
    }

    private void highlightCorrectOptionWithActualDrawable(String correctAnswerText, int correctBgResource) {
        if (binding == null) return;
        RadioButton[] options = {binding.option1, binding.option2, binding.option3, binding.option4};
        for (RadioButton option : options) {
            if (option.getText().toString().equals(correctAnswerText)) {
                QuestionState state = questionStates.get(currentIndex);
                boolean shouldHighlight = true;
                if (state != null && state.selectedRadioButtonId == option.getId()) {
                    if (state.status == AnswerStatus.WRONG || state.status == AnswerStatus.CORRECT) {
                        shouldHighlight = false;
                    }
                }
                if (shouldHighlight) {
                    option.setBackgroundResource(correctBgResource);
                }
                break;
            }
        }
    }

    private void startTimer() {
        if (binding == null || !isAdded()) return;
        stopTimer();
        binding.progressBar.setProgress(100);
        long timeMillis = (long) timePerQuestionInSeconds * 1000;

        timer = new CountDownTimer(timeMillis, 1000) {
            @SuppressLint("SetTextI18n")
            @Override
            public void onTick(long millisUntilFinished) {
                if (binding == null || !isAdded()) {
                    cancel();
                    return;
                }
                int secondsLeft = (int) (millisUntilFinished / 1000);
                binding.timerTextView.setText(String.format("TIME: 00:%02d", secondsLeft));
                binding.progressBar.setProgress((int) (millisUntilFinished * 100 / timeMillis));
            }

            @Override
            public void onFinish() {
                if (binding == null || questions == null || questionStates == null || getContext() == null || !isAdded()) {
                    return;
                }
                binding.progressBar.setProgress(0);
                QuestionState currentState = questionStates.get(currentIndex);

                if (currentState != null && currentState.status == AnswerStatus.NOT_ANSWERED) {
                    currentState.status = AnswerStatus.SKIPPED;
                    disableOptions();

                    String feedbackText = "Bypass Protocol Engaged!";
                    if (currentIndex < questions.size() && questions.get(currentIndex) != null) {
                        feedbackText += " Correct: " + questions.get(currentIndex).getCorrectAnswer();
                        highlightCorrectOptionWithActualDrawable(questions.get(currentIndex).getCorrectAnswer(), R.drawable.option_correct_bg);
                    }
                    binding.answerFeedbackTextView.setText(feedbackText);
                    binding.answerFeedbackTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.warning_yellow));
                    binding.answerFeedbackTextView.setVisibility(View.VISIBLE);
                    binding.explainAnswerButton.setVisibility(View.VISIBLE);

                    uiHandler.postDelayed(() -> {
                        if (binding != null && getActivity() != null && isAdded()) {
                            handleNextQuestion();
                        }
                    }, 2000);
                }
            }
        };
        timer.start();
    }

    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    @SuppressLint("SetTextI18n")
    private void submitQuiz() {
        stopTimer();
        if (questions == null || questionStates == null || !isAdded() || getParentFragmentManager() == null) {
            Toast.makeText(getContext(), "Error preparing results. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }
        int totalQuestions = questions.size();
        int finalCorrect = 0;
        int finalWrong = 0;
        int finalSkipped = 0;
        int currentQuizScorePoints = 0;

        for (int i = 0; i < totalQuestions; i++) {
            QuestionState state = questionStates.get(i);
            if (state != null) {
                switch (state.status) {
                    case CORRECT:
                        finalCorrect++;
                        currentQuizScorePoints += marksPerCorrectAnswer;
                        break;
                    case WRONG:
                        finalWrong++;
                        currentQuizScorePoints += marksPerWrongAnswer;
                        break;
                    case SKIPPED:
                    case NOT_ANSWERED:
                        finalSkipped++;
                        break;
                }
            } else {
                finalSkipped++;
                Log.e("SubmitQuiz", "QuestionState was null for index: " + i);
            }
        }
        this.score = currentQuizScorePoints;

        QuizResultsFragment resultsFragment = QuizResultsFragment.newInstance(
                totalQuestions,
                finalCorrect,
                finalWrong,
                finalSkipped,
                this.score,
                currentCategory,
                marksPerCorrectAnswer,
                marksPerWrongAnswer
        );

        FragmentManager fragmentManager = getParentFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.container, resultsFragment);
        fragmentTransaction.commit();

        updateFirebaseQuizResults(finalCorrect, finalWrong, finalSkipped);
    }

    private void updateFirebaseQuizResults(int numCorrect, int numWrong, int numSkipped) {
        if (mAuth.getCurrentUser() == null) {
            Log.e("FirebaseError", "User not logged in. Results not saved.");
            if (getContext() != null && isAdded())
                Toast.makeText(getContext(), "Not logged in. Results not saved.", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = mAuth.getCurrentUser().getUid();
        DatabaseReference database = FirebaseDatabase.getInstance().getReference();

        DatabaseReference userQuizResultsRef = database.child("users").child(userId).child("quizResults");
        DatabaseReference userTotalScoreRef = database.child("users").child(userId).child("score");
        final int calculatedQuizScore = this.score;

        userQuizResultsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                QuizResults currentResults = snapshot.getValue(QuizResults.class);
                if (currentResults == null) {
                    currentResults = new QuizResults(0, 0, 0, 0, 0);
                }

                final QuizResults finalCurrentResults = currentResults;
                userTotalScoreRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot scoreSnapshot) {
                        int currentOverallScore = 0;
                        if (scoreSnapshot.exists() && scoreSnapshot.getValue(Integer.class) != null) {
                            currentOverallScore = scoreSnapshot.getValue(Integer.class);
                        }

                        QuizResults updatedResults = new QuizResults(
                                finalCurrentResults.totalQuestions + (questions != null ? questions.size() : 0),
                                finalCurrentResults.correct + numCorrect,
                                finalCurrentResults.skipped + numSkipped,
                                finalCurrentResults.wrong + numWrong,
                                finalCurrentResults.totalQuizzes + 1
                        );
                        int newOverallScore = currentOverallScore + calculatedQuizScore;

                        userTotalScoreRef.setValue(newOverallScore);
                        userQuizResultsRef.setValue(updatedResults)
                                .addOnSuccessListener(aVoid -> Log.d("FirebaseUpdate", "Quiz results and score successfully updated."))
                                .addOnFailureListener(e -> Log.e("FirebaseError", "Failed to update quiz results.", e));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("FirebaseError", "Failed to read total score for update.", error.toException());
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Database error on reading quizResults: " + error.getMessage());
            }
        });
    }

    private void resetOptionViews() {
        if (binding == null || getContext() == null || !isAdded()) return;
        binding.optionsRadioGroup.clearCheck();

        int optionDefaultBg = R.drawable.option_background_selector;

        binding.option1.setBackgroundResource(optionDefaultBg);
        binding.option2.setBackgroundResource(optionDefaultBg);
        binding.option3.setBackgroundResource(optionDefaultBg);
        binding.option4.setBackgroundResource(optionDefaultBg);
    }

    private void disableOptions() {
        if (binding == null || !isAdded()) return;
        binding.option1.setEnabled(false);
        binding.option2.setEnabled(false);
        binding.option3.setEnabled(false);
        binding.option4.setEnabled(false);
    }

    private void enableOptions() {
        if (binding == null || !isAdded()) return;
        binding.option1.setEnabled(true);
        binding.option2.setEnabled(true);
        binding.option3.setEnabled(true);
        binding.option4.setEnabled(true);
    }

    @Override
    public void onClick(View view) {
        if (binding == null || questions == null || questionStates == null || currentIndex >= questions.size() || getContext() == null || !isAdded())
            return;

        QuestionState currentState = questionStates.get(currentIndex);
        if (currentState == null || currentState.status != AnswerStatus.NOT_ANSWERED) {
            return;
        }
        stopTimer();
        disableOptions();

        RadioButton buttonClicked = (RadioButton) view;
        currentState.selectedOptionText = buttonClicked.getText().toString();
        currentState.selectedRadioButtonId = buttonClicked.getId();

        Question currentQuestion = questions.get(currentIndex);
        if (currentQuestion == null) return;
        String correctAnswer = currentQuestion.getCorrectAnswer();

        int correctBgResource = R.drawable.option_correct_bg;
        int wrongBgResource = R.drawable.option_wrong_bg;

        if (currentState.selectedOptionText.equals(correctAnswer)) {
            currentState.status = AnswerStatus.CORRECT;
            binding.answerFeedbackTextView.setText("You marked the correct option!");
            binding.answerFeedbackTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.neon_green));
            buttonClicked.setBackgroundResource(correctBgResource);
        } else {
            currentState.status = AnswerStatus.WRONG;
            binding.answerFeedbackTextView.setText("You marked the wrong option! Correct: " + correctAnswer);
            binding.answerFeedbackTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.error_red));
            buttonClicked.setBackgroundResource(wrongBgResource);
            highlightCorrectOptionWithActualDrawable(correctAnswer, correctBgResource);
        }
        binding.answerFeedbackTextView.setVisibility(View.VISIBLE);
        binding.explainAnswerButton.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopTimer();
        uiHandler.removeCallbacksAndMessages(null);
        binding = null;
    }
}