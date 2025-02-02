package com.amitmatth.iqbooster.activities;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.amitmatth.iqbooster.R;
import com.amitmatth.iqbooster.data.Question;
import com.amitmatth.iqbooster.databinding.ActivityQuestionsBinding;
import com.amitmatth.iqbooster.model.QuizResults;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

public class QuestionsActivity extends AppCompatActivity implements View.OnClickListener {

    private ActivityQuestionsBinding binding;
    private final RadioButton[] checkButton = new RadioButton[4];
    private int currentIndex = 0;
    private int skippedQuestions = 0;
    private int correctQuestions = 0;
    private int wrongQuestions = 0;
    private int score = 0;

    private CountDownTimer timer;
    private final int totalTime = 10;
    private List<Question> questions;


    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQuestionsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showExitDialog();
            }
        });

        // Retrieve JSON string from Intent
        String json = getIntent().getStringExtra("questions");

//        Log.d("gemini response ", "the sended json is: " + json);

        if (json != null) {
            questions = new Gson().fromJson(json, new TypeToken<List<Question>>() {
            }.getType());
        } else {
            questions = new ArrayList<>(); // Prevent NullPointerException
        }

//        Log.d("gemini response QuestionsActivity", "Received Questions: " + questions.size());

        initComponents();
    }

    private void showExitDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_custom_layout, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        // Apply transparent background to avoid default corners
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        MaterialButton btnExit = dialogView.findViewById(R.id.btnExit);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnExit.setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });

        dialog.show();

    }

    private void initComponents() {
        checkButton[0] = binding.option1;
        checkButton[1] = binding.option2;
        checkButton[2] = binding.option3;
        checkButton[3] = binding.option4;

        for (RadioButton button : checkButton) {
            button.setOnClickListener(this);
        }

        if (!questions.isEmpty()) {
            setQuestionToView(questions.get(currentIndex));
            startTimer();
        }

        binding.mainMenuButton.setOnClickListener(v -> showExitDialog());
        binding.leaveQuiz.setOnClickListener(v -> showExitDialog());
    }

    private void startTimer() {
        timer = new CountDownTimer(totalTime * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secondsLeft = (int) (millisUntilFinished / 1000);
                binding.timerTextView.setText("Time Left: " + secondsLeft + " seconds");
                int progress = (int) (millisUntilFinished * 100 / (totalTime * 1000));
                binding.progressBar.setProgress(progress);
            }

            @Override
            public void onFinish() {
                boolean isOptionSelected = false;
                for (RadioButton button : checkButton) {
                    if (button.isChecked()) {
                        isOptionSelected = true;
                        break;
                    }
                }

                if (!isOptionSelected) {
                    skippedQuestions++;
                }
                moveToNextQuestion();
            }
        };

        timer.start();
    }

    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
        }
    }

    @SuppressLint("SetTextI18n")
    private void submitQuestion() {
        binding.questionsOverviewContainer.setVisibility(View.VISIBLE);
        binding.questionPlayground.setVisibility(View.GONE);
        binding.totalQuestionsCurrent.setText("" + (currentIndex));
        binding.skippedQuestionsCurrent.setText("" + skippedQuestions);
        binding.wrongQuestionsCurrent.setText("" + wrongQuestions);
        binding.correctQuestionsCurrent.setText("" + correctQuestions);
        binding.score.setText("Your score: " + score);

        updateQuizResults();
        stopTimer();
    }


    private void updateQuizResults() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference database = FirebaseDatabase.getInstance().getReference();

        database.keepSynced(true);

        DatabaseReference quizResultsRef = database.child("users").child(userId).child("quizResults");
        DatabaseReference totalScoreRef = database.child("users").child(userId).child("score");

        quizResultsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int totalQuizzes = 0, totalQuestions = 0, totalCorrectQuestions = 0, totalSkippedQuestions = 0, totalWrongQuestions = 0;

                if (snapshot.exists()) {
                    totalQuizzes = snapshot.child("totalQuizzes").exists() ? snapshot.child("totalQuizzes").getValue(Integer.class) : 0;
                    totalQuestions = snapshot.child("totalQuestions").exists() ? snapshot.child("totalQuestions").getValue(Integer.class) : 0;
                    totalCorrectQuestions = snapshot.child("correct").exists() ? snapshot.child("correct").getValue(Integer.class) : 0;
                    totalSkippedQuestions = snapshot.child("skipped").exists() ? snapshot.child("skipped").getValue(Integer.class) : 0;
                    totalWrongQuestions = snapshot.child("wrong").exists() ? snapshot.child("wrong").getValue(Integer.class) : 0;
                }

                int newTotalQuestions = totalQuestions + questions.size();
                int newCorrect = totalCorrectQuestions + correctQuestions;
                int newSkipped = totalSkippedQuestions + skippedQuestions;
                int newWrong = totalWrongQuestions + wrongQuestions;

                // Update database
                totalScoreRef.setValue(newCorrect * 5);
                quizResultsRef.setValue(new QuizResults(newTotalQuestions, newCorrect, newSkipped, newWrong, totalQuizzes + 1));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("questions error", "Database error: " + error.getMessage());
            }
        });
    }


    private void moveToNextQuestion() {
        currentIndex++;
        if (currentIndex < questions.size()) {
            setQuestionToView(questions.get(currentIndex));
            resetOptions();
            startTimer();
        } else {
            submitQuestion();
        }
    }

    private void resetOptions() {
        for (RadioButton button : checkButton) {
            button.setChecked(false);
            button.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    @SuppressLint("SetTextI18n")
    private void setQuestionToView(Question question) {
        binding.question.setText("Q)" + (currentIndex + 1) + ". " + question.getQuestionText());
        binding.option1.setText(question.getOption1());
        binding.option2.setText(question.getOption2());
        binding.option3.setText(question.getOption3());
        binding.option4.setText(question.getOption4());
    }

    @Override
    public void onClick(View view) {
        RadioButton buttonClicked = (RadioButton) view;

        String selectedAnswer = buttonClicked.getText().toString();
        String correctAnswer = questions.get(currentIndex).getCorrectAnswer();

        if (selectedAnswer.equals(correctAnswer)) {
            correctQuestions++;
            score += 5;
            buttonClicked.setBackgroundColor(Color.GREEN);
        } else {
            wrongQuestions++;
            buttonClicked.setBackgroundColor(Color.RED);
        }

        stopTimer();

        new android.os.Handler().postDelayed(this::moveToNextQuestion, 1000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimer();
        questions.clear();
    }

}
