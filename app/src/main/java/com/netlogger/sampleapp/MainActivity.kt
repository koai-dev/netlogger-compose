package com.netlogger.sampleapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.netlogger.lib.Netlogger
import com.netlogger.lib.presentation.util.LogUtil
import com.netlogger.sampleapp.ui.theme.NetloggerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class MainActivity : ComponentActivity() {

    // OkHttpClient with Netlogger interceptor attached
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(Netlogger.getInterceptor())
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NetloggerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(32.dp)
                    ) {
                        Greeting(
                            name = "Demo Netlogger",
                            modifier = Modifier.padding(innerPadding)
                        )
                        Button(
                            onClick = { fetchIpInfo() },
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Greeting(
                                name = "Call API",
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }

        // Log a general message
        LogUtil.log("MainActivity", "Activity created - fetching IP info...")

        // Trigger the sample API call
        fetchIpInfo()
    }

    /**
     * Sample API call: GET http://ip-api.com/json/
     * This call is intercepted by Netlogger and visible in the Netlogger UI.
     *
     * Equivalent curl:
     *   curl --location 'http://ip-api.com/json/'
     */
    private fun fetchIpInfo() {
        lifecycleScope.launch {
            try {
                val request = Request.Builder()
                    .url("http://ip-api.com/json/")
                    .get()
                    .build()

                val (statusCode, body) = withContext(Dispatchers.IO) {
                    val resp = okHttpClient.newCall(request).execute()
                    resp.use { Pair(it.code, it.body.string()) }
                }

                LogUtil.info("IpApi", "Response [$statusCode]: $body")
            } catch (e: Exception) {
                LogUtil.error("IpApi", "Request failed: ${e.message}")
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    NetloggerTheme {
        Greeting("Android")
    }
}