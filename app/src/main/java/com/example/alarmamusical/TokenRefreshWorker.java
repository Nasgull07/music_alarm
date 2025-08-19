package com.example.alarmamusical;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONObject;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.List;

public class TokenRefreshWorker extends Worker {

    public TokenRefreshWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d("TokenRefreshWorker", "Intentando renovar token...");

        SharedPreferences prefs = getApplicationContext().getSharedPreferences("SpotifyPrefs", Context.MODE_PRIVATE);
        long lastRefreshTime = prefs.getLong("last_refresh_time", 0);
        long currentTime = System.currentTimeMillis();

        // Verificar si ha pasado al menos una hora (3600000 ms)
        if (currentTime - lastRefreshTime < 3600000) {
            Log.d("TokenRefreshWorker", "La renovación del token ya se realizó recientemente. No es necesario renovarlo ahora.");
            return Result.success();
        }

        String refreshToken = prefs.getString("refresh_token", null);

        if (refreshToken == null) {
            Log.e("TokenRefreshWorker", "No se encontró el refresh token. Redirigiendo a autenticación.");

            if (!isSpotifyAuthActivityRunning()) {
                Intent intent = new Intent(getApplicationContext(), SpotifyAuthActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                getApplicationContext().startActivity(intent);
            }

            return Result.failure();
        }

        // Intentar renovar el token
        if (refreshSpotifyToken()) {
            // Guardar el timestamp de la última renovación
            prefs.edit().putLong("last_refresh_time", currentTime).apply();
            return Result.success();
        } else {
            return Result.retry();
        }
    }

    private boolean isSpotifyAuthActivityRunning() {
        ActivityManager activityManager = (ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager != null) {
            List<ActivityManager.RunningTaskInfo> tasks = activityManager.getRunningTasks(1);
            if (!tasks.isEmpty()) {
                ComponentName topActivity = tasks.get(0).topActivity;
                return topActivity != null && topActivity.getClassName().equals(SpotifyAuthActivity.class.getName());
            }
        }
        return false;
    }

    private boolean refreshSpotifyToken() {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences("SpotifyPrefs", Context.MODE_PRIVATE);
        String refreshToken = prefs.getString("refresh_token", null);

        if (refreshToken != null) {
            OkHttpClient client = new OkHttpClient();
            RequestBody body = new FormBody.Builder()
                    .add("grant_type", "refresh_token")
                    .add("refresh_token", refreshToken)
                    .add("client_id", "dc51fd1033aa43209b33ba5c17eb0c2a") // Reemplaza con tu Client ID
                    .add("client_secret", "c3cead8258cc4cf29a467edf9018cfcf") // Reemplaza con tu Client Secret
                    .build();

            Request request = new Request.Builder()
                    .url("https://accounts.spotify.com/api/token")
                    .post(body)
                    .build();

            try {
                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    // Parsear la respuesta para obtener el nuevo token
                    String newAccessToken = parseAccessToken(responseBody);
                    long expirationTime = System.currentTimeMillis() + (3600 * 1000); // 1 hora

                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("accessToken", newAccessToken);
                    editor.putLong("tokenExpirationTime", expirationTime);
                    editor.apply();

                    return true;
                } else {
                    Log.e("TokenRefreshWorker", "Error al renovar el token: " + response.message());
                }
            } catch (IOException e) {
                Log.e("TokenRefreshWorker", "Error de red al renovar el token", e);
            }
        } else {
            Log.e("TokenRefreshWorker", "No se encontró el refresh token.");
        }
        return false;
    }

    private String parseAccessToken(String responseBody) {
        try {
            JSONObject json = new JSONObject(responseBody);
            return json.getString("access_token");
        } catch (Exception e) {
            Log.e("TokenRefreshWorker", "Error al parsear el token de acceso", e);
            return null;
        }
    }
}