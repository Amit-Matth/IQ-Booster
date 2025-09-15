package com.amitmatth.iqbooster.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.amitmatth.iqbooster.R;
import com.amitmatth.iqbooster.activities.MainActivity;
import com.amitmatth.iqbooster.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);

        binding.arithmeticCard.setOnClickListener(v -> onCardClick("arithmetic"));
        binding.unitConversionCard.setOnClickListener(v -> onCardClick("unit_conversion"));
        binding.rootCard.setOnClickListener(v -> onCardClick("square_root"));

        return binding.getRoot();
    }

    private void onCardClick(String category) {
        QuizSetupFragment quizSetupFragment = QuizSetupFragment.newInstance(category);

        FragmentManager fragmentManager = getParentFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.container, quizSetupFragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setFullScreenMode(false);
        }
    }
}