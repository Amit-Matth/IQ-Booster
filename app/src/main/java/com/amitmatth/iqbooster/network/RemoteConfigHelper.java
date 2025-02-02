package com.amitmatth.iqbooster.network;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

public class RemoteConfigHelper {
    private static final String GEMINI_API_KEY = "gemini_api_key"; // Remote Config key
    private final FirebaseRemoteConfig remoteConfig;

    public RemoteConfigHelper() {
        remoteConfig = FirebaseRemoteConfig.getInstance();

        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600)
                .build();
        remoteConfig.setConfigSettingsAsync(configSettings);
    }

    public void fetchApiKey(FetchCompleteListener listener) {
        remoteConfig.fetchAndActivate().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String apiKey = remoteConfig.getString(GEMINI_API_KEY);
                listener.onFetchComplete(apiKey);
            } else {
                listener.onFetchComplete("default_api_key"); // Fallback
            }
        });
    }

    public interface FetchCompleteListener {
        void onFetchComplete(String apiKey);
    }
}
