@file:OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalTextApi::class,
    ExperimentalMaterial3Api::class
)

package com.example.sequentialpagecapture

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun ButtonComposableLayoutView(
    modifier: Modifier = Modifier,
    spcSections: SequentialPageSections?,
    buttonState: Boolean,
    scrollState: ScrollState,
    accessibilityTag: String,
    isDropShadowEnabled: Boolean,
    onNextButtonClicked: (String, String) -> Unit
) {
    spcSections?.buttons?.forEach { button ->
        button.text?.let { label ->
            val descriptionTag = contentDescriptionTagBuilder(
                viewLabel = label,
                selectedText = accessibilityTag,
                isContinueButtonEnabled = buttonState
            )

            val buttonParams = SequentialPageButtonLayoutParams(
                isEnabled = buttonState,
                ddaDescription = descriptionTag,
                label = label,
                style = button.style ?: DEFAULT_BUTTON_STYLE,
                action = button.action
            )

            ShowButtonView(
                modifier = modifier,
                isDropShadowEnabled = isDropShadowEnabled,
                buttonParams = buttonParams,
                scrollState = scrollState,
                onNextButtonClicked = { desc, buttonAction ->
                    onNextButtonClicked(desc, buttonAction)
                }
            )
        }
    }
}

@Composable
internal fun ShowButtonView(
    modifier: Modifier = Modifier,
    isDropShadowEnabled: Boolean,
    buttonParams: SequentialPageButtonLayoutParams,
    onNextButtonClicked: (String, String) -> Unit,
    scrollState: ScrollState,
) {
    if (isDropShadowEnabled) {
        SequentialPageShadowButtonLayout(
            modifier = modifier,
            scrollState = scrollState,
            buttonLayoutParams = buttonParams,
            onNextButtonClicked = { label, action ->
                onNextButtonClicked(label, action)
            }
        )
    } else {
        SequentialPageButtonLayout(
            buttonLayoutParams = buttonParams,
            onNextButtonClicked = { label, action ->
                onNextButtonClicked(label, action)
            }
        )
    }
}

@Composable
private fun SequentialPageShadowButtonLayout(
    modifier: Modifier = Modifier,
    buttonLayoutParams: SequentialPageButtonLayoutParams,
    scrollState: ScrollState,
    onNextButtonClicked: (String, String) -> Unit,
) {
    BlueprintScrollShadow(
        scrollState = scrollState
    ) {
        SequentialPageButtonLayout(
            modifier = modifier,
            buttonLayoutParams = buttonLayoutParams,
            onNextButtonClicked = { label, action ->
                onNextButtonClicked(label, action)
            }
        )
    }
}
