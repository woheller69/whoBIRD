package org.tensorflow.lite.examples.soundclassifier

import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

//Webview Client to load images from Macaulay Library
open class MlWebViewClient(activity: AppCompatActivity) : WebViewClient() {
    var mError = false
    var mActivity = activity
    var okHttp = OkHttpClient.Builder()
        .cache(
            Cache(
            directory = File(activity.application.cacheDir, "http_cache"),
            maxSize = 100L * 1024L * 1024L // 100 MiB
        )
        ).build()

    var downloadWidth = 320
    var targetWidth = 1200
    override fun onPageFinished(view: WebView, url: String?) {

        super.onPageFinished(view, url)
        // Inject JavaScript to scale the image to fit width
        // Full width is expected, but we downloaded smaller image so save data
        // Resize to expected size
        val factor = targetWidth.toFloat() / downloadWidth.toFloat()

        val javascript = "javascript:(function() {" +
                "var image = document.querySelector('.photo');" +
                "if (image) {" +
                "  var originalWidth = image.naturalWidth || image.width;" +
                "  var originalHeight = image.naturalHeight || image.height;" +
                "  image.style.width = (originalWidth * " + factor + ") + 'px';" +
                "  image.style.height = (originalHeight * " + factor + ") + 'px';" +
                "}" +
                "})()"
        view.loadUrl(javascript)
        if (view.visibility == View.INVISIBLE && !mError){
            Handler(Looper.getMainLooper()).postDelayed({
                view.setVisibility(View.VISIBLE)
            }, 50)
        }
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError
    ) {
        Toast.makeText(mActivity, mActivity.resources.getString(R.string.error_download), Toast.LENGTH_SHORT).show()
        mError = true
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        return true;
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        mError = false
    }

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {

        if (request.url.toString().startsWith("https://cdn.download.ams.birds.cornell.edu/api")) {
            val modifiedUrl = modifyUrl(request.url)
            targetWidth = Integer.parseInt(request.url.toString().split("/").last())
            Log.d("whoBird", "Target:" + targetWidth)
            Log.d("whoBird", "Load smaller image:" + modifiedUrl)
            val okHttpRequest: Request =
                Request.Builder()
                    .cacheControl(CacheControl.Builder().maxStale(Int.MAX_VALUE, TimeUnit.DAYS).build())
                    .url(java.lang.String.valueOf(modifiedUrl))
                    .build()
            try {
                val response: Response = okHttp.newCall(okHttpRequest).execute()
                return WebResourceResponse("", "", response.body!!.byteStream())
            } catch (e: IOException) {
                e.printStackTrace()
            }

            //this should never be reached
            return WebResourceResponse(
                "text/plain",
                "UTF-8",
                ByteArrayInputStream("".toByteArray())
            )

        } else if (request.url.toString()
                .contains("www.googletagmanager.com") || request.url.toString().endsWith(".js") || request.url.toString().contains("favicon")
        ) {
            Log.d("whoBird", "Blocked:" + request.url.toString())
            return WebResourceResponse(
                "text/plain",
                "UTF-8",
                ByteArrayInputStream("".toByteArray())
            )
        } else {
            Log.d("whoBird", "Allowed:" + request.url.toString())
            return null
        }

    }

    private fun modifyUrl(originalUrl: Uri): Uri {
        val urlParts = originalUrl.toString().split("/").toMutableList()
        if (urlParts.isNotEmpty()) {
            urlParts[urlParts.size - 1] = downloadWidth.toString()  //load image with height=160 instead of full size image
        }
        return Uri.parse(urlParts.joinToString("/"))
    }
}