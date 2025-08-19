package com.example.alarmamusical;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AlarmAdapter extends RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder> {

    private final List<Alarm> alarms;
    private final OnAlarmClickListener listener;

    public AlarmAdapter(List<Alarm> alarms, OnAlarmClickListener listener) {
        this.alarms = alarms;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AlarmViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_alarm, parent, false);
        return new AlarmViewHolder(view);
    }

   @Override
    public void onBindViewHolder(@NonNull AlarmViewHolder holder, int position) {
        Alarm alarm = alarms.get(position);
        holder.alarmTimeText.setText(alarm.getTime());

        // Recuperar el estado del toggle desde SharedPreferences
        SharedPreferences prefs = holder.itemView.getContext().getSharedPreferences("AlarmPrefs", Context.MODE_PRIVATE);
        boolean isActive = prefs.getBoolean("alarmActive_" + alarm.getKey(), alarm.isActive());
        holder.alarmToggle.setChecked(isActive);

        // Manejar el cambio de estado del toggle
        holder.alarmToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            android.util.Log.d("AlarmAdapter", "Alarma Key: " + alarm.getKey() + " Estado: " + (isChecked ? "Activa" : "Inactiva"));

            // Guardar el estado en SharedPreferences
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("alarmActive_" + alarm.getKey(), isChecked);
            editor.apply();
        });

        // Manejar clics en el elemento
        holder.itemView.setOnClickListener(v -> listener.onAlarmClick(alarm));
    }

    @Override
    public int getItemCount() {
        return alarms.size();
    }

    public static class AlarmViewHolder extends RecyclerView.ViewHolder {
        TextView alarmTimeText;
        Switch alarmToggle;

        public AlarmViewHolder(@NonNull View itemView) {
            super(itemView);
            alarmTimeText = itemView.findViewById(R.id.alarmTimeText);
            alarmToggle = itemView.findViewById(R.id.alarmToggle);
        }
    }

    @FunctionalInterface
    public interface OnAlarmClickListener {
        void onAlarmClick(Alarm alarm);
    }

    public void updateAlarms(List<Alarm> newAlarms) {
        this.alarms.clear();
        this.alarms.addAll(newAlarms);
        notifyDataSetChanged();
    }
}