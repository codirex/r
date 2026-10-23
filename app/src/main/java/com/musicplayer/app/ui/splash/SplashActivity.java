package com.musicplayer.app.ui.splash;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.musicplayer.app.R;
import com.musicplayer.app.ui.main.MainActivity;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SplashActivity extends AppCompatActivity {

    private ActivityResultLauncher<String[]> permissionLauncher;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    // Start main activity after permissions
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        startActivity(new Intent(SplashActivity.this, MainActivity.class));
                        finish();
                    }, 1000);
                }
        );
        
        requestPermissions();
    }
    
    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            String[] permissions = {android.Manifest.permission.READ_MEDIA_AUDIO, 
                                   android.Manifest.permission.POST_NOTIFICATIONS};
            if (hasPermissions(permissions)) {
                permissionLauncher.launch(permissions);
            } else {
                permissionLauncher.launch(permissions);
            }
        } else {
            String[] permissions = {android.Manifest.permission.READ_EXTERNAL_STORAGE};
            if (hasPermissions(permissions)) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    startActivity(new Intent(SplashActivity.this, MainActivity.class));
                    finish();
                }, 1000);
            } else {
                permissionLauncher.launch(permissions);
            }
        }
    }
    
    private boolean hasPermissions(String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}
