package com.homehub_cam;


import android.Manifest;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.contest.runtimepermission.permission.PermissionExtensionsKt;
import com.contest.runtimepermission.permission.PermissionManager;
import com.contest.runtimepermission.permission.PermissionRequest;
import com.contest.runtimepermission.permission.PermissionResult;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.homehub_cam.ui.fragments.VideoFragment;
import com.homehub_cam.utils.Utils;

import org.jetbrains.annotations.Nullable;

import kotlin.Unit;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int CAMERA_AUDIO_PERMISSION = 1001;

    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_main);
        checkPermissions();
    }

    private void attachFragment() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        VideoFragment mFragment = VideoFragment.newInstance(url -> {
            Log.d(TAG + ": VIDEO_URL", url);
        });
        ft.replace(R.id.frame, mFragment);
        ft.commit();
    }

    private void checkPermissions() {
        if (PermissionExtensionsKt.checkPermission(
                MainActivity.this,
                Manifest.permission.CAMERA) &
                PermissionExtensionsKt.checkPermission(
                        MainActivity.this,
                        Manifest.permission.RECORD_AUDIO)
        ) {
            attachFragment();
        } else {
            PermissionRequest permissionRequest = new PermissionRequest();
            permissionRequest.setRequestCode(1);
            permissionRequest.setResultCallback((request) -> success(request));
            permissionRequest.getRequestCode();
            PermissionManager.Companion.requestPermission(
                    MainActivity.this,
                    CAMERA_AUDIO_PERMISSION,
                    permissionRequest.getResultCallback(),
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.CAMERA);
        }
    }

    private Unit success(PermissionResult permissionResult) {
        if (permissionResult instanceof PermissionResult.PermissionGranted) {
            if (permissionResult.getRequestCode() == CAMERA_AUDIO_PERMISSION) {
                attachFragment();
            }
        } else if (permissionResult instanceof PermissionResult.PermissionDenied) {
            // left intentionally
        } else if (permissionResult instanceof PermissionResult.ShowRational) {
            new MaterialAlertDialogBuilder(this)
                    .setMessage("We need permission")
                    .setTitle("Rational")
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    })
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            String[] permission = new String[]{};
                            switch (permissionResult.getRequestCode()) {
                                case 1:
                                    permission = new String[]{Manifest.permission.CAMERA};
                                    break;
                                case 2:
                                    permission = new String[]{Manifest.permission.RECORD_AUDIO};
                                    break;
                                case 3:
                                    permission = new String[]{
                                            Manifest.permission.RECORD_AUDIO,
                                            Manifest.permission.CAMERA};
                                    break;
                            }
                            PermissionRequest permissionRequest = new PermissionRequest();
                            permissionRequest.setRequestCode(1);
                            permissionRequest.setResultCallback((request) -> success(request));
                            permissionRequest.getRequestCode();
                            PermissionManager.Companion.requestPermission(this,
                                    CAMERA_AUDIO_PERMISSION,
                                    permissionRequest.getResultCallback(),
                                    permission);
                        }
                    })
                    .create()
                    .show();
        }
        return Unit.INSTANCE;
    }
}
