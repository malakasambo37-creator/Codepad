package com.example.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.SearchMode
import com.example.ui.theme.EditorColorPalette
import com.example.ui.theme.NotepadTabActiveStripe

@Composable
fun FindReplaceBar(
    state: FindReplaceState,
    palette: EditorColorPalette,
    onSelectTab: (Int) -> Unit,
    onFindQueryChange: (String) -> Unit,
    onReplaceQueryChange: (String) -> Unit,
    onToggleMatchCase: () -> Unit,
    onSelectSearchMode: (SearchMode) -> Unit,
    onFindPrevious: () -> Unit,
    onFindNext: () -> Unit,
    onCountMatches: () -> Unit,
    onReplaceOne: () -> Unit,
    onReplaceAll: () -> Unit,
    onMarkAll: () -> Unit,
    onClearMarks: () -> Unit,
    onClose: () -> Unit
) {
    val textColor = if (palette.isDark) Color(0xFFE5E7EB) else Color(0xFF1F2937)
    val borderColor = if (palette.isDark) Color(0xFF3B4048) else Color(0xFFC4C8CE)

    AnimatedVisibility(visible = state.isVisible) {
        Surface(
            color = palette.chromeMenuBackground,
            tonalElevation = 3.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("find_replace_bar")
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Classic Notepad++ Dialog Tabs: Find | Replace | Mark + Close [X]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.chromeToolbarBackground)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val tabTitles = listOf(
                        stringResource(R.string.tab_find),
                        stringResource(R.string.tab_replace),
                        stringResource(R.string.tab_mark)
                    )
                    tabTitles.forEachIndexed { idx, label ->
                        val selected = state.activeTab == idx
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { onSelectTab(idx) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("find_dialog_tab_$idx")
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) textColor else textColor.copy(alpha = 0.65f)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Box(
                                modifier = Modifier
                                    .width(36.dp)
                                    .height(2.5.dp)
                                    .background(if (selected) NotepadTabActiveStripe else Color.Transparent)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(32.dp).testTag("find_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.cd_close_find),
                            tint = textColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HorizontalDivider(color = borderColor)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Find What Input + Prev / Next / Count
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = state.findQuery,
                            onValueChange = onFindQueryChange,
                            singleLine = true,
                            isError = state.hasRegexError,
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.find_hint),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            trailingIcon = {
                                val counterText = when {
                                    state.hasRegexError -> "Regex!"
                                    state.findQuery.isEmpty() -> ""
                                    state.matches.isEmpty() -> "0/0"
                                    else -> "${state.activeMatchIndex + 1}/${state.matches.size}"
                                }
                                if (counterText.isNotEmpty()) {
                                    Text(
                                        text = counterText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (state.hasRegexError) {
                                            MaterialTheme.colorScheme.error
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        },
                                        modifier = Modifier.padding(end = 6.dp)
                                    )
                                }
                            },
                            textStyle = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("find_query_input")
                        )

                        IconButton(
                            onClick = onFindPrevious,
                            enabled = state.matches.isNotEmpty(),
                            modifier = Modifier.size(38.dp).testTag("find_prev_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = stringResource(R.string.cd_find_prev),
                                tint = textColor
                            )
                        }

                        IconButton(
                            onClick = onFindNext,
                            enabled = state.matches.isNotEmpty(),
                            modifier = Modifier.size(38.dp).testTag("find_next_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = stringResource(R.string.cd_find_next),
                                tint = textColor
                            )
                        }

                        if (state.activeTab == 0) {
                            OutlinedButton(
                                onClick = onCountMatches,
                                enabled = state.findQuery.isNotEmpty(),
                                border = BorderStroke(1.dp, borderColor),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.height(38.dp).testTag("find_count_button")
                            ) {
                                Text(stringResource(R.string.btn_count), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    // Tab 1: Replace With Row
                    if (state.activeTab == 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = state.replaceQuery,
                                onValueChange = onReplaceQueryChange,
                                singleLine = true,
                                placeholder = {
                                    Text(
                                        text = stringResource(R.string.replace_hint),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                },
                                textStyle = MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("replace_query_input")
                            )

                            FilledTonalButton(
                                onClick = onReplaceOne,
                                enabled = state.matches.isNotEmpty(),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.height(40.dp).testTag("replace_one_button")
                            ) {
                                Text(stringResource(R.string.btn_replace_one), style = MaterialTheme.typography.labelMedium)
                            }

                            OutlinedButton(
                                onClick = onReplaceAll,
                                enabled = state.matches.isNotEmpty(),
                                border = BorderStroke(1.dp, borderColor),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.height(40.dp).testTag("replace_all_button")
                            ) {
                                Text(stringResource(R.string.btn_replace_all), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    // Tab 2: Mark All & Clear All Marks Row
                    if (state.activeTab == 2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = onMarkAll,
                                enabled = state.findQuery.isNotEmpty(),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(38.dp).testTag("mark_all_button")
                            ) {
                                Text(stringResource(R.string.btn_mark_all), style = MaterialTheme.typography.labelMedium)
                            }
                            OutlinedButton(
                                onClick = onClearMarks,
                                enabled = state.markedRanges.isNotEmpty(),
                                border = BorderStroke(1.dp, borderColor),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(38.dp).testTag("clear_marks_button")
                            ) {
                                Text(stringResource(R.string.btn_clear_marks), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    // Classic Notepad++ Search Mode Box: Match case | Normal | Extended (\n, \t) | Regular expression
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = state.matchCase,
                            onClick = onToggleMatchCase,
                            label = { Text("Match case", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.testTag("toggle_match_case_chip")
                        )

                        SearchModeRadio(
                            label = stringResource(R.string.search_mode_normal),
                            selected = state.searchMode == SearchMode.NORMAL,
                            textColor = textColor,
                            tag = "search_mode_normal",
                            onClick = { onSelectSearchMode(SearchMode.NORMAL) }
                        )
                        SearchModeRadio(
                            label = stringResource(R.string.search_mode_extended),
                            selected = state.searchMode == SearchMode.EXTENDED,
                            textColor = textColor,
                            tag = "search_mode_extended",
                            onClick = { onSelectSearchMode(SearchMode.EXTENDED) }
                        )
                        SearchModeRadio(
                            label = stringResource(R.string.search_mode_regex),
                            selected = state.searchMode == SearchMode.REGEX,
                            textColor = textColor,
                            tag = "toggle_regex_chip",
                            onClick = { onSelectSearchMode(SearchMode.REGEX) }
                        )
                    }
                }

                HorizontalDivider(color = borderColor)
            }
        }
    }
}

@Composable
private fun SearchModeRadio(
    label: String,
    selected: Boolean,
    textColor: Color,
    tag: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(end = 6.dp)
            .testTag(tag)
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            modifier = Modifier.size(28.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
            color = textColor
        )
    }
}
