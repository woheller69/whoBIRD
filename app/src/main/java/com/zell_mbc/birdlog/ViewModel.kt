package com.zell_mbc.birdlog

import androidx.lifecycle.AndroidViewModel
import android.app.Application
import android.util.Log
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import kotlin.sequences.forEach

data class Bird(
    var id: Int,
    var asset_id: String
)

data class BirdName(
    var id: Int,
    var commonName: String,
    var latinName: String
)

class ViewModel(application: Application): AndroidViewModel(application) {
    val allBirds = ArrayList<Bird>() // Holds all known birds
    var birdNames = ArrayList<BirdName>() // Holds the name of all known birds
    val language = "de"

    init {
        try {
            val reader = BufferedReader(InputStreamReader(application.assets.open("assets.txt")))  //TODO: Common definition for all classes
            var lineNo = 1
            reader.useLines { lines ->
                lines.forEach {
                    allBirds.add(Bird(id = lineNo, asset_id = it.trim()))
                    lineNo++
                }
            }
        } catch (e: IOException) {
            Log.e("ViewActivity", "Failed to read labels ${"assets.txt"}: ${e.message}")
        }
        try {
            val reader = BufferedReader(InputStreamReader(application.assets.open("labels_$language.txt")))  //TODO: Common definition for all classes
            reader.useLines { lines ->
                var lineNo = 1
                lines.forEach {
                    val names = it.trim().split("_")
                    birdNames.add(BirdName(id = lineNo, commonName = names[1], latinName = names[0]))
                    lineNo++
                }
            }
        } catch (e: IOException) {
            Log.e("ViewActivity", "Failed to read labels ${"assets.txt"}: ${e.message}")
        }
    }
}