package com.wmstein.transektcount

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

/***********************************************************************
 * PermissionsBackgroundDialogFragment provides the permission handling,
 * which is necessary since Android Android Q for
 * - ACCESS_BACKGROUND_LOCATION
 *
 * Based on RuntimePermissionsExample-master created by tylerjroach on 8/31/16,
 * licensed under the MIT License.
 *
 * Adopted for TourCount in Kotlin by wistein on 2025-02-22,
 * used in TransektCount on 2025-07-02,
 * last edited on 2025-12-29
 */
class PermissionsBackgroundDialogFragment : DialogFragment() {
    private var context: Context? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)

        this.context = context
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.d(TAG, "38, onCreate, PermBackgr Loc")

        setStyle(STYLE_NO_TITLE, R.style.PermissionsDialogFragmentStyle)
        isCancelable = false

        // Request background location permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val permission = Manifest.permission.ACCESS_BACKGROUND_LOCATION

            //launch permission request dialog
            permissionLauncherBackground.launch(permission)
        }
    }

    // Request background location permission in system settings dialog
    private val permissionLauncherBackground = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    )
    { isGranted ->
        if (isGranted) {
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.d(TAG, "59, permLauncherBackgrLoc granted: true")
            dismiss()
        } else {
            showAppSettingsBackgroundDialog()
        }
    }

    // Query and set optional background permission
    private fun showAppSettingsBackgroundDialog() {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.d(TAG, "69, AppSettingsBackgrLocDlg")

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.dialog_background_loc_title))
            .setMessage(
                getString(R.string.dialog_background_loc_message) + "\n\n"
                        + getString(R.string.grant_perm_later)
            )
            .setPositiveButton("Ok")
            { _: DialogInterface?, _: Int ->
                dismiss()
            }
            .create().show()
    }

    override fun onDetach() {
        super.onDetach()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.d(TAG, "88, onDetach")

        context = null
    }

    companion object {
        private const val TAG = "PermBackgrLocFragm"

        @JvmStatic
        fun newInstance(): PermissionsBackgroundDialogFragment {
            return PermissionsBackgroundDialogFragment()
        }
    }

}
