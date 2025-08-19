package com.example.alarmamusical;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class SpotifyService {
    private final OkHttpClient client = new OkHttpClient();

    public void getAvailableDevices(String accessToken, Callback callback) {
        Request request = new Request.Builder()
            .url("https://api.spotify.com/v1/me/player/devices")
            .addHeader("Authorization", "Bearer " + accessToken)
            .get()
            .build();

        client.newCall(request).enqueue(callback);
    }
}