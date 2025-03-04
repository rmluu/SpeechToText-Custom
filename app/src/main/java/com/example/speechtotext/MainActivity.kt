package com.example.speechtotext

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SpeechToTextScreen()
        }
    }
}

@Composable
fun SpeechToTextScreen() {
    val context = LocalContext.current
    var speechRecognizer: SpeechRecognizer? = remember { null }
    var speechText by remember { mutableStateOf("Tap the button and speak") }
    var isListening by remember { mutableStateOf(false) }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        val permissionStatus = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = speechText, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = {
                if (!isListening) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                    startSpeechRecognition(context, speechRecognizer!!) { result ->
                        speechText = result
                        isListening = false
                    }
                    isListening = true
                }
            }
        ) {
            Text(text = if (isListening) "Listening..." else "Start Speaking")
        }
    }
}

fun startSpeechRecognition(context: android.content.Context, speechRecognizer: SpeechRecognizer, onResult: (String) -> Unit) {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")

    speechRecognizer.setRecognitionListener(object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Toast.makeText(context, "Listening...", Toast.LENGTH_SHORT).show()
        }

        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {
            Toast.makeText(context, "Processing...", Toast.LENGTH_SHORT).show()
        }

        override fun onError(error: Int) {
            onResult("Error occurred, please try again.")
        }

        override fun onResults(results: Bundle?) {
            val data = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            onResult(data?.get(0) ?: "No speech detected")
        }

        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    })

    speechRecognizer.startListening(intent)
}

// File > Settings > Tools > Emulator > Deselect "Launch in the Running Devices tool window"
// Restart emulator
// Click the ellipses (...) in the emulator control bar > Microphone > Enable 'Virtual mic uses host audio input'