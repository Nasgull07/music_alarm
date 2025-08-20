package com.example.alarmamusical;

            import android.content.BroadcastReceiver;
            import android.content.Context;
            import android.content.Intent;
            import android.content.IntentFilter;
            import android.content.SharedPreferences;
            import android.util.Log;
            import android.widget.Toast;

            import com.spotify.android.appremote.api.ConnectionParams;
            import com.spotify.android.appremote.api.Connector;
            import com.spotify.android.appremote.api.SpotifyAppRemote;

            public class AlarmReceiver extends BroadcastReceiver {
                private static final String CLIENT_ID = "dc51fd1033aa43209b33ba5c17eb0c2a";
                private static final String REDIRECT_URI = "com.example.alarmamusical://callback";
                private static final String ACTION_STOP_ALARM = "com.example.alarmamusical.STOP_ALARM";
                private SpotifyAppRemote spotifyAppRemote;

                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    Log.d("AlarmReceiver", "onReceive llamado con acción: " + action);

                    if (ACTION_STOP_ALARM.equals(action)) {
                        Log.d("AlarmReceiver", "Acción STOP_ALARM recibida. Intentando detener la reproducción...");
                        stopSpotifyPlayback(context); // Pasar el contexto aquí
                        return;
                    }
                    // Obtener la clave de la alarma desde el Intent
                    String alarmKey = intent.getStringExtra("alarmKey");
                    if (alarmKey == null) {
                        Log.e("AlarmReceiver", "No se proporcionó clave de alarma en el Intent.");
                        return;
                    }
                    Log.d("AlarmReceiver", "Alarma recibida con clave: " + alarmKey);

                    // Verificar si la alarma está activa
                    SharedPreferences prefs = context.getSharedPreferences("AlarmPrefs", Context.MODE_PRIVATE);
                    boolean isActive = prefs.getBoolean("alarmActive_" + alarmKey, false);
                    if (!isActive) {
                        Log.d("AlarmReceiver", "La alarma está desactivada.");
                        return;
                    }

                    Toast.makeText(context, "¡Es hora de la alarma!", Toast.LENGTH_LONG).show();

                    // Obtener el playlist ID configurado
                    String playlistId = prefs.getString("playlistId_" + alarmKey, null);
                    Log.d("AlarmReceiver", "Playlist ID obtenido: " + playlistId);
                    if (playlistId == null) {
                        Log.e("AlarmReceiver", "Playlist ID no configurado.");
                        return;
                    }

                    // Conectar al SDK de Spotify y reproducir la playlist
                    connectToSpotify(context, playlistId, () -> {
                        // Abrir la actividad para detener la alarma después de iniciar la reproducción
                        Intent alarmIntent = new Intent(context, AlarmRingActivity.class);
                        alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(alarmIntent);
                    });

                }

                private void connectToSpotify(Context context, String playlistId, Runnable onPlaybackStarted) {
                    ConnectionParams connectionParams = new ConnectionParams.Builder(CLIENT_ID)
                            .setRedirectUri(REDIRECT_URI)
                            .showAuthView(true)
                            .build();

                    SpotifyAppRemote.connect(context, connectionParams, new Connector.ConnectionListener() {
                        @Override
                        public void onConnected(SpotifyAppRemote remote) {
                            spotifyAppRemote = remote;
                            Log.d("AlarmReceiver", "Conexión exitosa con Spotify.");
                            playPlaylist(playlistId, onPlaybackStarted);
                        }

                        @Override
                        public void onFailure(Throwable error) {
                            Log.e("AlarmReceiver", "Error al conectar con Spotify: " + error.getMessage(), error);
                        }
                    });
                }

                private void playPlaylist(String playlistUri, Runnable onPlaybackStarted) {
                    if (spotifyAppRemote != null) {
                        spotifyAppRemote.getPlayerApi().play("spotify:playlist:" + playlistUri).setResultCallback(empty -> {
                            Log.d("AlarmReceiver", "Reproduciendo playlist: " + playlistUri);
                            onPlaybackStarted.run();
                        }).setErrorCallback(error -> Log.e("AlarmReceiver", "Error al reproducir la playlist: " + error.getMessage()));
                    } else {
                        Log.e("AlarmReceiver", "SpotifyAppRemote no está conectado.");
                    }
                }

                public void stopSpotifyPlayback(Context context) {
                    if (spotifyAppRemote != null) {
                        Log.d("AlarmReceiver", "SpotifyAppRemote conectado. Pausando reproducción...");
                        spotifyAppRemote.getPlayerApi().pause();
                        SpotifyAppRemote.disconnect(spotifyAppRemote);
                        spotifyAppRemote = null;
                        Log.d("AlarmReceiver", "Reproducción detenida y SpotifyAppRemote desconectado.");
                    } else {
                        Log.e("AlarmReceiver", "SpotifyAppRemote no está conectado. Intentando reconectar...");
                        reconnectToSpotify(context);
                    }
                }

                private void reconnectToSpotify(Context context) {
                    ConnectionParams connectionParams = new ConnectionParams.Builder(CLIENT_ID)
                            .setRedirectUri(REDIRECT_URI)
                            .showAuthView(true)
                            .build();

                    SpotifyAppRemote.connect(context, connectionParams, new Connector.ConnectionListener() {
                        @Override
                        public void onConnected(SpotifyAppRemote remote) {
                            spotifyAppRemote = remote;
                            Log.d("AlarmReceiver", "Reconexión exitosa con Spotify. Pausando reproducción...");
                            spotifyAppRemote.getPlayerApi().pause();
                            SpotifyAppRemote.disconnect(spotifyAppRemote);
                            spotifyAppRemote = null;
                            Log.d("AlarmReceiver", "Reproducción detenida y SpotifyAppRemote desconectado tras reconexión.");
                        }

                        @Override
                        public void onFailure(Throwable error) {
                            Log.e("AlarmReceiver", "Error al reconectar con Spotify: " + error.getMessage(), error);
                        }
                    });
                }
            }