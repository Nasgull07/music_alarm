package com.example.alarmamusical;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Obtener la clave de la alarma desde el Intent
        String alarmKey = intent.getStringExtra("alarmKey");
        if (alarmKey == null) {
            Log.e("AlarmReceiver", "No se proporcionó clave de alarma en el Intent.");
            return; // Salir si no hay clave
        }
        Log.d("AlarmReceiver", "Alarma recibida con clave: " + alarmKey);

        // Verificar si la alarma está activa
        SharedPreferences prefs = context.getSharedPreferences("AlarmPrefs", Context.MODE_PRIVATE);
        boolean isActive = prefs.getBoolean("alarmActive_" + alarmKey, false);
        Log.d("AlarmReceiver", "Verificando si la alarma está activa...");
        if (!isActive) {
            Log.d("AlarmReceiver", "La alarma está desactivada.");
            return; // Salir si la alarma no está activa
        }

        Log.d("AlarmReceiver", "La alarma está activa.");
        Toast.makeText(context, "¡Es hora de la alarma!", Toast.LENGTH_LONG).show();

        // Obtener el token de acceso y la playlist configurada
        SharedPreferences spotifyPrefs = context.getSharedPreferences("SpotifyPrefs", Context.MODE_PRIVATE);
        String accessToken = spotifyPrefs.getString("access_token", null);
        String playlistId = prefs.getString("playlistId", null);

        if (accessToken == null) {
            Log.e("AlarmReceiver", "Token de acceso no encontrado.");
            return;
        }
        if (playlistId == null) {
            Log.e("AlarmReceiver", "Playlist ID no configurado.");
            return;
        }

        Log.d("AlarmReceiver", "Obteniendo playlist con ID: " + playlistId);
        SpotifyApiManager spotifyApiManager = new SpotifyApiManager();
        spotifyApiManager.getPlaylist(playlistId, accessToken, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("AlarmReceiver", "Error al obtener la playlist: " + e.getMessage(), e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e("AlarmReceiver", "Error al obtener la playlist: " + response.message());
                    response.close();
                    return;
                }

                String responseBody = response.body().string();
                response.close(); // Liberar recursos
                try {
                    // Parsear la respuesta para obtener las canciones
                    JSONObject json = new JSONObject(responseBody);
                    JSONArray tracks = json.getJSONObject("tracks").getJSONArray("items");

                    if (tracks.length() > 0) {
                        // Seleccionar una canción aleatoria
                        int randomIndex = (int) (Math.random() * tracks.length());
                        JSONObject randomTrack = tracks.getJSONObject(randomIndex).getJSONObject("track");
                        String trackUri = randomTrack.getString("uri");

//                        JSONObject firstTrack = tracks.getJSONObject(0).getJSONObject("track");
//                        String trackUri = firstTrack.getString("uri");

                        Log.d("AlarmReceiver", "Reproduciendo canción con URI: " + trackUri);
                        spotifyApiManager.playTrack(trackUri, accessToken, new Callback() {
                            @Override
                            public void onFailure(Call call, IOException e) {
                                Log.e("AlarmReceiver", "Error al iniciar la reproducciónnnn: " + e.getMessage(), e);
                            }

                            @Override
                            public void onResponse(Call call, Response response) throws IOException {
                                if (response.isSuccessful()) {
                                    Log.d("AlarmReceiver", "Reproducción iniciada correctamente.");
                                } else {
                                    Log.e("AlarmReceiver", "Error al iniciar la reproducción: " + response.message());
                                    Log.e("AlarmReceiver", "Cuerpo de la respuesta: " + response.body().string());
                                }
                                response.close(); // Liberar recursos
                            }
                        });
                    } else {
                        Log.e("AlarmReceiver", "La playlist no contiene canciones.");
                    }
                } catch (Exception e) {
                    Log.e("AlarmReceiver", "Error al parsear la respuesta de la playlist.", e);
                }
            }
        });
    }
}