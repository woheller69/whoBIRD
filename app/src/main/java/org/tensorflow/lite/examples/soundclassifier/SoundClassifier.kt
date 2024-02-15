/*
 * Copyright 2021 The TensorFlow Authors. All Rights Reserved.
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

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.MainThread
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.examples.soundclassifier.databinding.ActivityMainBinding
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption
import java.time.LocalDate
import java.util.Locale
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.scheduleAtFixedRate
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin

/**
 * Performs classification on sound.
 *
 * <p>The API supports models which accept sound input via {@code AudioRecord} and one classification output tensor.
 * The output of the recognition is emitted as LiveData of Map.
 *
 */
class SoundClassifier(
  context: Context,
  binding: ActivityMainBinding,
  private val options: Options = Options()
) :
  DefaultLifecycleObserver {
  internal var mContext: Context
  internal var mBinding: ActivityMainBinding
  private var database: BirdDBHelper? = null
  init {
    this.mContext = context.applicationContext
    this.mBinding = binding
    this.database = BirdDBHelper.getInstance(mContext)
  }
  class Options constructor(
    /** Path of the converted model label file, relative to the assets/ directory.  */
    val labelsBase: String = "labels",
    /** Path of the converted .tflite file, relative to the assets/ directory.  */
    val assetFile: String = "assets.txt",
    /** Path of the converted .tflite file, relative to the assets/ directory.  */
    val modelPath: String = "model.tflite",
    /** Path of the meta model .tflite file, relative to the assets/ directory.  */
    val metaModelPath: String = "metaModel.tflite",
    /** The required audio sample rate in Hz.  */
    val sampleRate: Int = 48000,
    /** Multiplier for audio samples  */
    val warmupRuns: Int = 3,
    /** Number of points in average to reduce noise. (default 10)*/
    val pointsInAverage: Int = 1,
    /** Probability value above which a class is labeled as active (i.e., detected) (default 0.3) */
    var probabilityThreshold: Float = 0.3f,  //min must be > 0
    /** Probability value above which a class in the meta model is labeled as active (i.e., detected) the display. (default 0.01) */
    var metaProbabilityThreshold: Float = 0.01f,  //min must be > 0
    /** Probability value above which a class is shown as image. (default 0.5) */
    var displayImageThreshold: Float = 0.65f,  //min must be > 0
  )

  var isRecording: Boolean = false
    private set

  var isClosed: Boolean = true
    private set

  /**
   * LifecycleOwner instance to deal with RESUME, PAUSE and DESTROY events automatically.
   * You can also handle those events by calling `start()`, `stop()` and `close()` methods
   * manually.
   */
  var lifecycleOwner: LifecycleOwner? = null
    @MainThread
    set(value) {
      if (field === value) return
      field?.lifecycle?.removeObserver(this)
      field = value?.also {
        it.lifecycle.addObserver(this)
      }
    }


  /** Paused by user */
  var isPaused: Boolean = false
    set(value) {
      field = value
      if (value) stop() else start()
    }

  /** Names of the model's output classes.  */
  lateinit var labelList: List<String>

  /** Names of the model's output classes.  */
  lateinit var assetList: List<String>

  /** How many milliseconds between consecutive model inference calls.  */
  private var inferenceInterval = 800L

  /** The TFLite interpreter instance.  */
  private lateinit var interpreter: Interpreter
  private lateinit var meta_interpreter: Interpreter

  /** Audio length (in # of PCM samples) required by the TFLite model.  */
  private var modelInputLength = 0

  /** input Length of the meta model  */
  private var metaModelInputLength = 0

  /** Number of output classes of the TFLite model.  */
  private var modelNumClasses = 0
  private var metaModelNumClasses = 0


  /** Used to hold the real-time probabilities predicted by the model for the output classes.  */
  private lateinit var predictionProbs: FloatArray
  private lateinit var metaPredictionProbs: FloatArray

  /** Latest prediction latency in milliseconds.  */
  private var latestPredictionLatencyMs = 0f

  private var recognitionTask: TimerTask? = null

  /** Used to record audio samples. */
  private lateinit var audioRecord: AudioRecord

  /** Buffer that holds audio PCM sample that are fed to the TFLite model for inference.  */
  private lateinit var inputBuffer: FloatBuffer
  private lateinit var metaInputBuffer: FloatBuffer

  init {
    loadLabels(context)
    loadAssetList(context)
    setupInterpreter(context)
    setupMetaInterpreter(context)
    warmUpModel()
  }

//  override fun onResume(owner: LifecycleOwner) = start()

//  override fun onPause(owner: LifecycleOwner) = stop()

  /**
   * Starts sound classification, which triggers running of
   * `recordingThread` and `recognitionThread`.
   */
  fun start() {
    if (!isPaused) {
      startAudioRecord()
    }
  }

  /**
   * Stops sound classification, which triggers interruption of
   * `recognitionThread`.
   */
  fun stop() {
    if (isClosed || !isRecording) return
    recognitionTask?.cancel()

    audioRecord.stop()
    isRecording = false

  }

  fun close() {
    stop()

    if (isClosed) return
    interpreter.close()

    isClosed = true
  }


  /** Retrieve asset list from "asset_list" file */
  private fun loadAssetList(context: Context) {

    try {
      val reader =
        BufferedReader(InputStreamReader(context.assets.open(options.assetFile)))
      val wordList = mutableListOf<String>()
      reader.useLines { lines ->
        lines.forEach {
          wordList.add(it.trim())
        }
      }
      assetList = wordList.map { it }
    } catch (e: IOException) {
      Log.e(TAG, "Failed to read labels ${options.assetFile}: ${e.message}")
    }
  }
  
  /** Retrieve labels from "labels.txt" file */
  private fun loadLabels(context: Context) {
    val localeList = context.resources.configuration.locales
    val language = localeList.get(0).language
    var filename = options.labelsBase+"_${language}.txt"

    //Check if file exists
    val assetManager = context.assets // Replace 'assets' with actual AssetManager instance
    try {
      val mapList = assetManager.list("")?.toMutableList()

      if (mapList != null) {
        if (!mapList.contains(filename)) {
          filename = options.labelsBase+"_en.txt"
        }
      }
    } catch (ex: IOException) {
      ex.printStackTrace()
      filename = options.labelsBase+"_en.txt"
    }

    Log.i(TAG,filename)
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
      Log.i(TAG, "Label list entries: ${labelList.size}")
    } catch (e: IOException) {
      Log.e(TAG, "Failed to read labels ${filename}: ${e.message}")
    }
  }

  private fun setupInterpreter(context: Context) {
    try {
      val modelFilePath = context.getDir("filesdir", Context.MODE_PRIVATE).absolutePath + "/"+ options.modelPath
      Log.i(TAG, "Trying to create TFLite buffer from $modelFilePath")
      val modelFile = File(modelFilePath)
      val tfliteBuffer: ByteBuffer = FileChannel.open(modelFile.toPath(), StandardOpenOption.READ).use { channel ->
        channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size())
      }
      Log.i(TAG, "Done creating TFLite buffer from $modelFilePath")

      interpreter = Interpreter(tfliteBuffer, Interpreter.Options())
    } catch (e: IOException) {
      Log.e(TAG, "Failed to load TFLite model - ${e.message}")
      return
    }
    // Inspect input and output specs.
    val inputShape = interpreter.getInputTensor(0).shape()
    Log.i(TAG, "TFLite model input shape: ${inputShape.contentToString()}")
    modelInputLength = inputShape[1]

    val outputShape = interpreter.getOutputTensor(0).shape()
    Log.i(TAG, "TFLite output shape: ${outputShape.contentToString()}")
    modelNumClasses = outputShape[1]
    if (modelNumClasses != labelList.size) {
      Log.e(
        TAG,
        "Mismatch between metadata number of classes (${labelList.size})" +
                " and model output length ($modelNumClasses)"
      )
    }
    // Fill the array with NaNs initially.
    predictionProbs = FloatArray(modelNumClasses) { Float.NaN }

    inputBuffer = FloatBuffer.allocate(modelInputLength)

  }

  private fun setupMetaInterpreter(context: Context) {

    try {
      val metaModelFilePath = context.getDir("filesdir", Context.MODE_PRIVATE).absolutePath + "/"+ options.metaModelPath
      Log.i(TAG, "Trying to create TFLite buffer from $metaModelFilePath")
      val metaModelFile = File(metaModelFilePath)
      val tfliteBuffer: ByteBuffer = FileChannel.open(metaModelFile.toPath(), StandardOpenOption.READ).use { channel ->
        channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size())
      }
      Log.i(TAG, "Done creating TFLite buffer from $metaModelFilePath")

      meta_interpreter = Interpreter(tfliteBuffer, Interpreter.Options())
    } catch (e: IOException) {
      Log.e(TAG, "Failed to load TFLite model - ${e.message}")
      return
    }
    // Inspect input and output specs.
    val metaInputShape = meta_interpreter.getInputTensor(0).shape()
    Log.i(TAG, "TFLite meta model input shape: ${metaInputShape.contentToString()}")
    metaModelInputLength = metaInputShape[1]

    val metaOutputShape = meta_interpreter.getOutputTensor(0).shape()
    Log.i(TAG, "TFLite meta model output shape: ${metaOutputShape.contentToString()}")
    metaModelNumClasses = metaOutputShape[1]
    if (metaModelNumClasses != labelList.size) {
      Log.e(
        TAG,
        "Mismatch between metadata number of classes (${labelList.size})" +
                " and meta model output length ($metaModelNumClasses)"
      )
    }
    // Fill the array with 1 initially.
    metaPredictionProbs = FloatArray(metaModelNumClasses) { 1f }
    metaInputBuffer = FloatBuffer.allocate(metaModelInputLength)

  }

  fun runMetaInterpreter(location: Location) {
    val dayOfYear = LocalDate.now().dayOfYear
    val week = ceil( dayOfYear*48.0/366.0) //model year has 48 weeks
    lat = location.latitude.toFloat()
    lon = location.longitude.toFloat()

    Handler(Looper.getMainLooper()).post {
      mBinding.gps.setText(mContext.getString(R.string.latitude)+": " + (round(lat*100.0)/100.0).toString() + " / " + mContext.getString(R.string.longitude) + ": " + (round(lon*100.0)/100).toString())
    }

    val weekMeta = cos(Math.toRadians(week * 7.5)) + 1.0

    metaInputBuffer.put(0, lat)
    metaInputBuffer.put(1, lon)
    metaInputBuffer.put(2, weekMeta.toFloat())
    metaInputBuffer.rewind() // Reset position to beginning of buffer
    val metaOutputBuffer = FloatBuffer.allocate(metaModelNumClasses)
    metaOutputBuffer.rewind()
    meta_interpreter.run(metaInputBuffer, metaOutputBuffer)
    metaOutputBuffer.rewind()
    metaOutputBuffer.get(metaPredictionProbs) // Copy data to metaPredictionProbs.


    for (i in metaPredictionProbs.indices) {
      metaPredictionProbs[i] = if (metaPredictionProbs[i] >= options.metaProbabilityThreshold) {
        1f
      } else {
        0f
      }
    }
  }

  private fun warmUpModel() {
    generateDummyAudioInput(inputBuffer)
    for (n in 0 until options.warmupRuns) {
      val t0 = SystemClock.elapsedRealtimeNanos()

      // Create input and output buffers.
      val outputBuffer = FloatBuffer.allocate(modelNumClasses)
      inputBuffer.rewind()
      outputBuffer.rewind()
      interpreter.run(inputBuffer, outputBuffer)

      Log.i(
        TAG,
        "Switches: Done calling interpreter.run(): %s (%.6f ms)".format(
          outputBuffer.array().contentToString(),
          (SystemClock.elapsedRealtimeNanos() - t0) / NANOS_IN_MILLIS
        )
      )
    }
  }

  private fun generateDummyAudioInput(inputBuffer: FloatBuffer) {
    val twoPiTimesFreq = 2 * Math.PI.toFloat() * 1000f
    for (i in 0 until modelInputLength) {
      val x = i.toFloat() / (modelInputLength - 1)
      inputBuffer.put(i, sin(twoPiTimesFreq * x.toDouble()).toFloat())
    }
  }

  /** Start recording and triggers recognition.  */
  @Synchronized
  private fun startAudioRecord() {
    if (isRecording) return
    setupAudioRecord()
    isClosed = false
    isRecording = true
  }

  @SuppressLint("MissingPermission")  //Permission already requested in MainActivity
  private fun setupAudioRecord() {
    var bufferSize = AudioRecord.getMinBufferSize(
      options.sampleRate,
      AudioFormat.CHANNEL_IN_MONO,
      AudioFormat.ENCODING_PCM_16BIT
    )
    Log.i(TAG, "min buffer size = $bufferSize")
    if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
      bufferSize = options.sampleRate * 2
      Log.w(TAG, "bufferSize has error or bad value")
    }
    // The buffer of AudioRecord should be larger than what model requires.
    val modelRequiredBufferSize = 2 * modelInputLength * Short.SIZE_BYTES
    if (bufferSize < modelRequiredBufferSize) {
      bufferSize = modelRequiredBufferSize
    }
    Log.i(TAG, "bufferSize = $bufferSize")
    audioRecord = AudioRecord(
      // including MIC, UNPROCESSED, and CAMCORDER.
      MediaRecorder.AudioSource.UNPROCESSED,
      options.sampleRate,
      AudioFormat.CHANNEL_IN_MONO,
      AudioFormat.ENCODING_PCM_16BIT,
      bufferSize
    )
    if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
      Log.e(TAG, "AudioRecord failed to initialize")
      return
    }
    Log.i(TAG, "Successfully initialized AudioRecord")

    audioRecord.startRecording()
    Log.i(TAG, "Successfully started AudioRecord recording")

    // Start recognition (model inference) thread.
    startRecognition()
  }

  private fun loadAudio(audioBuffer: ShortArray): Int {
    when (
      val loadedSamples = audioRecord.read(
        audioBuffer, 0, audioBuffer.size, AudioRecord.READ_NON_BLOCKING
      )
    ) {
      AudioRecord.ERROR_INVALID_OPERATION -> {
        Log.w(TAG, "AudioRecord.ERROR_INVALID_OPERATION")
      }
      AudioRecord.ERROR_BAD_VALUE -> {
        Log.w(TAG, "AudioRecord.ERROR_BAD_VALUE")
      }
      AudioRecord.ERROR_DEAD_OBJECT -> {
        Log.w(TAG, "AudioRecord.ERROR_DEAD_OBJECT")
      }
      AudioRecord.ERROR -> {
        Log.w(TAG, "AudioRecord.ERROR")
      }
      else -> {
        return loadedSamples
      }
    }
    // No new sample was loaded.
    return 0
  }

  private fun startRecognition() {
    if (modelInputLength <= 0 || modelNumClasses <= 0) {
      Log.e(TAG, "Switches: Cannot start recognition because model is unavailable.")
      return
    }

    val circularBuffer = ShortArray(modelInputLength)

    var j = 0 // Indices for the circular buffer next write

    Log.w(TAG, "recognitionPeriod:"+inferenceInterval)
    recognitionTask = Timer().scheduleAtFixedRate(inferenceInterval, inferenceInterval) task@{
      val outputBuffer = FloatBuffer.allocate(modelNumClasses)
      val recordingBuffer = ShortArray(modelInputLength)

      // Load new audio samples
      val sampleCounts = loadAudio(recordingBuffer)
      if (sampleCounts == 0) {
        return@task
      }


      // Copy new data into the circular buffer
      for (i in 0 until sampleCounts) {
        circularBuffer[j] = recordingBuffer[i]
        j = (j + 1) % circularBuffer.size
      }

      // Feed data to the input buffer.
      var samplesAreAllZero = true
      for (i in 0 until modelInputLength) {
        val s = if (i > options.pointsInAverage) {
          ((i - options.pointsInAverage + 1)..i).map {
            circularBuffer[(j + it) % modelInputLength]
          }
            .average()
        } else {
          circularBuffer[(i + j) % modelInputLength]
        }
        if (samplesAreAllZero && s.toInt() != 0) {
          samplesAreAllZero = false
        }
        inputBuffer.put(i, s.toFloat())
      }

      if (samplesAreAllZero) {
        Log.w(TAG, mContext.resources.getString(R.string.samples_zero))
        Handler(Looper.getMainLooper()).post {
          Toast.makeText(mContext,mContext.resources.getString(R.string.samples_zero),Toast.LENGTH_SHORT).show()
        }

        return@task
      }

      val t0 = SystemClock.elapsedRealtimeNanos()
      inputBuffer.rewind()
      outputBuffer.rewind()
      interpreter.run(inputBuffer, outputBuffer)
      outputBuffer.rewind()
      outputBuffer.get(predictionProbs) // Copy data to predictionProbs.

      val probList = mutableListOf<Float>()
      if (mBinding.checkIgnoreMeta.isChecked){
        for (value in predictionProbs) {
          probList.add(1 / (1 + kotlin.math.exp(-value)))  //apply sigmoid
        }
      } else {
        for (i in predictionProbs.indices) {
          probList.add( metaPredictionProbs[i] / (1+kotlin.math.exp(-predictionProbs[i])) )  //apply sigmoid
        }
      }

      if (mBinding.progressHorizontal.isIndeterminate){
        probList.withIndex().also {
          val max = it.maxByOrNull { entry -> entry.value }
          val labelAtMaxIndex = labelList[max!!.index].split("_").last()  //show in locale language
          //Log.i(TAG, "inference result: label=$labelAtMaxIndex, max=${max?.value}, index=${max?.index}")
          //Log.i(TAG, "inference result:" +probList.maxOrNull())
          if (max.value > options.probabilityThreshold) {
            Handler(Looper.getMainLooper()).post {
              mBinding.text1.setText(labelAtMaxIndex+ "  " + Math.round(max.value * 100.0) + "%")
              if (max.value < 0.5) mBinding.text1.setBackgroundResource(R.drawable.oval_holo_red_dark)
              else if (max.value < 0.65) mBinding.text1.setBackgroundResource(R.drawable.oval_holo_orange_dark)
              else if (max.value < 0.8) mBinding.text1.setBackgroundResource(R.drawable.oval_holo_orange_light)
              else mBinding.text1.setBackgroundResource(R.drawable.oval_holo_green_light)
              database?.addEntry(labelAtMaxIndex, lat, lon, max.index, max.value)
            }
          } else {
            Handler(Looper.getMainLooper()).post {
              mBinding.text1.setText("")
              mBinding.text1.setBackgroundColor(mContext.resources.getColor(R.color.dark_blue_gray700))
            }
          }

          if (mBinding.checkShowImages.isChecked){
            Handler(Looper.getMainLooper()).post {

              val url = if (max.value > options.displayImageThreshold && assetList[max.index] != "NO_ASSET") {
                "https://macaulaylibrary.org/asset/" + assetList[max.index] + "/embed"
              } else {
                "about:blank"
              }

              if (url == null || url == "about:blank"){
                mBinding.webview.setVisibility(View.GONE)
                mBinding.icon.setVisibility(View.VISIBLE)
                mBinding.webviewUrl.setText("")
                mBinding.webviewUrl.setVisibility(View.GONE)
                mBinding.webviewName.setText("")
                mBinding.webviewName.setVisibility(View.GONE)
              } else {
                if (mBinding.webview.url != url) {
                  mBinding.webview.setVisibility(View.INVISIBLE)
                  mBinding.webview.loadUrl(url)
                  mBinding.webviewUrl.setText(url)
                  mBinding.webviewUrl.setVisibility(View.VISIBLE)
                  mBinding.webviewName.setText(labelAtMaxIndex)
                  mBinding.webviewName.setVisibility(View.VISIBLE)
                  mBinding.icon.setVisibility(View.GONE)
                }
              }
            }
          } else {
            Handler(Looper.getMainLooper()).post {
              mBinding.webview.setVisibility(View.GONE)
              mBinding.icon.setVisibility(View.VISIBLE)
              mBinding.webview.loadUrl("about:blank")
              mBinding.webviewUrl.setText("")
              mBinding.webviewUrl.setVisibility(View.GONE)
              mBinding.webviewName.setText("")
              mBinding.webviewName.setVisibility(View.GONE)
            }
          }

        }
      }

      latestPredictionLatencyMs =
        ((SystemClock.elapsedRealtimeNanos() - t0) / 1e6).toFloat()
    }
  }


  companion object {
    private const val TAG = "SoundClassifier"
    var lat: Float = 0.0f
    var lon: Float = 0.0f
    /** Number of nanoseconds in a millisecond  */
    private const val NANOS_IN_MILLIS = 1_000_000.toDouble()
  }
}

private fun String.toTitleCase() =
  splitToSequence("_")
    .map { it.capitalize(Locale.ROOT) }
    .joinToString("_")
    .trim()
