package com.example.alarmamusical;

    import android.content.Intent;
    import android.os.Bundle;
    import android.util.Log;
    import android.widget.Button;

    import androidx.appcompat.app.AppCompatActivity;

    public class AlarmRingActivity extends AppCompatActivity {
        private static final String ACTION_STOP_ALARM = "com.example.alarmamusical.STOP_ALARM";

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_alarm_ring);

            Button stopAlarmButton = findViewById(R.id.stopAlarmButton);

            stopAlarmButton.setOnClickListener(v -> {
                Log.d("AlarmRingActivity", "Botón de detener alarma presionado. Enviando broadcast...");
                Intent stopIntent = new Intent(ACTION_STOP_ALARM);
                stopIntent.setPackage(getPackageName()); // Asegurar que el broadcast sea interno
                sendBroadcast(stopIntent);

                Log.d("AlarmRingActivity", "Broadcast enviado. Cerrando actividad...");
                finish(); // Cierra la actividad
            });
        }
    }