import httpx
from datetime import datetime, timedelta
from oura import OuraOAuth


class OuraTools:
    """
    Narzędzia MCP do pobierania danych z Oura API.
    Każda metoda = jedno narzędzie widoczne dla Claude.
    """

    BASE_URL = "https://api.ouraring.com/v2/usercollection"

    def __init__(self, oauth: OuraOAuth, store):
        self.oauth = oauth
        self.store = store

    async def _get(self, endpoint: str, params: dict = {}) -> dict:
        """Pomocnicza metoda HTTP GET z automatycznym tokenem i zapisem do bazy"""
        token = await self.oauth.get_valid_token()
        async with httpx.AsyncClient() as client:
            response = await client.get(
                f"{self.BASE_URL}/{endpoint}",
                headers={"Authorization": f"Bearer {token}"},
                params=params,
            )
            response.raise_for_status()
            data = response.json()
            # Zapisujemy do bazy historycznej
            self.store.save_health_data("oura", data, endpoint)
            return data

    async def get_sleep(self, days: int = 7) -> dict:
        """Pobiera dane o śnie z ostatnich N dni"""
        start = (datetime.now() - timedelta(days=days)).strftime("%Y-%m-%d")
        end = datetime.now().strftime("%Y-%m-%d")
        return await self._get("daily_sleep", {"start_date": start, "end_date": end})

    async def get_readiness(self, days: int = 7) -> dict:
        """Pobiera wynik gotowości (readiness) — jak wypoczęte jest ciało"""
        start = (datetime.now() - timedelta(days=days)).strftime("%Y-%m-%d")
        end = datetime.now().strftime("%Y-%m-%d")
        return await self._get("daily_readiness", {"start_date": start, "end_date": end})

    async def get_activity(self, days: int = 7) -> dict:
        """Pobiera dane o aktywności fizycznej"""
        start = (datetime.now() - timedelta(days=days)).strftime("%Y-%m-%d")
        end = datetime.now().strftime("%Y-%m-%d")
        return await self._get("daily_activity", {"start_date": start, "end_date": end})

    async def get_heart_rate(self, days: int = 1) -> dict:
        """Pobiera dane tętna"""
        start = (datetime.now() - timedelta(days=days)).strftime("%Y-%m-%dT00:00:00")
        end = datetime.now().strftime("%Y-%m-%dT23:59:59")
        return await self._get("heartrate", {"start_datetime": start, "end_datetime": end})

    # Definicje narzędzi dla MCP — Claude widzi te opisy i wie kiedy ich używać
    TOOL_DEFINITIONS = [
        {
            "name": "oura_get_sleep",
            "description": "Pobiera dane o śnie z pierścienia Oura. Zawiera fazy snu (REM, deep, light), czas snu, wynik jakości snu, tętno podczas snu.",
            "inputSchema": {
                "type": "object",
                "properties": {
                    "days": {"type": "integer", "description": "Liczba dni wstecz (domyślnie 7)", "default": 7}
                }
            }
        },
        {
            "name": "oura_get_readiness",
            "description": "Pobiera wynik readiness z Oura — ogólna gotowość ciała do wysiłku, regeneracja, HRV, temperatura.",
            "inputSchema": {
                "type": "object",
                "properties": {
                    "days": {"type": "integer", "description": "Liczba dni wstecz", "default": 7}
                }
            }
        },
        {
            "name": "oura_get_activity",
            "description": "Pobiera dane o aktywności fizycznej z Oura: kroki, kalorie, czas aktywny.",
            "inputSchema": {
                "type": "object",
                "properties": {
                    "days": {"type": "integer", "description": "Liczba dni wstecz", "default": 7}
                }
            }
        },
        {
            "name": "oura_get_heart_rate",
            "description": "Pobiera szczegółowe dane tętna z Oura.",
            "inputSchema": {
                "type": "object",
                "properties": {
                    "days": {"type": "integer", "description": "Liczba dni wstecz", "default": 1}
                }
            }
        },
    ]

    async def call_tool(self, name: str, args: dict) -> dict:
        """Router — wywołuje odpowiednią metodę na podstawie nazwy narzędzia"""
        days = args.get("days", 7)
        if name == "oura_get_sleep":
            return await self.get_sleep(days)
        elif name == "oura_get_readiness":
            return await self.get_readiness(days)
        elif name == "oura_get_activity":
            return await self.get_activity(days)
        elif name == "oura_get_heart_rate":
            return await self.get_heart_rate(days)
        raise ValueError(f"Nieznane narzędzie: {name}")
