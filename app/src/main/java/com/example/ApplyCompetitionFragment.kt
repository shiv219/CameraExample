package com.example

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.TextView
import androidx.core.graphics.scale
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.contest.runtimepermission.permission.PermissionResult
import com.contest.runtimepermission.permission.checkPermission
import com.contest.runtimepermission.permission.requestPermissions
import com.example.DialogUtil.OPERATION_CAPTURE_PHOTO
import com.example.DialogUtil.OPERATION_CHOOSE_PHOTO
import com.example.DialogUtil.UPLOAD_VIDEO
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.iknow.android.features.select.VideoSelectActivity

import kotlinx.coroutines.launch
import java.io.File
import java.io.FileNotFoundException


class ApplyFragment : Fragment(R.layout.fragment) {

    private lateinit var mImageFileLocation: String
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<TextView>(R.id.tvClick).setOnClickListener {
            showChooser()
        }

        view.findViewById<TextView>(R.id.tvVideo).setOnClickListener {
            startActivityForResult(
                Intent(requireActivity(), VideoSelectActivity::class.java),
                DialogUtil.UPLOAD_VIDEO
            )
        }
    }

    private fun showChooser() {
        DialogUtil.chooserDialog(
            requireActivity(), arrayOf(
                getString(R.string.take_photo),
                getString(R.string.choose_from_gallery)
            )
        ) {
            when (it) {
                OPERATION_CAPTURE_PHOTO -> {
                    if (checkPermission(Manifest.permission.CAMERA)) {
                        mImageFileLocation = captureImage()
                    } else
                        requestPermissions(Manifest.permission.CAMERA) {
                            requestCode = OPERATION_CAPTURE_PHOTO
                            resultCallback = {
                                handlePermissionsResult(this)
                            }
                        }
                }
                OPERATION_CHOOSE_PHOTO -> {
                    if (checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        openGallery()
                    } else
                        requestPermissions(
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ) {
                            requestCode = OPERATION_CHOOSE_PHOTO
                            resultCallback = {
                                handlePermissionsResult(this)
                            }
                        }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                UPLOAD_VIDEO -> {
                    val selectedVideoPath = data?.extras?.getString("Data")
                    val duration = data?.extras?.getString("Duration")
                    val thumb = selectedVideoPath?.let {
                        ThumbnailUtils.createVideoThumbnail(
                            it,
                            MediaStore.Images.Thumbnails.FULL_SCREEN_KIND
                        )
                    }
//                    mBinding.ivCompetitionImage.setImageBitmap(thumb)
//                    uploadVideoViewModell.apply {
//                        applyCompetitionRequestBody.apply_video_duration = duration
//                        applyCompetitionRequestBody.apply_video = selectedVideoPath
//                        applyCompetitionRequestBody.apply_video_poster =
//                            commonUtils.getFileFromBitmap(requireActivity(), thumb)
//                        uploadIconVisibility.value = false
//                    }
                }
                OPERATION_CHOOSE_PHOTO -> {
                    if (data != null) {
                        val uri = data.data
                        val options = BitmapFactory.Options()
                        options.inJustDecodeBounds = true
                        try {
                            BitmapFactory.decodeStream(
                                requireActivity().contentResolver.openInputStream(uri!!),
                                null,
                                options
                            )
                            options.inSampleSize = CommonUtils.calculateInSampleSize(options, 500, 500)
                            options.inJustDecodeBounds = false
                            val image: Bitmap? = BitmapFactory.decodeStream(
                                requireActivity().contentResolver.openInputStream(uri),
                                null,
                                options
                            )
                            val rotatedImage = CommonUtils.rotateImageIfRequired(requireActivity(), image!!, uri)
//                            mBinding.ivCompetitionImage.setImageBitmap(rotatedImage)
                            lifecycleScope.launch {
//                                compressBitmapImage(requireActivity(), rotatedImage!!) {
//                                    uploadVideoViewModel.applyCompetitionRequestBody.apply_image =
//                                        it
//                                    uploadVideoViewModel.uploadIconVisibility.value = false
//                                }
                            }
                        } catch (e: FileNotFoundException) {
                            e.printStackTrace()
                        }

                    }
                }
                OPERATION_CAPTURE_PHOTO -> {

                    val uri: Uri = Uri.fromFile(File(mImageFileLocation))
                    try {
                        val bitmap =
                            BitmapFactory.decodeFile(mImageFileLocation)
                        val rotatedImage = CommonUtils.rotateImageIfRequired(
                            requireActivity(),
                            bitmap.scale(500, 500)!!,
                            uri!!
                        )
//                        mBinding.ivCompetitionImage.setImageBitmap(rotatedImage)
                        lifecycleScope.launch {
//                            compressBitmapImage(requireActivity(), rotatedImage!!) {
//                                uploadVideoViewModel.applyCompetitionRequestBody.apply_image = it
//                                uploadVideoViewModel.uploadIconVisibility.value = false
//                            }
                        }
                    } catch (e: FileNotFoundException) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun handlePermissionsResult(permissionResult: PermissionResult) {
        when (permissionResult) {
            is PermissionResult.PermissionGranted -> {

                when (permissionResult.requestCode) {
                    OPERATION_CAPTURE_PHOTO -> {
                        mImageFileLocation = captureImage()
                    }
                    OPERATION_CHOOSE_PHOTO -> {
                        openGallery()
                    }
                }
            }
            is PermissionResult.PermissionDenied -> {
                // left intentionally
            }
            is PermissionResult.ShowRational -> {
                MaterialAlertDialogBuilder(requireContext())
                    .setMessage("We need permission")
                    .setTitle("Rational")
                    .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setPositiveButton("OK") { _, _ ->
                        val permissions = when (permissionResult.requestCode) {
                            1 -> {
                                arrayOf(Manifest.permission.CAMERA)
                            }
                            2 -> {
                                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            }
                            3 -> {
                                arrayOf(
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    Manifest.permission.CAMERA
                                )
                            }
                            else -> {
                                arrayOf()
                            }
                        }
                        requestPermissions(*permissions) {
                            requestCode = permissionResult.requestCode
                            resultCallback = {
                                handlePermissionsResult(this)
                            }
                        }
                    }
                    .create()
                    .show()
            }
            is PermissionResult.PermissionDeniedPermanently -> {
//                getViewDataBinding().root.snack("Denied permanently!") {
//                 }
            }
        }
    }
}
