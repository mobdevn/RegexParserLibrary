@file:OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalTextApi::class,
    ExperimentalMaterial3Api::class
)

package com.example.sequentialpagecapture

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration

@Composable
internal fun SequentialPageHyperLink(
    modifier: Modifier = Modifier,
    item: SequentialPageContent,
    onEventHandler: (SequentialPageEvent, String?) -> Unit
) {
    BlueprintText(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { role = Button }
            .clickable(
                onClick = {
                    onEventHandler(HyperlinkClicked, null)
                }
            )
            .then(modifier),
        style = LocalTypography.current.medium,
        text = item.hyperlinkText ?: DEFAULT_TEXT_LABEL,
        color = BlueprintTheme.colors.interactive,
        fontWeight = FontWeight.W500,
        fontSize = BlueprintFontSize.FontSizeMedium,
        textDecoration = TextDecoration.Underline,
        textAlign = TextAlign.Center
    )
}
