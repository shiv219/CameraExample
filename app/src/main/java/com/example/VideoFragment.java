package com.example;

import android.content.Context;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.constraintlayout.widget.Group;
import androidx.core.content.ContextCompat;


import java.io.File;


/**
 * Use the {@link VideoFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class VideoFragment extends CameraVideoFragment implements View.OnClickListener {

    private final String TAG = VideoFragment.class.getSimpleName();
    private final int MAX_RECORDING_TIME_IN_MILLIS = 60000;

    private Context mCtx;
    private AutoFitTextureView mTextureView;
    private TextView mRecordVideo;
    private VideoView mVideoView;
    private TextView mPlayVideo, vClose;
    private TextView vCounter, vDone, vRetake;
    private OnMediaSubmit onMediaSubmit;
    private Group recorderGroup, playerGroup;


    private String mOutputFilePath;
    private CountDownTimer timer;
    private LinearLayout vCounterLayout;

    public VideoFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     */


    public static VideoFragment newInstance(OnMediaSubmit onMediaSubmit) {
        VideoFragment fragment = new VideoFragment();
        fragment.onMediaSubmit = onMediaSubmit;
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_video, container, false);
        //setFullScreen();
        mCtx = getActivity();
        initViews(view);
        return view;
    }

    private void setFullScreen() {
        getActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        View decorView = getActivity().getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        //| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        //| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    private void initViews(View v) {
        mTextureView = v.findViewById(R.id.mTextureView);
        mRecordVideo = v.findViewById(R.id.mRecordVideo);
        mVideoView = v.findViewById(R.id.mVideoView);
        mPlayVideo = v.findViewById(R.id.mPlayVideo);
        vCounter = v.findViewById(R.id.counter);
        playerGroup = v.findViewById(R.id.videoPlayerGroup);
        recorderGroup = v.findViewById(R.id.videoRecorderGroup);
        vDone = v.findViewById(R.id.done);
        vRetake = v.findViewById(R.id.retake);
        vClose = v.findViewById(R.id.close);
        vCounterLayout = v.findViewById(R.id.counterLayout);
        setOnClickListener();
    }

    private void setOnClickListener() {
        mRecordVideo.setOnClickListener(this);
        mPlayVideo.setOnClickListener(this);
        vDone.setOnClickListener(this);
        vRetake.setOnClickListener(this);
        vClose.setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.mRecordVideo: {
                /**
                 * If media is not recoding then start recording else stop recording
                 */
                if (mIsRecordingVideo) {
                    vCounter.setText("00:00");
                    try {
                        stopRecordingVideo();
                        prepareVideoPlayerViews();
                        if (timer != null) {
                            timer.cancel();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } else {
                    startRecordingVideo();
                    mRecordVideo.setText("STOP");
                    mOutputFilePath = getCurrentFile().getAbsolutePath();
                    setCountDownTimer(MAX_RECORDING_TIME_IN_MILLIS, true);
                }
                break;
            }
            case R.id.mPlayVideo: {
                mVideoView.start();
                mPlayVideo.setVisibility(View.GONE);
                break;
            }
            case R.id.done: {
                submit();
                break;
            }
            case R.id.retake: {
                prepareVideoRecorderViews();
                prepareForRetake();
                break;
            }
            case R.id.close: {
//                Utility.popBackStack(mCtx);
                break;
            }
        }
    }

    @Override
    public int getTextureResource() {
        return R.id.mTextureView;
    }

    private void prepareVideoPlayerViews() {
        if (mVideoView.getVisibility() == View.GONE) {
            playerGroup.setVisibility(View.VISIBLE);
            recorderGroup.setVisibility(View.GONE);
            setMediaForRecordVideo();
        }
    }

    private void prepareVideoRecorderViews() {
        try {
            deleteTheFile(mOutputFilePath);
            playerGroup.setVisibility(View.GONE);
            recorderGroup.setVisibility(View.VISIBLE);
            stopRecordingVideo();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteTheFile(String mOutputFilePath) {
        try {
            File file = new File(mOutputFilePath);
            if (file.exists()) {
                file.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setMediaForRecordVideo() {
        // Set media controller
        mVideoView.setMediaController(new MediaController(getActivity()));
        mVideoView.requestFocus();
        mVideoView.setVideoPath(mOutputFilePath);
        mVideoView.seekTo(100);
        mVideoView.setOnCompletionListener(mp -> {
            // Reset player
            mPlayVideo.setVisibility(View.VISIBLE);
        });
    }

    private void submit() {
        onMediaSubmit.onMediaSubmit(mOutputFilePath);
    }

    private void prepareForRetake() {
        vCounter.setTextColor(ContextCompat.getColor(mCtx, R.color.white));
        vCounter.setText("00:" + MAX_RECORDING_TIME_IN_MILLIS / 1000);
        playerGroup.setVisibility(View.GONE);
        recorderGroup.setVisibility(View.VISIBLE);
        mRecordVideo.setText("RECORD");
        timer.cancel();
        openCamera(mTextureView.getWidth(), mTextureView.getHeight());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public boolean onBackPressed() {
        if (timer != null)
            timer.cancel();
        return false;
    }

    private void setCountDownTimer(int maxTimeInMillis, boolean setVideoPlayer) {
        vCounterLayout.setVisibility(View.VISIBLE);
        timer = new CountDownTimer(maxTimeInMillis, 1000) {

            public void onTick(long millisUntilFinished) {
                int remainingSec = (int) millisUntilFinished / 1000;
                if (remainingSec < 11) {
                    vCounter.setTextColor(ContextCompat.getColor(mCtx, R.color.red));
                } else {
                    vCounter.setTextColor(ContextCompat.getColor(mCtx, R.color.white));
                }
                vCounter.setText("00:" + remainingSec);
            }

            public void onFinish() {
                vCounter.setText("00:00");
                try {
                    if (setVideoPlayer) {
                        stopRecordingVideo();
                        prepareVideoPlayerViews();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }
}