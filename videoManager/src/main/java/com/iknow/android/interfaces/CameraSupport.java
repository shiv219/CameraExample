package com.iknow.android.interfaces;

public interface CameraSupport {
    CameraSupport open(int cameraId);

    int getOrientation(int cameraId);
}