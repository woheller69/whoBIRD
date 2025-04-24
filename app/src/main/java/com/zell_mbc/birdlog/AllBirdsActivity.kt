package com.zell_mbc.birdlog

import android.app.Activity
import android.os.Bundle
import android.widget.EditText
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.MutableState


class AllBirdsActivity: AppCompatActivity() {

    lateinit var app: Activity
    lateinit var viewModel: ViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        app = this
        //var x = LocalContext.current

        enableEdgeToEdge() //This will include/color the top Android info bar
        val viewModelProvider = ViewModelProvider(this)
        viewModel = viewModelProvider[ViewModel::class.java]

        setContent { BirdlogTheme { ShowContent() } }
    }

    @Composable
    fun ShowContent() {
        var searchValue by remember { mutableStateOf("") }
        var selection: MutableState<Bird?> = remember { mutableStateOf( null ) }
        var listState: LazyListState = rememberLazyListState()
        var assetId  by remember { mutableStateOf("") }

        Column(modifier = Modifier.safeDrawingPadding().padding(start = 8.dp, end = 8.dp,top = 8.dp)) {
            val checkedState = remember { mutableStateOf(false) }

            //Row {
            Surface(
                //modifier = Modifier.size(50.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0f)
            ) {
                if (assetId.isEmpty()) Image(painter = painterResource(id = R.drawable.icon_large), contentDescription = "Logo")
                else ShowBirdPicture(assetId)
            }
            OutlinedTextField(value = searchValue,
                onValueChange = { searchValue = it },
                singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = getString(R.string.search)) },
                label = { Text(text = getString(R.string.search))}, modifier = Modifier.fillMaxWidth()
            )

            //val state = remember { viewModel.allBirds }
            Text("")
            LazyColumn(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp)) {
                items(viewModel.allBirds) { item ->
                    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.secondaryContainer) // Lines starting from the top
                    Row(modifier = Modifier.clickable { selection.value = item }) { //modifier = Modifier.height(IntrinsicSize.Min).fillMaxWidth().width(IntrinsicSize.Max)) {
                        ShowName(item)
                    }
                }
            }

            /*Column(modifier = Modifier.padding(start = 8.dp, top = 6.dp)) {

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = checkedState.value,
                        onCheckedChange = {
                            checkedState.value = it
                        })
                    Text(
                        stringResource(id = R.string.thirtyTwoBit),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }*/
        }
    }

    @Composable
    fun ShowName(item: Bird) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            //Text(item.lineNo.toString())
            val name = viewModel.birdNames[item.id]
            Text(name.commonName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            Text(name.latinName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontStyle = FontStyle.Italic)
        }
    }

    @Composable
    fun ShowBirdPicture(assetId: String) {

    }
}