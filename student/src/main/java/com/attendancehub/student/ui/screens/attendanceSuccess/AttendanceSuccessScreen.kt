package com.attendancehub.student.ui.screens.attendanceSuccess

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.attendancehub.composables.Logo
import com.attendancehub.student.R
import com.attendancehub.student.ui.screens.attendanceSuccess.composable.CustomButton
import com.attendancehub.student.ui.screens.attendanceSuccess.composable.InfoCard

@Composable
fun AttendanceSuccessScreen(
    networkName: String,
    connectedDuration: String,
    markedAtTime: String,
    onDisconnect: () -> Unit,
    onScanQR: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(0.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF4CAF50)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.connected_header),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.attendance_marked_header),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
                Logo(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.2f),
                    icon = Icons.Default.CheckCircle,
                    iconTint = Color.White,
                    iconModifier = Modifier.size(28.dp)
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Logo(
                modifier = Modifier.size(120.dp),
                shape = CircleShape,
                color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                icon = Icons.Default.CheckCircle,
                iconTint = Color(0xFF4CAF50),
                iconModifier =  Modifier.size(60.dp)
            )
            Text(
                text = stringResource(R.string.youre_all_set),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top=24.dp,bottom=8.dp)
            )
            Text(
                text = stringResource(R.string.attendance_recorded_msg),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom=30.dp)
            )
            InfoCard(
                icon = Icons.Default.Wifi,
                label = stringResource(R.string.label_network),
                value = networkName,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            InfoCard(
                icon = Icons.Default.Add,
                label = stringResource(R.string.label_marked_at),
                value = markedAtTime,
                modifier = Modifier.padding(bottom = 80.dp)
            )
            CustomButton( //change to home button (color+icon) home ->network scan
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                onClick = onDisconnect,
                icon = Icons.Default.WifiOff,
                iconModifier = Modifier.size(20.dp),
                fontModifier =  Modifier.padding(start = 8.dp),
                text = stringResource(R.string.disconnect_button),
            )
            Text(
                text = stringResource(R.string.disconnect_warning),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top=12.dp, bottom = 5.dp)
            )
        }
    }
}

@Preview
@Composable
fun AttendanceSuccessScreenPreview(){
    AttendanceSuccessScreen(
        networkName = "SSID",
        connectedDuration = "10:30",
        markedAtTime = "12:00 PM",
        onDisconnect = {},
        onScanQR = {}
    )
}