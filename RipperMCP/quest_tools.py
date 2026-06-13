import json

VRI_CATALOG = {
    "com.beatgames.beatsaber":              ("Beat Saber",              7.00),
    "com.cloudheadgames.pistolwhip":        ("Pistol Whip",             7.43),
    "com.synthriders.quest":               ("Synth Riders",             4.80),
    "com.kluge.audio_trip":                ("Audio Trip",               8.12),
    "com.fitxr.boxvr":                     ("FitXR",                    8.59),
    "com.resolutiongames.audioshield":     ("Audioshield Workout",       6.82),
    "com.icarusstudios.thrillofthefight":  ("Thrill of the Fight",     12.53),
    "com.knockoutleague":                  ("Knockout League",           7.94),
    "com.creedrisetoglory":                ("Creed: Rise to Glory",      7.21),
    "com.supernatural":                    ("Supernatural",             12.55),
    "com.holoball":                        ("HoloBall",                  5.60),
    "com.spacepiratetrainer":              ("Space Pirate Trainer",       5.19),
    "com.until_you_fall":                  ("Until You Fall",            7.67),
    "com.gorngame":                        ("Gorn",                      5.27),
    "com.bladeandsorcery":                 ("Blade and Sorcery: Nomad",  5.01),
    "com.ninjalegends":                    ("Ninja Legends",              4.97),
    "com.hellsplitarena":                  ("Hellsplit: Arena",           4.79),
    "com.elevenvr.eleven":                 ("Eleven Table Tennis",        4.11),
    "com.racketfury":                      ("Racket Fury",               4.62),
    "com.racket_nx":                       ("Racket: Nx",                4.41),
    "com.finalsoccer":                     ("Final Soccer VR",           6.41),
    "com.hotsquat2":                       ("Hot Squat 2",               7.82),
    "com.jobsimulator":                    ("Job Simulator",             2.28),
    "com.superhot.superhot_vr":            ("SUPERHOT VR",               4.07),
    "com.recroom":                         ("Rec Room",                  3.92),
    "com.tiltbrush":                       ("Tiltbrush",                 3.15),
    "com.pavlovvr":                        ("Pavlov VR",                 3.15),
}

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

        processed_activities = []
        for entry in history:
            payload = entry.get("payload", {})
            data = payload.get("data", payload)

            package_name = data.get("packageName")
            duration_minutes = data.get("durationMinutes", 0)

            game_title = package_name
            kcal_estimated = None

            if package_name in VRI_CATALOG:
                game_title, kcal_per_min = VRI_CATALOG[package_name]
                kcal_estimated = round(duration_minutes * kcal_per_min, 1)

            activity = {
                "activityName": data.get("activityName", "VR Session"),
                "game_title": game_title,
                "package_name": package_name,
                "durationMinutes": duration_minutes,
                "caloriesBurned": data.get("caloriesBurned", 0),
                "kcal_estimated": kcal_estimated,
                "timestamp": entry.get("timestamp"),
                "isHeadsetWorn": data.get("isHeadsetWorn", True)
            }
            processed_activities.append(activity)

        return {"activities": processed_activities}
