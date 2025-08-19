package com.example.alarmamusical;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AlarmManagerService {

    public static void setAlarm(Context context, long triggerAtMillis, String alarmKey) throws Exception {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Log.d("AlarmManagerService", "Configurando alarma para: " + triggerAtMillis + " con clave: " + alarmKey);

        if (alarmManager == null) {
            Log.e("AlarmManagerService", "AlarmManager no disponible.");
            return;
        }

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("alarmKey", alarmKey); // Agregar la clave de la alarma

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                alarmKey.hashCode(), // Usar un identificador único basado en la clave
                intent,
                flags
        );

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
                Log.d("AlarmManagerService", "Alarma exacta configurada correctamente.");
            } else {
                throw new Exception("No se pueden programar alarmas exactas. Verifica los permisos.");
            }
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            Log.d("AlarmManagerService", "Alarma exacta configurada correctamente.");
        }
    }

    public static void cancelAlarm(Context context, String alarmKey) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Log.d("AlarmManagerService", "Cancelando alarma con clave: " + alarmKey);
        if (alarmManager == null) {
            Log.e("AlarmManagerService", "AlarmManager no disponible.");
        }

        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                alarmKey.hashCode(), // Usar un identificador único basado en la clave de la alarma
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        alarmManager.cancel(pendingIntent); // Cancelar la alarma
    }
}