package com.contest.runtimepermission.permission

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment


/**
 * @param permissions vararg of all the permissions for request.
 * @param requestBlock block constructing [PermissionRequest] object for permission request.
 */
inline fun AppCompatActivity.requestPermissions(
    vararg permissions: String,
    requestBlock: PermissionRequest.() -> Unit
) {
    PermissionManager.requestPermissions(this, *permissions) { this.requestBlock() }
}

fun AppCompatActivity.checkPermission(permission: String): Boolean {
    val checkSelfPermission = ContextCompat.checkSelfPermission(this, permission)
    if (checkSelfPermission != PackageManager.PERMISSION_GRANTED) {
        return false
    }
    return true
}
/**
 * @param permissions vararg of all the permissions for request.
 * @param requestBlock block constructing [PermissionRequest] object for permission request.
 */
inline fun Fragment.requestPermissions(
    vararg permissions: String,
    requestBlock: PermissionRequest.() -> Unit
) {
    PermissionManager.requestPermissions(this, *permissions) { this.requestBlock() }
}

fun Fragment.checkPermission(permission: String): Boolean {
    val checkSelfPermission = ContextCompat.checkSelfPermission(requireActivity(), permission)
    if (checkSelfPermission != PackageManager.PERMISSION_GRANTED) {
        return false
    }
    return true
}