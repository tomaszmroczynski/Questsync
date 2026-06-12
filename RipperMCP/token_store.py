import json
import os
from pathlib import Path
from cryptography.fernet import Fernet
from datetime import datetime
from database import RipperDatabase

class TokenStore:
    """
    Szyfrowane przechowywanie tokenów OAuth oraz danych zdrowotnych w bazie SQLite.
    """

    def __init__(self):
        self.tokens_path = Path("/app/data/tokens.enc")
        self.db = RipperDatabase()
        self.tokens_path.parent.mkdir(parents=True, exist_ok=True)
        
        key = os.getenv("ENCRYPTION_KEY")
        if not key:
            key = Fernet.generate_key().decode()
            print(f"\n⚠️  WYGENEROWANO NOWY KLUCZ SZYFROWANIA!")
            print(f"Dodaj do .env: ENCRYPTION_KEY={key}\n")
        
        self.fernet = Fernet(key.encode() if isinstance(key, str) else key)
        self._tokens = self._load()

    def _load(self) -> dict:
        if not self.tokens_path.exists():
            return {}
        try:
            encrypted = self.tokens_path.read_bytes()
            decrypted = self.fernet.decrypt(encrypted)
            return json.loads(decrypted)
        except Exception:
            return {}

    def _save(self):
        data = json.dumps(self._tokens).encode()
        encrypted = self.fernet.encrypt(data)
        self.tokens_path.write_bytes(encrypted)

    def set_token(self, service: str, token_data: dict):
        self._tokens[service] = token_data
        self._save()

    def get_token(self, service: str) -> dict | None:
        return self._tokens.get(service)

    def has_token(self, service: str) -> bool:
        return service in self._tokens

    def save_health_data(self, source: str, data: dict, data_type: str = "generic"):
        self.db.save_data(source, data_type, data)

    def get_health_history(self, source: str, data_type: str = None):
        return self.db.get_latest_data(source, data_type)
