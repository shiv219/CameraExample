package com.homehub_cam;


import android.Manifest;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.contest.runtimepermission.permission.PermissionExtensionsKt;
import com.contest.runtimepermission.permission.PermissionManager;
import com.contest.runtimepermission.permission.PermissionRequest;
import com.contest.runtimepermission.permission.PermissionResult;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.homehub_cam.listener.NavigationListener;
import com.homehub_cam.ui.fragments.BaseFragment;
import com.homehub_cam.ui.fragments.LauncherFragment;
import com.homehub_cam.ui.fragments.PhotoFragment;
import com.homehub_cam.ui.fragments.VideoFragment;

import org.jetbrains.annotations.Nullable;

import kotlin.Unit;

public class MainActivity extends AppCompatActivity implements NavigationListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int CAMERA_AUDIO_PERMISSION = 1001;
    public static final int Image_Fragment = 22;
    public static final int Video_Fragment = 33;

    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_main);
        checkPermissions();
        getSupportFragmentManager().addOnBackStackChangedListener(
                () -> {
                    FragmentManager fm = getSupportFragmentManager();
                    if (fm != null) {
                        int backStackCount = fm.getBackStackEntryCount();
                        if (backStackCount == 0) {
                            showStatusBar();
                        }
                    }
                });
    }

    public void showStatusBar() {
        View decorView = getWindow().getDecorView();
        // Show Status Bar.
        int uiOptions = View.SYSTEM_UI_FLAG_VISIBLE;
        decorView.setSystemUiVisibility(uiOptions);
    }

    public void hideStatusBar() {
        View decorView = getWindow().getDecorView();
        // Hide Status Bar.
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
    }

    private void attachFragment(BaseFragment fragment, String tag) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(R.id.frame, fragment).addToBackStack(tag);
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
            attachFragment(new LauncherFragment(this), "Launcher");
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
                attachFragment(new LauncherFragment(this), "Launcher");
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

    @Override
    public void navigate(int fragment) {
        hideStatusBar();
        if (fragment == Video_Fragment) {
            VideoFragment mFragment = VideoFragment.newInstance(url -> {
                Log.d(TAG + ": VIDEO_URL", url);
            });
            attachFragment(mFragment, "Video_Frag");
        } else if (fragment == Image_Fragment) {
            PhotoFragment mFragment = PhotoFragment.newInstance(url -> {
                Log.d(TAG + ": IMAGE_URL", url);
            });
            attachFragment(mFragment, "Photo_Frag");
        }
    }
}
