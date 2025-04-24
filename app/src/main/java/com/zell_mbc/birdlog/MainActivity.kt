package com.zell_mbc.birdlog

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebSettings
import android.widget.CompoundButton
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.os.LocaleListCompat
import androidx.preference.PreferenceManager
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import com.zell_mbc.birdlog.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

  private lateinit var soundClassifier: SoundClassifier
  private lateinit var binding: ActivityMainBinding
  val compose = true

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    //birdLogApplicationContext = applicationContext as BirdLog

    val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
    var language = sharedPref.getString("language", "").toString()
    setLanguage(language)

    WindowCompat.setDecorFitsSystemWindows(window, true)

    darkMode = sharedPref.getBoolean("darkMode", false)
    setTheme(this, darkMode)

    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)
    //setContent { BirdlogTheme { StartCompose() } }

    //Set aspect ratio for webview and icon
    val width = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      val windowMetrics = windowManager.currentWindowMetrics
      windowMetrics.bounds.width()
    } else {
      val displayMetrics = DisplayMetrics()
      windowManager.defaultDisplay.getMetrics(displayMetrics)
      displayMetrics.widthPixels
    }
    val paramsWebview: ViewGroup.LayoutParams =
      binding.webview.getLayoutParams() as ViewGroup.LayoutParams
    paramsWebview.height = (width / 1.8f).toInt()
    val paramsIcon: ViewGroup.LayoutParams =
      binding.icon.getLayoutParams() as ViewGroup.LayoutParams
    paramsIcon.height = (width / 1.8f).toInt()

    soundClassifier = SoundClassifier(this, binding, SoundClassifier.Options())
    //binding.gps.setText(getString(R.string.latitude)+": --.-- / " + getString(R.string.longitude) + ": --.--" )
    binding.webview.setWebViewClient(object : MlWebViewClient(this) {})
    binding.webview.settings.setDomStorageEnabled(true)
    binding.webview.settings.setJavaScriptEnabled(true)

    binding.fab.setOnClickListener {
      if (binding.progressHorizontal.isIndeterminate) {
        binding.progressHorizontal.setIndeterminate(false)
        binding.fab.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_record_24dp))
      } else {
        binding.progressHorizontal.setIndeterminate(true)
        binding.fab.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_pause_24dp))
      }
    }
    binding.bottomNavigationView.setOnItemSelectedListener { item ->
      when (item.itemId) {
        R.id.action_view -> {
          intent = Intent(this, ViewActivity::class.java)
          startActivity(intent)
        }

        R.id.action_bird_info -> {
          intent = if (compose) Intent(this, AllBirdsActivity::class.java)
          else  Intent(this, BirdInfoActivity::class.java)
          startActivity(intent)
        }

        R.id.action_settings -> {
          intent = Intent(this, SettingsActivity::class.java)
          startActivity(intent)
        }
      }
      true
    }

    //
    val isShowImagesActive = sharedPref.getBoolean("main_show_images", false)
    /*binding.checkShowImages.isChecked = isShowImagesActive
    binding.checkShowImages.setOnClickListener { view ->
      val editor=sharedPref.edit()
      if ((view as CompoundButton).isChecked) {
        editor.putBoolean("main_show_images", true)
        editor.apply()
      } else {
        editor.putBoolean("main_show_images", false)
        editor.apply()
      }
    }*/

    val isIgnoreLocationDateActive = sharedPref.getBoolean("main_ignore_meta", false)
    binding.checkIgnoreMeta.isChecked = isIgnoreLocationDateActive
    binding.checkIgnoreMeta.setOnClickListener { view ->
      val editor = sharedPref.edit()
      if ((view as CompoundButton).isChecked) {
        editor.putBoolean("main_ignore_meta", true)
        editor.apply()
      } else {
        editor.putBoolean("main_ignore_meta", false)
        editor.apply()
      }
    }
    requestPermissions()

  }

  override fun onResume() {
    super.onResume()

    LocationHelper.requestLocation(this, soundClassifier)
    if (!checkLocationPermission()) {
      Toast.makeText(
        this,
        this.resources.getString(R.string.error_location_permission),
        Toast.LENGTH_SHORT
      ).show()
    }
    if (checkMicrophonePermission()) {
      soundClassifier.start()
    } else {
      Toast.makeText(
        this,
        this.resources.getString(R.string.error_audio_permission),
        Toast.LENGTH_SHORT
      ).show()
    }

    keepScreenOn(true)
  }

  override fun onPause() {
    super.onPause()

    LocationHelper.stopLocation(this)
    if (soundClassifier.isRecording) soundClassifier.stop()

  }

  private fun checkMicrophonePermission(): Boolean {
    if (ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.RECORD_AUDIO
      ) == PackageManager.PERMISSION_GRANTED
    ) {
      return true
    } else {
      return false
    }
  }

  private fun checkLocationPermission(): Boolean {
    if (ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_COARSE_LOCATION
      ) == PackageManager.PERMISSION_GRANTED
    ) {
      return true
    } else {
      return false
    }
  }

  private fun requestPermissions() {
    val perms = mutableListOf<String>()
    if (ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.RECORD_AUDIO
      ) != PackageManager.PERMISSION_GRANTED
    ) {
      perms.add(Manifest.permission.RECORD_AUDIO)
    }
    if (ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_COARSE_LOCATION
      ) != PackageManager.PERMISSION_GRANTED
    ) {
      perms.add(Manifest.permission.ACCESS_COARSE_LOCATION)
      perms.add(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    if (!perms.isEmpty()) requestPermissions(perms.toTypedArray(), REQUEST_PERMISSIONS)
  }

  private fun keepScreenOn(enable: Boolean) =
    if (enable) {
      window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    } else {
      window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

  fun reload(view: View) {
    binding.webview.settings.setCacheMode(WebSettings.LOAD_DEFAULT)
    binding.webview.loadUrl(binding.webviewUrl.text.toString())
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    val inflater = menuInflater
    inflater.inflate(R.menu.main, menu)

    val themeMenuItem = menu.findItem(R.id.action_change_theme)

    // Tint the icons
    tintMenuIcon(this, themeMenuItem)

    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.action_change_theme -> {
        setTheme(this, !darkMode)
        return true
      }

      else -> return super.onOptionsItemSelected(item)
    }
  }

  @Composable
  fun StartCompose() {
    val scanning = remember { mutableStateOf(true) }
    val checkedState = remember { mutableStateOf(false) }
    var hit1 by remember { mutableStateOf(hitText1) }
    var hit2 by remember { mutableStateOf(hitText1) }

    Column(modifier = Modifier.safeDrawingPadding().padding(start = 16.dp, top = 8.dp, end = 16.dp,)) {
      BirdDetails()
      Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checkedState.value, onCheckedChange = { checkedState.value = it })
        Text(stringResource(id = R.string.ignore_gps_date), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
      }
      if (scanning.value)
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.secondary, trackColor = MaterialTheme.colorScheme.surfaceVariant)

      Text(hit1.value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
      Text(hit2.value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    }
  }

  @Composable
  fun BirdDetails() {
    Surface(
      //modifier = Modifier.size(50.dp),
      color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0f)) {
      Image(painter = painterResource(id = R.drawable.icon_large), contentDescription = "Logo") }
  }

  companion object {
    lateinit var birdLogApplicationContext: BirdLog
    var dynamicColorOn = false

    val hitText1 = mutableStateOf("")
    val hitText2 = mutableStateOf("")

    const val REQUEST_PERMISSIONS = 1337
    var darkMode = false
    var textColor = 0

    fun setTheme(context: Context, mode: Boolean = false) {
      if (mode == false) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)  // Light theme
        darkMode = false
        textColor = context.getColor(R.color.md_theme_light_primary)
      } else {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES) // Dark theme
        darkMode = true
        textColor = context.getColor(R.color.md_theme_dark_primary)
      }
      val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
      sharedPref.edit() {
        putBoolean("darkMode", darkMode)
      }
    }

    fun tintMenuIcon(context: Context, menuItem: MenuItem) {
      val icon = menuItem.icon
      val color = if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES)
        ContextCompat.getColor(context, R.color.md_theme_light_onSurface)  // Light theme
      else
        ContextCompat.getColor(context, R.color.md_theme_dark_onSurface) // Dark theme

      if (icon != null) {
        val wrappedIcon = DrawableCompat.wrap(icon)
        DrawableCompat.setTint(wrappedIcon, color)
        menuItem.setIcon(wrappedIcon)
      }
    }

    fun setLanguage(language: String) {
      if (language.length == 2) { // If nothing is set, do nothing and use the OS native language
        val appLocale = LocaleListCompat.forLanguageTags(language)
        AppCompatDelegate.setApplicationLocales(appLocale)
      }
    }
  }
}