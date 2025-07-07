package org.alexmagter.QuickYTD

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform

class MainActivity : ComponentActivity() {


    private lateinit var fileSaver: FileSaver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fileSaver = FileSaver(this)


        setContent {
            Navigation(fileSaver)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    Navigation(
        fileSaver = TODO()
    )
}