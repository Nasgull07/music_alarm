package com.example.alarmamusical;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class CreateAlarmActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_alarm);

        EditText hourInput = findViewById(R.id.hourInput);
        EditText minuteInput = findViewById(R.id.minuteInput);
        EditText playlistInput = findViewById(R.id.playlistInput);
        Button saveAlarmButton = findViewById(R.id.saveAlarmButton);

        saveAlarmButton.setOnClickListener(v -> {
            int hour = Integer.parseInt(hourInput.getText().toString());
            int minute = Integer.parseInt(minuteInput.getText().toString());
            String playlistId = playlistInput.getText().toString();

            // Guardar la alarma en SharedPreferences
            SharedPreferences prefs = getSharedPreferences("AlarmPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            int alarmId = (int) (System.currentTimeMillis() / 1000); // Generar un ID único
            editor.putString("alarm_" + alarmId, hour + ":" + minute + "," + playlistId);
            editor.apply();

            Toast.makeText(this, "Alarma creada", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}