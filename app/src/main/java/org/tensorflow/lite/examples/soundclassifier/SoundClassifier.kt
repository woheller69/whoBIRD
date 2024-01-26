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

package org.tensorflow.lite.examples.soundclassifier

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.annotation.MainThread
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.FloatBuffer
import java.util.Locale
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.scheduleAtFixedRate
import kotlin.math.sin
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil

/**
 * Performs classification on sound.
 *
 * <p>The API supports models which accept sound input via {@code AudioRecord} and one classification output tensor.
 * The output of the recognition is emitted as LiveData of Map.
 *
 */
class SoundClassifier(context: Context, private val options: Options = Options()) :
  DefaultLifecycleObserver {
  internal var mcontext: Context
  init {
    this.mcontext = context.applicationContext
  }
  class Options constructor(
    /** Path of the converted model label file, relative to the assets/ directory.  */
    val labelsBase: String = "labels",
    /** Path of the converted .tflite file, relative to the assets/ directory.  */
    val modelPath: String = "BirdNET_GLOBAL_6K_V2.4_Model_FP16.tflite",
    /** The required audio sample rate in Hz.  */
    val sampleRate: Int = 48000,
    /** Multiplier for audio samples  */
    val audioGain: Int = 10,
    /** Number of warm up runs to do after loading the TFLite model.  */
    val warmupRuns: Int = 3,
    /** Number of points in average to reduce noise. (default 10)*/
    val pointsInAverage: Int = 1,
    /** Overlap factor of recognition period */
    var overlapFactor: Float = 0.5f,
    /** Probability value above which a class is labeled as active (i.e., detected) the display. (default 0.3) */
    var probabilityThreshold: Float = 0.1f,
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

  /** Overlap factor of recognition period */
  var overlapFactor: Float
    get() = options.overlapFactor
    set(value) {
      options.overlapFactor = value.also {
        recognitionPeriod = (5000L * (1 - value)).toLong()
      }
    }

  /** Probability value above which a class is labeled as active (i.e., detected) the display.  */
  var probabilityThreshold: Float
    get() = options.probabilityThreshold
    set(value) {
      options.probabilityThreshold = value
    }

  /** Paused by user */
  var isPaused: Boolean = false
    set(value) {
      field = value
      if (value) stop() else start()
    }

  /** Names of the model's output classes.  */
  lateinit var labelList: List<String>
    private set

  /** How many milliseconds between consecutive model inference calls.  */
  private var recognitionPeriod = (5000L * (1 - overlapFactor)).toLong()

  /** The TFLite interpreter instance.  */
  private lateinit var interpreter: Interpreter

  /** Audio length (in # of PCM samples) required by the TFLite model.  */
  private var modelInputLength = 0

  /** Number of output classes of the TFLite model.  */
  private var modelNumClasses = 0

  /** Used to hold the real-time probabilities predicted by the model for the output classes.  */
  private lateinit var predictionProbs: FloatArray

  /** Latest prediction latency in milliseconds.  */
  private var latestPredictionLatencyMs = 0f

  private var recognitionTask: TimerTask? = null

  /** Used to record audio samples. */
  private lateinit var audioRecord: AudioRecord

  /** Buffer that holds audio PCM sample that are fed to the TFLite model for inference.  */
  private lateinit var inputBuffer: FloatBuffer

  init {
    loadLabels(context)
    setupInterpreter(context)
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
    } catch (e: IOException) {
      Log.e(TAG, "Failed to read model ${filename}: ${e.message}")
    }
  }

  private fun setupInterpreter(context: Context) {
    interpreter = try {
      val tfliteBuffer = FileUtil.loadMappedFile(context, options.modelPath)
      Log.i(TAG, "Done creating TFLite buffer from ${options.modelPath}")
      Interpreter(tfliteBuffer, Interpreter.Options())
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

    Log.w(TAG, "recognitionPeriod:"+recognitionPeriod)
    recognitionTask = Timer().scheduleAtFixedRate(recognitionPeriod, recognitionPeriod) task@{
      val outputBuffer = FloatBuffer.allocate(modelNumClasses)
      val recordingBuffer = ShortArray(modelInputLength)

      // Load new audio samples
      val sampleCounts = loadAudio(recordingBuffer)
      if (sampleCounts == 0) {
        return@task
      }

      var cliping = false
      // Copy new data into the circular buffer
      for (i in 0 until sampleCounts) {
        var sample =
          (recordingBuffer[i].toDouble() * options.audioGain).toInt() // Apply the gain
        if (sample > 32767){
          sample = 32767
          cliping = true
        }
        else if (sample < -32767) {
          sample = -32767
          cliping = true
        }

        circularBuffer[j] = sample.toShort()
        j = (j + 1) % circularBuffer.size
      }
      if (cliping) {
        Handler(Looper.getMainLooper()).post {
          Toast.makeText(mcontext, mcontext.getString(R.string.error_too_lound), Toast.LENGTH_SHORT).show()
        }
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
        Log.w(TAG, "No audio input: All audio samples are zero!")
        return@task
      }
      val t0 = SystemClock.elapsedRealtimeNanos()
      inputBuffer.rewind()
      outputBuffer.rewind()
      interpreter.run(inputBuffer, outputBuffer)
      outputBuffer.rewind()
      outputBuffer.get(predictionProbs) // Copy data to predictionProbs.

      val probList = predictionProbs.map {
        if (it > probabilityThreshold) it else 0f
      }

      probList.withIndex().also {
        val max = it.maxByOrNull { entry -> entry.value }
        val labelAtMaxIndex = labelList[max!!.index].split("_").last()  //show in locale language
        //Log.i(TAG, "inference result: label=$labelAtMaxIndex, max=${max?.value}, index=${max?.index}")
        //Log.i(TAG, "inference result:" +probList.maxOrNull())
        if (max.value > 0) {
          Handler(Looper.getMainLooper()).post {
            Toast.makeText(mcontext, labelAtMaxIndex+ "\n" + Math.round(max.value * 100.0) / 100.0 + " #" + max.index, Toast.LENGTH_SHORT).show()
          }
        }
      }


      latestPredictionLatencyMs =
        ((SystemClock.elapsedRealtimeNanos() - t0) / 1e6).toFloat()
    }
  }

  companion object {
    private const val TAG = "SoundClassifier"

    /** Number of nanoseconds in a millisecond  */
    private const val NANOS_IN_MILLIS = 1_000_000.toDouble()
  }
}

private fun String.toTitleCase() =
  splitToSequence("_")
    .map { it.capitalize(Locale.ROOT) }
    .joinToString("_")
    .trim()
