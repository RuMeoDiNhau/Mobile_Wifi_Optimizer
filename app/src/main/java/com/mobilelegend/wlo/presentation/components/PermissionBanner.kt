package com.mobilelegend.wlo.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilelegend.wlo.presentation.theme.DarkBackground
import com.mobilelegend.wlo.presentation.theme.SignalYellow

/**
 * Banner cảnh báo khi ứng dụng thiếu quyền Vị trí hoặc dịch vụ GPS đang tắt.
 */
@Composable
fun PermissionBanner(
    message: String,
    buttonText: String,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF2E2211))
            .border(1.dp, SignalYellow.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Cảnh báo",
            tint = SignalYellow,
            modifier = Modifier.size(28.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Cần hỗ trợ phần cứng",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = SignalYellow
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = message,
                fontSize = 12.sp,
                color = Color(0xFFE2E8F0)
            )
        }

        Button(
            onClick = onActionClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = SignalYellow,
                contentColor = DarkBackground
            ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = buttonText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
