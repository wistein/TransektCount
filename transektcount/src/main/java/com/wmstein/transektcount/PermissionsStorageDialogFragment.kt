package com.wmstein.transektcount

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast

import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment

import com.wmstein.transektcount.Utils.fromHtml

/*******************************************************************
 * PermissionsStorageDialogFragment provides the permission handling,
 * which is necessary since Android Marshmallow (M)
 *
 * Created in Kotlin on 2023-05-26,
 * last edited on 2026-05-23
 */
class PermissionsStorageDialogFragment : DialogFragment() {
    private var context: Context? = null // activity

    override fun onAttach(context: Context) {
        super.onAttach(context)

        this.context = context
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "37, onCreate, PermStorage")

        setStyle(STYLE_NO_TITLE, R.style.PermissionsDialogFragmentStyle)
        isCancelable = false

        // Request storage permission
        val permission = Manifest.permission.MANAGE_EXTERNAL_STORAGE

        // launch permission request dialog (Android >=11)
        permissionLauncherStorage.launch(permission)
    }

    // Request single permissions in system settings app
    private val permissionLauncherStorage = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    )
    { isGranted ->
        if (isGranted) {
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.i(TAG, "56, permLauncherStorage granted: true")
            dismiss()
        } else {
            // >= API 30
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.i(TAG, "61, ask manage storage")
            showAppSettingsManageStorageDialog()
        }
    }

    // Query missing manage storage permission for API >=30
    private fun showAppSettingsManageStorageDialog() {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "69, AppSettingsManageStorDlg")

        // API 30
        val mesg = getString(R.string.dialog_storage_message)
        Toast.makeText(
            this.context,
            fromHtml("<font color='red'><b>$mesg</b></font>"),
            Toast.LENGTH_LONG
        ).show()

        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
        val uri = Uri.fromParts("package", "com.wmstein.transektcount", null)
        intent.data = uri
        startActivity(intent)

        dismiss()
    }

    override fun onDetach() {
        super.onDetach()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "91, onDetach")

        context = null
    }

    companion object {
        private const val TAG = "PermStorFragm"

        @JvmStatic
        fun newInstance(): PermissionsStorageDialogFragment {
            return PermissionsStorageDialogFragment()
        }
    }

}
