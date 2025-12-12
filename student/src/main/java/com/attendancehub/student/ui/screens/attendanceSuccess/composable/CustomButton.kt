package com.attendancehub.student.ui.screens.attendanceSuccess.composable

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun CustomButton(
    onClick : ()->Unit,
    icon : ImageVector,
    text : String,
    modifier: Modifier = Modifier,
    fontModifier : Modifier = Modifier,
    iconModifier : Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.titleMedium,
    fontWeight: FontWeight = FontWeight.SemiBold
){
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFEF5350)
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = iconModifier
        )

        Text(
            text = text,
            style = textStyle,
            fontWeight = fontWeight,
            modifier = fontModifier
        )
    }
}

@Preview
@Composable
private fun CustomButtonPreview(){
    CustomButton(
        onClick = {},
        icon = Icons.Default.WifiOff,
        text = "Disconnect"
    )
}