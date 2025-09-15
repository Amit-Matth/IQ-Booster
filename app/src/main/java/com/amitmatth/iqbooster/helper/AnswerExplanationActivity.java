package com.amitmatth.iqbooster.helper;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.amitmatth.iqbooster.databinding.ActivityAnswerExplanationBinding;
import com.amitmatth.iqbooster.network.RemoteConfigHelper;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class AnswerExplanationActivity extends AppCompatActivity {

    private ActivityAnswerExplanationBinding binding;
    private ListenableFuture<GenerateContentResponse> explanationResponse;
    private String questionToExplain;
    private String correctAnswer;
    private String simpleExplanation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAnswerExplanationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.backToQuizButton.setOnClickListener(v -> finish());

        Intent intent = getIntent();
        questionToExplain = intent.getStringExtra("questionText");
        correctAnswer = intent.getStringExtra("correctAnswer");
        simpleExplanation = intent.getStringExtra("simpleExplanation");

        binding.questionToExplainTextView.setText("Question: " + (questionToExplain != null ? questionToExplain : "N/A"));
        binding.correctAnswerTextView.setText("Correct Answer: " + (correctAnswer != null ? correctAnswer : "N/A"));
        binding.simpleExplanationTextView.setText("Brief Explanation: " + (simpleExplanation != null ? simpleExplanation : "N/A"));

        if (questionToExplain != null && correctAnswer != null) {
            binding.aiExplanationTextView.setText("Fetching explanation from AI...");
            binding.explanationProgressBar.setVisibility(View.VISIBLE);
            fetchExplanation(questionToExplain, correctAnswer);
        } else {
            binding.aiExplanationTextView.setText("Question or answer not provided for AI explanation.");
            binding.explanationProgressBar.setVisibility(View.GONE);
        }
    }

    private void fetchExplanation(String question, String answer) {
        RemoteConfigHelper remoteConfigHelper = new RemoteConfigHelper();
        remoteConfigHelper.fetchApiKey(apiKey -> {
            if (apiKey == null) {
                Log.e("GeminiAPI", "API Key is null. Cannot fetch explanation.");
                binding.aiExplanationTextView.setText("Error: API Key not available for AI explanation.");
                binding.explanationProgressBar.setVisibility(View.GONE);
                return;
            }

            GenerativeModel gm = new GenerativeModel("gemini-2.0-flash", apiKey);
            GenerativeModelFutures model = GenerativeModelFutures.from(gm);

            Content content = new Content.Builder()
                    .addText("Explain concisely why for the question '" + question + "', the correct answer is '" + answer + "'.")
                    .build();

            explanationResponse = model.generateContent(content);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                Futures.addCallback(explanationResponse, new FutureCallback<GenerateContentResponse>() {
                    @Override
                    public void onSuccess(GenerateContentResponse result) {
                        binding.explanationProgressBar.setVisibility(View.GONE);
                        if (result != null && result.getText() != null && !result.getText().isEmpty()) {
                            binding.aiExplanationTextView.setText(result.getText());
                        } else {
                            binding.aiExplanationTextView.setText("AI could not retrieve an explanation for this answer.");
                            Log.e("GeminiExplanation", "Failed to get explanation: Empty or null response text");
                        }
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        binding.explanationProgressBar.setVisibility(View.GONE);
                        binding.aiExplanationTextView.setText("Error fetching AI explanation: " + t.getMessage());
                        Log.e("GeminiExplanation", "Error fetching explanation", t);
                    }
                }, getMainExecutor());
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (explanationResponse != null && !explanationResponse.isDone()) {
            explanationResponse.cancel(true);
        }
        binding = null;
    }
}