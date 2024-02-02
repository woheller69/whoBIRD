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
import java.math.BigInteger;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


@SuppressWarnings("ResultOfMethodCallIgnored")
public class Downloader {
    static String modelFILE = "model.tflite";
    static String metaModelFILE = "metaModel.tflite";
    static String modelURL = "https://raw.githubusercontent.com/woheller69/whoBIRD-TFlite/master/BirdNET_GLOBAL_6K_V2.4_Model_FP16.tflite";
    static String metaModelURL = "https://raw.githubusercontent.com/woheller69/whoBIRD-TFlite/master/BirdNET_GLOBAL_6K_V2.4_MData_Model_FP16.tflite";
    static String modelMD5 = "b1c981fe261910b473b9b7eec9ebcd4e";
    static String metaModelMD5 ="f1a078ae0f244a1ff5a8f1ccb645c805";

    public static boolean checkModels(final Activity activity) {
        File modelFile = new File(activity.getDir("filesdir", Context.MODE_PRIVATE) + "/" + modelFILE);
        File metaModelFile = new File(activity.getDir("filesdir", Context.MODE_PRIVATE) + "/" + metaModelFILE);
        String calcModelMD5 = "";
        String calcMetaModelMD5 = "";
        if (modelFile.exists()) {
            try {
                byte[] data = Files.readAllBytes(Paths.get(modelFile.getPath()));
                byte[] hash = MessageDigest.getInstance("MD5").digest(data);
                calcModelMD5 = new BigInteger(1, hash).toString(16);
            } catch (IOException | NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }
        if (metaModelFile.exists()) {
            try {
                byte[] data = Files.readAllBytes(Paths.get(metaModelFile.getPath()));
                byte[] hash = MessageDigest.getInstance("MD5").digest(data);
                calcMetaModelMD5 = new BigInteger(1, hash).toString(16);
            } catch (IOException | NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }

        if (modelFile.exists() && !calcModelMD5.equals(modelMD5)) modelFile.delete();
        if (metaModelFile.exists() && !calcMetaModelMD5.equals(metaModelMD5)) metaModelFile.delete();

        return calcModelMD5.equals(modelMD5) && calcMetaModelMD5.equals(metaModelMD5);
    }

    public static void downloadModels(final Activity activity, ActivityDownloadBinding binding) {
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

                    String calcModelMD5="";
                    if (modelFile.exists()) {
                        byte[] data = Files.readAllBytes(Paths.get(modelFile.getPath()));
                        byte[] hash = MessageDigest.getInstance("MD5").digest(data);
                        calcModelMD5 = new BigInteger(1, hash).toString(16);
                    } else {
                        throw new IOException();  //throw exception if there is no modelFile at this point
                    }

                    if (!calcModelMD5.equals(modelMD5)){
                        modelFile.delete();
                        activity.runOnUiThread(() -> {
                            Toast.makeText(activity, activity.getResources().getString(R.string.error_download), Toast.LENGTH_SHORT).show();
                        });
                    } else {
                        activity.runOnUiThread(() -> {
                            binding.downloadProgress.setProgress(binding.downloadProgress.getProgress()+50);
                            if (binding.downloadProgress.getProgress()==100) binding.buttonStart.setVisibility(View.VISIBLE);
                        });
                    }
                } catch (NoSuchAlgorithmException | IOException i) {
                    activity.runOnUiThread(() -> Toast.makeText(activity, activity.getResources().getString(R.string.error_download), Toast.LENGTH_SHORT).show());
                    modelFile.delete();
                    Log.w("whoBIRD", activity.getResources().getString(R.string.error_download), i);
                }
            });
            thread.start();
        } else {
            activity.runOnUiThread(() -> {
                binding.downloadProgress.setProgress(binding.downloadProgress.getProgress()+50);
                if (binding.downloadProgress.getProgress()==100) binding.buttonStart.setVisibility(View.VISIBLE);
            });
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

                    String calcMetaModelMD5="";
                    if (metaModelFile.exists()) {
                        byte[] data = Files.readAllBytes(Paths.get(metaModelFile.getPath()));
                        byte[] hash = MessageDigest.getInstance("MD5").digest(data);
                        calcMetaModelMD5 = new BigInteger(1, hash).toString(16);
                    } else {
                        throw new IOException();  //throw exception if there is no modelFile at this point
                    }

                    if (!calcMetaModelMD5.equals(metaModelMD5)){
                        metaModelFile.delete();
                        activity.runOnUiThread(() -> {
                            Toast.makeText(activity, activity.getResources().getString(R.string.error_download), Toast.LENGTH_SHORT).show();
                        });
                    } else {
                        activity.runOnUiThread(() -> {
                            binding.downloadProgress.setProgress(binding.downloadProgress.getProgress()+50);
                            if (binding.downloadProgress.getProgress()==100) binding.buttonStart.setVisibility(View.VISIBLE);
                        });
                    }
                } catch (NoSuchAlgorithmException | IOException i) {
                    activity.runOnUiThread(() -> Toast.makeText(activity, activity.getResources().getString(R.string.error_download), Toast.LENGTH_SHORT).show());
                    metaModelFile.delete();
                    Log.w("whoBIRD", activity.getResources().getString(R.string.error_download), i);
                }
            });
            thread.start();
        } else {
            activity.runOnUiThread(() -> {
                binding.downloadProgress.setProgress(binding.downloadProgress.getProgress()+50);
                if (binding.downloadProgress.getProgress()==100) binding.buttonStart.setVisibility(View.VISIBLE);
            });
        }

    }
}