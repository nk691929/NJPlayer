package com.example.Music;


import java.io.Serializable;

public class MusicModel implements Serializable {

    public String path;
    public String title;
    String duration;
    public MusicModel(String path, String title, String duration) {
        this.path = path;
        this.title = title;
        this.duration = duration;
    }

    public String getPath() {
        return path;
    }

    public String getTitle() {
        return title;
    }

    public String getDuration() {
        return duration;
    }
}