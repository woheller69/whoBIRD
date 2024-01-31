package org.tensorflow.lite.examples.soundclassifier

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import org.tensorflow.lite.examples.soundclassifier.databinding.ActivityDownloadBinding

class DownloadActivity  : AppCompatActivity() {
    private var binding: ActivityDownloadBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDownloadBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
    }

    override fun onResume() {
        super.onResume()
        if (Downloader.checkModels(this)){
            // call Main Activity
            binding?.downloadProgress?.setProgress(100)
            binding?.downloadProgress?.setVisibility(View.VISIBLE)
            binding?.buttonStart?.setVisibility(View.VISIBLE)
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    fun download(view: View) {
        binding?.downloadProgress?.setVisibility(View.VISIBLE)
        binding?.buttonStart?.setVisibility(View.INVISIBLE)
        Downloader.downloadModels(this, binding)
    }

    fun startMain(view: View) {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
