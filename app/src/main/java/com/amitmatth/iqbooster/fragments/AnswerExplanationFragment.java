package com.amitmatth.iqbooster.fragments;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.amitmatth.iqbooster.databinding.FragmentAnswerExplanationBinding;
import com.amitmatth.iqbooster.network.RemoteConfigHelper;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class AnswerExplanationFragment extends Fragment {

    private static final String ARG_QUESTION_TEXT = "questionText";
    private static final String ARG_CORRECT_ANSWER = "correctAnswer";
    private static final String ARG_USER_ANSWER = "userAnswer";

    private FragmentAnswerExplanationBinding binding;
    private ListenableFuture<GenerateContentResponse> explanationResponseFuture;
    private String questionToExplain;
    private String correctAnswer;
    private String userAnswer;

    public static AnswerExplanationFragment newInstance(String questionText, String correctAnswer, String userAnswer) {
        AnswerExplanationFragment fragment = new AnswerExplanationFragment();
        Bundle args = new Bundle();
        args.putString(ARG_QUESTION_TEXT, questionText);
        args.putString(ARG_CORRECT_ANSWER, correctAnswer);
        args.putString(ARG_USER_ANSWER, userAnswer);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            questionToExplain = getArguments().getString(ARG_QUESTION_TEXT);
            correctAnswer = getArguments().getString(ARG_CORRECT_ANSWER);
            userAnswer = getArguments().getString(ARG_USER_ANSWER);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAnswerExplanationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.backToQuizButton.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        binding.questionToExplainTextView.setText("Question: " + (questionToExplain != null ? questionToExplain : "N/A"));
        binding.correctAnswerTextView.setText((correctAnswer != null ? correctAnswer : "N/A"));
        binding.userAnswerTextView.setText((userAnswer != null ? userAnswer : "Not Answered"));

        if (questionToExplain != null && correctAnswer != null) {
            binding.aiExplanationTextView.setText("Fetching explanation from AI...");
            binding.explanationProgressBar.setVisibility(View.VISIBLE);
            fetchExplanationFromAI(questionToExplain, correctAnswer);
        } else {
            binding.aiExplanationTextView.setText("Question or correct answer not provided for AI explanation.");
            binding.explanationProgressBar.setVisibility(View.GONE);
        }
    }

    private void fetchExplanationFromAI(String question, String answer) {
        if (getContext() == null) {
            Log.e("GeminiAPI", "Context is null, cannot fetch explanation.");
            binding.aiExplanationTextView.setText("Error: Cannot initialize AI explanation service.");
            binding.explanationProgressBar.setVisibility(View.GONE);
            return;
        }
        RemoteConfigHelper remoteConfigHelper = new RemoteConfigHelper();
        remoteConfigHelper.fetchApiKey(apiKey -> {
            if (apiKey == null) {
                Log.e("GeminiAPI", "API Key is null. Cannot fetch explanation.");
                if (binding != null) {
                    binding.aiExplanationTextView.setText("Error: API Key not available for AI explanation.");
                    binding.explanationProgressBar.setVisibility(View.GONE);
                }
                return;
            }

            GenerativeModel gm = new GenerativeModel("gemini-1.5-flash", apiKey);
            GenerativeModelFutures model = GenerativeModelFutures.from(gm);

            Content content = new Content.Builder()
                    .addText("Explain concisely why for the question '" + question + "', the correct answer is '" + answer + "'.")
                    .build();

            explanationResponseFuture = model.generateContent(content);
            Futures.addCallback(explanationResponseFuture, new FutureCallback<GenerateContentResponse>() {
                @Override
                public void onSuccess(GenerateContentResponse result) {
                    if (binding == null || getContext() == null) return;
                    binding.explanationProgressBar.setVisibility(View.GONE);
                    if (result != null && result.getText() != null && !result.getText().isEmpty()) {
                        binding.aiExplanationTextView.setText(result.getText());
                    } else {
                        binding.aiExplanationTextView.setText("AI could not retrieve an explanation for this answer.");
                        Log.e("GeminiExplanation", "Failed to get explanation: Empty or null response text");
                    }
                }

                @Override
                public void onFailure(@NonNull Throwable t) {
                    if (binding == null || getContext() == null) return;
                    binding.explanationProgressBar.setVisibility(View.GONE);
                    binding.aiExplanationTextView.setText("Error fetching AI explanation: " + t.getMessage());
                    Log.e("GeminiExplanation", "Error fetching explanation", t);
                }
            }, ContextCompat.getMainExecutor(requireContext()));
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (explanationResponseFuture != null && !explanationResponseFuture.isDone()) {
            explanationResponseFuture.cancel(true);
        }
        explanationResponseFuture = null;
        binding = null;
    }
}