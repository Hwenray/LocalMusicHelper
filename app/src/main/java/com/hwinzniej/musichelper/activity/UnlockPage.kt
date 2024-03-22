package com.hwinzniej.musichelper.activity

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.hwinzniej.musichelper.R
import com.hwinzniej.musichelper.utils.Tools

class UnlockPage(
    val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    componentActivity: ComponentActivity
) : PermissionResultHandler {
    var unlockResult = mutableStateListOf<Map<String, Boolean>>()
    var selectedEncryptedPath = mutableStateOf("")
    var selectedDecryptedPath = mutableStateOf("")

    /**
     * 请求存储权限
     */

//==================== Android 11+ 使用====================

    @RequiresApi(Build.VERSION_CODES.R)
    private val requestPermissionLauncher = componentActivity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        if (Environment.isExternalStorageManager()) {
            init()
        } else {
            Toast.makeText(context, R.string.permission_not_granted_toast, Toast.LENGTH_SHORT)
                .show()
        }
    }

    fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { //Android 11+
            if (Environment.isExternalStorageManager()) {
                init()
            } else {
                Toast.makeText(context, R.string.request_permission_toast, Toast.LENGTH_SHORT)
                    .show()
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                val uri = Uri.fromParts("package", context.packageName, null)
                intent.data = uri
                requestPermissionLauncher.launch(intent)
//                startActivity(context, intent, null)
            }
        } else { //Android 10-
            if (allPermissionsGranted()) {
                init()
            } else {
                Toast.makeText(context, R.string.request_permission_toast, Toast.LENGTH_SHORT)
                    .show()
                ActivityCompat.requestPermissions(
                    context as Activity, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
                )
            }
        }
    }

//==================== Android 11+ 使用====================

//==================== Android 10- 使用====================

    private val REQUEST_CODE_PERMISSIONS = 1002
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    private fun allPermissionsGranted(): Boolean {
        for (permission in REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(
                    context, permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

    override fun onPermissionResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                init()
            } else {
                Toast.makeText(context, R.string.permission_not_granted_toast, Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
//==================== Android 10- 使用====================

    fun handleSelectedEncryptedPath(uri: Uri?) {
        if (uri == null) {
            return
        }
        selectedEncryptedPath.value = Tools().uriToAbsolutePath(uri)
    }

    fun handleSelectedDecryptedPath(uri: Uri?) {
        if (uri == null) {
            return
        }
        selectedDecryptedPath.value = Tools().uriToAbsolutePath(uri)
    }

    fun init() {

    }
}