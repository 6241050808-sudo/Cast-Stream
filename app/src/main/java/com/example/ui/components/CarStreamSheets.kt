package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Input
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
 * Fixes two well-known limitations in `thekirankumar/carstream-android-auto`:
 * 1. Missing `@`, `.`, `_`, `-` and domain tokens on the in-car keyboard when signing into accounts.
 * 2. Input fields getting wiped when pressing Enter on the head unit keyboard (provides both
 *    "Open URL / Search" and "Inject Directly into Focused Web Input without Reload").
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CarRotaryKeyboardSheet(
    currentInput: String,
    onAppendChar: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onSubmitUrlOrSearch: () -> Unit,
    onInjectIntoWebField: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isThaiKeyboard by remember { mutableStateOf(false) }

    val quickSnippets = listOf(
        "@", ".com", "https://", "youtube.com", "plex.tv", "@gmail.com", "1080p", "live"
    )

    val englishRows = listOf(
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
        listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
        listOf("a", "s", "d", "f", "g", "h", "j", "k", "l", "@"),
        listOf("z", "x", "c", "v", "b", "n", "m", ".", "-", "_")
    )

    val thaiRows = listOf(
        listOf("ๆ", "ไ", "ำ", "พ", "ะ", "ั", "ี", "ร", "น", "ย"),
        listOf("ฟ", "ห", "ก", "ด", "เ", "้", "่", "า", "ส", "ว"),
        listOf("ผ", "ป", "แ", "อ", "ิ", "ื", "ท", "ม", "ใ", "ฝ"),
        listOf("ง", "ช", "ค", "ต", "จ", "ข", "ล", "บ", "@", ".")
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CarbonSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "แป้นพิมพ์ในรถ CarStream (Rotary & Touch)",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "แก้ปัญหาปุ่ม @ หาย และแก้บั๊กกด Enter แล้วช่องล็อกอินถูกล้างค่า",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ElectricCyan
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "ปิดแป้นพิมพ์")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Input Preview Box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CockpitCard)
                    .border(1.dp, ElectricCyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = currentInput.ifEmpty { "พิมพ์คำค้นหา, อีเมล (@) หรือ URL..." },
                    style = MaterialTheme.typography.titleMedium,
                    color = if (currentInput.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = onBackspace,
                        modifier = Modifier.size(44.dp).testTag("rotary_key_backspace")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Backspace,
                            contentDescription = "ลบทีละตัว",
                            tint = CrimsonStream
                        )
                    }
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier.size(44.dp).testTag("rotary_key_clear")
                    ) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = "ล้างทั้งหมด",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Domain & Symbol Tokens (Addresses missing @ and .com issue)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                quickSnippets.forEach { token ->
                    Surface(
                        onClick = { onAppendChar(token) },
                        shape = RoundedCornerShape(10.dp),
                        color = ElectricCyan.copy(alpha = 0.14f),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        ) {
                            Text(
                                text = token,
                                style = MaterialTheme.typography.labelLarge,
                                color = ElectricCyan
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Keyboard Grid with large car-safe buttons
            val activeRows = if (isThaiKeyboard) thaiRows else englishRows
            activeRows.forEach { rowKeys ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    rowKeys.forEach { keyLabel ->
                        Surface(
                            onClick = { onAppendChar(keyLabel) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (keyLabel == "@" || keyLabel == ".") {
                                CrimsonStream.copy(alpha = 0.25f)
                            } else {
                                CockpitCard
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = keyLabel,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (keyLabel == "@" || keyLabel == ".") CrimsonStream else Color.White
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Spacebar + Language Toggle + Submit Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { isThaiKeyboard = !isThaiKeyboard },
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isThaiKeyboard) "EN" else "ไทย")
                }

                OutlinedButton(
                    onClick = { onAppendChar(" ") },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.SpaceBar, contentDescription = "เว้นวรรค")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SPACE")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dual Action Buttons: Safe Field Injection vs Direct Search/URL
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onInjectIntoWebField,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("btn_inject_web_input")
                ) {
                    Icon(Icons.Default.Input, contentDescription = null, tint = EmeraldTelemetry)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ส่งเข้าช่องกรอกเว็บ",
                        color = EmeraldTelemetry,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onSubmitUrlOrSearch,
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonStream),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("btn_submit_rotary_search")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ค้นหา / เปิดลิงก์",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AspectRatioCalibratorSheet(
    settings: CarStreamSettingsEntity,
    onSelectAspect: (String, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var zoomSlider by remember(settings.customZoomPercent) {
        mutableStateOf(settings.customZoomPercent.toFloat())
    }

    val modes = listOf(
        Triple("FIT_16_9", "16:9 มาตรฐาน (Fit)", "คงสัดส่วนต้นฉบับไม่ให้ภาพยืดเบี้ยว"),
        Triple("ULTRAWIDE_21_9", "21:9 จอกว้างในรถ (Ultrawide)", "ขยายเต็มจอกว้าง Coolwalk ลดขอบดำซ้าย-ขวา"),
        Triple("STRETCH_FULL", "เต็มหน้าจอ (Full Stretch)", "ดึงภาพเต็มกรอบแสดงผลของเครื่องเสียงรถยนต์"),
        Triple("ZOOM_115", "ซูม 115% (Cinema Crop)", "ซูมตัดขอบดำบน-ล่างสำหรับภาพยนตร์"),
        Triple("ZOOM_130", "ซูม 130% (Deep Zoom)", "ซูมขยายพิเศษสำหรับหน้าจอรถขนาดเล็ก")
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CarbonSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = "ปรับสัดส่วนภาพ & แก้ภาพยืดบนจอรถ (Aspect Ratio)",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "แก้ปัญหา Android Auto ดึงสัดส่วนภาพเพี้ยนหรือมีขอบดำบนหน้าจอ Ultrawide",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            modes.forEach { (key, title, desc) ->
                val selected = settings.aspectRatioMode == key
                Surface(
                    onClick = { onSelectAspect(key, zoomSlider.toInt()) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (selected) ElectricCyan.copy(alpha = 0.16f) else CockpitCard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .border(
                            width = 1.dp,
                            color = if (selected) ElectricCyan else Color.Transparent,
                            shape = RoundedCornerShape(14.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (selected) ElectricCyan else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (selected) {
                            Text(
                                text = "ใช้อยู่",
                                style = MaterialTheme.typography.labelLarge,
                                color = ElectricCyan
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ซูมละเอียด (Custom Frame Scale)",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "${zoomSlider.toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = ElectricCyan
                )
            }
            Slider(
                value = zoomSlider,
                onValueChange = {
                    zoomSlider = it
                    onSelectAspect(settings.aspectRatioMode, it.toInt())
                },
                valueRange = 85f..145f,
                colors = SliderDefaults.colors(
                    thumbColor = ElectricCyan,
                    activeTrackColor = ElectricCyan
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioSyncCalibratorSheet(
    settings: CarStreamSettingsEntity,
    onUpdateOffset: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var offsetSlider by remember(settings.audioOffsetMs) {
        mutableStateOf(settings.audioOffsetMs.toFloat())
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CarbonSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = "ซิงค์เสียงและภาพ (Bluetooth / Wireless AA A/V Sync)",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "แก้ปัญหาเสียงพูดไม่ตรงปากใน CarStream เมื่อเชื่อมต่อ Android Auto ไร้สาย",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ค่าชดเชยความหน่วงเสียง:",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${if (offsetSlider.toInt() >= 0) "+" else ""}${offsetSlider.toInt()} ms",
                    style = MaterialTheme.typography.headlineMedium,
                    color = EmeraldTelemetry
                )
            }

            Slider(
                value = offsetSlider,
                onValueChange = {
                    offsetSlider = it
                    onUpdateOffset(it.toInt())
                },
                valueRange = -400f..400f,
                steps = 15,
                colors = SliderDefaults.colors(
                    thumbColor = EmeraldTelemetry,
                    activeTrackColor = EmeraldTelemetry
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(-200, -100, 0, 100, 200).forEach { preset ->
                    FilterChip(
                        selected = settings.audioOffsetMs == preset,
                        onClick = {
                            offsetSlider = preset.toFloat()
                            onUpdateOffset(preset)
                        },
                        label = {
                            Text("${if (preset > 0) "+" else ""}${preset}ms")
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldTelemetry.copy(alpha = 0.2f),
                            selectedLabelColor = EmeraldTelemetry
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
