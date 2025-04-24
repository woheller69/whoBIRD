package com.zell_mbc.birdlog

import android.R.attr.onClick
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContentProviderCompat.requireContext
import com.zell_mbc.birdlog.Downloader.checkModels
import com.zell_mbc.birdlog.Downloader.downloadModels


class LoadModels : AppCompatActivity() {

    lateinit var app: Activity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        app = this
        //var x = LocalContext.current

        enableEdgeToEdge() //This will include/color the top Android info bar

        if (checkModels(app)) startMainActivity()
        else setContent { BirdlogTheme { ShowContent() } }
    }

    @Composable
    fun ShowContent() {
        Column(modifier = Modifier.safeDrawingPadding().padding(start = 16.dp, top = 8.dp))
        {
            val checkedState = remember { mutableStateOf(false) }

            //Row {
            Surface(
                //modifier = Modifier.size(50.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0f)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.icon_large),
                    contentDescription = null
                )
            }
            Column(modifier = Modifier.padding(start = 8.dp, top = 6.dp)) {
                Text(
                    stringResource(id = R.string.download_model),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Text(
                    stringResource(id = R.string.download_model_text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = checkedState.value,
                    onCheckedChange = {
                        checkedState.value = it
                    }
                )
                Text(
                    stringResource(id = R.string.thirtyTwoBit),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = {
                //downloadModels(app, checkedState.value)
            }) {
                Icon(
                    painterResource(id = R.drawable.baseline_cloud_download_24),
                    contentDescription = "Download",
                    //modifier = Modifier.size(50.dp)
                )
            }
        }
    }

    fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}