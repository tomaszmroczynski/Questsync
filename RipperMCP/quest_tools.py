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
    "com.ubisoft.assassinscreed.nexus":    ("Assassin's Creed Nexus",    5.20),
    "com.asgardswrath2":                   ("Asgard's Wrath 2",          4.85),
    "com.verticalrobot.redmatter2":        ("Red Matter 2",              3.10),
    "com.skydanceinteractive.twd_saints_sinners": ("TWD: Saints & Sinners", 4.95),
    "com.firestep.box-to-fit":             ("Box to Fit",                9.10),
    "com.funnyvg.totaleasy":               ("Les Mills Bodycombat",     10.50),
    "com.vr_workout":                      ("VRWorkout",                11.20),
    "com.resolutiongames.demeo":           ("Demeo",                     2.10),
    "com.polyarc.moss":                    ("Moss: Book II",             2.25),
    "com.tripwire.espire2":                ("Espire 2",                  4.30),
    "com.combat.vail":                     ("VAIL VR",                   4.10),
    "com.contractors":                     ("Contractors VR",            4.50),
    "com.breachers":                       ("Breachers",                 4.20),
    "com.ghostsof_tabor":                  ("Ghosts of Tabor",           4.75),
    "com.into_the_radius":                 ("Into the Radius",           4.40),
    "com.walkaboutmini-golf":              ("Walkabout Mini Golf",       2.50),
    "com.real-fishing-vr":                 ("Real VR Fishing",           2.30),
    "com.powerbeax.quest":                 ("PowerBeatsVR",              8.90),
    "com.odderslab.ohshape":               ("OhShape",                   6.70),
    "com.inandout.hitstream":              ("HitStream",                 7.80),
    "com.xreality.holofit":                ("HOLOFIT",                   7.50),
    "com.vz.vzfit":                        ("VZfit",                     8.10),
    "com.ragnarock":                       ("Ragnarock",                 6.40),
    "com.drunkn-bar-fight":                ("Drunkn Bar Fight",          5.50),
    "com.populationone":                   ("Population: ONE",           4.30),
    "com.gorillatag":                      ("Gorilla Tag",               7.90),
    "com.climb2":                          ("The Climb 2",               5.80),
    "com.superhot.superhot-vr":            ("SUPERHOT VR",               4.07),
    "com.amongusvr":                       ("Among Us VR",               2.80),
    "com.vrchat":                          ("VRChat",                    2.60),
    "com.oculus.browser":                  ("Meta Quest Browser",        1.50),
    "com.netflix.quest":                   ("Netflix",                   1.20),
    "com.amazon.primevideo":               ("Prime Video VR",            1.20),
    "com.bigscreen.bigscreen":             ("Bigscreen Beta",            1.80),
    "com.skybox.player":                   ("SKYBOX VR Video Player",    1.40),
    "com.deovr.player":                    ("DeoVR Video Player",        1.40),
    "com.espn.espnvr":                     ("ESPN",                      1.30),
    "com.youtube.vr":                      ("YouTube VR",                1.50),
    "com.wander.vr":                       ("Wander",                    2.20),
    "com.brinkworld":                      ("BRINK Traveler",            2.10),
    "com.wooordle":                        ("Wooordle",                  2.00),
    "com.puzzlingplaces":                  ("Puzzling Places",           1.90),
    "com.cubism":                          ("Cubism",                    1.80),
    "com.gravitysketch":                   ("Gravity Sketch",            2.40),
    "com.shaperlab":                       ("Shapelab",                  2.60),
    "com.sculptvr":                        ("SculptrVR",                 2.80),
    "com.tiltbrush.quest":                 ("Tilt Brush",                3.15),
    "com.multibrush":                      ("MultiBrush",                3.15),
    "com.vermillion":                      ("Vermillion",                2.90),
    "com.vrtuosi":                         ("Virtuoso",                  3.40),
    "com.beatsmith":                       ("BeatSmith",                 5.90),
    "com.paradiddle":                      ("Paradiddle",                6.10),
    "com.tripp.tripp":                     ("TRIPP",                     1.70),
    "com.guidedmeditationvr":              ("Guided Meditation VR",      1.40),
    "com.maloka.vr":                       ("Maloka",                    1.50),
    "com.vspeedway":                       ("V-Speedway",                3.20),
    "com.grid.legends":                    ("GRID Legends",              3.50),
    "com.polyphony.gt":                    ("Gran Turismo VR",           3.80),
    "com.dirt.rally":                      ("DiRT Rally VR",             4.10),
    "com.f1.manager":                      ("F1 23 VR",                  3.90),
    "com.warplanes.ww1":                   ("Warplanes: WW1 Fighters",   3.70),
    "com.vtolvr":                          ("VTOL VR",                   2.80),
    "com.fyian.TheThrillOfTheFight":        ("Thrill of the Fight (Quest)", 12.53),
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
