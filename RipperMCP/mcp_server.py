import json
from typing import AsyncGenerator
from fastapi import Request
from token_store import TokenStore
from oura import OuraOAuth
from withings import WithingsOAuth
from oura_tools import OuraTools
from withings_tools import WithingsTools
from samsung_tools import SamsungTools
from quest_tools import QuestTools
from history_tools import HistoryTools


class MCPServer:
    """
    Implementacja protokołu MCP (Model Context Protocol).
    
    MCP używa JSON-RPC 2.0 — standard dla komunikacji klient-serwer.
    Każda wiadomość ma: jsonrpc, method, params, id.
    Serwer odpowiada: jsonrpc, result lub error, id.
    
    Claude wysyła POST z zapytaniem JSON-RPC,
    serwer odpowiada z wynikiem narzędzia.
    """

    def __init__(self, store: TokenStore):
        self.store = store
        oura_oauth = OuraOAuth(store)
        withings_oauth = WithingsOAuth(store)
        self.oura = OuraTools(oura_oauth, store)
        self.withings = WithingsTools(withings_oauth, store)
        self.samsung = SamsungTools(store)
        self.quest = QuestTools(store)
        self.history = HistoryTools(store)

        self.all_tools = (
            OuraTools.TOOL_DEFINITIONS +
            WithingsTools.TOOL_DEFINITIONS +
            SamsungTools.TOOL_DEFINITIONS +
            QuestTools.TOOL_DEFINITIONS +
            HistoryTools.TOOL_DEFINITIONS
        )

    def get_capabilities(self) -> dict:
        """
        Pierwsza wiadomość wysyłana do Claude przy połączeniu.
        Informuje go jakie narzędzia są dostępne i jak je wywołać.
        """
        return {
            "jsonrpc": "2.0",
            "method": "notifications/initialized",
            "params": {
                "protocolVersion": "2024-11-05",
                "capabilities": {
                    "tools": {"listChanged": True},
                    "logging": {}
                },
                "serverInfo": {"name": "RipperMCP", "version": "2.3.0"}
            }
        }

    def format_sse(self, data: dict) -> str:
        """Formatuje dane jako SSE event — wymagany format: 'data: {...}\\n\\n'"""
        return f"data: {json.dumps(data)}\n\n"

    async def handle_client(self, request: Request) -> AsyncGenerator[dict, None]:
        """Generator — przetwarza wiadomości od Claude przez SSE"""
        # W trybie SSE Claude wysyła wiadomości przez osobne POST /mcp
        return
        yield

    async def handle_request(self, body: dict) -> dict:
        """
        Główny handler JSON-RPC.
        Dispatching na podstawie method.
        """
        method = body.get("method")
        params = body.get("params", {})
        req_id = body.get("id")

        try:
            if method == "initialize":
                result = self._handle_initialize(params)

            elif method == "tools/list":
                result = {"tools": self.all_tools}

            elif method == "tools/call":
                result = await self._handle_tool_call(params)

            else:
                return self._error(req_id, -32601, f"Nieznana metoda: {method}")

            return {"jsonrpc": "2.0", "id": req_id, "result": result}

        except Exception as e:
            return self._error(req_id, -32000, str(e))

    def _handle_initialize(self, params: dict) -> dict:
        return {
            "protocolVersion": "2024-11-05",
            "capabilities": {
                "tools": {"listChanged": True},
                "logging": {}
            },
            "serverInfo": {"name": "RipperMCP", "version": "2.3.0"}
        }

    async def _handle_tool_call(self, params: dict) -> dict:
        """
        Wywołuje narzędzie i zwraca czyste dane JSON.
        Claude używa skill.md do interpretacji tych danych.
        """
        tool_name = params.get("name")
        arguments = params.get("arguments", {})

        if tool_name.startswith("oura_"):
            data = await self.oura.call_tool(tool_name, arguments)
        elif tool_name.startswith("withings_"):
            data = await self.withings.call_tool(tool_name, arguments)
        elif tool_name.startswith("samsung_"):
            data = await self.samsung.call_tool(tool_name, arguments)
        elif tool_name.startswith("quest_"):
            data = await self.quest.call_tool(tool_name, arguments)
        elif tool_name.startswith("history_"):
            data = await self.history.call_tool(tool_name, arguments)
        else:
            raise ValueError(f"Nieznane narzędzie: {tool_name}")

        return {
            "content": [
                {"type": "text", "text": json.dumps(data, ensure_ascii=False, indent=2)}
            ]
        }

    def _error(self, req_id, code: int, message: str) -> dict:
        return {
            "jsonrpc": "2.0",
            "id": req_id,
            "error": {"code": code, "message": message}
        }
