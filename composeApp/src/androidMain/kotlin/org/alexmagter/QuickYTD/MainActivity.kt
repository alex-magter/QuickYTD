package org.alexmagter.QuickYTD

import android.content.Intent
import android.os.Bundle
import android.util.Log
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

        val receivedIntent: Intent? = intent

        val sharedText: String? = if (receivedIntent != null && receivedIntent.action == Intent.ACTION_SEND) {
            intent.getStringExtra(Intent.EXTRA_TEXT)
        } else null



        fileSaver = FileSaver(this)


        setContent {
            Navigation(fileSaver, sharedText)
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