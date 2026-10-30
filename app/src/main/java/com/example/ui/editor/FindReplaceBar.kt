package com.example.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R

@Composable
fun FindReplaceBar(
    state: FindReplaceState,
    onFindQueryChange: (String) -> Unit,
    onReplaceQueryChange: (String) -> Unit,
    onToggleMatchCase: () -> Unit,
    onToggleRegex: () -> Unit,
    onFindPrevious: () -> Unit,
    onFindNext: () -> Unit,
    onReplaceOne: () -> Unit,
    onReplaceAll: () -> Unit,
    onClose: () -> Unit
) {
    AnimatedVisibility(visible = state.isVisible) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("find_replace_bar")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                        },
                        textStyle = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("find_query_input")
                    )

                    IconButton(
                        onClick = onFindPrevious,
                        enabled = state.matches.isNotEmpty(),
                        modifier = Modifier.testTag("find_prev_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = stringResource(R.string.cd_find_prev)
                        )
                    }

                    IconButton(
                        onClick = onFindNext,
                        enabled = state.matches.isNotEmpty(),
                        modifier = Modifier.testTag("find_next_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.cd_find_next)
                        )
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("find_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.cd_close_find),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                        textStyle = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("replace_query_input")
                    )

                    FilledTonalButton(
                        onClick = onReplaceOne,
                        enabled = state.matches.isNotEmpty(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("replace_one_button")
                    ) {
                        Text(
                            text = stringResource(R.string.btn_replace_one),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    OutlinedButton(
                        onClick = onReplaceAll,
                        enabled = state.matches.isNotEmpty(),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("replace_all_button")
                    ) {
                        Text(
                            text = stringResource(R.string.btn_replace_all),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = state.matchCase,
                        onClick = onToggleMatchCase,
                        label = { Text("Aa Case", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.testTag("toggle_match_case_chip")
                    )
                    FilterChip(
                        selected = state.useRegex,
                        onClick = onToggleRegex,
                        label = { Text(".* Regex", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.testTag("toggle_regex_chip")
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
        }
    }
}
