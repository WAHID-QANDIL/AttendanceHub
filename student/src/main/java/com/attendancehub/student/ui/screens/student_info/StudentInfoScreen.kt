package com.attendancehub.student.ui.screens.student_info

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.attendancehub.student.R
import com.attendancehub.student.navigation.LocalNavController
import com.attendancehub.student.navigation.StudentScreen
import com.attendancehub.student.ui.screens.student_info.composable.CustomTextField
import com.attendancehub.utils.ObserveAsEffect
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentInfoScreen(
    viewModel: StudentInfoViewModel = koinViewModel()
) {
    val navController = LocalNavController.current

    ObserveAsEffect(viewModel.effect) {
        when (it) {
            is StudentInfoEffect.NavigateToPermissionScreen -> {
                navController.navigate(StudentScreen.Permissions.route)
            }
        }
    }

    var firstName: TextFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    var lastName: TextFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    var studentId: TextFieldValue by remember { mutableStateOf(TextFieldValue("")) }

    val isFormValid = firstName.text.isNotBlank() &&
                      lastName.text.isNotBlank() &&
                      studentId.text.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.screen_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )

                    Column(
                        modifier = Modifier.padding(start = 16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.enter_your_details),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = stringResource(R.string.sub_title),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            CustomTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = stringResource(R.string.firstName),
                placeholder = stringResource(R.string.firstNamePlaceholder),
                leadingIcon = Icons.Default.Person
            )

            CustomTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = stringResource(R.string.lastName),
                placeholder = stringResource(R.string.lastNamePlaceholder),
                leadingIcon = Icons.Default.Person
            )

            CustomTextField(
                value = studentId,
                onValueChange = { studentId = it },
                label = stringResource(R.string.studentId),
                placeholder = stringResource(R.string.studentIDPlaceholder),
                leadingIcon = Icons.Default.Badge
            )

            Spacer(modifier = Modifier.weight(1f))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = stringResource(R.string.infoText),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }

            Button(
                onClick = { viewModel.onContinueToScannerClick(firstName.text, lastName.text, studentId.text) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = isFormValid,
                shape = RoundedCornerShape(28.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null
                )
                Text(
                    text = stringResource(R.string.continue_to_scanner),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Preview
@Composable
private fun StudentInfoPreview(){
    StudentInfoScreen()
}