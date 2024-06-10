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
import android.os.Environment
import android.util.DisplayMetrics
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebSettings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import net.lingala.zip4j.ZipFile
import org.tensorflow.lite.examples.soundclassifier.databinding.ActivityMainBinding
import java.io.File


class MainActivity : AppCompatActivity() {

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
        binding.fab.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_play_24dp))
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
        R.id.action_share -> {
          val database = BirdDBHelper.getInstance(this)
          val intent = Intent(Intent.ACTION_SEND)
          val shareBody = database.exportAllEntriesAsCSV().joinToString("\n")
          intent.setType("text/plain")
          intent.putExtra(Intent.EXTRA_TEXT, shareBody)
          startActivity(Intent.createChooser(intent, ""))
        }
        R.id.action_delete -> {
          Snackbar.make(
            binding.bottomNavigationView,
            this.getString(R.string.delete),
            Snackbar.LENGTH_LONG
          ).setAction(this.getString(android.R.string.ok),
            {
              val database = BirdDBHelper.getInstance(this)
              database.clearAllEntries()
              Toast.makeText(this, getString(R.string.clear_db),Toast.LENGTH_SHORT).show()
            }).setTextColor(this.getColor(R.color.orange500)).show()
        }
      }
      true
    }

    if (GithubStar.shouldShowStarDialog(this)) GithubStar.starDialog(this, "https://github.com/woheller69/whoBIRD")

    requestPermissions()

  }

  override fun onResume() {
    super.onResume()
    Location.requestLocation(this, soundClassifier)
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
    Location.stopLocation(this)
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
    const val REQUEST_BACKUP_FILE = 1338
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
      R.id.action_about -> {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/woheller69/whobird")))
        return true
      }
      R.id.action_backup -> {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.setType("application/zip")
        intent.putExtra(Intent.EXTRA_TITLE, resources.getString(R.string.app_name))
        startActivityForResult(intent, REQUEST_BACKUP_FILE)
        return true
      }
      else -> return super.onOptionsItemSelected(item)
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, dataIntent: Intent?) {
    super.onActivityResult(requestCode, resultCode, dataIntent)
    if (requestCode == REQUEST_BACKUP_FILE && resultCode == RESULT_OK && dataIntent != null) {
      dataIntent.data?.let {
        performBackup(it)
      }
    }
  }

  private fun performBackup(uri: Uri) {
    val intData: File = File(
      Environment.getDataDirectory().toString() + "//data//" + this.packageName + "//databases//"
    )
    try {
      val tmpFile = File(cacheDir, "backup.zip")
      if (tmpFile.exists()) {
        tmpFile.delete()
      }
      ZipFile(tmpFile).addFolder(intData)
      var srcStream = tmpFile.inputStream()
      var dstStream = contentResolver.openOutputStream(uri)!!
      val buffer = ByteArray(1024)
      var read: Int
      while ((srcStream.read(buffer).also { read = it }) != -1) {
        dstStream.write(buffer, 0, read)
      }
      srcStream.close()
      dstStream.close()
      tmpFile.delete()
    } catch (e: Exception) {
      Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show()
      e.printStackTrace()
    }
  }
}
