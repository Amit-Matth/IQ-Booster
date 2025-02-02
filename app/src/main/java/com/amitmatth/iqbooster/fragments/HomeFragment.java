package com.amitmatth.iqbooster.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.amitmatth.iqbooster.databinding.FragmentHomeBinding;
import com.amitmatth.iqbooster.helper.QuestionsGeneratorActivity;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);

        binding.arithmeticCard.setOnClickListener(v -> onCardClick("math arithmetic operations easy questions", "What is 12 + 8?"));
        binding.unitConversionCard.setOnClickListener(v -> onCardClick("math unit conversion easy questions", "How many centimeters in 1 meter?"));
        binding.rootCard.setOnClickListener(v -> onCardClick("math complete square-root easy questions", "What is the square root of 49?"));

        return binding.getRoot();
    }

    private void onCardClick(String topic, String questionFormat) {
            Intent intent = new Intent(getActivity(), QuestionsGeneratorActivity.class);
            intent.putExtra("topic", topic);
            intent.putExtra("questionFormat", questionFormat);
            startActivity(intent);
    }
}
