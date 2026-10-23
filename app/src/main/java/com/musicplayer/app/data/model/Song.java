package com.musicplayer.app.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "songs")
public class Song {
    @PrimaryKey
    private long id;
    private String title;
    private String artist;
    private String album;
    private String albumId;
    private String genre;
    private String path;
    private long duration;
    private long size;
    private String dateAdded;
    private String year;
    private boolean isFavorite;
    private int playCount;
    private long lastPlayed;
    private long addedToLibrary;
    private String artworkUri;

    public Song(long id, String title, String artist, String album, String albumId, 
                String genre, String path, long duration, long size, String dateAdded, 
                String year, boolean isFavorite, int playCount, long lastPlayed, 
                long addedToLibrary, String artworkUri) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.albumId = albumId;
        this.genre = genre;
        this.path = path;
        this.duration = duration;
        this.size = size;
        this.dateAdded = dateAdded;
        this.year = year;
        this.isFavorite = isFavorite;
        this.playCount = playCount;
        this.lastPlayed = lastPlayed;
        this.addedToLibrary = addedToLibrary;
        this.artworkUri = artworkUri;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }
    public String getAlbum() { return album; }
    public void setAlbum(String album) { this.album = album; }
    public String getAlbumId() { return albumId; }
    public void setAlbumId(String albumId) { this.albumId = albumId; }
    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
    public String getDateAdded() { return dateAdded; }
    public void setDateAdded(String dateAdded) { this.dateAdded = dateAdded; }
    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }
    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }
    public int getPlayCount() { return playCount; }
    public void setPlayCount(int playCount) { this.playCount = playCount; }
    public long getLastPlayed() { return lastPlayed; }
    public void setLastPlayed(long lastPlayed) { this.lastPlayed = lastPlayed; }
    public long getAddedToLibrary() { return addedToLibrary; }
    public void setAddedToLibrary(long addedToLibrary) { this.addedToLibrary = addedToLibrary; }
    public String getArtworkUri() { return artworkUri; }
    public void setArtworkUri(String artworkUri) { this.artworkUri = artworkUri; }
}
