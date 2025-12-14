package org.wahid.attendancehub.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun Logo(
    modifier: Modifier = Modifier,
    iconModifier : Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(40.dp),
    color: Color = MaterialTheme.colorScheme.primary,
    iconTint: Color = Color.White,
    icon: ImageVector = Icons.Default.Shield
){
    Surface(
        modifier = modifier,
        shape = shape,
        color = color
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = iconModifier,
                tint = iconTint
            )
        }
    }
}
@Preview
@Composable
fun LogoPreview() {
    Logo()
}