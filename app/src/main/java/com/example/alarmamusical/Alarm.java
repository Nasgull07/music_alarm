package com.example.alarmamusical;


public class Alarm {
    private String key; // Cambiar de int a String
    private String time;
    private boolean isActive;

    public Alarm(String key, String time, boolean isActive) {
        this.key = key;
        this.time = time;
        this.isActive = isActive;
    }

    public String getKey() {
        return key;
    }

    public String getTime() {
        return time;
    }

    public boolean isActive() {
        return isActive;
    }
}