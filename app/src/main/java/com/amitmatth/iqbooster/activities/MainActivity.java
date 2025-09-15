package com.amitmatth.iqbooster.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.amitmatth.iqbooster.R;
import com.amitmatth.iqbooster.databinding.ActivityMainBinding;
import com.amitmatth.iqbooster.databinding.DialogNoInternetBinding;
import com.amitmatth.iqbooster.fragments.HomeFragment;
import com.amitmatth.iqbooster.fragments.LeaderboardFragment;
import com.amitmatth.iqbooster.fragments.ProgressFragment;
import com.amitmatth.iqbooster.fragments.ProfileFragment;
import com.amitmatth.iqbooster.network.InternetReceiver;
import com.amitmatth.iqbooster.network.NetworkUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private InternetReceiver internetStateReceiver;
    private AlertDialog noInternetDialog;

    private static final String ROOT_FRAGMENT_TAG = "root_fragment";

    private final BroadcastReceiver localInternetStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null) {
                if (intent.getAction().equals(InternetReceiver.ACTION_INTERNET_DISCONNECTED)) {
                    showNoInternetDialog();
                } else if (intent.getAction().equals(InternetReceiver.ACTION_INTERNET_CONNECTED)) {
                    dismissNoInternetDialog();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        internetStateReceiver = new InternetReceiver();
        IntentFilter globalFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(internetStateReceiver, globalFilter);

        IntentFilter localFilter = new IntentFilter();
        localFilter.addAction(InternetReceiver.ACTION_INTERNET_DISCONNECTED);
        localFilter.addAction(InternetReceiver.ACTION_INTERNET_CONNECTED);
        LocalBroadcastManager.getInstance(this).registerReceiver(localInternetStatusReceiver, localFilter);

        BottomNavigationView bottomNavigationView = binding.bottomNavigation;

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.home) {
                loadFragment(new HomeFragment(), true);
            } else if (id == R.id.progress) {
                loadFragment(new ProgressFragment(), false);
            } else if (id == R.id.leaderboard) {
                loadFragment(new LeaderboardFragment(), false);
            } else if (id == R.id.profile) {
                loadFragment(new ProfileFragment(), false);
            }
            return true;
        });

        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.home);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!NetworkUtils.isInternetAvailable(this)) {
            showNoInternetDialog();
        } else {
            dismissNoInternetDialog();
        }
    }

    private void showNoInternetDialog() {
        if (noInternetDialog == null || !noInternetDialog.isShowing()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            DialogNoInternetBinding dialogBinding = DialogNoInternetBinding.inflate(LayoutInflater.from(this));
            builder.setView(dialogBinding.getRoot());
            builder.setCancelable(false);

            noInternetDialog = builder.create();
            if (noInternetDialog.getWindow() != null) {
                noInternetDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }

            dialogBinding.dialogOkButton.setOnClickListener(v -> noInternetDialog.dismiss());
            noInternetDialog.show();
        }
    }

    private void dismissNoInternetDialog() {
        if (noInternetDialog != null && noInternetDialog.isShowing()) {
            noInternetDialog.dismiss();
        }
    }

    public void setFullScreenMode(boolean enable) {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), !enable);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        ActionBar actionBar = getSupportActionBar();

        if (enable) {
            if (controller != null) {
                controller.hide(WindowInsetsCompat.Type.systemBars());
                controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
            if (actionBar != null) {
                actionBar.hide();
            }
            if (binding != null && binding.bottomNavigation != null) {
                binding.bottomNavigation.setVisibility(View.GONE);
            }
        } else {
            if (controller != null) {
                controller.show(WindowInsetsCompat.Type.systemBars());
            }
            if (actionBar != null) {
                actionBar.show();
            }
            if (binding != null && binding.bottomNavigation != null) {
                binding.bottomNavigation.setVisibility(View.VISIBLE);
            }
        }
    }

    private void loadFragment(Fragment fragment, boolean isRoot) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        if (isRoot) {
            fm.popBackStack(ROOT_FRAGMENT_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            ft.replace(R.id.container, fragment, ROOT_FRAGMENT_TAG);
        } else {
            ft.replace(R.id.container, fragment);
            ft.addToBackStack(null);
        }
        ft.commit();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (internetStateReceiver != null) {
            unregisterReceiver(internetStateReceiver);
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(localInternetStatusReceiver);
        InternetReceiver.resetConnectionState();
        dismissNoInternetDialog();
        binding = null;
    }
}