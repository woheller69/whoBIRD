package org.tensorflow.lite.examples.soundclassifier

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.widget.CompoundButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import org.tensorflow.lite.examples.soundclassifier.databinding.ActivityViewBinding
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.Locale


class ViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityViewBinding
    private lateinit var database: BirdDBHelper
    private lateinit var adapter: RecyclerOverviewListAdapter
    private lateinit var birdObservations: ArrayList<BirdObservation>
    lateinit var assetList: List<String>
    lateinit var labelList: List<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewBinding.inflate(layoutInflater)
        database = BirdDBHelper.getInstance(this)
        setContentView(binding.root)

        //Set aspect ratio for webview and icon
        val windowMetrics = windowManager.currentWindowMetrics
        val width = windowMetrics.bounds.width()
        val paramsWebview: ViewGroup.LayoutParams = binding.webview.getLayoutParams() as ViewGroup.LayoutParams
        paramsWebview.height = (width / 1.8f).toInt()
        val paramsIcon: ViewGroup.LayoutParams = binding.icon.getLayoutParams() as ViewGroup.LayoutParams
        paramsIcon.height = (width / 1.8f).toInt()

        binding.webview.setWebViewClient(object : MlWebViewClient(this) {})
        binding.webview.settings.setDomStorageEnabled(true)
        binding.webview.settings.setJavaScriptEnabled(true)

        val linearLayoutManager = LinearLayoutManager(this)
        binding.recyclerObservations.setLayoutManager(linearLayoutManager)

        binding.checkDetailed.setOnClickListener { view ->
            if ((view as CompoundButton).isChecked) {
                birdObservations.clear()
                birdObservations.addAll(database.getAllBirdObservations(true).sortedByDescending { it.millis })
            } else {
                birdObservations.clear()
                birdObservations.addAll(database.getAllBirdObservations(false).sortedByDescending { it.millis })
            }
            adapter.notifyDataSetChanged()
        }

        binding.bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.action_mic -> {
                    finish()
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
                    val database = BirdDBHelper.getInstance(this)
                    database.clearAllEntries()
                    Toast.makeText(this, getString(R.string.clear_db),Toast.LENGTH_SHORT).show()
                    birdObservations.clear()
                    adapter.notifyDataSetChanged()
                    binding.webview.setVisibility(View.GONE)
                    binding.webview.loadUrl("about:blank")
                    binding.icon.setVisibility(View.VISIBLE)
                    binding.webviewUrl.setText("")
                    binding.webviewUrl.setVisibility(View.GONE)
                    binding.webviewName.setText("")
                    binding.webviewName.setVisibility(View.GONE)
                    binding.webviewReload.setVisibility(View.GONE)
                }
            }
            true
        }
        loadLabels(this)
        loadAssetList(this)
    }

    override fun onResume() {
        super.onResume()

        birdObservations = ArrayList(database.getAllBirdObservations(false).sortedByDescending { it.millis } )  //Conversion between Java ArrayList and Kotlin ArrayList

        adapter = RecyclerOverviewListAdapter(applicationContext, birdObservations)
        binding.recyclerObservations.setAdapter(adapter)
        binding.recyclerObservations.setFocusable(false)
        binding.recyclerObservations.addOnItemTouchListener(
            RecyclerItemClickListener(baseContext, binding.recyclerObservations, object : RecyclerItemClickListener.OnItemClickListener {
                override fun onItemClick(view: View?, position: Int) {

                    val url = if ( assetList[adapter.getSpeciesID(position)] != "NO_ASSET") {
                        "https://macaulaylibrary.org/asset/" + assetList[adapter.getSpeciesID(position)] + "/embed"
                    } else {
                        "about:blank"
                    }
                    if (url == "about:blank"){
                        binding.webview.setVisibility(View.GONE)
                        binding.webview.loadUrl(url)
                        binding.icon.setVisibility(View.VISIBLE)
                        binding.webviewUrl.setText("")
                        binding.webviewUrl.setVisibility(View.GONE)
                        binding.webviewName.setText("")
                        binding.webviewName.setVisibility(View.GONE)
                        binding.webviewReload.setVisibility(View.GONE)
                    } else {
                        if (binding.webview.url != url) {
                            binding.webview.setVisibility(View.INVISIBLE)
                            binding.webview.settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK)
                            binding.webview.loadUrl(url)
                            binding.webviewUrl.setText(url)
                            binding.webviewUrl.setVisibility(View.VISIBLE)
                            binding.webviewName.setText(labelList[adapter.getSpeciesID(position)].split("_").last())
                            binding.webviewName.setVisibility(View.VISIBLE)
                            binding.webviewReload.setVisibility(View.VISIBLE)
                            binding.icon.setVisibility(View.GONE)
                        }
                    }
                }

                override fun onLongItemClick(view: View?, position: Int) {}
            })
        )
    }


    /** Retrieve asset list from "asset_list" file */
    private fun loadAssetList(context: Context) {

        try {
            val reader =
                BufferedReader(InputStreamReader(context.assets.open("assets.txt")))  //TODO: Common definition for all classes
            val wordList = mutableListOf<String>()
            reader.useLines { lines ->
                lines.forEach {
                    wordList.add(it.trim())
                }
            }
            assetList = wordList.map { it }
        } catch (e: IOException) {
            Log.e("ViewActivity", "Failed to read labels ${"assets.txt"}: ${e.message}")
        }
    }

    /** Retrieve labels from "labels.txt" file */
    private fun loadLabels(context: Context) { //TODO: Refactor
        val localeList = context.resources.configuration.locales
        val language = localeList.get(0).language
        var filename = "labels"+"_${language}.txt"    // TODO: Common definition for all classes

        //Check if file exists
        val assetManager = context.assets // Replace 'assets' with actual AssetManager instance
        try {
            val mapList = assetManager.list("")?.toMutableList()

            if (mapList != null) {
                if (!mapList.contains(filename)) {
                    filename = "labels"+"_en.txt"
                }
            }
        } catch (ex: IOException) {
            ex.printStackTrace()
            filename = "labels"+"_en.txt"
        }

        try {
            val reader =
                BufferedReader(InputStreamReader(context.assets.open(filename)))
            val wordList = mutableListOf<String>()
            reader.useLines { lines ->
                lines.forEach {
                    wordList.add(it)
                }
            }
            labelList = wordList.map { it.toTitleCase() }
            Log.i("ViewActivity", "Label list entries: ${labelList.size}")
        } catch (e: IOException) {
            Log.e("ViewActivity", "Failed to read labels ${filename}: ${e.message}")
        }
    }

    private fun String.toTitleCase() =
        splitToSequence("_")
            .map { it.capitalize(Locale.ROOT) }
            .joinToString("_")
            .trim()

    companion object {

    }

    fun reload(view: View) {
        binding.webview.settings.setCacheMode(WebSettings.LOAD_DEFAULT)
        binding.webview.reload()
    }
}
