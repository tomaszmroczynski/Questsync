import os
import httpx
from token_store import TokenStore


class OuraOAuth:
    """
    OAuth 2.0 Authorization Code Flow dla Oura API.
    
    Flow:
    1. get_authorization_url() → redirect użytkownika do Oura
    2. Oura przekierowuje na /callback/oura?code=XYZ
    3. exchange_code(code) → wymiana code na access_token + refresh_token
    """

    AUTH_URL = "https://cloud.ouraring.com/oauth/authorize"
    TOKEN_URL = "https://api.ouraring.com/oauth/token"

    def __init__(self, store: TokenStore):
        self.store = store
        self.client_id = os.getenv("OURA_CLIENT_ID")
        self.client_secret = os.getenv("OURA_CLIENT_SECRET")
        self.redirect_uri = os.getenv("BASE_URL") + "/auth/oura/callback"

    def get_authorization_url(self) -> str:
        """
        Buduje URL do Oura OAuth.
        scope określa do jakich danych aplikacja prosi o dostęp.
        Zasada najmniejszych uprawnień — prosimy tylko o to co potrzebujemy.
        """
        params = {
            "response_type": "code",
            "client_id": self.client_id,
            "redirect_uri": self.redirect_uri,
            "scope": "personal email daily heartrate workout tag session",
        }
        query = "&".join(f"{k}={v}" for k, v in params.items())
        return f"{self.AUTH_URL}?{query}"

    async def exchange_code(self, code: str):
        """
        Wymienia jednorazowy code na access_token.
        Code jest ważny tylko kilka minut i może być użyty tylko raz.
        W zamian dostajemy access_token (krótki) + refresh_token (długi).
        """
        async with httpx.AsyncClient() as client:
            response = await client.post(
                self.TOKEN_URL,
                data={
                    "grant_type": "authorization_code",
                    "code": code,
                    "redirect_uri": self.redirect_uri,
                    "client_id": self.client_id,
                    "client_secret": self.client_secret,
                },
            )
            response.raise_for_status()
            token_data = response.json()
            self.store.set_token("oura", token_data)
            print("✅ Oura token zapisany")

    async def get_valid_token(self) -> str:
        """
        Zwraca ważny access_token.
        Jeśli wygasł — automatycznie odświeża przez refresh_token.
        To tzw. "silent refresh" — użytkownik nie musi się ponownie logować.
        """
        token_data = self.store.get_token("oura")
        if not token_data:
            raise Exception("Brak tokenu Oura. Wejdź na /auth/oura aby się zalogować.")

        # Sprawdź czy token wymaga odświeżenia
        # Oura access_token wygasa po 24h
        if "refresh_token" in token_data:
            async with httpx.AsyncClient() as client:
                response = await client.post(
                    self.TOKEN_URL,
                    data={
                        "grant_type": "refresh_token",
                        "refresh_token": token_data["refresh_token"],
                        "client_id": self.client_id,
                        "client_secret": self.client_secret,
                    },
                )
                if response.status_code == 200:
                    new_token = response.json()
                    self.store.set_token("oura", new_token)
                    return new_token["access_token"]

        return token_data["access_token"]
