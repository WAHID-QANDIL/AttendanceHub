package org.wahid.attendancehub.student.ui.screens.home

import org.wahid.attendancehub.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import org.koin.androidx.compose.koinViewModel
import org.wahid.attendancehub.core.SharedPrefs
import org.wahid.attendancehub.models.WifiNetwork
import org.wahid.attendancehub.student.navigation.StudentScreen
import org.wahid.attendancehub.student.ui.screens.ManualEntryDialog
import org.wahid.attendancehub.student.ui.screens.StudentInfoBottomSheet
import org.wahid.attendancehub.utils.ObserveAsEffect


@Composable
fun StudentNetworkScanScreen(
    navController: NavController,
    viewModel: NetworkScanViewModel = koinViewModel<NetworkScanViewModel>()
) {
    val context = LocalContext.current
    val sharedPrefs = remember { SharedPrefs.getInstance(context) }
    val firstName by sharedPrefs.firstName.collectAsStateWithLifecycle()
    val lastName by sharedPrefs.lastName.collectAsStateWithLifecycle()
    val studentId by sharedPrefs.studentId.collectAsStateWithLifecycle()

    val state by viewModel.state.collectAsStateWithLifecycle()
    val availableNetworks by viewModel.availableNetworks.collectAsStateWithLifecycle()

    var showManualEntryDialog by remember { mutableStateOf(false) }

    // Handle navigation effects
    ObserveAsEffect(viewModel.effect) { effect ->
        when (effect) {
            is NetworkScanEffect.NavigateToQRScanner -> {
                navController.navigate(StudentScreen.QRScanner.route)
            }
            is NetworkScanEffect.NavigateToManualEntry -> {
                showManualEntryDialog = true
            }
            is NetworkScanEffect.NavigateToConnecting -> {
                // TODO: Navigate to connecting screen with network info
            }
        }
    }

    // Manual Entry Dialog
    if (showManualEntryDialog) {
        ManualEntryDialog(
            onDismiss = { showManualEntryDialog = false },
            onConnect = { ssid, password ->
                showManualEntryDialog = false
                // TODO: Handle manual connection with SSID and password
                viewModel.onNetworkSelected(org.wahid.attendancehub.models.WifiNetwork(
                    ssid = ssid,
                    password = password,
                    signalStrength = 5,
                    isSecured = true,
                    isTeacherNetwork = false
                ))
            }
        )
    }

    // Show student info bottom sheet when needed
    if (state is NetworkScanUiState.ShowStudentInfoSheet) {
        StudentInfoBottomSheet(
            onDismiss = { viewModel.dismissStudentInfoSheet() },
            onInfoSaved = { first, last, id ->
                val deviceId = sharedPrefs.deviceId.value
                sharedPrefs.saveStudentInfo(first, last, id, deviceId)
                viewModel.onStudentInfoSaved()
            },
            existingFirstName = firstName,
            existingLastName = lastName,
            existingStudentId = studentId
        )
    }

    StudentNetworkScanContent(
        availableNetworks = availableNetworks,
        onNetworkSelected = { viewModel.onNetworkSelected(it) },
        onRefresh = { viewModel.scanNetworks() },
        onScanQR = { viewModel.onScanQRClicked() },
        onManualEntry = { viewModel.onManualEntryClicked() },
        isScanning = state is NetworkScanUiState.Scanning,
        studentName = if (firstName.isNotBlank()) "$firstName $lastName" else null,
        onOpenInfoSheet = { viewModel.showStudentInfoSheet() }
    )
}



@Composable
private fun StudentNetworkScanContent(
    availableNetworks: List<WifiNetwork>,
    onNetworkSelected: (WifiNetwork) -> Unit,
    onRefresh: () -> Unit,
    onScanQR: () -> Unit,
    onManualEntry: () -> Unit,
    isScanning: Boolean = false,
    studentName: String? = null,
    onOpenInfoSheet:()->Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding()
    ) {

        stickyHeader {
            // Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(0.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = stringResource(R.string.attendance_check_in),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.connect_teacher_network_prompt),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Student Info Card (if exists)
                studentName?.let { name ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp).clickable(onClick = onOpenInfoSheet),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Logged in as",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // Available Networks Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.available_networks),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            IconButton(onClick = onRefresh) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = stringResource(R.string.refresh),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        HorizontalDivider()
                        // Teacher network (highlighted)
                        val teacherNetwork = availableNetworks.find { it.isTeacherNetwork }
                        teacherNetwork?.let {
                            TeacherNetworkItem(network = it, onClick = { onNetworkSelected(it) })
                            HorizontalDivider()
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        // Other networks
//                        val otherNetworks = availableNetworks.filter { !it.isTeacherNetwork }
//                        LazyColumn(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .height(200.dp)
//                        ) {
//                            items(otherNetworks) { network ->
//                                NetworkListItem(
//                                    network = network,
//                                    onClick = { onNetworkSelected(network) }
//                                )
//                                if (network != otherNetworks.last()) {
//                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
//                                }
//                            }
//                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Info card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF9C4)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = Color(0xFFF57F17),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.looking_for_class),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFF57F17)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.looking_for_class_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF827717)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                Spacer(modifier = Modifier.weight(1f))

                // QR Scan Button (Primary - Filled)
                Button(
                    onClick = onScanQR,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.scan_qr_code),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Manual Entry Button (Secondary - Outlined)
                OutlinedButton(
                    onClick = onManualEntry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF4CAF50)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.enter_network_manually),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }


        }

    }
}

@Composable
fun TeacherNetworkItem(
    network: WifiNetwork,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE1BEE7)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        color = Color(0xFF4CAF50)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = network.ssid,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SignalWifi4Bar,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.strong_signal),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Icon(
                    imageVector = Icons.Default.SignalWifi4Bar,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Text(
                    text = stringResource(R.string.connect_to_class),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    // Teacher found card
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE8F5E9)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(20.dp)
            )
            Spacer(
                modifier = Modifier
                    .width(8.dp)

            )
            Column {
                Text(
                    text = stringResource(R.string.teacher_network_found),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2E7D32)
                )
                Text(
                    text = stringResource(R.string.teacher_network_found_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF388E3C)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}

@Composable
fun NetworkListItem(
    network: WifiNetwork,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = if (network.isSecured) Icons.Default.Lock else Icons.Default.WifiLock,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = network.ssid,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (network.isSecured) stringResource(R.string.secured_network) else stringResource(R.string.open_network),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Icon(
            imageVector = when (network.signalStrength) {
                4 -> Icons.Default.NetworkWifi
                3 -> Icons.Default.NetworkWifi
                2 -> Icons.Default.NetworkWifi2Bar
                1 -> Icons.Default.NetworkWifi1Bar
                else -> Icons.Default.NetworkWifi
            },
            contentDescription = stringResource(R.string.signal_strength),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}


@Preview(showBackground = true)
@Composable
private fun StudentNetworkScanScreenPreview() {
    StudentNetworkScanScreen(
        navController = rememberNavController(),
    )
}
