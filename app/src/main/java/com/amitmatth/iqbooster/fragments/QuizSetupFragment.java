package com.amitmatth.iqbooster.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.amitmatth.iqbooster.R;
import com.amitmatth.iqbooster.activities.MainActivity;
import com.amitmatth.iqbooster.databinding.FragmentQuizSetupBinding;

public class QuizSetupFragment extends Fragment {

    private FragmentQuizSetupBinding binding;
    private String selectedCategory;

    private int numberOfQuestions = 10;
    private String difficulty = "Medium";
    private int timePerQuestionInSeconds = 10;
    private int marksPerCorrectAnswer = 3;
    private int marksPerWrongAnswer = -3;

    public static QuizSetupFragment newInstance(String category) {
        QuizSetupFragment fragment = new QuizSetupFragment();
        Bundle args = new Bundle();
        args.putString("category", category);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            selectedCategory = getArguments().getString("category", "N/A");
        }
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
        binding = FragmentQuizSetupBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.categoryTextView.setText("Category: " + selectedCategory);

        String[] numQuestionsOptions = {"5", "10", "15", "20"};
        ArrayAdapter<String> numQuestionsAdapter = new ArrayAdapter<>(requireContext(), R.layout.dropdown_item_simple, numQuestionsOptions);
        if (binding.numQuestionsDropdown instanceof AutoCompleteTextView) {
            ((AutoCompleteTextView) binding.numQuestionsDropdown).setAdapter(numQuestionsAdapter);
            ((AutoCompleteTextView) binding.numQuestionsDropdown).setText(String.valueOf(numberOfQuestions), false);
        }

        String[] difficultyOptions = {"Easy", "Medium", "Hard"};
        ArrayAdapter<String> difficultyAdapter = new ArrayAdapter<>(requireContext(), R.layout.dropdown_item_simple, difficultyOptions);
        if (binding.difficultyDropdown instanceof AutoCompleteTextView) {
            ((AutoCompleteTextView) binding.difficultyDropdown).setAdapter(difficultyAdapter);
            ((AutoCompleteTextView) binding.difficultyDropdown).setText(difficulty, false);
            updateQuizRules(difficulty);

            ((AutoCompleteTextView) binding.difficultyDropdown).setOnItemClickListener((parent, view1, position, id) -> {
                difficulty = (String) parent.getItemAtPosition(position);
                updateQuizRules(difficulty);
            });
        }

        binding.backButton.setOnClickListener(v -> {
            getParentFragmentManager().popBackStack();
        });

        binding.startQuizButton.setOnClickListener(v -> {
            try {
                String numQuestionsStr = binding.numQuestionsDropdown.getText().toString();
                numberOfQuestions = Integer.parseInt(numQuestionsStr);
                difficulty = binding.difficultyDropdown.getText().toString();

                switch (difficulty) {
                    case "Easy":
                        timePerQuestionInSeconds = 20;
                        marksPerCorrectAnswer = 1;
                        marksPerWrongAnswer = -1;
                        break;
                    case "Medium":
                        timePerQuestionInSeconds = 10;
                        marksPerCorrectAnswer = 3;
                        marksPerWrongAnswer = -3;
                        break;
                    case "Hard":
                        timePerQuestionInSeconds = 5;
                        marksPerCorrectAnswer = 5;
                        marksPerWrongAnswer = -5;
                        break;
                }

                QuestionsFragment questionsFragment = QuestionsFragment.newInstance(
                        selectedCategory,
                        numberOfQuestions,
                        difficulty,
                        timePerQuestionInSeconds,
                        marksPerCorrectAnswer,
                        marksPerWrongAnswer
                );

                FragmentManager fragmentManager = getParentFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.container, questionsFragment);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();

            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Please select valid options", Toast.LENGTH_SHORT).show();
            } catch (IllegalStateException e) {
                Toast.makeText(getContext(), "Error initializing quiz. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateQuizRules(String selectedDifficulty) {
        String rulesText;
        switch (selectedDifficulty) {
            case "Easy":
                rulesText = "Time: 20 seconds per question\nCorrect: +1 mark\nWrong: -1 mark\nSkip: No effect on marks";
                break;
            case "Medium":
                rulesText = "Time: 10 seconds per question\nCorrect: +3 marks\nWrong: -3 marks\nSkip: No effect on marks";
                break;
            case "Hard":
                rulesText = "Time: 5 seconds per question\nCorrect: +5 marks\nWrong: -5 marks\nSkip: No effect on marks";
                break;
            default:
                rulesText = "Select difficulty to see rules.";
                break;
        }
        binding.quizRulesTextView.setText(rulesText);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}