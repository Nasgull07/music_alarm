package com.example.alarmamusical;

import android.util.Log;
import okhttp3.*;

public class SpotifyApiManager {
    private static final String TAG = "SpotifyApiManager";
    private static final String BASE_URL = "https://api.spotify.com/v1";
    private OkHttpClient client = new OkHttpClient();

   public void getPlaylist(String playlistId, String accessToken, Callback callback) {
        if (playlistId == null || playlistId.isEmpty()) {
            Log.e(TAG, "El playlistId es nulo o vacío.");
            return;
        }
        if (accessToken == null || accessToken.isEmpty()) {
            Log.e(TAG, "El accessToken es nulo o vacío.");
            return;
        }

        Request request = new Request.Builder()
                .url(BASE_URL + "/playlists/" + playlistId)
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();
        client.newCall(request).enqueue(callback);
    }

public void playTrack(String uri, String accessToken, Callback callback) {
    if (uri == null || uri.isEmpty()) {
        Log.e(TAG, "El URI es nulo o vacío.");
        return;
    }
    if (accessToken == null || accessToken.isEmpty()) {
        Log.e(TAG, "El accessToken es nulo o vacío.");
        return;
    }

    // Cambiar a "uris" en lugar de "context_uri"
    String jsonBody = "{\"uris\": [\"" + uri + "\"]}";
    Log.d(TAG, "Cuerpo de la solicitud JSON: " + jsonBody);

    RequestBody body = RequestBody.create(
            jsonBody,
            MediaType.parse("application/json")
    );

    Request request = new Request.Builder()
            .url(BASE_URL + "/me/player/play")
            .addHeader("Authorization", "Bearer " + accessToken)
            .put(body)
            .build();

    Log.d(TAG, "URL de la solicitud: " + request.url());
    Log.d(TAG, "Encabezado de autorización: " + request.header("Authorization"));

    client.newCall(request).enqueue(callback);
}

    public void setActiveDevice(String deviceId, String accessToken, Callback callback) {
        RequestBody body = RequestBody.create(
                MediaType.parse("application/json"),
                "{\"device_ids\": [\"" + deviceId + "\"], \"play\": false}"
        );
        Request request = new Request.Builder()
                .url(BASE_URL + "/me/player")
                .put(body)
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();
        client.newCall(request).enqueue(callback);
    }

}