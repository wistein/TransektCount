package com.wmstein.transektcount

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

/**
 * PermissionsDialogFragment provides the permission handling,
 * which is necessary since Android Marshmallow (M)
 *
 * Created in Kotlin on 2023-05-26,
 * last edited on 2025-02-21
 */
class PermissionsDialogFragment : DialogFragment() {
    private var context: Context? = null
    private var shouldResolve = false
    private var externalGrantNeeded = false
    private var externalGrant30Needed = false

    override fun onAttach(context: Context) {
        super.onAttach(context)

        this.context = context
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NO_TITLE, R.style.PermissionsDialogFragmentStyle)
        isCancelable = false

        // Check for given storage permission
        requestStoragePermissions()
    }

    // Request storage permission
    private fun requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // launch permission request dialog (Android >=11)
            val permission = Manifest.permission.MANAGE_EXTERNAL_STORAGE
            permissionLauncherStorage.launch(permission)
        } else {
            // Android < 11
            val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
            permissionLauncherStorage.launch(permission)
        }
    }

    // Request single permissions in system settings app
    private val permissionLauncherStorage = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    )
    { isGranted ->
        shouldResolve = true
        if (MyDebug.DLOG) Log.d(TAG, "65, onActivityResult: isGranted: $isGranted")

        if (isGranted) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                externalGrantNeeded = false
            } else {
                externalGrant30Needed = false
            }
            dismiss()
        } else {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                externalGrantNeeded = true
            } else {
                externalGrant30Needed = true
            }
            if (MyDebug.DLOG) Log.d(TAG, "79, onActivityResult: Permission denied...")
        }
    }

    override fun onResume() {
        super.onResume()

        if (shouldResolve) {
            if (externalGrantNeeded) {
                if (MyDebug.DLOG) Log.i(TAG, "90, ask storage")
                showAppSettingsStorageDialog()
            }
            if (externalGrant30Needed) {
                // >= API 30
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (MyDebug.DLOG) Log.i(TAG, "95, ask manage storage")
                    showAppSettingsManageStorageDialog()
                }
            }
        }
    }

    // Query missing  external storage permissions for API <30
    private fun showAppSettingsStorageDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.dialog_storage_title))
            .setMessage(getString(R.string.dialog_storage_message))
            .setPositiveButton(getString(R.string.app_settings))
            { _: DialogInterface?, _: Int ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", "com.wmstein.transektcount", null)
                intent.data = uri
                requireContext().startActivity(intent)
                dismiss()
            }
            .setNegativeButton(getString(R.string.cancel))
            { _: DialogInterface?, _: Int ->
                dismiss()
            }
            .create().show()
    }

    // Query missing manage storage permission for API >=30
    @RequiresApi(Build.VERSION_CODES.R)
    private fun showAppSettingsManageStorageDialog() {
        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
        val uri = Uri.fromParts("package", "com.wmstein.transektcount", null)
        intent.data = uri
        startActivity(intent)
        dismiss()
    }

    override fun onDetach() {
        super.onDetach()

        context = null
    }

    companion object {
        private const val TAG = "PermStorFragm"

        @JvmStatic
        fun newInstance(): PermissionsDialogFragment {
            return PermissionsDialogFragment()
        }
    }

}
