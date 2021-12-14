package com.homehub_cam.utils;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import androidx.core.content.FileProvider;

import com.bumptech.glide.load.resource.bitmap.TransformationUtils;
import com.homehub_cam.listener.OnPatchCreateClickListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;

public class Utils {

    public static void writeCoordinatesIntoFile (File photo, double latitude, double longitude) throws IOException{
        ExifInterface exif = null;

        try{
            exif = new ExifInterface(photo.getCanonicalPath());
            if (exif != null) {
                double latitu = latitude;
                double longitu = longitude;
                double alat = Math.abs(latitu);
                double along = Math.abs(longitu);
                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, String.valueOf(alat));
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, String.valueOf(along));
                exif.saveAttributes();
                /*String lati = exif.getAttribute (ExifInterface.TAG_GPS_LATITUDE);
                String longi = exif.getAttribute (ExifInterface.TAG_GPS_LONGITUDE);
                Log.v("latiResult", ""+ lati);
                Log.v("longiResult", ""+ longi);*/
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (halfHeight / inSampleSize > reqHeight
                    && halfWidth / inSampleSize > reqWidth
            ) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    public static Bitmap rotateImageIfRequired(Context context,
                                               Bitmap img,
                                               Uri selectedImage) throws IOException {
        InputStream input = context.getContentResolver().openInputStream(selectedImage);
        ExifInterface ei = null;
        if (Build.VERSION.SDK_INT > 23) {
            ei = new ExifInterface(input);
        } else {
            ei = new ExifInterface(selectedImage.getPath());
        }
        int orientation = ei.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
        );
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return TransformationUtils.rotateImage(img, 90);
            case ExifInterface.ORIENTATION_ROTATE_180:
                return TransformationUtils.rotateImage(img, 180);
            case ExifInterface.ORIENTATION_ROTATE_270:
                return TransformationUtils.rotateImage(img, 270);
            default:
                return img;
        }
    }


    public static String captureImage(Context context) {

        Intent callCameraApplicationIntent = new Intent();
        callCameraApplicationIntent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = null;

        // If the createImageFile will be successful, the photo file will have the address of the file
        photoFile = createImageFile();
        // Here we call the function that will try to catch the exception made by the throw function

        // Here we add an extra file to the intent to put the address on to. For this purpose we use the FileProvider, declared in the AndroidManifest.
        Uri outputUri = FileProvider.getUriForFile(
                context,
                "com.example.fileprovider",
                photoFile
        );
        callCameraApplicationIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputUri);

        // The following is a new line with a trying attempt
        callCameraApplicationIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);


        // The following strings calls the camera app and wait for his file in return.
        ((Activity) context).startActivityForResult(callCameraApplicationIntent, 1000);

        return photoFile.getAbsolutePath();
    }

    public static File createImageFile() {

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmSS").format(new Date());
        String imageFileName = "IMAGE_" + timeStamp;
        File storageDirectory =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES + "/homehub");

        // Then we create the storage directory if does not exists
        if (!storageDirectory.exists()) storageDirectory.mkdir();

        // Here we create the file using a prefix, a suffix and a directory
        File image = new File(storageDirectory, imageFileName + ".jpg");
        // File image = File.createTempFile(imageFileName, ".jpg", storageDirectory);

        // fileUri = Uri.parse(mImageFileLocation);
        // The file is returned to the previous intent across the camera application
        return image;
    }

    public static String capturePhoto(Context context) throws IOException {
        Uri mUri;
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File capturedImage = File.createTempFile(
                Calendar.getInstance().getTimeInMillis() + "My_Captured_Photo.jpg",  /* prefix */
                ".jpg",  /* suffix */
                storageDir /* directory */
        );

/*
    val capturedImage = File(requireActivity().externalCacheDir, Calendar.getInstance().timeInMillis.toString()+"My_Captured_Photo.jpg")
    if (capturedImage.exists()) {
        capturedImage.delete()
    }
    capturedImage.createNewFile()*/

        if (Build.VERSION.SDK_INT >= 24) {
            mUri = FileProvider.getUriForFile(
                    context, "com.example.fileprovider",
                    capturedImage
            );
        } else {
            mUri = Uri.fromFile(capturedImage);
        }
        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        intent.putExtra(MediaStore.EXTRA_OUTPUT, mUri);
        ((Activity) context).startActivityForResult(intent, 1000);
        return capturedImage.getAbsolutePath();

    }


    public static void compressImage(Context context, String imagePath, OnPatchCreateClickListener listener) throws IOException {
        File f = new File(context.getCacheDir(), Calendar.getInstance().getTimeInMillis() + ".jpg");
        f.createNewFile();
        Thread backThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);

                int options = 90;
                byte[] bitmapdata = out.toByteArray();
                while (bitmapdata.length / 1024 > 400) {  //Loop if compressed picture is greater than 400kb, than to compression
                    out.reset(); //Reset baos is empty baos
                    bitmap.compress(
                            Bitmap.CompressFormat.JPEG,
                            options,
                            out
                    ); //The compression options%, storing the compressed data to the baos
                    options -= 10; //Every time reduced by 10
                }

                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(f);
                    fos.write(bitmapdata);
                    fos.flush();
                    fos.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException exception) {
                    exception.printStackTrace();
                }

            }
        });
        backThread.start();
        listener.onImagePathCreated(f.getAbsolutePath());
    }

    public static void compressBitmapImage(Context context, Bitmap bitmap, OnPatchCreateClickListener listener) throws IOException {
        File f = new File(context.getCacheDir(), Calendar.getInstance().getTimeInMillis() + ".jpg");
        f.createNewFile();
        Thread backThread = new Thread(new Runnable() {
            @Override
            public void run() {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);

                int options = 90;
                byte[] bitmapdata = out.toByteArray();
                while (bitmapdata.length / 1024 > 400) {  //Loop if compressed picture is greater than 400kb, than to compression
                    out.reset(); //Reset baos is empty baos
                    bitmap.compress(
                            Bitmap.CompressFormat.JPEG,
                            options,
                            out
                    ); //The compression options%, storing the compressed data to the baos
                    options -= 10; //Every time reduced by 10
                }

                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(f);
                    fos.write(bitmapdata);
                    fos.flush();
                    fos.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException exception) {
                    exception.printStackTrace();
                }

            }
        });
        backThread.start();
    }


};

