import os
import httpx
from token_store import TokenStore


class WithingsOAuth:
    """
    OAuth 2.0 dla Withings API.
    Withings używa niestandardowego parametru 'action' w token endpoint.
    """

    AUTH_URL = "https://account.withings.com/oauth2_user/authorize2"
    TOKEN_URL = "https://wbsapi.withings.net/v2/oauth2"

    def __init__(self, store: TokenStore):
        self.store = store
        self.client_id = os.getenv("WITHINGS_CLIENT_ID")
        self.client_secret = os.getenv("WITHINGS_CLIENT_SECRET")
        self.redirect_uri = os.getenv("BASE_URL") + "/auth/withings/callback"

    def get_authorization_url(self) -> str:
        params = {
            "response_type": "code",
            "client_id": self.client_id,
            "redirect_uri": self.redirect_uri,
            "scope": "user.metrics,user.activity,user.sleepevents",
            "state": "health-mcp",
        }
        query = "&".join(f"{k}={v}" for k, v in params.items())
        return f"{self.AUTH_URL}?{query}"

    async def exchange_code(self, code: str):
        """
        Withings wymaga action=requesttoken w body — niestandardowe vs OAuth spec.
        To częste przy starszych API które implementowały OAuth przed standaryzacją.
        """
        async with httpx.AsyncClient() as client:
            response = await client.post(
                self.TOKEN_URL,
                data={
                    "action": "requesttoken",
                    "grant_type": "authorization_code",
                    "client_id": self.client_id,
                    "client_secret": self.client_secret,
                    "code": code,
                    "redirect_uri": self.redirect_uri,
                },
            )
            response.raise_for_status()
            data = response.json()
            # Withings owija odpowiedź w {"status": 0, "body": {...}}
            token_data = data["body"]
            self.store.set_token("withings", token_data)
            print("✅ Withings token zapisany")

    async def get_valid_token(self) -> str:
        """Odświeża token Withings jeśli potrzeba"""
        token_data = self.store.get_token("withings")
        if not token_data:
            raise Exception("Brak tokenu Withings. Wejdź na /auth/withings aby się zalogować.")

        async with httpx.AsyncClient() as client:
            response = await client.post(
                self.TOKEN_URL,
                data={
                    "action": "requesttoken",
                    "grant_type": "refresh_token",
                    "client_id": self.client_id,
                    "client_secret": self.client_secret,
                    "refresh_token": token_data["refresh_token"],
                },
            )
            # Withings zwraca status 200 nawet przy błędach, trzeba sprawdzić pole "status" w body
            data = response.json()
            if response.status_code == 200 and data.get("status") == 0:
                new_token = data["body"]
                self.store.set_token("withings", new_token)
                return new_token["access_token"]
            elif data.get("status") != 0:
                print(f"❌ Withings refresh failed: {data.get('error') or data.get('status')}")
                # Jeśli refresh token wygasł (status 401 w specyfice Withings),
                # Claude dostanie błąd i użytkownik będzie musiał się zalogować ponownie.

        return token_data["access_token"]
