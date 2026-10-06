package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.BookmarkEntity
import com.example.data.HistoryEntity
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CockpitCard
import com.example.ui.theme.CrimsonStream
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldTelemetry

@Composable
fun BookmarksAndHistoryScreen(
    bookmarks: List<BookmarkEntity>,
    history: List<HistoryEntity>,
    showAddDialog: Boolean,
    onOpenAddDialog: (Boolean) -> Unit,
    onAddBookmark: (String, String, String) -> Unit,
    onDeleteBookmark: (Int) -> Unit,
    onSelectBookmark: (BookmarkEntity) -> Unit,
    onSelectHistory: (HistoryEntity) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("bookmarks_history_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header + Add Bookmark Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "คลังบุ๊กมาร์ก & ประวัติการเล่น",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "รายการโปรดจะซิงค์ขึ้นหน้าจอ Android Auto Coolwalk อัตโนมัติ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = { onOpenAddDialog(true) },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonStream),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("btn_open_add_bookmark_dialog")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("เพิ่มบุ๊กมาร์ก")
                }
            }
        }

        // Bookmarks Section
        item {
            Text(
                text = "บุ๊กมาร์กที่ปักหมุด (${bookmarks.size})",
                style = MaterialTheme.typography.titleLarge,
                color = ElectricCyan
            )
        }

        items(bookmarks, key = { "bm_${it.id}" }) { item ->
            Surface(
                onClick = { onSelectBookmark(item) },
                shape = RoundedCornerShape(16.dp),
                color = CockpitCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(CrimsonStream.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = null,
                                tint = CrimsonStream
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = item.url,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onSelectBookmark(item) }) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "เล่นบุ๊กมาร์กนี้",
                                tint = ElectricCyan
                            )
                        }
                        IconButton(onClick = { onDeleteBookmark(item.id) }) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "ลบบุ๊กมาร์ก",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Playback History Section
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ประวัติการเล่นล่าสุด (${history.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = EmeraldTelemetry
                )
                if (history.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onClearHistory,
                        modifier = Modifier.testTag("btn_clear_history")
                    ) {
                        Text("ล้างประวัติ")
                    }
                }
            }
        }

        if (history.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CockpitCard)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ยังไม่มีประวัติการสตรีม เลือกช่องหรือค้นหาวิดีโอเพื่อเริ่มเล่นได้เลย",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(history, key = { "hist_${it.id}" }) { hist ->
                Surface(
                    onClick = { onSelectHistory(hist) },
                    shape = RoundedCornerShape(14.dp),
                    color = CarbonSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (hist.sourceType == "LOCAL_VIDEO") Icons.Default.VideoFile else Icons.Default.History,
                            contentDescription = null,
                            tint = EmeraldTelemetry
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = hist.title,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = hist.url,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "เล่นต่อ",
                            style = MaterialTheme.typography.labelLarge,
                            color = ElectricCyan
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddBookmarkModalDialog(
            onDismiss = { onOpenAddDialog(false) },
            onConfirm = onAddBookmark
        )
    }
}

@Composable
private fun AddBookmarkModalDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("https://") }
    var category by remember { mutableStateOf("YOUTUBE") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CarbonSurface,
        title = {
            Text("เพิ่มช่องสตรีม / บุ๊กมาร์กใหม่", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("ชื่อช่อง (เช่น YouTube Live, Plex)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_bookmark_title")
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL หรือคำค้นหา") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_bookmark_url")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("YOUTUBE", "PLEX", "STREAM", "WEB").forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (url.isNotBlank()) {
                        onConfirm(title.ifBlank { url }, url, category)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonStream),
                modifier = Modifier.testTag("btn_confirm_add_bookmark")
            ) {
                Text("บันทึก")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("ยกเลิก")
            }
        }
    )
}
