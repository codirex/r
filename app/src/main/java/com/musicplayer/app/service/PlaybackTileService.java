package com.musicplayer.app.service;

import android.content.Intent;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import com.musicplayer.app.ui.main.MainActivity;

/**
 * Quick settings tile (shown when the user pulls down the status bar, alongside
 * Wi-Fi / Bluetooth / Airplane mode tiles): toggles playback with a single tap.
 */
public class PlaybackTileService extends TileService {

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        // Toggle playback through the service's media button intent.
        Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        intent.setPackage(getPackageName());
        intent.putExtra(Intent.EXTRA_KEY_EVENT, new android.view.KeyEvent(
                android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
        sendBroadcast(intent);
        intent.putExtra(Intent.EXTRA_KEY_EVENT, new android.view.KeyEvent(
                android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
        sendBroadcast(intent);
        updateTile();
    }

    private void updateTile() {
        Tile tile = getQsTile();
        if (tile == null) return;
        tile.setLabel(getString(com.musicplayer.app.R.string.app_name));
        tile.setState(Tile.STATE_ACTIVE);
        tile.updateTile();
    }

    @Override
    public void onAddTile() {
        super.onAddTile();
        // Open the app once when the tile is first added so the user can
        // grant notification permission for media playback.
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivityAndCollapse(intent);
    }
}
