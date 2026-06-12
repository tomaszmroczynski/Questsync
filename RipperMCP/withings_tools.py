import httpx
from datetime import datetime, timedelta
from withings import WithingsOAuth


class WithingsTools:
    """
    Narzędzia MCP do pobierania danych z Withings API.
    Withings używa innego stylu API niż Oura — endpoint /measure z parametrem meastype.
    """

    BASE_URL = "https://wbsapi.withings.net"

    # Typy pomiarów Withings (meastype)
    MEASURE_TYPES = {
        1: "weight_kg",
        5: "fat_free_mass_kg",
        6: "fat_ratio_percent",
        8: "fat_mass_weight_kg",
        9: "diastolic_mmhg",
        10: "systolic_mmhg",
        76: "muscle_mass_kg",
        77: "hydration_kg",
        88: "bone_mass_kg",
        170: "visceral_fat",
    }

    def __init__(self, oauth: WithingsOAuth, store):
        self.oauth = oauth
        self.store = store

    async def _post(self, endpoint: str, params: dict = {}) -> dict:
        """Withings API używa POST z action w body — ich specyficzny styl i zapis do bazy"""
        token = await self.oauth.get_valid_token()
        async with httpx.AsyncClient() as client:
            response = await client.post(
                f"{self.BASE_URL}/{endpoint}",
                headers={"Authorization": f"Bearer {token}"},
                data=params,
            )
            response.raise_for_status()
            data = response.json()
            body = data.get("body", data)
            # Zapisujemy do bazy historycznej
            action = params.get("action", "unknown")
            self.store.save_health_data("withings", body, f"{endpoint}/{action}")
            return body

    async def get_weight(self, days: int = 30) -> dict:
        """
        Pobiera pomiary wagi i składu ciała.
        meastype=1,5,6,8,76,88 = waga + pełny skład ciała
        """
        start = int((datetime.now() - timedelta(days=days)).timestamp())
        end = int(datetime.now().timestamp())

        data = await self._post("measure", {
            "action": "getmeas",
            "meastype": "1,5,6,8,76,88",
            "category": 1,  # 1 = pomiary rzeczywiste (nie cel)
            "startdate": start,
            "enddate": end,
        })

        # Przetłumacz surowe dane Withings na czytelny format
        measurements = []
        for group in data.get("measuregrps", []):
            entry = {
                "date": datetime.fromtimestamp(group["date"]).strftime("%Y-%m-%d %H:%M"),
                "measures": {}
            }
            for measure in group["measures"]:
                # Withings zwraca wartości jako integer * 10^unit
                # np. value=7050, unit=-2 → 70.50 kg
                value = measure["value"] * (10 ** measure["unit"])
                mtype = self.MEASURE_TYPES.get(measure["type"])
                if mtype:
                    entry["measures"][mtype] = round(value, 2)
            measurements.append(entry)

        return {"measurements": measurements}

    async def get_activity(self, days: int = 7) -> dict:
        """Pobiera aktywność dzienną z Withings"""
        start = (datetime.now() - timedelta(days=days)).strftime("%Y-%m-%d")
        end = datetime.now().strftime("%Y-%m-%d")

        return await self._post("v2/measure", {
            "action": "getactivity",
            "startdateymd": start,
            "enddateymd": end,
        })
    async def get_blood_pressure(self, days: int = 30) -> dict:
        start = int((datetime.now() - timedelta(days=days)).timestamp())
        end = int(datetime.now().timestamp())

        data = await self._post("measure", {
            "action": "getmeas",
            "meastype": "9,10",
            "category": 1,
            "startdate": start,
            "enddate": end,
        })

        measurements = []
        for group in data.get("measuregrps", []):
            entry = {
                "date": datetime.fromtimestamp(group["date"]).strftime("%Y-%m-%d %H:%M"),
                "measures": {}
            }
            for measure in group["measures"]:
                value = measure["value"] * (10 ** measure["unit"])
                mtype = self.MEASURE_TYPES.get(measure["type"])
                if mtype:
                    entry["measures"][mtype] = round(value, 0)
            if entry["measures"]:
                measurements.append(entry)

        return {"measurements": measurements}

    TOOL_DEFINITIONS = [
        {
            "name": "withings_get_weight",
            "description": "Pobiera historię wagi i składu ciała z wagi Withings. Zawiera wagę (kg), procent tłuszczu, masę mięśniową, masę kostną, nawodnienie.",
            "inputSchema": {
                "type": "object",
                "properties": {
                    "days": {"type": "integer", "description": "Liczba dni wstecz (domyślnie 30)", "default": 30}
                }
            }
        },
        {
            "name": "withings_get_activity",
            "description": "Pobiera dane aktywności z urządzenia Withings: kroki, kalorie, dystans.",
            "inputSchema": {
                "type": "object",
                "properties": {
                    "days": {"type": "integer", "description": "Liczba dni wstecz", "default": 7}
                }
            }
        },
        {
            "name": "withings_get_blood_pressure",
            "description": "Pobiera historię pomiarów ciśnienia krwi z urządzenia Withings. Zawiera ciśnienie skurczowe i rozkurczowe w mmHg.",
            "inputSchema": {
                "type": "object",
                "properties": {
                    "days": {"type": "integer", "description": "Liczba dni wstecz (domyślnie 30)", "default": 30}
                }
            }
        },
    ]

    async def call_tool(self, name: str, args: dict) -> dict:
        days = args.get("days", 30)
        if name == "withings_get_weight":
            return await self.get_weight(days)
        elif name == "withings_get_activity":
            return await self.get_activity(days)
        elif name == "withings_get_blood_pressure":
            return await self.get_blood_pressure(days)
        raise ValueError(f"Nieznane narzędzie: {name}")
