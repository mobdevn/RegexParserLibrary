@file:OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalTextApi::class,
    ExperimentalMaterial3Api::class
)

package com.example.sequentialpagecapture

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSpacing
import androidx.compose.ui.text.font.FontWeight

@Composable
internal fun SequentialPageHeaders(
    pageViewState: SequentialPageState
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = spacedBy(LocalSpacing.current.medium)
    ) {
        with(pageViewState.sequentialPageSections) {
            this?.heading.takeUnless { it.isNullOrEmpty() }?.let { label ->
                BlueprintText(
                    modifier = Modifier
                        .padding(top = LocalSpacing.current.large)
                        .semantics { heading() },
                    style = LocalTypography.current.large,
                    text = label,
                    color = BlueprintTheme.colors.title,
                    fontWeight = FontWeight.Medium,
                )
            }

            this?.subheading.takeUnless { it.isNullOrEmpty() }?.let { label ->
                BlueprintText(text = label)
            }
        }
    }
}
