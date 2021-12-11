package com.homehub_cam.ui.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.Group;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.homehub_cam.R;
import com.homehub_cam.listener.OnMediaSubmit;
import com.homehub_cam.ui.views.AutoFitTextureView;
import com.homehub_cam.utils.CompareSizesByArea;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class VideoFragment extends BaseFragment implements View.OnClickListener,
        TextureView.SurfaceTextureListener {

    private static final String TAG = VideoFragment.class.getSimpleName();
    private final int MAX_RECORDING_TIME_IN_MILLIS = 60000;

    private static final int SENSOR_ORIENTATION_INVERSE_DEGREES = 270;
    private static final int SENSOR_ORIENTATION_DEFAULT_DEGREES = 90;
    private static final SparseIntArray INVERSE_ORIENTATIONS = new SparseIntArray();
    private static final SparseIntArray DEFAULT_ORIENTATIONS = new SparseIntArray();

    static {
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_270, 0);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_180, 90);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_90, 180);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_0, 270);
    }

    static {
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_90, 0);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_0, 90);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_270, 180);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_180, 270);
    }

    private Integer mSensorOrientation;
    private CaptureRequest.Builder mPreviewBuilder;

    private CameraDevice mCameraDevice;
    private CameraCaptureSession mPreviewSession;

    private Size mPreviewSize;
    private Size mVideoSize;
    private MediaRecorder mMediaRecorder;
    public boolean mIsRecordingVideo;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private final Semaphore mCameraOpenCloseLock = new Semaphore(1);

    private File mCurrentFile;
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
    }

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
        mCtx = getActivity();
        init(view);
        return view;
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        mTextureView = view.findViewById(R.id.mTextureView);
    }


    @Override
    public void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();
        setUpTextureAndOpenCamera();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    private void init(View v) {
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
                    setCountDownTimer(true);
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

    private void prepareForRetake() {
        vCounter.setTextColor(ContextCompat.getColor(mCtx, R.color.white));
        vCounter.setText("00:" + MAX_RECORDING_TIME_IN_MILLIS / 1000);
        playerGroup.setVisibility(View.GONE);
        recorderGroup.setVisibility(View.VISIBLE);
        mRecordVideo.setText("RECORD");
        timer.cancel();
        openCamera(mTextureView.getWidth(), mTextureView.getHeight());
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

    private void setCountDownTimer(boolean setVideoPlayer) {
        vCounterLayout.setVisibility(View.VISIBLE);
        timer = new CountDownTimer(MAX_RECORDING_TIME_IN_MILLIS, 1000) {

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

    protected File getCurrentFile() {
        return mCurrentFile;
    }

    public void setUpTextureAndOpenCamera() {
        if (mTextureView.isAvailable()) {
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(this);
        }
    }

    public void openCamera(int width, int height) {
        final Activity activity = getActivity();
        if (null == activity || activity.isFinishing()) {
            return;
        }
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            String cameraId = manager.getCameraIdList()[0];

            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics
                    .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            if (map == null) {
                throw new RuntimeException("Cannot get available preview/video sizes");
            }
            mVideoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder.class));
            mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                    width, height, mVideoSize);

            int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mTextureView.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            } else {
                mTextureView.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
            }
            configureTransform(width, height);
            mMediaRecorder = new MediaRecorder();
            if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                setUpTextureAndOpenCamera();
                return;
            }
            manager.openCamera(cameraId, mStateCallback, null);
        } catch (CameraAccessException e) {
            Log.e(TAG, "openCamera: Cannot access the camera.");
        } catch (NullPointerException e) {
            Log.e(TAG, "Camera2API is not supported on the device.");
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.");
        }
    }

    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            closePreviewSession();
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mMediaRecorder) {
                mMediaRecorder.release();
                mMediaRecorder = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.");
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    private void startPreview() {
        if (null == mCameraDevice || !mTextureView.isAvailable() || null == mPreviewSize) {
            return;
        }
        try {
            closePreviewSession();
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            Surface previewSurface = new Surface(texture);
            mPreviewBuilder.addTarget(previewSurface);
            mCameraDevice.createCaptureSession(Collections.singletonList(previewSurface),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            mPreviewSession = session;
                            updatePreview();
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Log.e(TAG, "onConfigureFailed: Failed ");
                        }
                    }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void updatePreview() {
        if (null == mCameraDevice) {
            return;
        }
        try {
            setUpCaptureRequestBuilder(mPreviewBuilder);
            HandlerThread thread = new HandlerThread("CameraPreview");
            thread.start();
            mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void closePreviewSession() {
        if (mPreviewSession != null) {
            mPreviewSession.close();
            mPreviewSession = null;
        }
    }

    private void setUpCaptureRequestBuilder(CaptureRequest.Builder builder) {
        builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
    }

    private void setUpMediaRecorder() throws IOException {
        final Activity activity = getActivity();
        if (null == activity) {
            return;
        }
//        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mCurrentFile = getFileLocation();

        mMediaRecorder.setOutputFile(mCurrentFile.getAbsolutePath());
        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
        mMediaRecorder.setVideoFrameRate(profile.videoFrameRate);
        mMediaRecorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);
        mMediaRecorder.setVideoEncodingBitRate(profile.videoBitRate);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);

        /* mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mMediaRecorder.setAudioEncodingBitRate(profile.audioBitRate);
        mMediaRecorder.setAudioSamplingRate(profile.audioSampleRate);*/

        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        switch (mSensorOrientation) {
            case SENSOR_ORIENTATION_DEFAULT_DEGREES:
                mMediaRecorder.setOrientationHint(DEFAULT_ORIENTATIONS.get(rotation));
                break;
            case SENSOR_ORIENTATION_INVERSE_DEGREES:
                mMediaRecorder.setOrientationHint(INVERSE_ORIENTATIONS.get(rotation));
                break;
        }
        mMediaRecorder.prepare();
    }

    public void startRecordingVideo() {
        if (null == mCameraDevice || !mTextureView.isAvailable() || null == mPreviewSize) {
            return;
        }
        try {
            //closePreviewSession();
            setUpMediaRecorder();
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            List<Surface> surfaces = new ArrayList<>();

            Surface previewSurface = new Surface(texture);
            surfaces.add(previewSurface);
            mPreviewBuilder.addTarget(previewSurface);

            //MediaRecorder setup for surface
            Surface recorderSurface = mMediaRecorder.getSurface();
            surfaces.add(recorderSurface);
            mPreviewBuilder.addTarget(recorderSurface);

            // Start a capture session
            mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    mPreviewSession = cameraCaptureSession;
                    updatePreview();
                    requireActivity().runOnUiThread(() -> {
                        mIsRecordingVideo = true;
                        mMediaRecorder.start();
                    });
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Log.e(TAG, "onConfigureFailed: Failed");
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException | IOException e) {
            e.printStackTrace();
        }

    }

    public void stopRecordingVideo() throws CameraAccessException {
        mIsRecordingVideo = false;
        mPreviewSession.stopRepeating();
        mPreviewSession.abortCaptures();
        // Stop recording
        mMediaRecorder.stop();
        mMediaRecorder.reset();
    }

    private File getFileLocation() {
        File mediaStorageDir = new File(mCtx.getFilesDir(), "HOME_HUB");

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        return new File(mediaStorageDir.getPath() + File.separator +
                "VID_" + timeStamp + ".mp4");
    }


    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * In this sample, we choose a video size with 3x4 for  aspect ratio. for more perfectness 720 as well Also, we don't use sizes
     * larger than 1080p, since MediaRecorder cannot handle such a high-resolution video.
     *
     * @param choices The list of available sizes
     * @return The video size 1080p,720px
     */
    private static Size chooseVideoSize(Size[] choices) {
        for (Size size : choices) {
            if (1920 == size.getWidth() && 1080 == size.getHeight()) {
                return size;
            }
        }
        for (Size size : choices) {
            if (size.getWidth() == size.getHeight() * 4 / 3 && size.getWidth() <= 1080) {
                return size;
            }
        }
        Log.e(TAG, "Couldn't find any suitable video size");
        return choices[choices.length - 1];
    }


    /**
     * Given {@code choices} of {@code Size}s supported by a camera, chooses the smallest one whose
     * width and height are at least as large as the respective requested values, and whose aspect
     * ratio matches with the specified value.
     *
     * @param choices     The list of sizes that the camera supports for the intended output class
     * @param width       The minimum desired width
     * @param height      The minimum desired height
     * @param aspectRatio The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    private static Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio) {
        List<Size> bigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * h / w &&
                    option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }

        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    /**
     * Configures the necessary {@link Matrix} transformation to `mTextureView`.
     * This method should not to be called until the camera preview size is determined in
     * openCamera, or until the size of `mTextureView` is fixed.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        Activity activity = getActivity();
        if (null == mTextureView || null == mPreviewSize || null == activity) {
            return;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }


    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
        openCamera(width, height);

    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture,
                                            int width, int height) {
        configureTransform(width, height);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
    }

    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            mCameraDevice = cameraDevice;
            startPreview();
            mCameraOpenCloseLock.release();
            if (null != mTextureView) {
                configureTransform(mTextureView.getWidth(), mTextureView.getHeight());
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            Activity activity = getActivity();
            if (null != activity) {
                activity.finish();
            }
        }

    };

}

