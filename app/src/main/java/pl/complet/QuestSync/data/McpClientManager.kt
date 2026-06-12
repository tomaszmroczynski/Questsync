package pl.complet.QuestSync.data

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.sse.*
import io.ktor.serialization.kotlinx.json.*
import io.modelcontextprotocol.kotlin.sdk.client.*
import io.modelcontextprotocol.kotlin.sdk.types.*
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonPrimitive
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class McpClientManager(private val serverUrl: String) {

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
        install(SSE)
        install(Logging) {
            level = LogLevel.ALL
            logger = object : Logger {
                override fun log(message: String) {
                    // Log to both system and a dedicated tag for raw inspection
                    Log.d("McpClientManager", "NETWORK: $message")
                }
            }
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 180000 // 3 minutes total
            connectTimeoutMillis = 45000
            socketTimeoutMillis = 180000
        }
    }

    suspend fun getInsights(healthData: String): String {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("McpClientManager", "Starting insight generation. Target: $serverUrl")
                
                val transport = SseClientTransport(httpClient, serverUrl)
                val implementation = Implementation(name = "QuestSyncAndroid", version = "1.0.0")
                val client = Client(implementation)
                
                Log.d("McpClientManager", "Connecting to SSE stream...")
                
                // Extremely long timeout for the handshake. 
                // If it fails exactly at 60s, it's the client side timing out.
                withTimeout(90000) {
                    client.connect(transport)
                }
                
                Log.d("McpClientManager", "Handshake successful! Sending analysis request...")
                
                val result = withTimeout(120000) {
                    client.callTool(
                        name = "generate_health_summary",
                        arguments = mapOf("data" to JsonPrimitive(healthData))
                    )
                }
                
                Log.d("McpClientManager", "Response received from AI.")
                
                val contentList = result.content
                if (contentList.isNotEmpty()) {
                    val firstContent = contentList[0]
                    if (firstContent is TextContent) {
                        firstContent.text
                    } else {
                        "Received non-text response: ${firstContent::class.simpleName}"
                    }
                } else {
                    "AI Summary is empty."
                }
            } catch (e: CancellationException) {
                Log.e("McpClientManager", "Job cancelled. State: ${e.message}", e)
                "Timeout Error: The AI server is taking too long to respond. This usually happens if Cloudflare buffers the stream. Please ensure padding fix is applied on the server."
            } catch (e: Exception) {
                Log.e("McpClientManager", "Connection error", e)
                val msg = e.message ?: "Unknown error"
                if (msg.contains("closed", ignoreCase = true)) {
                    "Stream Closed: The connection was dropped by the server or proxy. Check your Synology Reverse Proxy WebSocket headers."
                } else {
                    "System Error: $msg"
                }
            }
        }
    }
}
