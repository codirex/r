package com.musicplayer.app.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "playlists")
public class Playlist {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String name;
    private String coverImagePath;
    private String mood;
    private boolean isSystemPlaylist; // all songs, favourite, most played
    private long createdAt;
    
    public Playlist(String name, String coverImagePath, String mood, boolean isSystemPlaylist) {
        this.name = name;
        this.coverImagePath = coverImagePath;
        this.mood = mood;
        this.isSystemPlaylist = isSystemPlaylist;
        this.createdAt = System.currentTimeMillis();
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCoverImagePath() { return coverImagePath; }
    public void setCoverImagePath(String coverImagePath) { this.coverImagePath = coverImagePath; }
    public String getMood() { return mood; }
    public void setMood(String mood) { this.mood = mood; }
    public boolean isSystemPlaylist() { return isSystemPlaylist; }
    public void setSystemPlaylist(boolean systemPlaylist) { isSystemPlaylist = systemPlaylist; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
