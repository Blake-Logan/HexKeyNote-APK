package com.hexkeynote.app.ui

import android.content.Context
import android.os.Environment
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoveDown
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hexkeynote.app.ui.theme.HexKeyNoteTheme
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

private const val STORAGE_DIR_NAME = "HexKeyNote"
private const val TREE_FILE_NAME = "notes_tree.json"

private data class AppFolder(
    val id: String,
    val name: String,
    val parentId: String?,
)

private data class AppNote(
    val id: String,
    val title: String,
    val content: String,
    val folderId: String,
    val createdAt: Long,
    val updatedAt: Long,
)

private data class NoteTreeState(
    val folders: List<AppFolder>,
    val notes: List<AppNote>,
)

private sealed interface TreeRow {
    data class FolderRow(
        val folder: AppFolder,
        val depth: Int,
    ) : TreeRow

    data class NoteRow(
        val note: AppNote,
        val depth: Int,
    ) : TreeRow
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HexKeyNoteApp() {
    val context = LocalContext.current
    val initialTree = remember { defaultNoteTree() }
    var treeState by remember { mutableStateOf(initialTree) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedFolderId by remember { mutableStateOf(initialTree.folders.first().id) }
    var selectedNoteId by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var showMoveDialog by remember { mutableStateOf(false) }
    var moveTargetFolderId by remember { mutableStateOf<String?>(null) }

    val storagePath = remember(context) { resolveStorageDirectory(context) }

    LaunchedEffect(Unit) {
        delay(900)
        treeState = loadNoteTree(context)
        val rootId = treeState.folders.firstOrNull()?.id ?: "root"
        selectedFolderId = rootId
        val firstNote = treeState.notes.firstOrNull { it.folderId == rootId }
        selectedNoteId = firstNote?.id
        isLoading = false
    }

    val rows = remember(treeState, selectedFolderId) {
        buildTreeRows(treeState.folders, treeState.notes, selectedFolderId)
    }
    val selectedNote = remember(treeState.notes, selectedNoteId) {
        treeState.notes.firstOrNull { it.id == selectedNoteId }
    }
    val activeFolder = remember(treeState.folders, selectedFolderId) {
        treeState.folders.firstOrNull { it.id == selectedFolderId }
    }

    val visibleNotes = remember(treeState.notes, selectedFolderId, searchQuery.text) {
        treeState.notes.filter { note ->
            val matchesFolder = note.folderId == selectedFolderId
            val matchesSearch = searchQuery.text.isBlank() ||
                note.title.contains(searchQuery.text, ignoreCase = true) ||
                note.content.contains(searchQuery.text, ignoreCase = true)
            matchesFolder && matchesSearch
        }
    }

    fun updateSelectedNote(title: String, content: String) {
        val current = selectedNote ?: return
        treeState = treeState.copy(
            notes = treeState.notes.map { note ->
                if (note.id == current.id) {
                    note.copy(
                        title = title,
                        content = content,
                        updatedAt = System.currentTimeMillis(),
                    )
                } else {
                    note
                }
            },
        )
        persistNoteTree(context, treeState)
    }

    fun createNote() {
        val folderId = selectedFolderId
        val newNote = AppNote(
            id = UUID.randomUUID().toString(),
            title = "새 메모",
            content = "여기에 메모를 입력하세요.",
            folderId = folderId,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        )
        treeState = treeState.copy(notes = treeState.notes + newNote)
        selectedNoteId = newNote.id
        persistNoteTree(context, treeState)
    }

    fun deleteCurrentNote() {
        val current = selectedNote ?: return
        treeState = treeState.copy(notes = treeState.notes.filterNot { it.id == current.id })
        selectedNoteId = treeState.notes.firstOrNull { it.folderId == selectedFolderId }?.id
        persistNoteTree(context, treeState)
    }

    fun createFolder() {
        val trimmed = newFolderName.trim()
        if (trimmed.isBlank()) return
        val newFolder = AppFolder(
            id = UUID.randomUUID().toString(),
            name = trimmed,
            parentId = selectedFolderId,
        )
        treeState = treeState.copy(folders = treeState.folders + newFolder)
        selectedFolderId = newFolder.id
        newFolderName = ""
        showNewFolderDialog = false
        persistNoteTree(context, treeState)
    }

    fun moveSelectedNote(targetFolderId: String?) {
        val current = selectedNote ?: return
        if (targetFolderId == null) return
        treeState = treeState.copy(
            notes = treeState.notes.map { note ->
                if (note.id == current.id) {
                    note.copy(folderId = targetFolderId, updatedAt = System.currentTimeMillis())
                } else {
                    note
                }
            },
        )
        selectedFolderId = targetFolderId
        persistNoteTree(context, treeState)
    }

    if (isLoading) {
        LoadingScreen(storagePath = storagePath)
        return
    }

    HexKeyNoteTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.surface,
                            ),
                        ),
                    ),
            ) {
                Scaffold(
                    containerColor = Color.Transparent,
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = androidx.compose.material.icons.Icons.Default.FolderOpen,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "HexKeyNote",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            },
                        )
                    },
                    floatingActionButton = {
                        FloatingActionButton(onClick = { createNote() }) {
                            Icon(Icons.Default.Add, contentDescription = "새 메모")
                        }
                    },
                ) { paddingValues ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Storage,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                    Text("저장 위치", style = MaterialTheme.typography.labelLarge)
                                }
                                Text(
                                    text = storagePath,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Card(
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            text = "폴더 트리",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Button(onClick = { showNewFolderDialog = true }) {
                                            Icon(Icons.Default.CreateNewFolder, contentDescription = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("폴더")
                                        }
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(start = 12.dp),
                                        )
                                        OutlinedTextField(
                                            value = searchQuery,
                                            onValueChange = { searchQuery = it },
                                            placeholder = { Text("검색") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = androidx.compose.material3.TextFieldDefaults.colors(
                                                focusedContainerColor = Color.Transparent,
                                                unfocusedContainerColor = Color.Transparent,
                                                focusedIndicatorColor = Color.Transparent,
                                                unfocusedIndicatorColor = Color.Transparent,
                                            ),
                                        )
                                    }

                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        rows.forEach { row ->
                                            when (row) {
                                                is TreeRow.FolderRow -> {
                                                    val isSelected = row.folder.id == selectedFolderId
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .clickable { selectedFolderId = row.folder.id }
                                                            .background(
                                                                if (isSelected) {
                                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                                                } else {
                                                                    Color.Transparent
                                                                },
                                                            )
                                                            .padding(horizontal = 4.dp, vertical = 2.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                    ) {
                                                        Spacer(modifier = Modifier.width((row.depth * 18).dp))
                                                        Icon(
                                                            imageVector = if (isSelected) Icons.Default.FolderOpen else Icons.Default.Folder,
                                                            contentDescription = null,
                                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = row.folder.name,
                                                            style = MaterialTheme.typography.bodyLarge,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        )
                                                    }
                                                }

                                                is TreeRow.NoteRow -> {
                                                    val isSelected = row.note.id == selectedNoteId
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .clickable {
                                                                selectedNoteId = row.note.id
                                                                selectedFolderId = row.note.folderId
                                                            }
                                                            .background(
                                                                if (isSelected) {
                                                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f)
                                                                } else {
                                                                    Color.Transparent
                                                                },
                                                            )
                                                            .padding(horizontal = 4.dp, vertical = 2.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                    ) {
                                                        Spacer(modifier = Modifier.width((row.depth * 18).dp))
                                                        Icon(
                                                            imageVector = Icons.AutoMirrored.Filled.Note,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.size(18.dp),
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = row.note.title,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Card(
                                modifier = Modifier.weight(2f).fillMaxHeight(),
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            ) {
                                if (selectedNote != null) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(18.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp),
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                text = activeFolder?.name ?: "메모",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(onClick = { showMoveDialog = true }) {
                                                    Icon(Icons.Default.MoveDown, contentDescription = null)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("이동")
                                                }
                                                Button(
                                                    onClick = { deleteCurrentNote() },
                                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                                    ),
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = null)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("삭제")
                                                }
                                            }
                                        }

                                        OutlinedTextField(
                                            value = selectedNote.title,
                                            onValueChange = { title ->
                                                treeState = treeState.copy(
                                                    notes = treeState.notes.map { note ->
                                                        if (note.id == selectedNote.id) {
                                                            note.copy(title = title, updatedAt = System.currentTimeMillis())
                                                        } else {
                                                            note
                                                        }
                                                    },
                                                )
                                                persistNoteTree(context, treeState)
                                            },
                                            label = { Text("제목") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                        )

                                        OutlinedTextField(
                                            value = selectedNote.content,
                                            onValueChange = { content ->
                                                treeState = treeState.copy(
                                                    notes = treeState.notes.map { note ->
                                                        if (note.id == selectedNote.id) {
                                                            note.copy(content = content, updatedAt = System.currentTimeMillis())
                                                        } else {
                                                            note
                                                        }
                                                    },
                                                )
                                                persistNoteTree(context, treeState)
                                            },
                                            label = { Text("내용") },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f),
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                        ) {
                                            Text(
                                                text = "마지막 수정: ${formatTime(selectedNote.updatedAt)}",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                style = MaterialTheme.typography.bodySmall,
                                            )
                                        }
                                    }
                                } else {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("메모를 선택해 주세요.")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("새 폴더 만들기") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("폴더 이름") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(onClick = { createFolder() }) {
                    Text("추가")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) {
                    Text("취소")
                }
            },
        )
    }

    if (showMoveDialog && selectedNote != null) {
        val targets = treeState.folders.filter { it.id != selectedNote.folderId }
        AlertDialog(
            onDismissRequest = { showMoveDialog = false },
            title = { Text("메모 이동") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    targets.forEach { folder ->
                        TextButton(
                            onClick = {
                                moveTargetFolderId = folder.id
                                moveSelectedNote(folder.id)
                                showMoveDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(folder.name)
                        }
                    }
                }
            },
            confirmButton = {},
        )
    }
}

@Composable
private fun LoadingScreen(storagePath: String) {
    HexKeyNoteTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "HexKeyNote를 준비하는 중...",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = storagePath,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

private fun resolveStorageDirectory(context: Context): String {
    val root = File(Environment.getExternalStorageDirectory(), STORAGE_DIR_NAME)
    root.mkdirs()
    return root.absolutePath
}

private fun defaultNoteTree(): NoteTreeState {
    val rootId = "root"
    val workId = "work"
    val personalId = "personal"
    val now = System.currentTimeMillis()
    val rootFolder = AppFolder(rootId, "모든 메모", null)
    val workFolder = AppFolder(workId, "업무", rootId)
    val personalFolder = AppFolder(personalId, "개인", rootId)
    val notes = listOf(
        AppNote(UUID.randomUUID().toString(), "오늘 할 일", "- 회의 준비\n- 가입자 업데이트\n- 저장소 점검", rootId, now - 2_000, now - 1_000),
        AppNote(UUID.randomUUID().toString(), "기획 메모", "사용자는 폴더와 노트 이동이 매우 직관적이어야 한다. 데이터는 외부 저장소에 안전하게 저장한다.", workId, now - 10_000, now - 8_000),
        AppNote(UUID.randomUUID().toString(), "개인 일정", "운동, 장보기, 저녁 약속", personalId, now - 30_000, now - 20_000),
    )
    return NoteTreeState(folders = listOf(rootFolder, workFolder, personalFolder), notes = notes)
}

private fun buildTreeRows(
    folders: List<AppFolder>,
    notes: List<AppNote>,
    selectedFolderId: String,
): List<TreeRow> {
    val rows = mutableListOf<TreeRow>()
    val folderByParent = folders.groupBy { it.parentId ?: "__root__" }

    fun appendFolder(folder: AppFolder, depth: Int) {
        rows += TreeRow.FolderRow(folder, depth)
        val folderNotes = notes.filter { it.folderId == folder.id }
            .sortedByDescending { it.updatedAt }
        folderNotes.forEach { note -> rows += TreeRow.NoteRow(note, depth + 1) }
        val children = (folderByParent[folder.id] ?: emptyList())
            .sortedBy { it.name.lowercase() }
        children.forEach { appendFolder(it, depth + 1) }
    }

    val root = folders.firstOrNull { it.id == selectedFolderId } ?: folders.firstOrNull() ?: return rows
    val rootFamily = folderByParent[root.parentId ?: "__root__"]
    if (root.parentId == null) {
        folders.filter { it.parentId == null }
            .sortedBy { it.name.lowercase() }
            .forEach { appendFolder(it, 0) }
    } else {
        appendFolder(root, 0)
    }
    return rows
}

private fun persistNoteTree(context: Context, treeState: NoteTreeState) {
    try {
        val file = File(resolveStorageDirectory(context), TREE_FILE_NAME)
        val root = JSONObject()
        val folderArray = JSONArray()
        treeState.folders.forEach { folder ->
            val item = JSONObject()
            item.put("id", folder.id)
            item.put("name", folder.name)
            item.put("parentId", folder.parentId)
            folderArray.put(item)
        }
        val noteArray = JSONArray()
        treeState.notes.forEach { note ->
            val item = JSONObject()
            item.put("id", note.id)
            item.put("title", note.title)
            item.put("content", note.content)
            item.put("folderId", note.folderId)
            item.put("createdAt", note.createdAt)
            item.put("updatedAt", note.updatedAt)
            noteArray.put(item)
        }
        root.put("folders", folderArray)
        root.put("notes", noteArray)
        file.writeText(root.toString(2), Charsets.UTF_8)
    } catch (_: Throwable) {
    }
}

private fun loadNoteTree(context: Context): NoteTreeState {
    val file = File(resolveStorageDirectory(context), TREE_FILE_NAME)
    if (!file.exists()) {
        val defaultTree = defaultNoteTree()
        persistNoteTree(context, defaultTree)
        return defaultTree
    }

    return try {
        val json = JSONObject(file.readText(Charsets.UTF_8))
        val folders = mutableListOf<AppFolder>()
        val folderArray = json.getJSONArray("folders")
        for (index in 0 until folderArray.length()) {
            val item = folderArray.getJSONObject(index)
            folders += AppFolder(
                id = item.getString("id"),
                name = item.getString("name"),
                parentId = item.optString("parentId", "").ifBlank { null },
            )
        }

        val notes = mutableListOf<AppNote>()
        val noteArray = json.getJSONArray("notes")
        for (index in 0 until noteArray.length()) {
            val item = noteArray.getJSONObject(index)
            notes += AppNote(
                id = item.getString("id"),
                title = item.getString("title"),
                content = item.getString("content"),
                folderId = item.optString("folderId", "root"),
                createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = item.optLong("updatedAt", System.currentTimeMillis()),
            )
        }

        NoteTreeState(folders = folders, notes = notes)
    } catch (_: Throwable) {
        val defaultTree = defaultNoteTree()
        persistNoteTree(context, defaultTree)
        defaultTree
    }
}

private fun formatTime(epochMillis: Long): String {
    val date = java.util.Date(epochMillis)
    return java.text.SimpleDateFormat("yyyy.MM.dd HH:mm", java.util.Locale.getDefault()).format(date)
}
