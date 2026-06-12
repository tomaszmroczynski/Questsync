import json

class HistoryTools:
    TOOL_DEFINITIONS = [
        {
            "name": "history_get_all_metrics",
            "description": "Pobiera historyczne dane ze wszystkich źródeł (Quest, Oura, Withings, Samsung) zapisane w lokalnej bazie SQLite.",
            "inputSchema": {
                "type": "object",
                "properties": {
                    "source": {"type": "string", "description": "Źródło danych (np. 'quest', 'samsung_health', 'oura', 'withings')"},
                    "data_type": {"type": "string", "description": "Typ danych (opcjonalnie)"}
                }
            }
        }
    ]

    def __init__(self, store):
        self.store = store

    async def call_tool(self, name: str, arguments: dict) -> dict:
        if name == "history_get_all_metrics":
            source = arguments.get("source")
            data_type = arguments.get("data_type")
            return self.get_history(source, data_type)
        return {"error": "Tool not found"}

    def get_history(self, source: str = None, data_type: str = None) -> dict:
        if not source:
            return {"error": "Musisz podać źródło danych (source: 'quest', 'oura', 'withings', 'samsung_health')."}

        # Jeśli szukamy quest, sprawdzamy też aliasy
        history = self.store.get_health_history(source, data_type)
        if not history and source == "quest":
            history = self.store.get_health_history("quest_sniffer", data_type)

        return {"source": source, "history": history}
