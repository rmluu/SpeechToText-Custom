/**
 * Richard Luu [387861]
 * ADEV3007 (261251)
 * Co-op 1 - Speech-to-Text (Custom)
 * This program implements a custom Speech-to-Text system using Android's SpeechRecognizer API.
 * It directly processes audio input, listens for speech, and transcribes it in real-time.
 * 03/04/2025
 * Resource: https://youtu.be/jyXf1WS-wI8?si=L6Irvh8Nwa45Y-Wk
 **/

package com.example.speechtotext

import android.Manifest
import android.Manifest.permission.RECORD_AUDIO
import android.content.Context
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.checkSelfPermission
import java.util.Locale

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text(text = "Custom Speech-to-Text") }
                    )
                }
            ) { paddingValues ->
                SpeechToTextScreen(modifier = Modifier.padding(paddingValues))
            }
        }
    }
}

@Composable
fun SpeechToTextScreen(modifier: Modifier = Modifier) {
    // Initialize current context (needed for speech recognizer and permission requests)
    val context = LocalContext.current
    // Initialize state var for result
    var speechText by remember { mutableStateOf("Tap the button and speak") }
    // Initialize state var for speech recognition progress
    var isListening by remember { mutableStateOf(false) }

    // Permission request launcher requests microphone access if not already granted
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            // Show toast message if permission denied
            Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    // Check for microphone permission on initial launch
    LaunchedEffect(Unit) {
        // Check if microphone permission is already granted
        if (checkSelfPermission(context, RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // Not granted, launch permission request
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // UI
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Displays transcribed speech text
        Text(
            text = speechText,
            fontSize = 20.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                if (!isListening) {
                    isListening = true

                    // Start speech recognition process
                    val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                    startSpeechRecognition(context, speechRecognizer, { result ->
                        speechText = result
                    }, {
                        // Stop listening once recognition completes or error occurs
                        isListening = false
                    })
                }
            },
            modifier = Modifier.clip(RoundedCornerShape(10.dp))
        ) {
            // Updates button text to indicate if app is listening
            Text(
                text = if (isListening) "Listening..." else "Start Speaking",
                fontSize = 20.sp,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
    }
}

/**
 * Initializes the speech recognition process to convert spoken words into text.
 *
 * @param context The context of the application, required to create the SpeechRecognizer.
 * @param speechRecognizer The SpeechRecognizer instance used to process speech input.
 * @param onResult A callback function that is called with the recognized speech result when available.
 */
private fun startSpeechRecognition(
    context: Context,
    speechRecognizer: SpeechRecognizer,
    onResult: (String) -> Unit,
    onListeningEnd: () -> Unit
) {
    // Check if speech recognition is available on the device
    if (!SpeechRecognizer.isRecognitionAvailable(context)) {
        Toast.makeText(context, "Speech recognition is not available on this device", Toast.LENGTH_SHORT).show()
        return
    }

    // Create an intent to initiate speech recognition
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        // Specify language model to use for recognition (free-form speech currently used)
        // Use LANGUAGE_MODEL_FREE_FORM for general, conversational speech
        // Use LANGUAGE_MODEL_WEB_SEARCH for search queries and commands (e.g. "Open YouTube", "Find restaurants near me.")
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        // Set default language to device's current local
        // Can specify a variety of locales (e.g. Locale("en", "US), Locale("es", "ES"), Locale("fr", "FR"))
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
    }

    // Set up listener to handle results of speech recognition
    speechRecognizer.setRecognitionListener(object : RecognitionListener {
        // Called when speech recognizer is ready to start listening
        override fun onReadyForSpeech(params: Bundle?) {}

        // Called when speech input begins (user starts speaking)
        override fun onBeginningOfSpeech() {}

        // Called when there is a change in the sound level of the user's speech
        override fun onRmsChanged(rmsdB: Float) {}

        // Called when speech data is being processed
        override fun onBufferReceived(buffer: ByteArray?) {}

        // Called when speech input ends (user stops speaking)
        override fun onEndOfSpeech() {
        }

        // Called when an error occurs during speech recognition
        override fun onError(error: Int) {
            onResult("Error occurred: $error")
            onListeningEnd()
        }

        // Called when speech recognition results are ready
        override fun onResults(results: Bundle?) {
            // Extract recognized speech results
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            // If results are found, pass first match to onResult callback
            if (matches != null && matches.isNotEmpty()) {
                onResult(matches[0])
            }
            onListeningEnd()
        }

        // Called when partial speech recognition results are available
        override fun onPartialResults(partialResults: Bundle?) {}

        // Called for additional events during speech recognition
        override fun onEvent(eventType: Int, params: Bundle?) {}

        /* The unused override methods can be used for things like:
         *    Tracking the start or end of speech (onBeginningOfSpeech, onEndOfSpeech)
         *    Processing intermediate results (onPartialResults) - use for real-time transcription
         *    Monitoring sound level (onRmsChanged)
         *    Handling extra events (onEvent) e.g. RecognitionStarted, RecognitionEnded, NoSpeechDetected
         */
    })

    // Start speech recognition process using specified intent
    speechRecognizer.startListening(intent)
}

/* Workaround for dysfunctional emulator extended controls window (to allow mic input from your pc)
 *    File > Settings > Tools > Emulator > Deselect "Launch in the Running Devices tool window"
 *    Restart emulator
 *    Click the ellipses (...) in the emulator control bar > Microphone > Enable 'Virtual mic uses host audio input'
 */

/* Permissions Set-up
 * AndroidManifest.xml
 *    add <uses-permission android:name="android.permission.RECORD_AUDIO"/>
 *    add <service
 *          android:name="android.speech.RecognitionService"
 *          android:permission="android.permission.BIND_SPEECH_RECOGNITION_SERVICE"/> - add the service inside <application> tag
 */