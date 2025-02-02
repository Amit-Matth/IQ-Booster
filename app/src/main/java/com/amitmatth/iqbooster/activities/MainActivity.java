package com.amitmatth.iqbooster.activities;

import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.amitmatth.iqbooster.R;
import com.amitmatth.iqbooster.databinding.ActivityMainBinding;
import com.amitmatth.iqbooster.fragments.HomeFragment;
import com.amitmatth.iqbooster.fragments.LeaderboardFragment;
import com.amitmatth.iqbooster.fragments.ProfileFragment;
import com.amitmatth.iqbooster.network.InternetReceiver;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private InternetReceiver internetReceiver;

    private String ROOT_FRAGMENT_TAG = "root_fragment";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflate and get instance of binding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Register the receiver dynamically
        internetReceiver = new InternetReceiver();
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(internetReceiver, filter);

        BottomNavigationView bottomNavigationView = binding.bottomNavigation;

        bottomNavigationView.setOnNavigationItemSelectedListener(item ->

        {
            int id = item.getItemId();

            if (id == R.id.home) {
                loadFragment(new HomeFragment(), true);
            } else if (id == R.id.leaderboard) {
                loadFragment(new LeaderboardFragment(), false);
            } else if (id == R.id.profile) {
                loadFragment(new ProfileFragment(), false);
            }

            return true;
        });

        // Load the default fragment (e.g., HomeFragment)
        bottomNavigationView.setSelectedItemId(R.id.home);
    }

    private void loadFragment(Fragment fragment, boolean flag) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        if (flag) {
            ft.add(R.id.container, fragment);
            fm.popBackStack(ROOT_FRAGMENT_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            ft.addToBackStack(ROOT_FRAGMENT_TAG);
        } else {
            ft.replace(R.id.container, fragment);
            ft.addToBackStack(null);
        }
        ft.commit();
    }


    @Override
    public void onBackPressed() {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.getBackStackEntryCount() > 1) {
            fm.popBackStack();
        } else {
            finish();
            super.onBackPressed();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null; // This is fine if you are using View Binding
        unregisterReceiver(internetReceiver);
    }

}
