package com.attendancehub.student.ui.screens.permission

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import com.attendancehub.student.navigation.LocalNavController
import com.attendancehub.student.navigation.StudentScreen
import com.attendancehub.composables.PermissionCard
import com.attendancehub.utils.ObserveAsEffect
import org.koin.androidx.compose.koinViewModel

@Composable
fun PermissionsScreen(
    viewModel: PermissionViewModel = koinViewModel()
) {
    val navController = LocalNavController.current

        ObserveAsEffect(viewModel.effect) { effect ->
            when (effect) {
                is PermissionEffect.NavigateToStudentInfo -> {
                    navController.navigate(StudentScreen.NetworkScan.route) {
                        popUpTo(StudentScreen.Permissions.route) { inclusive = true }
                    }
                }
            }
        }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
           viewModel.onGrantPermissionsEnabled()
        }
    }

    val requiredPermissions = buildList {
        add(Manifest.permission.CAMERA) // For QR scanning
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.CHANGE_WIFI_STATE)
        add(Manifest.permission.ACCESS_WIFI_STATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(top=40.dp)
        ) {
            Logo(
                shape = RoundedCornerShape(40.dp),
                color = MaterialTheme.colorScheme.primary,
                iconTint = Color.White,
                icon = Icons.Default.Shield,
                modifier = Modifier.size(80.dp),
                iconModifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.app_name_display),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = stringResource(R.string.permissions_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            PermissionCard(
                icon = Icons.Default.Wifi,
                title = stringResource(R.string.permission_wifi_title),
                description = stringResource(R.string.permission_wifi_desc),
            )
            Spacer(Modifier.height(16.dp))
            PermissionCard(
                icon = Icons.Default.LocationOn,
                title = stringResource(R.string.permission_location_title),
                description = stringResource(R.string.permission_location_desc),
            )
            Spacer(Modifier.height(16.dp))
            PermissionCard(
                icon = Icons.Default.CameraAlt,
                title = stringResource(R.string.permission_camera_title),
                description = stringResource(R.string.permission_camera_desc)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                permissionLauncher.launch(requiredPermissions.toTypedArray())
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text(
                text = stringResource(R.string.grant_permissions_button),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        Text(
            text = stringResource(R.string.permissions_footer),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(top=8.dp)
        )
    }
}
@Preview
@Composable
fun PermissionsScreenPreview() {
    PermissionsScreen()
}