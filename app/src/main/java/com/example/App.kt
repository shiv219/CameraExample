package com.example

import android.app.Application
import android.content.Context
import android.util.Log
//import com.github.hiteshsondhi88.libffmpeg.FFmpeg
import iknow.android.utils.BaseUtils
import nl.bravobit.ffmpeg.FFmpeg

class App : Application() {
override fun onCreate() {
        super.onCreate()

        BaseUtils.init(this)
        initFFmpegBinary(this)
    }

    private fun initFFmpegBinary(context: Context) {
        if (!FFmpeg.getInstance(context).isSupported()) {
            Log.e("ZApplication", "Android cup arch not supported!")
        }
    }
}
