package com.androidmod.displaybrightnesscontrol;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.Toast;

public class DisplayBrightnessControl extends Activity {

    boolean success;
    SeekBar mSeekBarBrightness;
    final int minBrightness = 155;
    final int closeInterval = 2000;

    Handler handler = new Handler();
    Runnable finishRunnable = this::finish;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.control_brightness_display);
        load();
    }

    private void load() {

        mSeekBarBrightness = findViewById(R.id.seekBarBrightness);
        mSeekBarBrightness.setMax(100);
        mSeekBarBrightness.setProgress(getBrightness() - minBrightness);

        try {
            getPermission();
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }

        mSeekBarBrightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (b && success) {
                    setBrightness(i + minBrightness);
                    cancelFinishTimer();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                cancelFinishTimer();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if(!success) {
                    Toast.makeText(DisplayBrightnessControl.this,
                            "Permission not granted", Toast.LENGTH_SHORT).show();
                }
                startFinishTimer();
            }
        });

        startFinishTimer();
    }

    private void startFinishTimer() {
        handler.postDelayed(finishRunnable, closeInterval);
    }

    private void cancelFinishTimer() {
        handler.removeCallbacks(finishRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == 1000 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean value = Settings.System.canWrite(getApplicationContext());
            if(value) {
                success = true;
            } else {
                Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).
                        show();
            }
        }
    }

    private void setBrightness(int brightness) {
        ContentResolver contentResolver = getApplicationContext().getContentResolver();
        Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, brightness);
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        layoutParams.screenBrightness = brightness / 255.0f;
        getWindow().setAttributes(layoutParams);
    }

    private int getBrightness() {
        int brightness = minBrightness;

        try{
            ContentResolver contentResolver = getContentResolver();
            brightness = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS);

        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }

        return brightness;
    }

    private void getPermission() throws Settings.SettingNotFoundException {
        boolean value;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            value = Settings.System.canWrite(getApplicationContext());
            if(value){
                success = true;
            } else {
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + getApplicationContext().getPackageName()));
                startActivityForResult(intent, 1000);
            }
        } else {
            value = Settings.System.getInt(getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE) ==
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;
            if(value){
                success = true;
            } else {
                Intent intent = new Intent(Settings.ACTION_DISPLAY_SETTINGS);
                startActivity(intent);
            }
        }
    }
}