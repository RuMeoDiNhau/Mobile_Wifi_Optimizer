package com.mobilelegend.wlo.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilelegend.wlo.presentation.components.AppTopHeader
import com.mobilelegend.wlo.presentation.components.WifiNeedOption
import com.mobilelegend.wlo.presentation.theme.AccentEmerald
import com.mobilelegend.wlo.presentation.theme.AppBackground
import com.mobilelegend.wlo.presentation.theme.CardBorder
import com.mobilelegend.wlo.presentation.theme.DividerLight
import com.mobilelegend.wlo.presentation.theme.ForestDarkCard
import com.mobilelegend.wlo.presentation.theme.ForestDarkCardEnd
import com.mobilelegend.wlo.presentation.theme.LightGreenSurface
import com.mobilelegend.wlo.presentation.theme.PrimaryGreen
import com.mobilelegend.wlo.presentation.theme.SurfaceWhite
import com.mobilelegend.wlo.presentation.theme.TextMuted
import com.mobilelegend.wlo.presentation.theme.TextPrimary
import com.mobilelegend.wlo.presentation.theme.TextSecondary

@Composable
fun ProfileScreen(
    selectedNeed: WifiNeedOption,
    totalMeasurements: Int,
    onCycleNeed: () -> Unit,
    onOpenAppSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var notificationsEnabled by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 18.dp, bottom = 24.dp)
    ) {
        // 1. Header
        AppTopHeader(
            overline = "TÀI KHOẢN",
            title = "Cá nhân"
        )

        Spacer(modifier = Modifier.height(18.dp))

        // 2. Thẻ Thông tin User (Minh Anh)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 3.dp,
                    shape = RoundedCornerShape(22.dp),
                    ambientColor = Color.Black.copy(alpha = 0.03f),
                    spotColor = Color.Black.copy(alpha = 0.05f)
                )
                .clip(RoundedCornerShape(22.dp))
                .background(SurfaceWhite)
                .border(1.dp, CardBorder, RoundedCornerShape(22.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(PrimaryGreen),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "MA",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Minh Anh",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "minhanh@email.com",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Thẻ Cấp độ đóng góp (Nhà khám phá Wi-Fi)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(24.dp),
                    ambientColor = ForestDarkCard.copy(alpha = 0.25f),
                    spotColor = ForestDarkCard.copy(alpha = 0.25f)
                )
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            ForestDarkCard,
                            ForestDarkCardEnd
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "CẤP ĐỘ ĐÓNG GÓP",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = AccentEmerald
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Nhà khám phá Wi-Fi",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Thêm 3 lượt đo để lên cấp tiếp theo",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.65f)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = "04",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.16f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(AccentEmerald)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Thẻ Thống kê 3 Cột (12 Lượt đo | 8 Địa điểm | 2.4k Hữu ích)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 3.dp,
                    shape = RoundedCornerShape(22.dp),
                    ambientColor = Color.Black.copy(alpha = 0.03f),
                    spotColor = Color.Black.copy(alpha = 0.05f)
                )
                .clip(RoundedCornerShape(22.dp))
                .background(SurfaceWhite)
                .border(1.dp, CardBorder, RoundedCornerShape(22.dp))
                .padding(vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfileStatColumn(
                value = "$totalMeasurements",
                label = "Lượt đo",
                modifier = Modifier.weight(1f)
            )
            VerticalStatDivider()
            ProfileStatColumn(
                value = "8",
                label = "Địa điểm",
                modifier = Modifier.weight(1f)
            )
            VerticalStatDivider()
            ProfileStatColumn(
                value = "2.4k",
                label = "Hữu ích",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(26.dp))

        // 5. Danh sách "Cài đặt"
        Text(
            text = "Cài đặt",
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(10.dp))

        ProfileSettingRow(
            icon = Icons.Outlined.LocationOn,
            title = "Quyền vị trí",
            subtitle = "Luôn cho phép",
            onClick = onOpenAppSettings
        )
        SettingRowDivider()

        ProfileSettingRow(
            icon = Icons.Outlined.Notifications,
            title = "Thông báo",
            subtitle = if (notificationsEnabled) "Đang bật" else "Đang tắt",
            onClick = { notificationsEnabled = !notificationsEnabled }
        )
        SettingRowDivider()

        ProfileSettingRow(
            icon = Icons.Outlined.WorkOutline,
            title = "Nhu cầu mặc định",
            subtitle = selectedNeed.label,
            onClick = onCycleNeed
        )
        SettingRowDivider()

        ProfileSettingRow(
            icon = Icons.Outlined.PersonOutline,
            title = "Quyền riêng tư",
            subtitle = null,
            onClick = {}
        )
        SettingRowDivider()
    }
}

@Composable
private fun ProfileStatColumn(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextMuted
        )
    }
}

@Composable
private fun VerticalStatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(34.dp)
            .background(DividerLight)
    )
}

@Composable
private fun ProfileSettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(LightGreenSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = PrimaryGreen,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun SettingRowDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(DividerLight)
    )
}
