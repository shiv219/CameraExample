package com.example

import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.telephony.TelephonyManager
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.*
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import com.bumptech.glide.load.resource.bitmap.TransformationUtils.rotateImage
import kotlinx.coroutines.launch
import java.io.*
import java.util.*

object CommonUtils {

        fun setErrorNull(
            errorView: TextInputLayout?,
            errorMessage: String,
            nullErrorView: Array<TextInputLayout>?
        ) {
            errorView?.error = errorMessage
            errorView?.errorIconDrawable = null
            nullErrorView?.forEach {
                it.error = null
            }
        }

        private fun getCountryISoCode(context: Context): String {
            val manager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            return manager.simCountryIso.toUpperCase(Locale.ROOT)
        }

      private fun getImagePath(context: Context, uri: Uri?, selection: String?): String {
            var path: String? = null
            val cursor = context.contentResolver.query(uri!!, null, selection, null, null)
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA))
                }
                cursor.close()
            }
            return path!!
        }


        fun getDate(
            context: Context,
            startCalendar: Calendar? = null,
            endCalendar: Calendar? = null,
            onDateSelected: (calendar: Calendar) -> Unit
        ) {
            val calendar = startCalendar ?: endCalendar
            val myCalendar = Calendar.getInstance()
            val mYear = myCalendar.get(Calendar.YEAR)
            val mMonth = myCalendar.get(Calendar.MONTH)
            val mDay = myCalendar.get(Calendar.DAY_OF_MONTH)


            val mDatePicker = DatePickerDialog(
                context,
                DatePickerDialog.OnDateSetListener { _, selectedYear, selectedMonth, selectedDay ->
                    myCalendar.set(Calendar.YEAR, selectedYear)
                    myCalendar.set(Calendar.MONTH, selectedMonth)
                    myCalendar.set(Calendar.DAY_OF_MONTH, selectedDay)
                    onDateSelected(myCalendar)
                }, mYear - 1, mMonth, mDay
            )
            if (calendar == null) {
                val value = myCalendar.timeInMillis
                mDatePicker.datePicker.minDate = value
            } else {
                mDatePicker.datePicker.minDate = (calendar.timeInMillis + (24 * 60 * 60 * 1000))
            }
            mDatePicker.show()
        }

        fun calculateInSampleSize(
            options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int
        ): Int {
            // Raw height and width of image
            val height = options.outHeight
            val width = options.outWidth
            var inSampleSize = 1
            if (height > reqHeight || width > reqWidth) {
                val halfHeight = height / 2
                val halfWidth = width / 2

                // Calculate the largest inSampleSize value that is a power of 2 and keeps both
                // height and width larger than the requested height and width.
                while (halfHeight / inSampleSize > reqHeight
                    && halfWidth / inSampleSize > reqWidth
                ) {
                    inSampleSize *= 2
                }
            }
            return inSampleSize
        }

        @Throws(IOException::class)
        fun rotateImageIfRequired(
            context: Context,
            img: Bitmap,
            selectedImage: Uri
        ): Bitmap? {
            val input: InputStream? = context.contentResolver.openInputStream(selectedImage)
            val ei: ExifInterface
            ei =
                if (Build.VERSION.SDK_INT > 23) ExifInterface(input!!) else ExifInterface(selectedImage.path!!)
            val orientation = ei.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            return when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(img, 90)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(img, 180)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(img, 270)
                else -> img
            }
        }

        fun getFileSize(path: String): Long {
            val file = File(path)
            val mOriginSizeFile = file.length()
            val fileSizeInKB: Long = mOriginSizeFile / 1024
            return fileSizeInKB / 1024
        }


        fun getFileFromBitmap(context: Context, bmp: Bitmap): String? {
//create a file to write bitmap data
            val f = File(
                context.cacheDir,
                Calendar.getInstance().timeInMillis.toString() + ".jpg"
            );
            f.createNewFile();
//Convert bitmap to byte array
            val bos = ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, bos);
            val bitmapData = bos.toByteArray();

//write the bytes in file
            val fos = FileOutputStream(f);
            fos.write(bitmapData);
            fos.flush();
            fos.close();
            return f.absolutePath
        }

        fun dateToTimeInMil(dateStr: String): Long {
            val DATE_DASH_FORMAT = "yyyy-MM-dd"
            var time: Long = 0
            try {
                val date: Date? = SimpleDateFormat(DATE_DASH_FORMAT, Locale.ENGLISH).parse(dateStr)
                time = date?.time!!
            } catch (e: ParseException) {
                e.printStackTrace()
            }
            return time
        }

        fun todayTimeInMili() = Calendar.getInstance().timeInMillis


        fun deleteCache(context: Context) {
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val dir = context.cacheDir
                    deleteDir(dir)
                } catch (e: Exception) {
                }
            }
        }

        private fun deleteDir(dir: File?): Boolean {
            return if (dir != null && dir.isDirectory) {
                val children = dir.list()
                for (i in children.indices) {
                    val success = deleteDir(File(dir, children[i]))
                    if (!success) {
                        return false
                    }
                }
                dir.delete()
            } else if (dir != null && dir.isFile) {
                dir.delete()
            } else {
                false
            }
        }
    }