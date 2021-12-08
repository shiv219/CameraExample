package com.contest.runtimepermission.permission




class PermissionRequest(
    var requestCode: Int? = null,
    var resultCallback: (PermissionResult.() -> Unit)? = null
)