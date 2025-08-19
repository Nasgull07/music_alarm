package com.example.alarmamusical;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.Calendar;


public class MainActivity extends AppCompatActivity {

    private TextView alarmInfo;
    private Button authButton;
    private Button setAlarmButton;
    private Button checkDevicesButton; // Nuevo botón para verificar dispositivos
    private SpotifyService spotifyService;
    private TextView tokenStatus;
    private TextView devicesStatus;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        WorkManagerSetup.setupTokenRefresh(this);

        tokenStatus = findViewById(R.id.tokenStatus);
        devicesStatus = findViewById(R.id.devicesStatus);

        // Verificar el estado del token
        checkTokenState();

        // Verificar el estado del token y dispositivos
        checkTokenAndDevices();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        authButton = findViewById(R.id.authButton);
        setAlarmButton = findViewById(R.id.setAlarmButton);
        checkDevicesButton = findViewById(R.id.checkDevicesButton); // Asocia el nuevo botón

        spotifyService = new SpotifyService();

        authButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, SpotifyAuthActivity.class);
            startActivity(intent);
        });

        setAlarmButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });

        checkDevicesButton.setOnClickListener(v -> {
            String accessToken = getSharedPreferences("SpotifyPrefs", MODE_PRIVATE)
                    .getString("access_token", null);

            if (accessToken != null) {
                spotifyService.getAvailableDevices(accessToken, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        e.printStackTrace();
                        runOnUiThread(() -> devicesStatus.setText("Error al obtener dispositivos."));
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        try {
                            if (response.isSuccessful()) {
                                String responseBody = response.body().string();
                                System.out.println("Dispositivos disponibles: " + responseBody);
                                String devicesInfo = parseDevices(responseBody);
                                runOnUiThread(() -> devicesStatus.setText(devicesInfo));
                            } else {
                                System.out.println("Error al obtener dispositivos: " + response.message());
                                runOnUiThread(() -> devicesStatus.setText("No se encontraron dispositivos."));
                            }
                        } finally{
                            response.close();
                        }
                    }
                });
            } else {
                devicesStatus.setText("Token de acceso no disponible.");
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        TextView alarmStatusText = findViewById(R.id.alarmStatusText); // Asegúrate de tener este TextView en tu layout
        SharedPreferences prefs = getSharedPreferences("AlarmPrefs", MODE_PRIVATE);

        long currentTimeMillis = System.currentTimeMillis();
        long closestAlarmTime = Long.MAX_VALUE;
        String closestAlarmTimeText = null;
        boolean isNextDay = false;

        for (String key : prefs.getAll().keySet()) {
            if (key.startsWith("alarm_")) {
                boolean isActive = prefs.getBoolean("alarmActive_" + key, false); // Verificar si la alarma está activa
                if (!isActive) {
                    continue; // Ignorar alarmas inactivas
                }

                String value = prefs.getString(key, "");
                if (value != null) {
                    String[] parts = value.split(",");
                    String[] timeParts = parts[0].split(":");
                    int hour = Integer.parseInt(timeParts[0]);
                    int minute = Integer.parseInt(timeParts[1]);

                    Calendar calendar = Calendar.getInstance();
                    calendar.set(Calendar.HOUR_OF_DAY, hour);
                    calendar.set(Calendar.MINUTE, minute);
                    calendar.set(Calendar.SECOND, 0);

                    // Si la hora ya pasó, programa para el día siguiente
                    if (calendar.getTimeInMillis() < currentTimeMillis) {
                        calendar.add(Calendar.DAY_OF_YEAR, 1);
                        isNextDay = true;
                    } else {
                        isNextDay = false;
                    }

                    long alarmTimeMillis = calendar.getTimeInMillis();
                    if (alarmTimeMillis < closestAlarmTime) {
                        closestAlarmTime = alarmTimeMillis;
                        closestAlarmTimeText = String.format("%02d:%02d", hour, minute);
                    }
                }
            }
        }

        if (closestAlarmTimeText != null) {
            if (isNextDay) {
                alarmStatusText.setText("Próxima alarma configurada para mañana a las " + closestAlarmTimeText);
            } else {
                alarmStatusText.setText("Próxima alarma configurada a las " + closestAlarmTimeText);
            }
        } else {
            alarmStatusText.setText("No hay alarmas activas configuradas.");
        }
    }

    private void checkTokenState() {
        getAuthStateForMain(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> tokenStatus.setText("No hay token guardado o es inválido."));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try{
                    if (response.isSuccessful()) {
                        runOnUiThread(() -> tokenStatus.setText("Token válido."));
                    } else {
                        runOnUiThread(() -> tokenStatus.setText("Token caducado o inválido."));
                    }

                } finally{
                    response.close();
                }
            }
        });
    }

    private void getAuthStateForMain(Callback callback) {
        String accessToken = getSharedPreferences("SpotifyPrefs", MODE_PRIVATE)
                .getString("access_token", null);

        if (accessToken != null) {
            // Verificar si el token es válido
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url("https://api.spotify.com/v1/me")
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .build();

            client.newCall(request).enqueue(callback);
        } else {
            // No hay token, devolver estado al callback
            callback.onFailure(null, new IOException("No hay token guardado."));
        }
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
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url("https://api.spotify.com/v1/me/player/devices")
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> devicesStatus.setText("Error al obtener dispositivos."));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        if (response.isSuccessful()) {
                            String responseBody = response.body().string();
                            // Parsear la respuesta para obtener los dispositivos
                            String devicesInfo = parseDevices(responseBody);
                            runOnUiThread(() -> devicesStatus.setText(devicesInfo));
                        } else {
                            runOnUiThread(() -> devicesStatus.setText("No se encontraron dispositivos."));
                        }
                    } finally {
                        response.close();
                    }
                }
            });
        }
    }

    private String parseDevices(String responseBody) {
        // Aquí puedes usar una librería como Gson o JSONObject para parsear la respuesta
        // Ejemplo simple:
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
}