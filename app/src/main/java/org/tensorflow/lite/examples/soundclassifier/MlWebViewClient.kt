package org.tensorflow.lite.examples.soundclassifier

import android.graphics.Bitmap
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
import java.io.ByteArrayInputStream

//Webview Client to load images from Macaulay Library
open class MlWebViewClient(activity: AppCompatActivity) : WebViewClient() {
    var mError = false
    var mActivity = activity

    override fun onPageFinished(view: WebView, url: String?) {

        super.onPageFinished(view, url)
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
        return false  //allow redirects for new bot detection
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        mError = false
    }

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {

        if (request.url.toString()
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

}