================================================================================
RIPPER MCP SERVER - SYNOLOGY NAS DEPLOYMENT GUIDE
================================================================================

Target Domain: https://ripper-mcp.complet-ai.no
Local Port: 8103
Persistence: /app/data (SQLite + Encrypted Tokens)

--------------------------------------------------------------------------------
STEP 1: PREPARE FILES
--------------------------------------------------------------------------------
1. Copy the entire 'RipperMCP' folder from your computer to your Synology NAS.
   Recommended location: /volume1/docker/RipperMCP

2. Ensure the 'data' subfolder exists locally or will be created by Docker.
   The docker-compose.yml maps './data' to '/app/data' inside the container.

--------------------------------------------------------------------------------
STEP 2: ENVIRONMENT CONFIGURATION
--------------------------------------------------------------------------------
1. In the RipperMCP folder on the NAS, create or edit the '.env' file.
2. Add your API keys and the encryption key:
   OURA_CLIENT_ID=your_id
   OURA_CLIENT_SECRET=your_secret
   WITHINGS_CLIENT_ID=your_id
   WITHINGS_CLIENT_SECRET=your_secret
   ENCRYPTION_KEY=your_generated_key (Check container logs on first run if empty)

--------------------------------------------------------------------------------
STEP 3: DEPLOY WITH CONTAINER MANAGER
--------------------------------------------------------------------------------
1. Open 'Container Manager' on your Synology.
2. Go to 'Project' -> 'Create'.
3. Set a Project Name (e.g., RipperMCP).
4. Select the path where you copied the files.
5. Select 'Use existing docker-compose.yml'.
6. Follow the wizard and click 'Done' to build and start the container.

--------------------------------------------------------------------------------
STEP 4: SETUP REVERSE PROXY (HTTPS)
--------------------------------------------------------------------------------
1. Go to 'Control Panel' -> 'Login Portal' -> 'Advanced' -> 'Reverse Proxy'.
2. Click 'Create'.
3. General Tab:
   - Description: RipperMCP
   - Source:
     - Protocol: HTTPS
     - Hostname: ripper-mcp.complet-ai.no
     - Port: 443
     - Enable HSTS: Checked
   - Destination:
     - Protocol: HTTP
     - Hostname: localhost
     - Port: 8103
4. Custom Header Tab:
   - Click 'Create' -> 'WebSocket'.
   - This automatically adds 'Upgrade' and 'Connection' headers.
   - CRITICAL: Required for MCP Server-Sent Events (SSE).
5. Click OK.

--------------------------------------------------------------------------------
STEP 5: SSL CERTIFICATE (CLOUDFLARE WILDCARD)
--------------------------------------------------------------------------------
1. Go to 'Control Panel' -> 'Security' -> 'Certificate'.
2. Ensure your *.complet-ai.no (or specific) certificate is imported.
3. Click 'Settings'.
4. Find 'ripper-mcp.complet-ai.no' in the list and assign your Cloudflare
   Wildcard certificate to it.
5. Click OK.

--------------------------------------------------------------------------------
STEP 6: VERIFY CONNECTION
--------------------------------------------------------------------------------
1. Android App:
   - Ensure local.properties has:
     MCP_SERVER_URL=https://ripper-mcp.complet-ai.no/mcp
   - Build and run the 'QuestSync' app.
   - Tap 'AI Insights' or 'Sync' to test the connection.

2. Web Browser:
   - Visit: https://ripper-mcp.complet-ai.no/status
   - You should see a JSON response: {"oura": false, "withings": false, "server": "running"}

--------------------------------------------------------------------------------
SUPPORT:
If SSE connections time out, ensure your Synology Firewall and Router Port
Forwarding (443) are correctly configured.
================================================================================
