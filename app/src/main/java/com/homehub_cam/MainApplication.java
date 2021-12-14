package com.homehub_cam;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import nl.bravobit.ffmpeg.FFmpeg;


public class MainApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        initFFmpegBinary(getApplicationContext());
    }
    private void initFFmpegBinary(Context context) {
        if (!FFmpeg.getInstance(context).isSupported()) {
            Log.e("ZApplication", "Android cup arch not supported!");
        }
    }
}
