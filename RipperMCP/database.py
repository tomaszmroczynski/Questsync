import sqlite3
import json
from pathlib import Path
from datetime import datetime
from typing import List, Dict, Any, Optional

class RipperDatabase:
    def __init__(self, db_path: str = "/app/data/ripper_health.db"):
        self.db_path = Path(db_path)
        self.db_path.parent.mkdir(parents=True, exist_ok=True)
        self._init_db()

    def _get_connection(self):
        return sqlite3.connect(self.db_path)

    def _init_db(self):
        with self._get_connection() as conn:
            conn.execute("""
                CREATE TABLE IF NOT EXISTS health_data (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    source TEXT NOT NULL,
                    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                    data_type TEXT NOT NULL,
                    payload TEXT NOT NULL
                )
            """)
            conn.execute("CREATE INDEX IF NOT EXISTS idx_source ON health_data(source)")
            conn.execute("CREATE INDEX IF NOT EXISTS idx_timestamp ON health_data(timestamp)")

    def save_data(self, source: str, data_type: str, payload: Dict[str, Any]):
        with self._get_connection() as conn:
            conn.execute(
                "INSERT INTO health_data (source, data_type, payload) VALUES (?, ?, ?)",
                (source, data_type, json.dumps(payload))
            )

    def get_latest_data(self, source: str, data_type: Optional[str] = None) -> List[Dict[str, Any]]:
        query = "SELECT source, timestamp, data_type, payload FROM health_data WHERE source = ?"
        params = [source]
        if data_type:
            query += " AND data_type = ?"
            params.append(data_type)
        query += " ORDER BY timestamp DESC LIMIT 50"

        with self._get_connection() as conn:
            cursor = conn.execute(query, params)
            return [
                {
                    "source": row[0],
                    "timestamp": row[1],
                    "data_type": row[2],
                    "payload": json.loads(row[3])
                }
                for row in cursor.fetchall()
            ]

    def get_data_range(self, source: str, start_date: str, end_date: str) -> List[Dict[str, Any]]:
        with self._get_connection() as conn:
            cursor = conn.execute(
                "SELECT source, timestamp, data_type, payload FROM health_data WHERE source = ? AND timestamp BETWEEN ? AND ? ORDER BY timestamp ASC",
                (source, start_date, end_date)
            )
            return [
                {
                    "source": row[0],
                    "timestamp": row[1],
                    "data_type": row[2],
                    "payload": json.loads(row[3])
                }
                for row in cursor.fetchall()
            ]
