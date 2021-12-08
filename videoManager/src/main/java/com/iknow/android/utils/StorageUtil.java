package com.iknow.android.utils;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import iknow.android.utils.BaseUtils;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * author : J.Chou
 * e-mail : who_know_me@163.com
 * time   : 2017/08/22/11:18 AM
 * version: 1.0
 * description:
 */
@SuppressWarnings({ "ResultOfMethodCallIgnored", "FieldCanBeLocal" })
public class StorageUtil {

  private static final String TAG = "StorageUtil";
  private static String APP_DATA_PATH = "/Android/data/" + "com.talent.contest";
  private static String sDataDir;
  private static String sCacheDir;

  public static String getAppDataDir() {
    if (TextUtils.isEmpty(sDataDir)) {
      try {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
          sDataDir = android.os.Environment.getExternalStorageDirectory().getPath() + APP_DATA_PATH;
          if (TextUtils.isEmpty(sDataDir)) {
            sDataDir = BaseUtils.getContext().getFilesDir().getAbsolutePath();
          }
        } else {
          sDataDir = BaseUtils.getContext().getFilesDir().getAbsolutePath();
        }
      } catch (Throwable e) {
        e.printStackTrace();
        sDataDir = BaseUtils.getContext().getFilesDir().getAbsolutePath();
      }
      File file = new File(sDataDir);
      if (!file.exists()) {//判断文件目录是否存在
        file.mkdirs();
      }
    }
    return sDataDir;
  }

  public static String getCacheDir() {
    if (TextUtils.isEmpty(sCacheDir)) {
      File file = null;
      Context context = BaseUtils.getContext();
      try {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
          file = context.getExternalCacheDir();
          if (file == null || !file.exists()) {
            file = getExternalCacheDirManual(context);
          }
        }
        if (file == null) {
          file = context.getCacheDir();
          if (file == null || !file.exists()) {
            file = getCacheDirManual(context);
          }
        }
        Log.w(TAG, "cache dir = " + file.getAbsolutePath());
        sCacheDir = file.getAbsolutePath();
      } catch (Throwable ignored) {
      }
    }
    return sCacheDir;
  }

  private static File getExternalCacheDirManual(Context context) {
    File dataDir = new File(new File(Environment.getExternalStorageDirectory(), "Android"), "data");
    File appCacheDir = new File(new File(dataDir, context.getPackageName()), "cache");
    if (!appCacheDir.exists()) {
      if (!appCacheDir.mkdirs()) {//
        Log.w(TAG, "Unable to create external cache directory");
        return null;
      }
      try {
        new File(appCacheDir, ".nomedia").createNewFile();
      } catch (IOException e) {
        Log.i(TAG, "Can't create \".nomedia\" file in application external cache directory");
      }
    }
    return appCacheDir;
  }

  @SuppressLint("SdCardPath")
  private static File getCacheDirManual(Context context) {
    String cacheDirPath = "/data/data/" + context.getPackageName() + "/cache";
    return new File(cacheDirPath);
  }

  // 功能描述:删除文件夹下所有文件和文件夹
  public static boolean delFiles(String path) {
    File cacheFile = new File(path);
    if (!cacheFile.exists()) {
      return false;
    }
    File[] files = cacheFile.listFiles();
    for (int i = 0; i < files.length; i++) {
      // 是文件则直接删除
      if (files[i].exists() && files[i].isFile()) {
        files[i].delete();
      } else if (files[i].exists() && files[i].isDirectory()) {
        // 递归删除文件
        delFiles(files[i].getAbsolutePath());
        // 删除完目录下面的所有文件后再删除该文件夹
        files[i].delete();
      }
    }

    return true;
  }

  public static long sizeOfDirectory(File dir) {
    if (dir.exists()) {
      long result = 0;
      File[] fileList = dir.listFiles();
      for (int i = 0; i < fileList.length; i++) {
        // Recursive call if it's a directory
        if (fileList[i].isDirectory()) {
          result += sizeOfDirectory(fileList[i]);
        } else {
          // Sum the file size in bytes
          result += fileList[i].length();
        }
      }
      return result; // return the file size
    }
    return 0;
  }

  /**
   * @param length 长度 byte为单位
   * 将文件大小转换为KB,MB格式
   */
  public static String getFileSize(long length) {
    int MB = 1024 * 1024;
    if (length < MB) {
      double resultKB = length * 1.0 / 1024;
      return String.format(Locale.getDefault(), "%.1f", resultKB) + "Kb";
    }
    double resultMB = length * 1.0 / MB;
    return String.format(Locale.getDefault(), "%.1f", resultMB) + "Mb";
  }

  public static boolean isFileExist(String path) {
    if (TextUtils.isEmpty(path)) return false;
    File file = new File(path);
    return file.exists();
  }

  /**
   * @param path 路径
   * @return 是否删除成功
   */
  public static boolean deleteFile(String path) {
    if (TextUtils.isEmpty(path)) return true;
    return deleteFile(new File(path));
  }

  /**
   * @return 是否删除成功
   */
  public static boolean deleteFile(File file) {
    if (file == null || !file.exists()) return true;

    if (file.isFile()) {
      return file.delete();
    }

    if (!file.isDirectory()) {
      return false;
    }

    for (File f : file.listFiles()) {
      if (f.isFile()) {
        f.delete();
      } else if (f.isDirectory()) {
        deleteFile(f);
      }
    }
    return file.delete();
  }

  @SuppressLint("NewApi")
  public static String getPath(final Context context, final Uri uri) {

    final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

    if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
      if (isExternalStorageDocument(uri)) {
        final String docId = DocumentsContract.getDocumentId(uri);
        final String[] split = docId.split(":");
        final String type = split[0];

        if ("primary".equalsIgnoreCase(type)) {
          return Environment.getExternalStorageDirectory() + "/" + split[1];
        }

      }
      else if (isDownloadsDocument(uri)) {

        final String id = DocumentsContract.getDocumentId(uri);
        final Uri contentUri = ContentUris.withAppendedId(
                Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

        return getDataColumn(context, contentUri, null, null);
      }
      else if (isMediaDocument(uri)) {
        final String docId = DocumentsContract.getDocumentId(uri);
        final String[] split = docId.split(":");
        final String type = split[0];

        Uri contentUri = null;
        if ("image".equals(type)) {
          contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        } else if ("video".equals(type)) {
          contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        } else if ("audio".equals(type)) {
          contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }

        final String selection = "_id=?";
        final String[] selectionArgs = new String[]{
                split[1]
        };

        return getDataColumn(context, contentUri, selection, selectionArgs);
      }
    }
    else if ("content".equalsIgnoreCase(uri.getScheme())) {

      if (isGooglePhotosUri(uri))
        return uri.getLastPathSegment();

      return getDataColumn(context, uri, null, null);
    }
    else if ("file".equalsIgnoreCase(uri.getScheme())) {
      return uri.getPath();
    }

    return null;
  }

  /**
   * Get the value of the data column for this Uri. This is useful for
   * MediaStore Uris, and other file-based ContentProviders.
   *
   * @param context       The context.
   * @param uri           The Uri to query.
   * @param selection     (Optional) Filter used in the query.
   * @param selectionArgs (Optional) Selection arguments used in the query.
   * @return The value of the _data column, which is typically a file path.
   * @author Deep Patel
   */
public static String getDataColumn(@NonNull Context context, Uri uri, String selection, String[] selectionArgs) {

    Cursor cursor = null;
    final String column = "_data";
    final String[] projection = {column};

    try {
      cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
      if (cursor != null && cursor.moveToFirst()) {
        final int column_index = cursor.getColumnIndexOrThrow(column);
        return cursor.getString(column_index);
      }
    } finally {
      if (cursor != null)
        cursor.close();
    }
    return null;
  }

  /**
   * @param uri The Uri to check.
   * @return Whether the Uri authority is Google Photos.
   */
  private static boolean isGooglePhotosUri(Uri uri) {
    return "com.google.android.apps.photos.content".equals(uri.getAuthority());
  }

  /**
   * @param uri The Uri to check.
   * @return Whether the Uri authority is ExternalStorageProvider.
   */
  private static boolean isExternalStorageDocument(@NonNull Uri uri) {
    return "com.android.externalstorage.documents".equals(uri.getAuthority());
  }

  /**
   * @param uri The Uri to check.
   * @return Whether the Uri authority is DownloadsProvider.
   */
  private static boolean isDownloadsDocument(@NonNull Uri uri) {
    return "com.android.providers.downloads.documents".equals(uri.getAuthority());
  }

  /**
   * @param uri The Uri to check.
   * @return Whether the Uri authority is MediaProvider.
   */
  private static boolean isMediaDocument(@NonNull Uri uri) {
    return "com.android.providers.media.documents".equals(uri.getAuthority());
  }
}
