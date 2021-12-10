package com.example;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;

import androidx.fragment.app.Fragment;

import com.contest.runtimepermission.permission.PermissionExtensionsKt;
import com.contest.runtimepermission.permission.PermissionManager;
import com.contest.runtimepermission.permission.PermissionRequest;
import com.contest.runtimepermission.permission.PermissionResult;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.iknow.android.features.select.VideoSelectActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import kotlin.Unit;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MainFragment extends Fragment {
    private String mImageFileLocation;
    public static final int OPERATION_CAPTURE_PHOTO = 1;
    public static final int OPERATION_CAPTURE_VIDEO = 12;

    public void onViewCreated(@NotNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.tvClick).setOnClickListener(it ->
                MainFragment.this.showChooser()
        );
        view.findViewById(R.id.tvVideo).setOnClickListener(it ->
                dispatchTakeVideoIntent()
        );
//        view.findViewById(R.id.tvVideo).setOnClickListener(it ->
//                this.startActivityForResult(
//                        new Intent(getContext(), VideoSelectActivity.class),
//                        OPERATION_CAPTURE_VIDEO)
//        );
    }

    public MainFragment() {
        super(R.layout.fragment);
    }

    static final int REQUEST_VIDEO_CAPTURE = 1;

    private void dispatchTakeVideoIntent() {
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (takeVideoIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);
        }
    }

    private void showChooser() {
        if (PermissionExtensionsKt.checkPermission(
                MainFragment.this,
                Manifest.permission.CAMERA)
        ) {
            mImageFileLocation = Utils.captureImage(getContext());
        } else {
            PermissionRequest permissionRequest = new PermissionRequest();
            permissionRequest.setRequestCode(1);
            permissionRequest.setResultCallback((request) -> success(request));
            permissionRequest.getRequestCode();
            PermissionManager.Companion.requestPermission(
                    MainFragment.this,
                    OPERATION_CAPTURE_PHOTO,
                    permissionRequest.getResultCallback(),
                    Manifest.permission.CAMERA);
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUEST_VIDEO_CAPTURE && resultCode == RESULT_OK) {
            Uri videoUri = intent.getData();
//            videoView.setVideoURI(videoUri);
        }
    }

//    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (resultCode == Activity.RESULT_OK) {
//            switch (requestCode) {
//                case OPERATION_CAPTURE_VIDEO:
//                    String selectedVideoPath = data.getExtras().getString("Data");
//                    String duration = data.getExtras().getString("Duration");
//                    Bitmap thumb = ThumbnailUtils.createVideoThumbnail(
//                            selectedVideoPath,
//                            MediaStore.Images.Thumbnails.FULL_SCREEN_KIND
//                    );
//                    //
//                    //                    setBitmap here setImageBitmap(thumb)
//                    //                    commonUtils.getFileFromBitmap(requireActivity(), thumb)
//                    break;
//                case OPERATION_CAPTURE_PHOTO:
//                    Uri uri = Uri.fromFile(new File(mImageFileLocation));
//                    try {
//                        Bitmap bitmap =
//                                BitmapFactory.decodeFile(mImageFileLocation);
//                        Bitmap rotatedImage = Utils.rotateImageIfRequired(
//                                getContext(),
//                                Bitmap.createScaledBitmap(bitmap, 500, 500, true),
//                                uri
//                        );
//                        Utils.compressBitmapImage(getContext(), rotatedImage, new OnPatchCreateClickListener() {
//                            @Override
//                            public void onImagePathCreated(String path) {
////                     path
//                            }
//                        });
//                    } catch (FileNotFoundException e) {
//                        e.printStackTrace();
//                    } catch (IOException exception) {
//                        exception.printStackTrace();
//                    }
//                    break;
//            }
//        }
//    }


    private Unit success(PermissionResult permissionResult) {
        if (permissionResult instanceof PermissionResult.PermissionGranted) {
            if (permissionResult.getRequestCode() == OPERATION_CAPTURE_PHOTO) {
                mImageFileLocation = Utils.captureImage(getContext());
            }
        } else if (permissionResult instanceof PermissionResult.PermissionDenied) {
            // left intentionally
        } else if (permissionResult instanceof PermissionResult.ShowRational) {
            new MaterialAlertDialogBuilder(getContext())
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
                                    permission = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
                                    break;
                                case 3:
                                    permission = new String[]{
                                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                            Manifest.permission.CAMERA};
                                    break;
                            }
                            PermissionRequest permissionRequest = new PermissionRequest();
                            permissionRequest.setRequestCode(1);
                            permissionRequest.setResultCallback((request) -> success(request));
                            permissionRequest.getRequestCode();
                            PermissionManager.Companion.requestPermission(MainFragment.this,
                                    OPERATION_CAPTURE_PHOTO,
                                    permissionRequest.getResultCallback(),
                                    permission);
                        }
                    }).create()
                    .show();
        }
        return Unit.INSTANCE;
    }

}
