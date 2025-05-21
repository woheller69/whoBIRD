package org.tensorflow.lite.examples.soundclassifier;

import static java.lang.Math.cos;
import static java.lang.Math.log10;
import static java.lang.Math.sin;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;

import androidx.preference.PreferenceManager;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MelSpectrogram {
    public static final int N_FFT = 800;
    public static final int N_MEL = 40;
    public static final int HOP_LENGTH = 960;
    private static float[] melFilters = null;
    private static final Mel mel = new Mel();

    public static Bitmap getMelBitmap(Context context, FloatBuffer audioBuffer, int sampleRate) {
        if (melFilters == null) melFilters = createMelFilterBank(N_FFT, sampleRate / 2, N_MEL);  //we are downsampling from 48000 to 24000Hz
        audioBuffer.rewind();
        int bufferLength = audioBuffer.remaining();
        float[] floatArray = new float[bufferLength / 2]; //half size for downsampling
        float maxAbsValue = 0.0f;

        for (int i = 0; i < floatArray.length; i++) {
            floatArray[i] = audioBuffer.get() / 32768.0f;
            if (Math.abs(floatArray[i]) > maxAbsValue) {
                maxAbsValue = Math.abs(floatArray[i]);
            }
            audioBuffer.get(); //skip next value for downsampling
        }

        // Normalize the samples
        if (maxAbsValue > 0.0f) {
            for (int i = 0; i < floatArray.length; i++) {
                floatArray[i] /= maxAbsValue;
            }
        }

        float[] melSpectrogram = getMelSpectrogram(floatArray);

        return calcMelSpectrogramBMP(context, melSpectrogram);
    }


    public static float[] getMelSpectrogram(float[] samples) {

        int nThreads =  Runtime.getRuntime().availableProcessors();
        int fftSize = N_FFT;
        int fftStep = HOP_LENGTH;
        int nSamples = samples.length;

        mel.nMel = N_MEL;
        mel.nLen = nSamples / fftStep;
        mel.data = new float[mel.nMel * mel.nLen];

        float[] hann = new float[fftSize];
        for (int i = 0; i < fftSize; i++) {
            hann[i] = (float) (0.5 * (1.0 - cos(2.0 * Math.PI * i / fftSize)));
        }

        int nFft = 1 + fftSize / 2;

        // Calculate mel values using multiple threads
        List<Thread> workers = new ArrayList<>();
        for (int iw = 0; iw < nThreads; iw++) {
            final int ith = iw;  // Capture iw in a final variable for use in the lambda
            Thread thread = new Thread(() -> {
                // Inside the thread, ith will have the same value as iw (first value is 0)

                float[] fftIn = new float[fftSize];
                Arrays.fill(fftIn, 0.0f);
                float[] fftOut = new float[fftSize * 2];

                for (int i = ith; i < mel.nLen; i += nThreads) {

                    int offset = i * fftStep;

                    // apply Hanning window
                    for (int j = 0; j < fftSize; j++) {
                        if (offset + j < nSamples) {
                            fftIn[j] = hann[j] * samples[offset + j];
                        } else {
                            fftIn[j] = 0.0f;
                        }
                    }

                    // FFT -> mag^2
                    fft(fftIn, fftOut);
                    for (int j = 0; j < fftSize; j++) {
                        fftOut[j] = fftOut[2 * j] * fftOut[2 * j] + fftOut[2 * j + 1] * fftOut[2 * j + 1];
                    }

                    for (int j = 1; j < fftSize / 2; j++) {
                        fftOut[j] += fftOut[fftSize - j];
                    }

                    // mel spectrogram
                    for (int j = 0; j < mel.nMel; j++) {
                        double sum = 0.0;
                        for (int k = 0; k < nFft; k++) {
                            sum += (fftOut[k] * melFilters[j * nFft + k]);
                        }

                        if (sum < 1e-10) {
                            sum = 1e-10;
                        }

                        sum = log10(sum);
                        mel.data[j * mel.nLen + i] = (float) sum;
                    }
                }

            });
            workers.add(thread);
            thread.start();
        }

        // Wait for all threads to finish
        for (Thread worker : workers) {
            try {
                worker.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // clamping and normalization
        double mmax = -1e20;
        for (int i = 0; i < mel.nMel * mel.nLen; i++) {
            if (mel.data[i] > mmax) {
                mmax = mel.data[i];
            }
        }

        mmax -= 8.0;
        for (int i = 0; i < mel.nMel * mel.nLen; i++) {
            if (mel.data[i] < mmax) {
                mel.data[i] = (float) mmax;
            }
            mel.data[i] = (float) ((mel.data[i] + 4.0) / 4.0);
        }

        return mel.data;
    }

    private static void dft(float[] input, float[] output) {
        int inSize = input.length;
        for (int k = 0; k < inSize; k++) {
            float re = 0.0f;
            float im = 0.0f;
            for (int n = 0; n < inSize; n++) {
                float angle = (float) (2 * Math.PI * k * n / inSize);
                re += input[n] * cos(angle);
                im -= input[n] * sin(angle);
            }
            output[k * 2 + 0] = re;
            output[k * 2 + 1] = im;
        }
    }

    private static void fft(float[] input, float[] output) {
        int inSize = input.length;
        if (inSize == 1) {
            output[0] = input[0];
            output[1] = 0.0f;
            return;
        }

        if (inSize % 2 == 1) {
            dft(input, output);
            return;
        }

        float[] even = new float[inSize / 2];
        float[] odd = new float[inSize / 2];

        int indxEven = 0;
        int indxOdd = 0;
        for (int i = 0; i < inSize; i++) {
            if (i % 2 == 0) {
                even[indxEven] = input[i];
                indxEven++;
            } else {
                odd[indxOdd] = input[i];
                indxOdd++;
            }
        }

        float[] evenFft = new float[inSize];
        float[] oddFft = new float[inSize];

        fft(even, evenFft);
        fft(odd, oddFft);
        for (int k = 0; k < inSize / 2; k++) {
            float theta = (float) (2 * Math.PI * k / inSize);
            float re = (float) cos(theta);
            float im = (float) -sin(theta);
            float reOdd = oddFft[2 * k + 0];
            float imOdd = oddFft[2 * k + 1];
            output[2 * k + 0] = evenFft[2 * k + 0] + re * reOdd - im * imOdd;
            output[2 * k + 1] = evenFft[2 * k + 1] + re * imOdd + im * reOdd;
            output[2 * (k + inSize / 2) + 0] = evenFft[2 * k + 0] - re * reOdd + im * imOdd;
            output[2 * (k + inSize / 2) + 1] = evenFft[2 * k + 1] - re * imOdd - im * reOdd;
        }
    }

    private static class Mel {
        int nLen = 0;
        int nMel = 0;
        float[] data;
    }


    public static Bitmap calcMelSpectrogramBMP(Context context, float[] data) {

       /*  Layout of data is:
        [mel_0_frame_0, mel_0_frame_1, ..., mel_0_frame_N,
         mel_1_frame_0, mel_1_frame_1, ..., mel_1_frame_N,
         ...
         mel_M_frame_0, mel_M_frame_1, ..., mel_M_frame_N]
        */

        int startMel = 2; //remove MELs at bottom which are always white
        int nFrames = data.length / N_MEL;
        int width = nFrames;
        int height = N_MEL - startMel;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        // Normalize data
        float min = Float.POSITIVE_INFINITY;
        float max = Float.NEGATIVE_INFINITY;

        for (float v : data) {
            if (!Float.isNaN(v) && !Float.isInfinite(v)) {
                if (v < min) min = v;
                if (v > max) max = v;
            }
        }

        float range = max - min;
        if (range == 0) range = 1;

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        int color = 0xFFFFFF;

        int baseRed = (color >> 16) & 0xFF;
        int baseGreen = (color >> 8) & 0xFF;
        int baseBlue = color & 0xFF;

        // Fill bitmap
        for (int i = 0; i < nFrames; i++) {                 // Time frame
            for (int j = startMel; j < N_MEL; j++) {        // Mel band
                int index = j * nFrames + i;
                float value = (data[index] - min) / range;

                int intensity = (int) (value * 255);
                // Scale each component by intensity
                int r = (int) ((baseRed * (long) intensity) / 255);
                int g = (int) ((baseGreen * (long) intensity) / 255);
                int b = (int) ((baseBlue * (long) intensity) / 255);

                int pixel = 0xFF000000 | (r << 16) | (g << 8) | b;

                // Flip vertically so lowest frequency is at bottom
                bitmap.setPixel(i, height - 1 - j + startMel, pixel);
            }
        }
        return bitmap;
    }

    private static float[] createMelFilterBank(int nFft, int sampleRate, int nMel) {
        int nFftBins = nFft / 2 + 1;
        float[] melFilters = new float[nMel * nFftBins];

        // Convert frequencies to Mel scale
        float minMel = 0f;
        float maxMel = 2595f * (float) Math.log10(1f + (float) sampleRate / 700f);
        float[] mels = new float[nMel + 2];
        for (int i = 0; i < nMel + 2; i++) {
            mels[i] = minMel + i * (maxMel - minMel) / (nMel + 1);
        }

        // Convert back to linear frequency
        float[] hz = new float[nMel + 2];
        for (int i = 0; i < nMel + 2; i++) {
            hz[i] = 700f * (float) (Math.pow(10, mels[i] / 2595f) - 1);
        }

        // Map to FFT bins
        int[] bins = new int[nMel + 2];
        for (int i = 0; i < nMel + 2; i++) {
            bins[i] = Math.min(nFftBins - 1, (int) Math.floor((nFftBins) * hz[i] / sampleRate));
        }

        // Create triangular filters (ensure overlap)
        for (int m = 0; m < nMel; m++) {
            int fPrev = bins[m];
            int fCurr = bins[m + 1];
            int fNext = bins[m + 2];

            for (int k = fPrev; k < fNext; k++) {
                float weight = 0f;
                if (k < fCurr) {
                    weight = (k - fPrev) / (float) (fCurr - fPrev);
                } else if (k >= fCurr && k < fNext) {
                    weight = (fNext - k) / (float) (fNext - fCurr);
                }
                melFilters[m * nFftBins + k] = weight;
            }
        }

        return melFilters;
    }
}
