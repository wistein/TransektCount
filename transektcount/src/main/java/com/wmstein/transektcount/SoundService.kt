package com.wmstein.transektcount

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.IBinder
import android.util.Log

import androidx.core.net.toUri

/*********************************************************************
 * SoundService produces the button sounds for CountingActivity
 * in background.
 *
 * Created for TransektCount (and TourCount) by wmstein on 2026-02-24,
 * last edited on 2026-05-19
 */
class SoundService : Service {
    private var mContext: Context? = null
    private var audioAttributionContext: Context? = null
    private var uriM: Uri? = null
    private var uriP: Uri? = null
    private var rToneM: MediaPlayer? = null
    private var rToneP: MediaPlayer? = null

    // prefs
    private var buttonSoundMinus: String = ""
    private var buttonSoundPlus: String = ""
    private var prefs = TransektCountApplication.getPrefs()
    private var buttonSoundPref = false

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    // Default constructor is demanded for service declaration
    constructor() // not to be removed!

    constructor(context: Context) {
        this.mContext = context
        makeSound()
    }

    private fun makeSound() {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "49, onCreate")

        audioAttributionContext = mContext!!.createAttributionContext("ringSound")

        buttonSoundPref = prefs.getBoolean("pref_button_sound", false) // make button sound
        buttonSoundMinus = prefs.getString("button_sound_minus", null).toString() // use deeper button sound
        buttonSoundPlus = prefs.getString("button_sound", null).toString() // use button sound
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "60, onStartCommand")

        return super.onStartCommand(intent, flags, startId)
    }

    fun soundMinusButtonSound() {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "67, soundMinus")
        if (buttonSoundPref) {
            uriM = if (buttonSoundMinus.isNotBlank())
                buttonSoundMinus.toUri()
            else
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            rToneM = MediaPlayer.create(audioAttributionContext, uriM)

            if (rToneM!!.isPlaying) {
                rToneM!!.stop()
                rToneM!!.release()
            }
            rToneM!!.start()
        }
    }

    fun soundPlusButtonSound() {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "86, soundPlus")
        if (buttonSoundPref) {
            uriP = if (buttonSoundPlus.isNotBlank())
                buttonSoundPlus.toUri()
            else
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            rToneP = MediaPlayer.create(audioAttributionContext, uriP)

            if (rToneP!!.isPlaying) {
                rToneP!!.stop()
                rToneP!!.release()
            }
            rToneP!!.start()
        }
    }

    // Release (-)-button sound, called by CountingActivity and WelcomeActivity
    fun releaseSoundM() {
        if (buttonSoundPref && rToneM != null) {
            rToneM!!.reset()
            rToneM!!.release()
            rToneM = null
        }
    }

    // Release (+)-button sound, called by EditIndividualActivity and WelcomeActivity
    fun releaseSoundP() {
        if (buttonSoundPref && rToneP != null) {
            rToneP!!.reset()
            rToneP!!.release()
            rToneP = null
        }
    }

    override fun onDestroy() {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "123, onDestroy")

        if (buttonSoundPref) {
            if (rToneM != null) {
                rToneM!!.reset()
                rToneM!!.release()
                rToneM = null
            }

            if (rToneP != null) {
                rToneP!!.reset()
                rToneP!!.release()
                rToneP = null
            }
        }
        super.onDestroy()
    }

    companion object {
        private const val TAG = "SoundService"
    }
}
