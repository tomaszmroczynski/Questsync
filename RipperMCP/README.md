# RipperMCP Health Server

Unified MCP server for health data integration (Oura, Withings, Samsung Health, Meta Quest) with SQLite persistent storage.

## Features

- **Multi-source Integration**: Oura Ring, Withings, Samsung Health, and Meta Quest.
- **SQLite Persistence**: Stores historical health metrics for long-term analysis.
- **MCP Protocol**: Compatible with Claude LLM and other MCP-aware agents.
- **Synology NAS Ready**: Optimized for Docker deployment on Synology.

## Setup

1. **OAuth Registration**:
   - Register your Oura and Withings apps with `https://your-domain.com/auth/oura/callback` and `https://your-domain.com/auth/withings/callback`.

2. **Environment**:
   - `cp .env.example .env` and fill in your credentials.

3. **Deployment**:
   ```bash
   docker-compose up -d --build
   ```
   *Note: Check logs on first run for the generated `ENCRYPTION_KEY`.*

4. **Authorization**:
   - Visit `https://your-domain.com/auth/oura` and `https://your-domain.com/auth/withings`.

## Android App Integration (QuestSync)

Configure the app to sync data to the server's `/sync/health-connect` endpoint. The server will store it in the SQLite database for Claude to access via MCP.

## Historical Data Queries

Claude can now query historical data using tools like:
- `quest_get_activity_history`
- `samsung_get_health_summary` (returns latest from history)
- Oura and Withings historical tools.
