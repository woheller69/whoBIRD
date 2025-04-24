package com.zell_mbc.birdlog

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import org.acra.config.dialog
import org.acra.config.mailSender
import org.acra.data.StringFormat
import org.acra.dialog.BuildConfig
import org.acra.ktx.initAcra


class BirdLog: Application() {
    private lateinit var preferences: SharedPreferences

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)

        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        //if (!preferences.getBoolean(SettingsActivity.KEY_PREF_ENABLECRASHLOGS, true)) return

        initAcra {
            //core configuration:
            buildConfigClass = BuildConfig::class.java
            reportFormat = StringFormat.KEY_VALUE_LIST

            mailSender {
                //required
                mailTo = "birdlog@zell-mbc.com"
                reportAsFile = true
                reportFileName = "crashLog.txt"
                subject = getString(R.string.crashTitle)
                body = getString(R.string.crashMessageEmail)
            }
            dialog {
                text = getString(R.string.crashMessageDialog)
                title = getString(R.string.crashTitle)
                positiveButtonText = getString(R.string.ok)
                negativeButtonText = getString(R.string.cancel)
                commentPrompt = getString(R.string.crashContext)
                resIcon = android.R.drawable.ic_dialog_alert
                emailPrompt = ""
            }
        }
    }
}