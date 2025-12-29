package com.wmstein.transektcount

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

/**********************************************************************
 * PermissionsForegroundDialogFragment provides the permission handling, which
 * is necessary since Android Marshmallow (M) for
 * - ACCESS_COARSE_LOCATION,
 * - ACCESS_FINE_LOCATION
 *
 * Based on RuntimePermissionsExample-master created by tylerjroach on 8/31/16,
 * licensed under the MIT License.
 *
 * Adopted for TourCount by wistein on 2019-02-08,
 * last edited in java on 2024-09-30,
 * converted to Kotlin on 2025-01-22,
 * used ba TransektCount on 2025-07-02,
 * last edited on 2025-12-02
 */
class PermissionsForegroundDialogFragment : DialogFragment() {
    private var context: Context? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)

        this.context = context
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "43, onCreate, PermForegr Loc")

        setStyle(STYLE_NO_TITLE, R.style.PermissionsDialogFragmentStyle)
        isCancelable = false

        val permission = Manifest.permission.ACCESS_FINE_LOCATION

        //launch permission request dialog
        permissionLauncherForeground.launch(permission)
    }

    // Request foreground location permission in system settings dialog
    private val permissionLauncherForeground = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    )
    { isGranted ->
        if (isGranted) {
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.d(TAG, "61, permLauncherForegrLoc granted: true")
            dismiss()
        } else {
            showAppSettingsForegroundDialog()
        }
    }

    // Query and set missing foreground permissions
    private fun showAppSettingsForegroundDialog() {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.d(TAG, "71, AppSettingsForegrLocDialog")

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.dialog_fine_location_title))
            .setMessage(getString(R.string.dialog_fine_location_message))
            .setPositiveButton(getString(R.string.app_settings))
            { _: DialogInterface?, _: Int ->
                val intent = Intent()
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                val uri =
                    Uri.fromParts("package", requireContext().applicationContext.packageName, null)
                intent.data = uri
                requireContext().startActivity(intent)
                dismiss()
            }
            .setNegativeButton(getString(R.string.cancelButton)) { _: DialogInterface?, _: Int -> dismiss() }
            .create().show()
    }

    override fun onDetach() {
        super.onDetach()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "94, onDetach")
        context = null
    }

    companion object {
        private const val TAG = "PermLocFragm"

        @JvmStatic
        fun newInstance(): PermissionsForegroundDialogFragment {
            return PermissionsForegroundDialogFragment()
        }
    }

}
