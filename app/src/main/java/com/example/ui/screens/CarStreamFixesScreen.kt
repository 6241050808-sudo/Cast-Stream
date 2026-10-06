package com.example.ui.screens

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.CarStreamSettingsEntity
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CockpitCard
import com.example.ui.theme.CrimsonStream
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldTelemetry

/**
 * Dedicated screen showing how every limitation & condition from `thekirankumar/carstream-android-auto`
 * has been engineered and resolved, with interactive toggles and live calibration controls.
 */
@Composable
fun CarStreamFixesScreen(
    settings: CarStreamSettingsEntity,
    onUpdateUserAgent: (String) -> Unit,
    onToggleAdShield: (Boolean) -> Unit,
    onToggleSponsorSkip: (Boolean) -> Unit,
    onToggleBackgroundKeepAlive: (Boolean) -> Unit,
    onToggleRotaryHighlight: (Boolean) -> Unit,
    onOpenAspectCalibrator: () -> Unit,
    onOpenAudioSyncCalibrator: () -> Unit,
    onOpenRotaryKeyboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("engine_fixes_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CockpitCard)
                    .border(1.dp, EmeraldTelemetry.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = EmeraldTelemetry,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CARSTREAM ENGINE DIAGNOSTICS & UPGRADES",
                        style = MaterialTheme.typography.labelLarge,
                        color = EmeraldTelemetry
                    )
                }
                Text(
                    text = "ระบบปรับปรุงข้อจำกัดจากโค้ด CarStream ต้นฉบับ",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "ตรวจพบอุปกรณ์ Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT}) • สถานะ MediaBrowserService & Projection พร้อมทำงานโดยไม่ติดบล็อก 'Media unavailable'",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 1. User-Agent Spoofing Engine (Fixes Desktop vs Mobile vs TV playback restrictions)
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = CarbonSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Devices, contentDescription = null, tint = ElectricCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "1. โหมดจำลองเบราว์เซอร์ (Multi-Profile User-Agent)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "แก้ข้อจำกัดของ WebView เดิมที่บางเว็บไซต์บังคับให้เปิดแอปหรือบล็อกความละเอียด 1080p",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "CHROME_DESKTOP" to "Desktop 1080p",
                            "MOBILE_ANDROID" to "Mobile Touch",
                            "SMART_TV" to "Smart TV UI"
                        ).forEach { (key, label) ->
                            FilterChip(
                                selected = settings.userAgentMode == key,
                                onClick = { onUpdateUserAgent(key) },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = ElectricCyan
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // 2. Quick Calibration Launchers for Original CarStream Issues
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LimitationFixActionCard(
                    title = "แก้ภาพยืดบนจอรถ",
                    subtitle = "โหมดปัจจุบัน: ${settings.aspectRatioMode}",
                    buttonLabel = "ตั้งค่าสัดส่วนภาพ",
                    icon = Icons.Default.AspectRatio,
                    tint = ElectricCyan,
                    onClick = onOpenAspectCalibrator,
                    modifier = Modifier.weight(1f)
                )
                LimitationFixActionCard(
                    title = "แก้เสียงดีเลย์ (A/V Sync)",
                    subtitle = "ชดเชย: ${settings.audioOffsetMs} ms",
                    buttonLabel = "จูนความหน่วงเสียง",
                    icon = Icons.Default.GraphicEq,
                    tint = EmeraldTelemetry,
                    onClick = onOpenAudioSyncCalibrator,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 3. In-Car Keyboard Fix Card
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = CarbonSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Keyboard, contentDescription = null, tint = Color(0xFFFBBF24))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "แป้นพิมพ์ในรถ (แก้บั๊กปุ่ม @ หาย & ช่องล็อกอินถูกล้าง)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "มาพร้อมปุ่มลัด @, .com และระบบส่งข้อความเข้าช่อง Input ของเว็บโดยไม่รีโหลดหน้า",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = onOpenRotaryKeyboard,
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonStream)
                    ) {
                        Text("ทดสอบแป้นพิมพ์")
                    }
                }
            }
        }

        // 4. Engine Switches (Ad-Shield, Background Keep-Alive, Rotary Controller)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(CarbonSurface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = CrimsonStream)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ระบบเสริมความเสถียรขณะขับขี่",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                EngineToggleRow(
                    title = "Ad-Shield & Auto-Skip โฆษณา",
                    subtitle = "ซ่อนแบนเนอร์โฆษณาและกดปุ่มข้ามโฆษณาอัตโนมัติเพื่อไม่ให้ต้องละมือจากพวงมาลัย",
                    checked = settings.adShieldEnabled,
                    onCheckedChange = onToggleAdShield
                )

                EngineToggleRow(
                    title = "ป้องกันเสียงตัดเมื่อสลับไปดูแผนที่ (Audio Keep-Alive)",
                    subtitle = "เชื่อม MediaSession กับ Android Auto เพื่อเล่นเสียงต่อเนื่องขณะเปิด Google Maps",
                    checked = settings.backgroundAudioKeepAlive,
                    onCheckedChange = onToggleBackgroundKeepAlive
                )

                EngineToggleRow(
                    title = "แถบควบคุมปุ่มหมุนคอนโซลรถ (Rotary / Non-Touchscreen HUD)",
                    subtitle = "แสดงปุ่มเลื่อนซ้าย-ขวาและกรอบไฮไลต์สีฟ้าสำหรับรถที่ไม่มีหน้าจอสัมผัส",
                    checked = settings.rotaryDpadHighlightEnabled,
                    onCheckedChange = onToggleRotaryHighlight
                )

                EngineToggleRow(
                    title = "ข้ามช่วงแนะนำคลิปอัตโนมัติ (Sponsor Skip Hint)",
                    subtitle = "ช่วยให้เข้าถึงเนื้อหาหลักของวิดีโอได้รวดเร็วขึ้น",
                    checked = settings.sponsorSkipHintEnabled,
                    onCheckedChange = onToggleSponsorSkip
                )
            }
        }
    }
}

@Composable
private fun LimitationFixActionCard(
    title: String,
    subtitle: String,
    buttonLabel: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = CarbonSurface,
        modifier = modifier.border(1.dp, tint.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint)
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = tint.copy(alpha = 0.2f),
                    contentColor = tint
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(buttonLabel, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EngineToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = EmeraldTelemetry
            )
        )
    }
}
