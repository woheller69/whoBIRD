/*
 * Copyright 2020 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// Modifications by woheller69

package org.tensorflow.lite.examples.soundclassifier

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.google.android.material.slider.Slider
import org.tensorflow.lite.examples.soundclassifier.databinding.ActivityMainBinding

class MainActivity : BaseActivity() {

  private lateinit var soundClassifier: SoundClassifier
  private lateinit var binding: ActivityMainBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)

    //Set aspect ratio for webview and icon
    val width = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      val windowMetrics = windowManager.currentWindowMetrics
      windowMetrics.bounds.width()
    } else {
      val displayMetrics = DisplayMetrics()
      windowManager.defaultDisplay.getMetrics(displayMetrics)
      displayMetrics.widthPixels
    }
    val paramsWebview: ViewGroup.LayoutParams = binding.webview.getLayoutParams() as ViewGroup.LayoutParams
    paramsWebview.height = (width / 1.8f).toInt()
    val paramsIcon: ViewGroup.LayoutParams = binding.icon.getLayoutParams() as ViewGroup.LayoutParams
    paramsIcon.height = (width / 1.8f).toInt()

    soundClassifier = SoundClassifier(this, binding, SoundClassifier.Options())
    binding.gps.setText(getString(R.string.latitude)+": --.-- / " + getString(R.string.longitude) + ": --.--" )
    binding.webview.setWebViewClient(object : MlWebViewClient(this) {})
    binding.webview.settings.setDomStorageEnabled(true)
    binding.webview.settings.setJavaScriptEnabled(true)

    binding.fab.setOnClickListener {
      if (binding.progressHorizontal.isIndeterminate) {
        binding.progressHorizontal.setIndeterminate(false)
        binding.fab.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_record_24dp))
      }
      else {
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
          intent = Intent(this, BirdInfoActivity::class.java)
          startActivity(intent)
        }
        R.id.action_settings -> {
          intent = Intent(this, SettingsActivity::class.java)
          startActivity(intent)
        }
      }
      true
    }

    val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)

    val metaModelInfluence = sharedPref.getFloat("meta_model_influence", 60.0f)
    binding.metaInfluenceSlider.value = metaModelInfluence
    binding.metaInfluenceSlider.addOnChangeListener { slider, value, fromUser ->
      val editor=sharedPref.edit()
      editor.putFloat("meta_model_influence", value)
      editor.apply()
    }

    if (GithubStar.shouldShowStarDialog(this)) GithubStar.starDialog(this, "https://github.com/woheller69/whoBIRD")

    requestPermissions()

  }

  override fun onResume() {
    super.onResume()
    LocationHelper.requestLocation(this, soundClassifier)
    if (!checkLocationPermission()){
      Toast.makeText(this, this.resources.getString(R.string.error_location_permission), Toast.LENGTH_SHORT).show()
    }
    if (checkMicrophonePermission()){
      soundClassifier.start()
    } else {
      Toast.makeText(this, this.resources.getString(R.string.error_audio_permission), Toast.LENGTH_SHORT).show()
    }
    keepScreenOn(true)
  }

  override fun onPause() {
    super.onPause()
    LocationHelper.stopLocation(this)
    if (soundClassifier.isRecording) soundClassifier.stop()
  }

  private fun checkMicrophonePermission(): Boolean {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO ) == PackageManager.PERMISSION_GRANTED) {
      return true
    } else {
      return false
    }
  }

  private fun checkLocationPermission(): Boolean {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
      return true
    } else {
      return false
    }
  }

  private fun requestPermissions() {
    val perms = mutableListOf<String>()
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
      perms.add(Manifest.permission.RECORD_AUDIO)
    }
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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

  companion object {
    const val REQUEST_PERMISSIONS = 1337
  }

  fun reload(view: View) {
    binding.webview.settings.setCacheMode(WebSettings.LOAD_DEFAULT)
    binding.webview.loadUrl(binding.webviewUrl.text.toString())
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    val inflater = menuInflater
    inflater.inflate(R.menu.main, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.action_share_app -> {
        val intent = Intent(Intent.ACTION_SEND)
        val shareBody = "https://f-droid.org/packages/org.woheller69.whobird/"
        intent.setType("text/plain")
        intent.putExtra(Intent.EXTRA_TEXT, shareBody)
        startActivity(Intent.createChooser(intent, ""))
        return true
      }
      R.id.action_info -> {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/woheller69/whobird")))
        return true
      }
      else -> return super.onOptionsItemSelected(item)
    }
  }

}
