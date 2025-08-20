package com.example.alarmamusical;

import java.util.ArrayList;
import java.util.List;

public class Playlist {
    private String id;
    private String name;
    private List<String> songs;

    public Playlist(String id, String name, List<String> songs) {
        this.id = id;
        this.name = name;
        this.songs = songs != null ? songs : new ArrayList<>(); // Asignar lista vacía si es nula
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<String> getSongs() {
        return songs;
    }
}