package com.example

import android.content.Context
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog

object DialogUtil {


    const val OPERATION_CAPTURE_PHOTO = 1
    const val OPERATION_CHOOSE_PHOTO = 2
    const val UPLOAD_VIDEO = 12


    fun chooserDialog(
        context: Context,
        optionArray: Array<String>,
        onSelectItem: (selected: Int) -> Unit
    ) {
        val builder = AlertDialog.Builder(context)
        builder.setSingleChoiceItems(optionArray, -1,
            DialogInterface.OnClickListener { dialog, which ->
                dialog.cancel()
                if (which == 0)
                    onSelectItem(OPERATION_CAPTURE_PHOTO)
                else onSelectItem(OPERATION_CHOOSE_PHOTO)
            })
        builder.create().show()
        builder.setCancelable(true)
    }
}