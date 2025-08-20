package com.example.alarmamusical;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;

import org.json.JSONArray;
import org.json.JSONObject;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView alarmStatusText;
    private Button authButton;
    private Button setAlarmButton;
    private SpotifyService spotifyService;
    private TextView tokenStatus;
    private TextView devicesStatus;
    private DrawerLayout drawerLayout;

    private RecyclerView playlistsRecyclerView;
    private PlaylistAdapter playlistAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        playlistsRecyclerView = findViewById(R.id.playlistsRecyclerView);
        playlistsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        fetchPlaylists();
        // Inicializar elementos del layout
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.navigation_view);
        //tokenStatus = findViewById(R.id.tokenStatus);
        //devicesStatus = findViewById(R.id.devicesStatus);
        //authButton = findViewById(R.id.authButton);

        tokenStatus = navigationView.findViewById(R.id.tokenStatus);
        devicesStatus = navigationView.findViewById(R.id.devicesStatus);

        setAlarmButton = findViewById(R.id.setAlarmButton);
        alarmStatusText = findViewById(R.id.alarmStatusText);

        spotifyService = new SpotifyService();

        // Configurar menú lateral
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_authorize) {
                startActivity(new Intent(this, SpotifyAuthActivity.class));
            } else if (id == R.id.nav_check_devices) {
                fetchConnectedDevices();
            }
            drawerLayout.closeDrawers();
            return true;
        });

        MenuItem authorizeItem = navigationView.getMenu().findItem(R.id.nav_authorize);
        if (authorizeItem != null) {
            String tokenStatusText = "Autorizar con Spotify\n(" + tokenStatus.getText().toString() + ")";
            authorizeItem.setTitle(tokenStatusText);
        }

        // Configurar botones
        // authButton.setOnClickListener(v -> startActivity(new Intent(this, SpotifyAuthActivity.class)));
        setAlarmButton.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));

        // Verificar estado del token y dispositivos
        checkTokenAndDevices();

        // Ajustar insets para el diseño
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawer_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateAlarmStatus();
    }

    private void updateAlarmStatus() {
        SharedPreferences prefs = getSharedPreferences("AlarmPrefs", MODE_PRIVATE);
        long currentTimeMillis = System.currentTimeMillis();
        long closestAlarmTime = Long.MAX_VALUE;
        String closestAlarmTimeText = null;
        boolean isNextDay = false;

        for (String key : prefs.getAll().keySet()) {
            if (key.startsWith("alarm_")) {
                boolean isActive = prefs.getBoolean("alarmActive_" + key, false);
                if (!isActive) continue;

                String value = prefs.getString(key, "");
                if (value != null) {
                    String[] parts = value.split(",");
                    String[] timeParts = parts[0].split(":");
                    int hour = Integer.parseInt(timeParts[0]);
                    int minute = Integer.parseInt(timeParts[1]);

                    long alarmTimeMillis = calculateAlarmTimeMillis(hour, minute, currentTimeMillis);
                    if (alarmTimeMillis < closestAlarmTime) {
                        closestAlarmTime = alarmTimeMillis;
                        closestAlarmTimeText = String.format("%02d:%02d", hour, minute);
                        isNextDay = alarmTimeMillis > currentTimeMillis + 24 * 60 * 60 * 1000;
                    }
                }
            }
        }

        if (closestAlarmTimeText != null) {
            alarmStatusText.setText(isNextDay
                    ? "Próxima alarma configurada para mañana a las " + closestAlarmTimeText
                    : "Próxima alarma configurada a las " + closestAlarmTimeText);
        } else {
            alarmStatusText.setText("No hay alarmas activas configuradas.");
        }
    }

    private long calculateAlarmTimeMillis(int hour, int minute, long currentTimeMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        if (calendar.getTimeInMillis() < currentTimeMillis) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        return calendar.getTimeInMillis();
    }

    private void checkTokenAndDevices() {
        getAuthStateForMain(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    tokenStatus.setText("No hay token guardado o es inválido.");
                    devicesStatus.setText("No se pueden verificar los dispositivos.");
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (response.isSuccessful()) {
                        runOnUiThread(() -> tokenStatus.setText("Token válido."));
                        fetchConnectedDevices();
                    } else {
                        runOnUiThread(() -> {
                            tokenStatus.setText("Token caducado o inválido.");
                            devicesStatus.setText("No se pueden verificar los dispositivos.");
                        });
                    }
                } finally {
                    response.close();
                }
            }
        });
    }

    private void fetchConnectedDevices() {
        String accessToken = getSharedPreferences("SpotifyPrefs", MODE_PRIVATE)
                .getString("access_token", null);

        if (accessToken != null) {
            spotifyService.getAvailableDevices(accessToken, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> devicesStatus.setText("Error al obtener dispositivos."));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        if (response.isSuccessful() && response.body() != null) {
                            String responseBody = response.body().string();
                            String devicesInfo = parseDevices(responseBody);
                            runOnUiThread(() -> {
                                devicesStatus.setText(devicesInfo);
                            });
                        } else {
                            runOnUiThread(() -> {
                                devicesStatus.setText("No se encontraron dispositivos.");
                            });                        }
                    } finally {
                        response.close();
                    }
                }
            });
        } else {

            devicesStatus.setText("Token de acceso no disponible.");

        }
    }

    private String parseDevices(String responseBody) {
        StringBuilder devicesInfo = new StringBuilder("Dispositivos conectados:\n");
        try {
            JSONObject json = new JSONObject(responseBody);
            JSONArray devices = json.getJSONArray("devices");
            for (int i = 0; i < devices.length(); i++) {
                JSONObject device = devices.getJSONObject(i);
                String name = device.getString("name");
                boolean isActive = device.getBoolean("is_active");
                devicesInfo.append("- ").append(name).append(" (")
                        .append(isActive ? "Activo" : "Inactivo").append(")\n");
            }
        } catch (Exception e) {
            devicesInfo = new StringBuilder("Error al parsear la lista de dispositivos.");
        }
        return devicesInfo.toString();
    }

    private void getAuthStateForMain(Callback callback) {
        String accessToken = getSharedPreferences("SpotifyPrefs", MODE_PRIVATE)
                .getString("access_token", null);

        if (accessToken != null) {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url("https://api.spotify.com/v1/me")
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .build();

            client.newCall(request).enqueue(callback);
        } else {
            callback.onFailure(null, new IOException("No hay token guardado."));
        }
    }

    private void fetchPlaylists() {
        String accessToken = getSharedPreferences("SpotifyPrefs", MODE_PRIVATE)
                .getString("access_token", null);
        Log.d("MainActivity", "Token de acceso: " + accessToken);
        if (accessToken != null) {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url("https://api.spotify.com/v1/me/playlists")
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("MainActivity", "Error al realizar la solicitud de playlists: " + e.getMessage());
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error al obtener playlists", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    Log.d("MainActivity", "Respuesta de la API: " + response.code());
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        Log.d("MainActivity", "Cuerpo de la respuesta: " + responseBody);
                        List<Playlist> playlists = parsePlaylists(responseBody);
                        runOnUiThread(() -> {
                            playlistAdapter = new PlaylistAdapter(playlists);
                            playlistsRecyclerView.setAdapter(playlistAdapter);
                        });
                    }
                }
            });
        }
    }

    private List<Playlist> parsePlaylists(String responseBody) {
        List<Playlist> playlists = new ArrayList<>();
        try {
            JSONObject json = new JSONObject(responseBody);

            // Verificar si la clave "items" existe
            if (json.has("items") && !json.isNull("items")) {
                JSONArray items = json.getJSONArray("items");
                for (int i = 0; i < items.length(); i++) {
                    JSONObject playlistJson = items.getJSONObject(i);
                    String id = playlistJson.getString("id");
                    String name = playlistJson.getString("name");

                    // Agregar solo el ID y el nombre de la playlist
                    playlists.add(new Playlist(id, name, null));
                }
            } else {
                Log.e("MainActivity", "La clave 'items' no está presente en el JSON.");
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Error al parsear las playlists: " + e.getMessage());
            e.printStackTrace();
        }
        return playlists;
    }
}