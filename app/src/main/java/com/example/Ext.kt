package com.example

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.MainThread
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.Logger
import kotlin.reflect.KClass

fun Fragment.captureImage(): String {

    val callCameraApplicationIntent = Intent()
    callCameraApplicationIntent.action = MediaStore.ACTION_IMAGE_CAPTURE

    // We give some instruction to the intent to save the image
    var photoFile: File? = null

    try {
        // If the createImageFile will be successful, the photo file will have the address of the file
        photoFile = createImageFile()
        // Here we call the function that will try to catch the exception made by the throw function
    } catch (e: IOException) {
        Logger.getAnonymousLogger().info("Exception error in generating the file")
        e.printStackTrace()
    }

    // Here we add an extra file to the intent to put the address on to. For this purpose we use the FileProvider, declared in the AndroidManifest.
    val outputUri = FileProvider.getUriForFile(
        requireActivity(),
        "com.example.fileprovider",
        photoFile!!
    )
    callCameraApplicationIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputUri)

    // The following is a new line with a trying attempt
    callCameraApplicationIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)


    // The following strings calls the camera app and wait for his file in return.
    startActivityForResult(callCameraApplicationIntent, DialogUtil.OPERATION_CAPTURE_PHOTO)

    return photoFile.absolutePath
}

@Throws(IOException::class)
internal fun createImageFile(): File {

    // Here we create a "non-collision file name", alternatively said, "an unique filename" using the "timeStamp" functionality
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmSS").format(Date())
    val imageFileName = "IMAGE_" + timeStamp
    // Here we specify the environment location and the exact path where we want to save the so-created file
    val storageDirectory =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES + "/photo_saving_app")

    // Then we create the storage directory if does not exists
    if (!storageDirectory.exists()) storageDirectory.mkdir()

    // Here we create the file using a prefix, a suffix and a directory
    val image = File(storageDirectory, imageFileName + ".jpg")
    // File image = File.createTempFile(imageFileName, ".jpg", storageDirectory);

    // fileUri = Uri.parse(mImageFileLocation);
    // The file is returned to the previous intent across the camera application
    return image
}

fun Fragment.capturePhoto(): String {
    var mUri: Uri?
    val storageDir: File? = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    val capturedImage = File.createTempFile(
        Calendar.getInstance().timeInMillis.toString() + "My_Captured_Photo.jpg",  /* prefix */
        ".jpg",  /* suffix */
        storageDir /* directory */
    )

/*
    val capturedImage = File(requireActivity().externalCacheDir, Calendar.getInstance().timeInMillis.toString()+"My_Captured_Photo.jpg")
    if (capturedImage.exists()) {
        capturedImage.delete()
    }
    capturedImage.createNewFile()*/
    mUri = if (Build.VERSION.SDK_INT >= 24) {
        FileProvider.getUriForFile(
            requireActivity(), "com.example.fileprovider",
            capturedImage
        )
    } else {
        Uri.fromFile(capturedImage)
    }
    val intent = Intent("android.media.action.IMAGE_CAPTURE")
    intent.putExtra(MediaStore.EXTRA_OUTPUT, mUri)
    startActivityForResult(intent, DialogUtil.OPERATION_CAPTURE_PHOTO)
    return capturedImage.absolutePath

}

fun Fragment.openGallery() {
    val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
    intent.type = "image/*"
    startActivityForResult(intent, DialogUtil.OPERATION_CHOOSE_PHOTO)

}


suspend fun Fragment.compressImage(
    context: Context,
    imagePath: String?,
    onImagePathCreated: (image: String) -> Unit
) {
    val f = File(context!!.cacheDir, Calendar.getInstance().timeInMillis.toString() + ".jpg")
    f.createNewFile()
    withContext(Dispatchers.IO) {
        val bitmap = BitmapFactory.decodeFile(imagePath)
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)

        var options = 90
        while (out.toByteArray().size / 1024 > 400) {  //Loop if compressed picture is greater than 400kb, than to compression
            out.reset() //Reset baos is empty baos
            bitmap.compress(
                Bitmap.CompressFormat.JPEG,
                options,
                out
            ) //The compression options%, storing the compressed data to the baos
            options -= 10 //Every time reduced by 10
        }
        val bitmapdata: ByteArray = out.toByteArray()


//write the bytes in file

//write the bytes in file
        val fos = FileOutputStream(f)
        fos.write(bitmapdata)
        fos.flush()
        fos.close()
    }
    onImagePathCreated(f.absolutePath)
}

suspend fun Fragment.compressBitmapImage(
    context: Context,
    bitmap: Bitmap,
    onImagePathCreated: (image: String) -> Unit
) {
    val f = File(context!!.cacheDir, Calendar.getInstance().timeInMillis.toString() + ".jpg")
    f.createNewFile()
    withContext(Dispatchers.IO) {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)

        var options = 90
        while (out.toByteArray().size / 1024 > 400) {  //Loop if compressed picture is greater than 400kb, than to compression
            out.reset() //Reset baos is empty baos
            bitmap.compress(
                Bitmap.CompressFormat.JPEG,
                options,
                out
            ) //The compression options%, storing the compressed data to the baos
            options -= 10 //Every time reduced by 10
        }
        val bitmapdata: ByteArray = out.toByteArray()


//write the bytes in file

//write the bytes in file
        val fos = FileOutputStream(f)
        fos.write(bitmapdata)
        fos.flush()
        fos.close()
    }
    onImagePathCreated(f.absolutePath)
}

