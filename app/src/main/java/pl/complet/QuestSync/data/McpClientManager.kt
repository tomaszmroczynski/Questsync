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
                    // Raw network stream log for deep diagnostic
                    Log.v("McpNetworkRaw", message)
                }
            }
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 180000 
            connectTimeoutMillis = 45000
            socketTimeoutMillis = 180000
        }
    }

    suspend fun getInsights(healthData: String): String {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("McpClientManager", "DECODER: Syncing with Neural Node: $serverUrl")
                
                val transport = SseClientTransport(httpClient, serverUrl)
                val implementation = Implementation(name = "QuestSyncAndroid", version = "1.0.0")
                val client = Client(implementation)
                
                Log.d("McpClientManager", "DECODER: Waiting for stream handshake...")
                
                // Allow enough time for padding and initial event parsing
                withTimeout(90000) {
                    client.connect(transport)
                }
                
                Log.d("McpClientManager", "DECODER: Link stabilized. Executing 'generate_health_summary'...")
                
                val result = withTimeout(120000) {
                    client.callTool(
                        name = "generate_health_summary",
                        arguments = mapOf("data" to JsonPrimitive(healthData))
                    )
                }
                
                Log.d("McpClientManager", "DECODER: Response decrypted.")
                
                val contentList = result.content
                if (contentList.isNotEmpty()) {
                    val firstContent = contentList[0]
                    if (firstContent is TextContent) {
                        firstContent.text
                    } else {
                        "DECODER ALERT: Unexpected signal format (${firstContent::class.simpleName})"
                    }
                } else {
                    "DECODER ALERT: Neural node returned empty payload."
                }
            } catch (e: CancellationException) {
                Log.e("McpClientManager", "DECODER: Link timeout or severed", e)
                "Connection Timeout: The AI server is struggling with latency. Verify 'WebSocket' custom headers on Synology."
            } catch (e: Exception) {
                Log.e("McpClientManager", "DECODER: Critical interface failure", e)
                val msg = e.message ?: "Unknown interference"
                if (msg.contains("closed", ignoreCase = true)) {
                    "Interface Closed: The SSE stream died before handshake. This usually means Cloudflare buffered the 'endpoint' signal."
                } else {
                    "Decoder Error: $msg"
                }
            }
        }
    }
}
