import json

class SamsungTools:
    TOOL_DEFINITIONS = [
        {
            "name": "samsung_get_health_summary",
            "description": "Pobiera najnowsze dane z Samsung Health (kroki, tętno, ciśnienie, sen) przesłane z telefonu.",
            "inputSchema": {
                "type": "object",
                "properties": {}
            }
        }
    ]

    def __init__(self, store):
        self.store = store

    async def call_tool(self, name: str, arguments: dict) -> dict:
        if name == "samsung_get_health_summary":
            return self.get_health_summary()
        return {"error": "Tool not found"}

    def get_health_summary(self) -> dict:
        # Pobieramy najnowsze dane z bazy SQLite
        history = self.store.get_health_history("samsung_health")
        if not history:
            return {"message": "Brak danych z Samsung Health. Uruchom synchronizację w aplikacji QuestSync na telefonie."}

        # Zwracamy najnowszy wpis
        return history[0]
