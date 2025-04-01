package org.tensorflow.lite.examples.soundclassifier

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.format.DateFormat
import android.util.DisplayMetrics
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.widget.CompoundButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import net.lingala.zip4j.ZipFile
import org.tensorflow.lite.examples.soundclassifier.databinding.ActivityViewBinding
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class ViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityViewBinding
    private lateinit var database: BirdDBHelper
    private lateinit var adapter: RecyclerOverviewListAdapterObservations
    private lateinit var birdObservations: ArrayList<BirdObservation>
    private lateinit var assetList: List<String>
    private lateinit var labelList: List<String>
    private lateinit var eBirdList: List<String>
    private lateinit var mContext: Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewBinding.inflate(layoutInflater)
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

        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        val isDetailedFilterActive = sharedPref.getBoolean("view_detailed", false)
        binding.checkDetailed.isChecked = isDetailedFilterActive
        binding.checkDetailed.setOnClickListener { view ->
            val editor=sharedPref.edit()
            if ((view as CompoundButton).isChecked) {
                birdObservations.clear()
                birdObservations.addAll(database.getAllBirdObservations(true).sortedByDescending { it.millis })
                editor.putBoolean("view_detailed", true)
                editor.apply()
            } else {
                birdObservations.clear()
                birdObservations.addAll(database.getAllBirdObservations(false).sortedByDescending { it.millis })
                editor.putBoolean("view_detailed", false)
                editor.apply()
            }
            adapter.notifyDataSetChanged()
        }

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.action_mic -> {
                    intent = Intent(this, MainActivity::class.java)
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
        loadLabels(this)
        loadAssetList(this)
        loadEbirdList(this)
    }

    override fun onResume() {
        super.onResume()
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        val isDetailedFilterActive = sharedPref.getBoolean("view_detailed", false)
        birdObservations = ArrayList(database.getAllBirdObservations(isDetailedFilterActive).sortedByDescending { it.millis } )  //Conversion between Java ArrayList and Kotlin ArrayList

        adapter = RecyclerOverviewListAdapterObservations(applicationContext, birdObservations)
        binding.recyclerObservations.setAdapter(adapter)
        binding.recyclerObservations.setFocusable(false)
        binding.recyclerObservations.addOnItemTouchListener(
            RecyclerItemClickListener(baseContext, binding.recyclerObservations, object : RecyclerItemClickListener.OnItemClickListener {
                override fun onItemClick(view: View?, position: Int) {
                    WavUtils.playWaveFile(mContext, adapter.getMillis(position))
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
                        binding.webviewShare.setVisibility(View.GONE)
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
                            binding.webviewShare.setVisibility(View.VISIBLE)
                            binding.webviewShare.setTag(position)
                            binding.icon.setVisibility(View.GONE)
                        }
                    }
                }

                override fun onLongItemClick(view: View?, position: Int) {}
            })
        )
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
            Log.e("ViewActivity", "Failed to read labels ${"assets.txt"}: ${e.message}")
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
            Log.e("ViewActivity", "Failed to read labels ${"taxo_code.txt"}: ${e.message}")
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
            .map { it.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() } }
            .joinToString("_")
            .trim()

    companion object {

    }

    fun reload(view: View) {
        binding.webview.settings.setCacheMode(WebSettings.LOAD_DEFAULT)
        binding.webview.loadUrl(binding.webviewUrl.text.toString())
    }

    fun share(view: View) {
        val position = binding.webviewShare.tag as Int

        val id = adapter.getSpeciesID(position)

        val sdf: SimpleDateFormat
        val date = Date(adapter.getMillis(position))
        sdf = if (DateFormat.is24HourFormat(this)) {
            SimpleDateFormat("HH:mm", Locale.getDefault())
        } else {
            SimpleDateFormat("hh:mm aa", Locale.getDefault())
        }
        val timeString = sdf.format(date)

        val df = java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT)
        val dateString = df.format(adapter.getMillis(position))

        val locationString = adapter.getLocation(position)

        val shareString = dateString + ", " + timeString + ", " + labelList[id].replace("_",", ") + ", " + locationString +"\n\nGet whoBIRD on F-Droid"

        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareString)
        startActivity(Intent.createChooser(shareIntent, ""))
    }

    fun ebird(view: View) {
        val position = binding.webviewEbird.tag as Int
        val id = adapter.getSpeciesID(position)
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://ebird.org/species/"+eBirdList[id])))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.view, menu)

        var themeMenuItem = menu.findItem(R.id.action_change_theme)
        MainActivity.tintMenuIcon(this, themeMenuItem)
        themeMenuItem = menu.findItem(R.id.action_share_db)
        MainActivity.tintMenuIcon(this, themeMenuItem)
        themeMenuItem = menu.findItem(R.id.action_delete_db)
        MainActivity.tintMenuIcon(this, themeMenuItem)
        themeMenuItem = menu.findItem(R.id.action_save_db)
        MainActivity.tintMenuIcon(this, themeMenuItem)

        return true
    }

    @SuppressLint("SimpleDateFormat")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_change_theme -> {
                MainActivity.setTheme(this, !MainActivity.darkMode)
                return true
            }
            R.id.action_share_db -> {
                val database = BirdDBHelper.getInstance(this)
                val intent = Intent(Intent.ACTION_SEND)
                val shareBody = database.exportAllEntriesAsCSV().joinToString("\n")
                intent.setType("text/plain")
                intent.putExtra(Intent.EXTRA_TEXT, shareBody)
                startActivity(Intent.createChooser(intent, ""))
                return true
            }
            R.id.action_delete_db -> {
                val builder = AlertDialog.Builder(this)
                builder.setTitle(getString(R.string.delete))
                    .setPositiveButton(this.getString(android.R.string.ok), { _, _ ->
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
                        binding.webviewLatinname.setText("")
                        binding.webviewLatinname.setVisibility(View.GONE)
                        binding.webviewReload.setVisibility(View.GONE)
                        binding.webviewEbird.setVisibility(View.GONE)
                        binding.webviewShare.setVisibility(View.GONE)
                    })
                    .setNegativeButton(this.getString(android.R.string.cancel), { _, _ -> })
                    .create().show()
                return true
            }
            R.id.action_save_db -> {
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.setType("application/zip")
                val timestamp = Date()
                val formatter = SimpleDateFormat("yyyy-MM-dd") // Define the desired format
                val formattedDate = formatter.format(timestamp) // Format the Date object
                intent.putExtra(Intent.EXTRA_TITLE, "$formattedDate " + resources.getString(R.string.app_name))
                resultLauncher.launch(intent)
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            result.data?.data?.let {
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
            val srcStream = tmpFile.inputStream()
            val dstStream = contentResolver.openOutputStream(uri)!!
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
