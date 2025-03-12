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
    static final String modelFILE = "model.tflite";
    static final String metaModelFILE = "metaModel.tflite";
    static final String model16URL = "https://raw.githubusercontent.com/woheller69/whoBIRD-TFlite/master/BirdNET_GLOBAL_6K_V2.4_Model_FP16.tflite";
    static final String model32URL = "https://raw.githubusercontent.com/woheller69/whoBIRD-TFlite/master/BirdNET_GLOBAL_6K_V2.4_Model_FP32.tflite";
    static final String metaModelURL = "https://raw.githubusercontent.com/woheller69/whoBIRD-TFlite/master/BirdNET_GLOBAL_6K_V2.4_MData_Model_FP16.tflite";
    static final String model16MD5 = "b1c981fe261910b473b9b7eec9ebcd4e";
    static final String model32MD5 = "6c7c42106e56550fc8563adb31bc120e";
    static final String metaModelMD5 ="f1a078ae0f244a1ff5a8f1ccb645c805";
    static long model16Size = 25932528;
    static final long model32Size = 51726412;
    static final long metaModelSize = 7071440;
    static long downloadModelSize = 0;
    static long downloadMetaModelSize = 0;
    static boolean downloadModelFinished = false;
    static boolean downloadMetaModelFinished = false;
    static long modelSize;

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

        if (modelFile.exists() && !(calcModelMD5.equals(model16MD5) || calcModelMD5.equals(model32MD5))){modelFile.delete(); downloadModelFinished = false;}
        if (metaModelFile.exists() && !calcMetaModelMD5.equals(metaModelMD5)) {metaModelFile.delete();downloadMetaModelFinished = false;}

        return (calcModelMD5.equals(model16MD5) || calcModelMD5.equals(model32MD5)) && calcMetaModelMD5.equals(metaModelMD5);
    }

    public static void downloadModels(final Activity activity, ActivityDownloadBinding binding) {
        checkModels(activity);

        modelSize = binding.option32bit.isChecked() ? model32Size : model16Size;

        binding.downloadProgress.setProgress(0);
        binding.downloadButton.setEnabled(false);

        File modelFile = new File(activity.getDir("filesdir", Context.MODE_PRIVATE) + "/" + modelFILE);
        if (!modelFile.exists() || modelFile.length() != modelSize) {
            if (modelFile.exists()) {modelFile.delete(); downloadModelFinished = false;}
            Log.d("whoBIRD", "model file does not exist or wrong model");
            Thread thread = new Thread(() -> {
                try {
                    URL url;
                    if (binding.option32bit.isChecked()) url = new URL(model32URL);
                    else url = new URL(model16URL);

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
                        if (modelFile.exists()) downloadModelSize = modelFile.length();
                        activity.runOnUiThread(() -> {
                            binding.downloadProgress.setProgress((int) (((double)(downloadModelSize + downloadMetaModelSize) / (modelSize + metaModelSize)) * 100));
                        });
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

                    if (!(calcModelMD5.equals(model16MD5) || calcModelMD5.equals(model32MD5) )){
                        modelFile.delete();
                        downloadModelFinished = false;
                        activity.runOnUiThread(() -> {
                            Toast.makeText(activity, activity.getResources().getString(R.string.error_download), Toast.LENGTH_SHORT).show();
                        });
                    } else {
                        downloadModelFinished = true;
                        activity.runOnUiThread(() -> {
                            if (downloadModelFinished && downloadMetaModelFinished) binding.buttonStart.setVisibility(View.VISIBLE);
                        });
                    }
                } catch (NoSuchAlgorithmException | IOException i) {
                    activity.runOnUiThread(() -> Toast.makeText(activity, activity.getResources().getString(R.string.error_download), Toast.LENGTH_SHORT).show());
                    modelFile.delete();
                    downloadModelFinished = false;
                    Log.w("whoBIRD", activity.getResources().getString(R.string.error_download), i);
                }
            });
            thread.start();
        } else {
            downloadModelSize = modelSize;
            downloadModelFinished = true;
            activity.runOnUiThread(() -> {
                if (downloadModelFinished && downloadMetaModelFinished) binding.buttonStart.setVisibility(View.VISIBLE);
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
                        if (metaModelFile.exists()) downloadMetaModelSize = metaModelFile.length();
                        activity.runOnUiThread(() -> {
                            binding.downloadProgress.setProgress((int) (((double)(downloadModelSize + downloadMetaModelSize) / (modelSize + metaModelSize)) * 100));
                        });
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
                        downloadMetaModelFinished = false;
                        activity.runOnUiThread(() -> {
                            Toast.makeText(activity, activity.getResources().getString(R.string.error_download), Toast.LENGTH_SHORT).show();
                        });
                    } else {
                        downloadMetaModelFinished = true;
                        activity.runOnUiThread(() -> {
                            if (downloadModelFinished && downloadMetaModelFinished) binding.buttonStart.setVisibility(View.VISIBLE);
                        });
                    }
                } catch (NoSuchAlgorithmException | IOException i) {
                    activity.runOnUiThread(() -> Toast.makeText(activity, activity.getResources().getString(R.string.error_download), Toast.LENGTH_SHORT).show());
                    metaModelFile.delete();
                    downloadMetaModelFinished = false;
                    Log.w("whoBIRD", activity.getResources().getString(R.string.error_download), i);
                }
            });
            thread.start();
        } else {
            downloadMetaModelSize = metaModelSize;
            downloadMetaModelFinished = true;
            activity.runOnUiThread(() -> {
                if (downloadModelFinished && downloadMetaModelFinished) binding.buttonStart.setVisibility(View.VISIBLE);
            });
        }

    }
}