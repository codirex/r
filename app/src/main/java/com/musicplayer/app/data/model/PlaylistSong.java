package com.musicplayer.app.data.model;

import androidx.room.Entity;
import androidx.room.ForeignKey;

@Entity(tableName = "playlist_songs", 
        primaryKeys = {"playlistId", "songId"},
        foreignKeys = {
            @ForeignKey(entity = Playlist.class, parentColumns = "id", childColumns = "playlistId", onDelete = ForeignKey.CASCADE),
            @ForeignKey(entity = Song.class, parentColumns = "id", childColumns = "songId", onDelete = ForeignKey.CASCADE)
        })
public class PlaylistSong {
    private long playlistId;
    private long songId;
    private int position;
    
    public PlaylistSong(long playlistId, long songId, int position) {
        this.playlistId = playlistId;
        this.songId = songId;
        this.position = position;
    }

    public long getPlaylistId() { return playlistId; }
    public void setPlaylistId(long playlistId) { this.playlistId = playlistId; }
    public long getSongId() { return songId; }
    public void setSongId(long songId) { this.songId = songId; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
}
