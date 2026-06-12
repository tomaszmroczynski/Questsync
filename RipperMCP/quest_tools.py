import json

class QuestTools:
    TOOL_DEFINITIONS = [
        {
            "name": "quest_get_activity_history",
            "description": "Pobiera historię aktywności z Meta Quest zapisanych w bazie.",
            "inputSchema": {
                "type": "object",
                "properties": {}
            }
        }
    ]

    def __init__(self, store):
        self.store = store

    async def call_tool(self, name: str, arguments: dict) -> dict:
        if name == "quest_get_activity_history":
            return self.get_activity_history()
        return {"error": "Tool not found"}

    def get_activity_history(self) -> dict:
        # Przeszukujemy bazę pod kątem źródła "quest"
        # Szukamy zarówno "quest" jak i potencjalnych starszych wpisów "quest_sniffer"
        history = self.store.get_health_history("quest")
        if not history:
            # Próba ratunkowa - sprawdzenie innej nazwy źródła
            history = self.store.get_health_history("quest_sniffer")

        if not history:
            return {
                "message": "Brak danych z Meta Quest w bazie SQLite.",
                "hint": "Upewnij się, że w aplikacji mobilnej QuestSync uruchomiłeś 'Sniffer' (ikonka Play) i widzisz logi 'Server Response: 200 OK'."
            }
        return {"activities": history}
