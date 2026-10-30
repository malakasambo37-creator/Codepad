package com.example.ui.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CodeDocument
import com.example.data.model.CodeLanguage
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ExplorerPanelContent(
    documents: List<CodeDocument>,
    activeDocId: String?,
    dirtyDocIds: Set<String>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSelectDocument: (CodeDocument) -> Unit,
    onTogglePin: (CodeDocument) -> Unit,
    onRequestDelete: (CodeDocument) -> Unit,
    onNewDocumentClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filtered = remember(documents, searchQuery) {
        if (searchQuery.isBlank()) {
            documents
        } else {
            val q = searchQuery.trim()
            documents.filter {
                it.title.contains(q, ignoreCase = true) ||
                    it.content.contains(q, ignoreCase = true)
            }
        }
    }
    val pinnedDocs = remember(filtered) { filtered.filter { it.isPinned } }
    val unpinnedDocs = remember(filtered) { filtered.filterNot { it.isPinned } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.explorer_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            FilledTonalIconButton(
                onClick = onNewDocumentClick,
                modifier = Modifier.testTag("explorer_new_doc_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.cd_new_document)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            },
            placeholder = {
                Text(
                    text = stringResource(R.string.explorer_search_hint),
                    style = MaterialTheme.typography.bodySmall
                )
            },
            textStyle = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("explorer_search_input")
        )

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(8.dp))

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.explorer_empty_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.explorer_empty_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                if (pinnedDocs.isNotEmpty()) {
                    item(key = "header_pinned") {
                        Text(
                            text = stringResource(R.string.explorer_section_pinned),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(pinnedDocs, key = { it.id }) { doc ->
                        ExplorerDocCard(
                            doc = doc,
                            isSelected = doc.id == activeDocId,
                            isDirty = doc.id in dirtyDocIds,
                            onClick = { onSelectDocument(doc) },
                            onTogglePin = { onTogglePin(doc) },
                            onDelete = { onRequestDelete(doc) }
                        )
                    }
                }

                if (unpinnedDocs.isNotEmpty()) {
                    item(key = "header_all") {
                        Text(
                            text = stringResource(R.string.explorer_section_all),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
                        )
                    }
                    items(unpinnedDocs, key = { it.id }) { doc ->
                        ExplorerDocCard(
                            doc = doc,
                            isSelected = doc.id == activeDocId,
                            isDirty = doc.id in dirtyDocIds,
                            onClick = { onSelectDocument(doc) },
                            onTogglePin = { onTogglePin(doc) },
                            onDelete = { onRequestDelete(doc) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExplorerDocCard(
    doc: CodeDocument,
    isSelected: Boolean,
    isDirty: Boolean,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit
) {
    val language = CodeLanguage.fromId(doc.language)
    val displayTime = remember(doc.updatedAt, doc.createdAt) {
        val ts = doc.updatedAt ?: doc.createdAt ?: Timestamp.now()
        SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()).format(ts.toDate())
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            }
        ),
        border = if (isSelected) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("explorer_doc_item_${doc.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = language.badgeColor.copy(alpha = 0.2f)
            ) {
                Text(
                    text = language.shortBadge,
                    style = MaterialTheme.typography.labelSmall,
                    color = language.badgeColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = doc.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isDirty) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(MaterialTheme.colorScheme.tertiary, CircleShape)
                        )
                    }
                }
                Text(
                    text = "${language.displayName} • $displayTime",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            IconButton(
                onClick = onTogglePin,
                modifier = Modifier.testTag("pin_doc_button_${doc.id}")
            ) {
                Icon(
                    imageVector = if (doc.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                    contentDescription = stringResource(R.string.cd_pin_document),
                    tint = if (doc.isPinned) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_doc_button_${doc.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = stringResource(R.string.cd_delete_document),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmptyWorkspaceView(
    onCreateBlank: () -> Unit,
    onOpenLocalFile: () -> Unit,
    onCreateTemplate: (String, CodeLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    val quickTemplates = remember {
        listOf(
            "main.kt" to CodeLanguage.KOTLIN,
            "script.py" to CodeLanguage.PYTHON,
            "app.js" to CodeLanguage.JAVASCRIPT,
            "index.html" to CodeLanguage.HTML,
            "main.cpp" to CodeLanguage.CPP,
            "config.json" to CodeLanguage.JSON,
            "query.sql" to CodeLanguage.SQL,
            "README.md" to CodeLanguage.MARKDOWN
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 540.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.empty_workspace_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.empty_workspace_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
            ) {
                Button(
                    onClick = onCreateBlank,
                    modifier = Modifier
                        .height(50.dp)
                        .testTag("empty_new_blank_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_new_blank_file))
                }

                OutlinedButton(
                    onClick = onOpenLocalFile,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .height(50.dp)
                        .testTag("empty_open_local_button")
                ) {
                    Icon(
                        Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_open_device_file))
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = stringResource(R.string.section_quick_templates),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickTemplates.forEach { (fileName, lang) ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .clickable { onCreateTemplate(fileName, lang) }
                            .testTag("quick_template_${lang.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(lang.badgeColor, CircleShape)
                            )
                            Text(
                                text = fileName,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
