import os
import asyncio
import json
from fastapi import FastAPI, Request, Response
from fastapi.responses import RedirectResponse, StreamingResponse, HTMLResponse
from fastapi.middleware.cors import CORSMiddleware
from typing import Optional, AsyncGenerator
from dotenv import load_dotenv

from token_store import TokenStore
from oura import OuraOAuth
from withings import WithingsOAuth
from mcp_server import MCPServer

load_dotenv()

app = FastAPI(title="RipperMCP Health Server")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

token_store = TokenStore()
oura_oauth = OuraOAuth(token_store)
withings_oauth = WithingsOAuth(token_store)
mcp_server = MCPServer(token_store)

@app.head("/auth/withings/callback")
async def callback_withings_head():
    return Response(status_code=200)

@app.head("/auth/oura/callback")
async def callback_oura_head():
    return Response(status_code=200)

@app.get("/auth/oura")
async def auth_oura():
    url = oura_oauth.get_authorization_url()
    return RedirectResponse(url)

@app.get("/auth/oura/callback")
async def callback_oura(code: str, state: Optional[str] = None):
    await oura_oauth.exchange_code(code)
    return HTMLResponse("<h2>✅ Oura połączona! Możesz zamknąć to okno.</h2>")

@app.get("/auth/withings")
async def auth_withings():
    url = withings_oauth.get_authorization_url()
    return RedirectResponse(url)

@app.get("/auth/withings/callback")
async def callback_withings(code: str, state: Optional[str] = None):
    await withings_oauth.exchange_code(code)
    return HTMLResponse("<h2>✅ Withings połączona! Możesz zamknąć to okno.</h2>")

@app.post("/sync/health-connect")
async def sync_health_connect(request: Request):
    try:
        data = await request.json()
        source = data.get("source", "samsung_health")
        data_type = data.get("type", "generic")
        payload = data.get("data", data)

        print(f"📥 [SYNC] Source: {source}, Type: {data_type}")

        # LOG RAW PAYLOAD IF SOURCE IS UNKNOWN
        if source == "quest" and (payload.get("packageName") == "Unknown" or payload.get("caloriesBurned") == 0):
            print(f"🔍 [DEBUG RAW] Received: {json.dumps(data, indent=2)}")

        token_store.save_health_data(source, payload, data_type)

        if source == "quest":
            calories = payload.get("caloriesBurned", 0)
            pkg = payload.get("packageName", "unknown")
            print(f"⚡ [QUEST SNIFFER] Saved: {payload.get('activityName')} | pkg: {pkg} | {calories} kcal")

        return {"status": "success"}
    except Exception as e:
        print(f"❌ [SYNC ERROR] {e}")
        return Response(content=str(e), status_code=400)

@app.get("/status")
async def status():
    return {
        "oura": token_store.has_token("oura"),
        "withings": token_store.has_token("withings"),
        "server": "running"
    }

@app.get("/mcp")
async def mcp_sse(request: Request):
    print(f"📡 New SSE connection request from {request.client.host}")
    async def event_stream() -> AsyncGenerator[str, None]:
        # 1. SEND PADDING (8KB) to force buffer flush on Cloudflare/Nginx
        padding = ":" + " " * 8192 + "\n\n"
        yield padding

        # 2. SEND KEEP-ALIVE COMMENT
        yield ": keep-alive\n\n"

        # 3. SEND ENDPOINT EVENT
        # Explicit absolute URL with hyphenated domain as primary fallback
        host = request.headers.get("host", "ripper-mcp.complet-ai.no")
        scheme = request.headers.get("x-forwarded-proto", "https")
        endpoint_url = f"{scheme}://{host}/mcp"

        # Override to strict target domain to avoid proxy header confusion
        if "complet-ai.no" in host:
             endpoint_url = "https://ripper-mcp.complet-ai.no/mcp"

        print(f"🔗 Broadcasting endpoint: {endpoint_url}")
        yield f"event: endpoint\ndata: {endpoint_url}\n\n"

        # 4. SEND INITIAL CAPABILITIES
        capabilities = mcp_server.get_capabilities()
        print(f"📤 Initializing handshake: {capabilities}")
        yield mcp_server.format_sse(capabilities)

        # 5. ENTER HEARTBEAT & MESSAGE LOOP
        client_gen = mcp_server.handle_client(request)
        try:
            while True:
                try:
                    # Wait for a message with a timeout to send heartbeats
                    message = await asyncio.wait_for(anext(client_gen), timeout=15.0)
                    yield mcp_server.format_sse(message)
                except asyncio.TimeoutError:
                    # Send a keep-alive comment every 15 seconds to prevent proxy timeout
                    yield ": heartbeat\n\n"
                except StopAsyncIteration:
                    break
        except Exception as e:
            print(f"⚠️ SSE Stream error: {e}")

    return StreamingResponse(
        event_stream(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache, no-transform",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",
            "Content-Type": "text/event-stream",
            "Transfer-Encoding": "chunked"
        }
    )

@app.post("/mcp")
async def mcp_post(request: Request):
    body = await request.json()
    response = await mcp_server.handle_request(body)
    return response
