package org.alexmagter.QuickYTD

import android.content.Intent
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

        val receivedIntent: Intent? = intent

        if (receivedIntent != null && receivedIntent.action == Intent.ACTION_SEND) {
            handleSharedText(receivedIntent)
        }

        fileSaver = FileSaver(this)


        setContent {
            Navigation(fileSaver)
        }
    }

    private fun handleSharedText(intent: Intent) {
        val receivedType: String? = intent.type

        if (receivedType == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)

            if (sharedText != null) {
                // Aquí procesas el texto recibido
                // Por ejemplo, lo muestras en un TextView
                // val textView = findViewById<TextView>(R.id.mi_texto_compartido)
                // textView.text = sharedText
            }
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