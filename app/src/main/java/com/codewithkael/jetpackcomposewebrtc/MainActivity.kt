package com.codewithkael.jetpackcomposewebrtc

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.PixelCopy
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.view.doOnPreDraw
import androidx.core.view.drawToBitmap
import com.codewithkael.jetpackcomposewebrtc.ui.components.ControlButtonsLayout
import com.codewithkael.jetpackcomposewebrtc.ui.components.IncomingCallComponent
import com.codewithkael.jetpackcomposewebrtc.ui.components.SurfaceViewRendererComposable
import com.codewithkael.jetpackcomposewebrtc.ui.components.WhoToCallLayout
import com.codewithkael.jetpackcomposewebrtc.ui.theme.JetpackComposeWebrtcTheme
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.webrtc.SurfaceViewRenderer
import java.util.UUID

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private var localSurfaceViewRenderer: SurfaceViewRenderer? = null
    private var remoteSurfaceViewRenderer: SurfaceViewRenderer? = null

    private var myUsername = UUID.randomUUID().toString().substring(0, 2)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            JetpackComposeWebrtcTheme {

                val requestPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    if (permissions.all { it.value }) {
                        myUsername = intent?.getStringExtra("username") ?: "1"
                        //start the process
                        mainViewModel.init(myUsername)
                    }
                }
                LaunchedEffect(key1 = Unit) {
                    requestPermissionLauncher.launch(
                        arrayOf(
                            android.Manifest.permission.CAMERA,
                            android.Manifest.permission.RECORD_AUDIO,
                        )
                    )
                }


                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column {
                        val incomingCallState =
                            mainViewModel.incomingCallerSection.collectAsState(null)
                        if (incomingCallState.value != null) {
                            IncomingCallComponent(
                                incomingCallerName = incomingCallState.value?.name,
                                modifier = Modifier.weight(1f),
                                onAcceptPressed = mainViewModel::acceptCall,
                                onRejectPressed = mainViewModel::rejectCall
                            )
                        } else {
                            WhoToCallLayout(
                                modifier = Modifier.weight(2f),
                                onStartCallButtonClicked = mainViewModel::startCall
                            )
                        }


                        SurfaceViewRendererComposable(
                            modifier = Modifier.weight(4f),
                            onSurfaceReady = { remoteSurface ->
                                remoteSurfaceViewRenderer = remoteSurface
                                mainViewModel.setRemoteView(remoteSurface)
                            }
                        )


                        Spacer(
                            modifier = Modifier
                                .height(5.dp)
                                .background(color = Color.Gray)
                        )

                        SurfaceViewRendererComposable(
                            modifier = Modifier.weight(4f),
                            onSurfaceReady = { localSurface ->
                                localSurfaceViewRenderer = localSurface
                                mainViewModel.setLocalView(localSurface)

                                val handler = Handler(Looper.getMainLooper())
                                val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

                                val surfaceHolder = localSurface.holder
                                surfaceHolder.addCallback(object : SurfaceHolder.Callback {
                                    override fun surfaceCreated(holder: SurfaceHolder) {
                                        // Surface has been created, but size is not known yet
                                    }

                                    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {

                                        fun runTextDetection() {
                                            // Surface size has changed
                                            if (width > 0 && height > 0) {
                                                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                                                val location = IntArray(2)
                                                localSurface.getLocationInWindow(location)

                                                PixelCopy.request(localSurface, bitmap, { result ->
                                                    if (result == PixelCopy.SUCCESS) {
                                                        val image = InputImage.fromBitmap(bitmap, 0)
                                                        textRecognizer.process(image)
                                                            .addOnSuccessListener { visionText ->
                                                                val detectedText = visionText.text
                                                                Log.e("TAG", "Detected text: $detectedText")
                                                            }
                                                            .addOnFailureListener { e ->
                                                                Log.e("TextRecognition", "Error during text recognition", e)
                                                            }
                                                    } else {
                                                        Log.e("PixelCopy", "Failed to copy pixels: $result")
                                                    }
                                                }, handler)
                                            }
                                        }

                                        fun scheduleTextDetection() {
                                            runTextDetection()
                                            handler.postDelayed({ scheduleTextDetection() }, 500)
                                        }

                                        scheduleTextDetection()
                                    }

                                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                                        // Surface has been destroyed
                                    }
                                })
                            }
                        )


                        ControlButtonsLayout(
                            modifier = Modifier.weight(1f),
                            onAudioButtonClicked = mainViewModel::onAudioButtonClicked,
                            onCameraButtonClicked = mainViewModel::onCameraButtonClicked,
                            onEndCallClicked = mainViewModel::onEndCallClicked,
                            onSwitchCameraClicked = mainViewModel::onSwitchCameraClicked,
                        )
                    }
                }
            }
        }
    }

}
