package com.example.alarmamusical;

            import android.app.AlarmManager;
            import android.content.Context;
            import android.content.Intent;
            import android.content.SharedPreferences;
            import android.os.Bundle;
            import android.widget.Button;
            import android.widget.EditText;
            import android.widget.TimePicker;
            import android.widget.Toast;

            import androidx.appcompat.app.AppCompatActivity;

            import java.util.Calendar;

            public class AlarmDetailsActivity extends AppCompatActivity {
                private TimePicker timePicker;
                private EditText playlistIdInput;
                private Button saveSettingsButton, viewAlarmsButton, clearAlarmsButton, backButton;

                @Override
                protected void onCreate(Bundle savedInstanceState) {
                    super.onCreate(savedInstanceState);
                    setContentView(R.layout.activity_alarm_details);

                    timePicker = findViewById(R.id.timePicker);
                    playlistIdInput = findViewById(R.id.playlistIdInput);
                    saveSettingsButton = findViewById(R.id.saveSettingsButton);
                    viewAlarmsButton = findViewById(R.id.viewAlarmsButton);
                    clearAlarmsButton = findViewById(R.id.clearAlarmsButton);
                    backButton = findViewById(R.id.backButton);

                    SharedPreferences prefs = getSharedPreferences("AlarmPrefs", MODE_PRIVATE);

                    // Obtener la clave de la alarma desde el intent
                    String alarmKey = getIntent().getStringExtra("alarmKey");

                    if (alarmKey != null) {
                        // Cargar los datos de la alarma específica
                        String alarmData = prefs.getString(alarmKey, null);
                        if (alarmData != null) {
                            String[] parts = alarmData.split(",");
                            String[] timeParts = parts[0].split(":");
                            int hour = Integer.parseInt(timeParts[0]);
                            int minute = Integer.parseInt(timeParts[1]);
                            String playlistId = parts.length > 1 ? parts[1] : "";

                            timePicker.setHour(hour);
                            timePicker.setMinute(minute);
                            playlistIdInput.setText(playlistId);
                        }
                    }

                    saveSettingsButton.setOnClickListener(v -> {
                        int selectedHour = timePicker.getHour();
                        int selectedMinute = timePicker.getMinute();
                        String selectedPlaylistId = playlistIdInput.getText().toString();

                        String localAlarmKey = alarmKey; // Copia local de alarmKey
                        if (localAlarmKey == null) {
                            // Generar una nueva clave si no existe
                            localAlarmKey = "alarm_" + System.currentTimeMillis();
                        }

                        // Guardar configuración en SharedPreferences
                        SharedPreferences.Editor editor = prefs.edit();
                        String alarmValue = selectedHour + ":" + selectedMinute + "," + selectedPlaylistId;
                        editor.putString(localAlarmKey, alarmValue);
                        editor.putBoolean("alarmActive_" + localAlarmKey, true);
                        editor.apply();

                        // Verificar si la alarma está activa
                        boolean isActive = prefs.getBoolean("alarmActive_" + localAlarmKey, true);
                        if (isActive) {
                            // Calcular el tiempo de activación en milisegundos
                            Calendar calendar = Calendar.getInstance();
                            calendar.set(Calendar.HOUR_OF_DAY, selectedHour);
                            calendar.set(Calendar.MINUTE, selectedMinute);
                            calendar.set(Calendar.SECOND, 0);

                            // Si la hora ya pasó, programa para el día siguiente
                            if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                                calendar.add(Calendar.DAY_OF_YEAR, 1);
                            }

                            long triggerAtMillis = calendar.getTimeInMillis();

                            // Configurar la alarma usando AlarmManagerService
                            try {
                                AlarmManagerService.setAlarm(this, triggerAtMillis, localAlarmKey);
                                Toast.makeText(this, "Configuración guardada y alarma programada", Toast.LENGTH_SHORT).show();
                            } catch (Exception e) {
                                e.printStackTrace();
                                Toast.makeText(this, "Error al configurar la alarma", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            // Cancelar la alarma si está desactivada
                            AlarmManagerService.cancelAlarm(this, localAlarmKey);
                            Toast.makeText(this, "Alarma desactivada", Toast.LENGTH_SHORT).show();
                        }

                        finish();
                    });

                    viewAlarmsButton.setOnClickListener(v -> {
                        if (alarmKey != null) {
                            String alarmData = prefs.getString(alarmKey, null);
                            if (alarmData != null) {
                                Toast.makeText(this, "Datos de la alarma: " + alarmData, Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(this, "No hay datos para esta alarma.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(this, "No se encontró la clave de la alarma.", Toast.LENGTH_SHORT).show();
                        }
                    });

                    clearAlarmsButton.setOnClickListener(v -> {
                        if (alarmKey != null) {
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.remove(alarmKey); // Eliminar usando la clave completa
                            editor.apply();

                            Toast.makeText(this, "Alarma eliminada", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(this, "Error al eliminar la alarma", Toast.LENGTH_SHORT).show();
                        }
                    });

                    backButton.setOnClickListener(v -> {
                        // Volver a la actividad principal
                        Intent intent = new Intent(AlarmDetailsActivity.this, SettingsActivity.class);
                        startActivity(intent);
                        finish(); // Cierra la actividad actual
                    });
                }
            }