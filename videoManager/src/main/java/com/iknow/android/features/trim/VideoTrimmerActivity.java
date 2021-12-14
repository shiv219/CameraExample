package com.iknow.android.features.trim;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.fragment.app.FragmentActivity;

import com.iknow.android.R;
import com.iknow.android.features.common.ui.BaseActivity;
import com.iknow.android.features.compress.VideoCompressor;
import com.iknow.android.interfaces.VideoCompressListener;
import com.iknow.android.interfaces.VideoTrimListener;
import com.iknow.android.utils.StorageUtil;
import com.iknow.android.utils.ToastUtil;
import com.iknow.android.widget.VideoTrimmerView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Author：J.Chou
 * Date：  2016.08.01 2:23 PM
 * Email： who_know_me@163.com
 * Describe:
 */
public class VideoTrimmerActivity extends BaseActivity implements VideoTrimListener {

    private static final String TAG = "jason";
    private static final String VIDEO_PATH_KEY = "video-file-path";
    private static final String COMPRESSED_VIDEO_FILE_NAME = "compress.mp4";
    public static final int VIDEO_TRIM_REQUEST_CODE = 0x001;
    //  private ActivityVideoTrimBinding mBinding;
    private ProgressDialog mProgressDialog;
    private VideoTrimmerView trimmerView;
    private Context context;

    public static void call(FragmentActivity from, String videoPath) {
        if (!TextUtils.isEmpty(videoPath)) {
            Bundle bundle = new Bundle();
            bundle.putString(VIDEO_PATH_KEY, videoPath);
            Intent intent = new Intent(from, VideoTrimmerActivity.class);
            intent.putExtras(bundle);
            from.startActivityForResult(intent, VIDEO_TRIM_REQUEST_CODE);
        }
    }

    String copressedOutput;

    @Override
    public void initUI() {
        setContentView(R.layout.activity_video_trim);
        context = this;
        trimmerView = findViewById(R.id.trimmer_view);
        Bundle bd = getIntent().getExtras();
        String path = "";
        if (bd != null) path = bd.getString(VIDEO_PATH_KEY);
        if (trimmerView != null) {
            final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            copressedOutput = StorageUtil.getCacheDir() + "/" + timeStamp + COMPRESSED_VIDEO_FILE_NAME;
            trimmerView.setOnTrimVideoListener(this);
            trimmerView.setOnCompressVideoListener(videoCompressListener, copressedOutput);
            trimmerView.initVideoByURI(Uri.parse(path));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        trimmerView.onVideoPause();
        trimmerView.setRestoreState(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        trimmerView.onDestroy();
    }

    @Override
    public void onStartTrim() {
        buildDialog(getResources().getString(R.string.trimming)).show();
    }

    @Override
    public void onFinishTrim(String in, String duration) {
        if (mProgressDialog.isShowing())
            mProgressDialog.dismiss();
        if (in.equals("FAILED")) {
            ToastUtil.longShow(this, getResources().getString(R.string.failed));
            return;
        }
//        ToastUtil.longShow(this, getString(R.string.trimmed_done));
        long size = getFileSize(in);
        Log.d("VIDEO_TRIMMED_SIZE", String.valueOf(size));
        setData(in, duration);
/*//    TODO: please handle your trimmed video url here!!!
        final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String out = StorageUtil.getCacheDir() + "/" + timeStamp + COMPRESSED_VIDEO_FILE_NAME;
        buildDialog(getResources().getString(R.string.compressing)).show();
        VideoCompressor.compress(this, in, out, new VideoCompressListener() {
            @Override
            public void onSuccess(String message) {
                if (mProgressDialog.isShowing())
                    mProgressDialog.dismiss();
                long compressedSize = getFileSize(out);
                Log.d("VIDEO_COMPRESSED_SIZE", String.valueOf(compressedSize));
                ToastUtil.longShow(context, String.valueOf(compressedSize));
//                setResult(out,duration);
            }

            @Override
            public void onFailure(String message) {
                if (mProgressDialog.isShowing())
                    mProgressDialog.dismiss();
                ToastUtil.longShow(context, getString(R.string.failed_compressing));
            }

            @Override
            public void onFinish() {
                if (mProgressDialog.isShowing()) mProgressDialog.dismiss();
            }
        });*/
    }

    @Override
    public void onCancel() {
        trimmerView.onDestroy();
        finish();
    }

    String duration;

    private void setData(String in, String duration) {
        if (duration != null)
            this.duration = duration;
        long size = getFileSize(in);
        Log.d("VIDEO_TRIMMED_SIZE", String.valueOf(size));
        if (size <= 1) {
            if (mProgressDialog.isShowing()) mProgressDialog.dismiss();
            setResult(in, this.duration);
            return;
        }
        final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        copressedOutput = StorageUtil.getCacheDir() + "/" + timeStamp + COMPRESSED_VIDEO_FILE_NAME;
        if (!mProgressDialog.isShowing())
            buildDialog(getResources().getString(R.string.compressing)).show();
        VideoCompressor.compress(this, in, copressedOutput, videoCompressListener);
    }

    private ProgressDialog buildDialog(String msg) {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
        }
        mProgressDialog.setMessage(msg);
        mProgressDialog.setCancelable(false);
        return mProgressDialog;
    }

    private void setResult(String path, String duration) {
        Intent intent = new Intent();
        intent.putExtra("Data", path);
        intent.putExtra("Duration", duration);
        setResult(RESULT_OK, intent);
        finish();
    }

    private long getFileSize(String path) {
        File file = new File(path);
        Long mOriginSizeFile = file.length();
        long fileSizeInKB = mOriginSizeFile / 1024;

        return fileSizeInKB / 1024;
    }

    VideoCompressListener videoCompressListener = new VideoCompressListener() {
        @Override
        public void onSuccess(String message) {
//            if (mProgressDialog.isShowing()) mProgressDialog.dismiss();
            long compressedSize = getFileSize(copressedOutput);
            ToastUtil.longShow(context, String.valueOf(compressedSize));
            setData(copressedOutput, null);

            Log.d("VIDEO_COMPRESSED_SIZE", String.valueOf(compressedSize));
        }

        @Override
        public void onFailure(String message) {
            if (mProgressDialog.isShowing())
                mProgressDialog.dismiss();
            ToastUtil.longShow(context, getString(R.string.failed_compressing));
        }

        @Override
        public void onStart() {
            buildDialog(getResources().getString(R.string.compressing)).show();
        }

        @Override
        public void onFinish() {
//            if (mProgressDialog.isShowing()) mProgressDialog.dismiss();
        }
    };

}
