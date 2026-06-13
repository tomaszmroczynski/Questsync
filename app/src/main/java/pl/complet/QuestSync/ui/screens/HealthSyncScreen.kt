package pl.complet.QuestSync.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.SettingsInputComponent
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.complet.QuestSync.ui.components.CyberContainer
import pl.complet.QuestSync.ui.components.CyberHeader
import pl.complet.QuestSync.ui.theme.CyberBlack
import pl.complet.QuestSync.ui.theme.RipperGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthSyncScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBlack)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = RipperGold
                    ),
                    title = { Text("CORE WEARABLE NODES", letterSpacing = 2.sp) }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Rounded.SettingsInputComponent,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = RipperGold
                )
                
                CyberHeader("Integrated Sources")
                
                CyberSourceStatusItem("OURA BIOMETRIC", true)
                CyberSourceStatusItem("WITHINGS SCALE", true)
                CyberSourceStatusItem("SAMSUNG CONNECT", true)
                
                Spacer(modifier = Modifier.weight(1f))
                
                Button(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = RipperGold, contentColor = CyberBlack)
                ) {
                    Text("CONFIGURE NEURAL PATHS", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CyberSourceStatusItem(name: String, isConnected: Boolean) {
    CyberContainer(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                name, 
                modifier = Modifier.weight(1f), 
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 1.sp
            )
            Text(
                if (isConnected) "ONLINE" else "OFFLINE",
                color = if (isConnected) RipperGold else Color.Red,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
