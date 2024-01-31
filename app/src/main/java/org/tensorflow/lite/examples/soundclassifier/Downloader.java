package org.tensorflow.lite.examples.soundclassifier;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.tensorflow.lite.examples.soundclassifier.databinding.ActivityDownloadBinding;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;


@SuppressWarnings("ResultOfMethodCallIgnored")
public class Downloader {
    static String modelFILE = "model.tflite";
    static String metaModelFILE = "metaModel.tflite";

    public static boolean checkModels(final Context context) {
        File modelFile = new File(context.getDir("filesdir", Context.MODE_PRIVATE) + "/" + modelFILE);
        File metaModelFile = new File(context.getDir("filesdir", Context.MODE_PRIVATE) + "/" + metaModelFILE);
        return modelFile.exists() && metaModelFile.exists();
    }

    public static void downloadModels(final Activity activity, ActivityDownloadBinding binding) {

        String modelURL = "https://raw.githubusercontent.com/woheller69/BirdNet-lite-Android/master/app/src/main/assets/BirdNET_GLOBAL_6K_V2.4_Model_FP16.tflite";
        String metaModelURL = "https://raw.githubusercontent.com/woheller69/BirdNet-lite-Android/master/app/src/main/assets/BirdNET_GLOBAL_6K_V2.4_MData_Model_FP16.tflite";
        File modelFile = new File(activity.getDir("filesdir", Context.MODE_PRIVATE) + "/" + modelFILE);
        if (!modelFile.exists()) {
            Log.d("whoBIRD", "model file does not exist");
            Thread thread = new Thread(() -> {
                try {
                    URL url = new URL(modelURL);
                    Log.d("whoBIRD", "Download model");

                    URLConnection ucon = url.openConnection();
                    ucon.setReadTimeout(5000);
                    ucon.setConnectTimeout(10000);

                    InputStream is = ucon.getInputStream();
                    BufferedInputStream inStream = new BufferedInputStream(is, 1024 * 5);

                    modelFile.createNewFile();

                    FileOutputStream outStream = new FileOutputStream(modelFile);
                    byte[] buff = new byte[5 * 1024];

                    int len;
                    while ((len = inStream.read(buff)) != -1) {
                        outStream.write(buff, 0, len);
                    }
                    outStream.flush();
                    outStream.close();
                    inStream.close();
                    activity.runOnUiThread(() -> {
                        binding.downloadProgress.setProgress(binding.downloadProgress.getProgress()+50);
                        if (binding.downloadProgress.getProgress()==100) binding.buttonStart.setVisibility(View.VISIBLE);
                    });
                } catch (IOException i) {
                    activity.runOnUiThread(() -> {
                        Toast.makeText(activity, "Error Downloading model file", Toast.LENGTH_SHORT).show();
                    });
                    modelFile.delete();
                    Log.w("whoBIRD", "Error Downloading model file", i);
                }
            });
            thread.start();
        }

        File metaModelFile = new File(activity.getDir("filesdir", Context.MODE_PRIVATE) + "/" + metaModelFILE);
        if (!metaModelFile.exists()) {
            Log.d("whoBIRD", "meta model file does not exist");
            Thread thread = new Thread(() -> {
                try {
                    URL url = new URL(metaModelURL);
                    Log.d("whoBIRD", "Download meta model");

                    URLConnection ucon = url.openConnection();
                    ucon.setReadTimeout(5000);
                    ucon.setConnectTimeout(10000);

                    InputStream is = ucon.getInputStream();
                    BufferedInputStream inStream = new BufferedInputStream(is, 1024 * 5);

                    metaModelFile.createNewFile();

                    FileOutputStream outStream = new FileOutputStream(metaModelFile);
                    byte[] buff = new byte[5 * 1024];

                    int len;
                    while ((len = inStream.read(buff)) != -1) {
                        outStream.write(buff, 0, len);
                    }
                    outStream.flush();
                    outStream.close();
                    inStream.close();
                    activity.runOnUiThread(() -> {
                        binding.downloadProgress.setProgress(binding.downloadProgress.getProgress()+50);
                        if (binding.downloadProgress.getProgress()==100) binding.buttonStart.setVisibility(View.VISIBLE);
                    });
                } catch (IOException i) {
                    activity.runOnUiThread(() -> {
                        Toast.makeText(activity, "Error Downloading model file", Toast.LENGTH_SHORT).show();
                    });
                    metaModelFile.delete();
                    Log.w("whoBIRD", "Error Downloading meta model file", i);
                }
            });
            thread.start();
        }

    }
}