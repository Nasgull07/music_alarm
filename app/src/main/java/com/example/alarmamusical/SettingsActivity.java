package com.example.alarmamusical;

    import android.content.Intent;
    import android.content.SharedPreferences;
    import android.os.Bundle;
    import android.widget.Button;
    import android.widget.Toast;
    import com.example.alarmamusical.Alarm;
    import androidx.appcompat.app.AppCompatActivity;
    import androidx.recyclerview.widget.LinearLayoutManager;
    import androidx.recyclerview.widget.RecyclerView;

    import java.util.ArrayList;
    import java.util.List;

    public class SettingsActivity extends AppCompatActivity {

        private RecyclerView alarmsRecyclerView;
        private AlarmAdapter alarmAdapter;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_settings);

            alarmsRecyclerView = findViewById(R.id.alarmsRecyclerView);
            alarmsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            Button backToMainButton = findViewById(R.id.backToMainButton);
            backToMainButton.setOnClickListener(v -> {
                Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
                startActivity(intent);
                finish(); // Cierra SettingsActivity
            });

            Button createAlarmButton = findViewById(R.id.createAlarmButton);
            createAlarmButton.setOnClickListener(v -> {
                Intent intent = new Intent(SettingsActivity.this, CreateAlarmActivity.class);
                startActivity(intent);
            });

            // Simulación de alarmas configuradas
            List<Alarm> alarms = getConfiguredAlarms();

            alarmAdapter = new AlarmAdapter(alarms, alarm -> {
                Intent intent = new Intent(SettingsActivity.this, AlarmDetailsActivity.class);
                intent.putExtra("alarmKey", alarm.getKey()); // Pasar la clave completa
                startActivity(intent);
            });

            alarmsRecyclerView.setAdapter(alarmAdapter);
        }


        @Override
        protected void onResume() {
            super.onResume();
            // Obtener las alarmas configuradas nuevamente
            List<Alarm> updatedAlarms = getConfiguredAlarms();
            // Actualizar la lista de alarmas en el adaptador existente
            alarmAdapter.updateAlarms(updatedAlarms);
        }

        private List<Alarm> getConfiguredAlarms() {
            List<Alarm> alarms = new ArrayList<>();
            SharedPreferences prefs = getSharedPreferences("AlarmPrefs", MODE_PRIVATE);

            for (String key : prefs.getAll().keySet()) {
                if (key.startsWith("alarm_")) {
                    String value = prefs.getString(key, "");
                    if (value != null) {
                        String[] parts = value.split(",");
                        String[] timeParts = parts[0].split(":");
                        int hour = Integer.parseInt(timeParts[0]);
                        int minute = Integer.parseInt(timeParts[1]);
                        String time = String.format("%02d:%02d", hour, minute);
                        alarms.add(new Alarm(key, time, true)); // Usar la clave completa como ID
                    }
                }
            }

            alarms.sort((a1, a2) -> a1.getTime().compareTo(a2.getTime()));
            return alarms;
        }
    }