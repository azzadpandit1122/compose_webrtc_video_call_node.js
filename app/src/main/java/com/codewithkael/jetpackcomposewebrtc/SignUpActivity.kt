package com.codewithkael.jetpackcomposewebrtc

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.codewithkael.jetpackcomposewebrtc.ui.theme.JetpackComposeWebrtcTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignUpActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JetpackComposeWebrtcTheme {
                extracted()
            }
        }
    }
    @Preview
    @Composable
    private fun extracted() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        )
        {
            var username by remember { mutableStateOf("") }

            TextField(
                value = username,
                onValueChange = { username = it },
                label = {
                    Text("Username",)
                }
            )

            Button(modifier = Modifier.padding(10.dp),onClick = {
                if (!username.isEmpty()) {
                    val intent = Intent(this@SignUpActivity, MainActivity::class.java).apply {
                        putExtra("username", username)
                    }
                    startActivity(intent)
                }
            }) {
                Text(text = "continue ")
            }
        }
    }

}
