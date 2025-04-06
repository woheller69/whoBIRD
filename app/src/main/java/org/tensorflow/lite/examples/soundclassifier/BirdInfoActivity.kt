package org.tensorflow.lite.examples.soundclassifier

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.WebSettings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import org.tensorflow.lite.examples.soundclassifier.databinding.ActivityBirdInfoBinding
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.Locale


class BirdInfoActivity : BaseActivity() {

    private lateinit var binding: ActivityBirdInfoBinding
    private lateinit var database: BirdDBHelper
    private lateinit var adapter: RecyclerOverviewListAdapterBirdInfo
    private lateinit var assetList: List<String>
    private lateinit var labelList: List<String>
    private lateinit var eBirdList: List<String>
    private lateinit var mContext: Context
    private lateinit var allBirdsList: ArrayList<Pair<Int, String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBirdInfoBinding.inflate(layoutInflater)
        database = BirdDBHelper.getInstance(this)
        mContext = this
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

        binding.webview.setWebViewClient(object : MlWebViewClient(this) {})
        binding.webview.settings.setDomStorageEnabled(true)
        binding.webview.settings.setJavaScriptEnabled(true)
        binding.webview.settings.setBuiltInZoomControls(true);
        binding.webview.settings.setDisplayZoomControls(false);

        val linearLayoutManager = LinearLayoutManager(this)
        binding.recyclerObservations.setLayoutManager(linearLayoutManager)

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.action_mic -> {
                    intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                }
                R.id.action_view -> {
                    intent = Intent(this, ViewActivity::class.java)
                    startActivity(intent)
                }
                R.id.action_settings -> {
                    intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                }
            }
            true
        }
        loadLabels(this)
        loadAssetList(this)
        loadEbirdList(this)
        allBirdsList = labelList.mapIndexed { index, element ->
            Pair(index, element)
        }.sortedBy { it.second.split("_")[1] }.toCollection(ArrayList())

    }

    override fun onResume() {
        super.onResume()

        adapter = RecyclerOverviewListAdapterBirdInfo(applicationContext, allBirdsList)
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
                        binding.webviewLatinname.setText("")
                        binding.webviewLatinname.setVisibility(View.GONE)
                        binding.webviewReload.setVisibility(View.GONE)
                        binding.webviewEbird.setVisibility(View.GONE)
                    } else {
                        if (binding.webviewUrl.toString() != url) {
                            binding.webview.setVisibility(View.INVISIBLE)
                            binding.webview.settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK)
                            binding.webview.loadUrl("javascript:document.open();document.close();") //clear view
                            binding.webview.loadUrl(url)
                            binding.webviewUrl.setText(url)
                            binding.webviewUrl.setVisibility(View.VISIBLE)
                            binding.webviewName.setText(labelList[adapter.getSpeciesID(position)].split("_").last())
                            binding.webviewName.setVisibility(View.VISIBLE)
                            binding.webviewLatinname.setText(labelList[adapter.getSpeciesID(position)].split("_").first())
                            binding.webviewLatinname.setVisibility(View.VISIBLE)
                            binding.webviewReload.setVisibility(View.VISIBLE)
                            binding.webviewEbird.setVisibility(View.VISIBLE)
                            binding.webviewEbird.setTag(position)
                            binding.icon.setVisibility(View.GONE)
                        }
                    }
                }

                override fun onLongItemClick(view: View?, position: Int) {}
            })
        )
        binding.searchEdit.doOnTextChanged { text, start, before, count ->
            allBirdsList = labelList.mapIndexed { index, element ->
                Pair(index, element)
            }.filter { it.second.contains(text.toString(), ignoreCase = true) } // Add the filter here
                .sortedBy { it.second.split("_")[1] }
                .toCollection(ArrayList())
            adapter.updateBirdList(allBirdsList);
        }

    }


    /** Retrieve asset list from "assets" file */
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
            Log.e("BirdInfoActivity", "Failed to read labels ${"assets.txt"}: ${e.message}")
        }
    }

    /** Retrieve eBird taxonomy list from "taxo_code" file */
    private fun loadEbirdList(context: Context) {

        try {
            val reader =
                BufferedReader(InputStreamReader(context.assets.open("taxo_code.txt")))
            val wordList = mutableListOf<String>()
            reader.useLines { lines ->
                lines.forEach {
                    wordList.add(it.trim())
                }
            }
            eBirdList = wordList.map { it }
        } catch (e: IOException) {
            Log.e("BirdInfoActivity", "Failed to read labels ${"taxo_code.txt"}: ${e.message}")
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
            Log.i("BirdInfoActivity", "Label list entries: ${labelList.size}")
        } catch (e: IOException) {
            Log.e("BirdInfoActivity", "Failed to read labels ${filename}: ${e.message}")
        }
    }

    private fun String.toTitleCase() =
        splitToSequence("_")
            .map { it.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() } }
            .joinToString("_")
            .trim()

    companion object {

    }

    fun reload(view: View) {
        binding.webview.settings.setCacheMode(WebSettings.LOAD_DEFAULT)
        binding.webview.loadUrl(binding.webviewUrl.text.toString())
    }


    fun ebird(view: View) {
        val position = binding.webviewEbird.tag as Int
        val id = adapter.getSpeciesID(position)
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://ebird.org/species/"+eBirdList[id])))
    }

}
