@file:OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalTextApi::class,
    ExperimentalMaterial3Api::class
)

package com.example.sequentialpagecapture

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSpacing

@Composable
internal fun SequentialPageContent(
    modifier: Modifier = Modifier,
    state: SequentialPageState,
    pageStateEvents: SequentialPageStateEvents,
    layoutConfig: SequentialPageLayoutConfig,
    isContinueButtonEnabled: Boolean,
    scrollState: ScrollState
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(bottom = LocalSpacing.current.medium)
            .ifTrue(layoutConfig.dynamicSpacer) {
                Modifier
                    .weight(EqualWeight)
                    .verticalScroll(scrollState)
            }
            .then(modifier)
    ) {
        SequentialPageComponentsView(
            state = state,
            pageEvents = pageStateEvents,
            layoutConfig = layoutConfig
        )

        Spacer(
            modifier = Modifier
                .ifTrue(layoutConfig.buttonSpacer) {
                    Modifier.weight(EqualWeight)
                }
                .then(modifier)
        )

        with(state.sequentialPageSections) {
            ButtonComposableLayoutView(
                modifier = modifier,
                spcSections = this,
                onNextButtonClicked = { label, actionId ->
                    pageStateEvents.onUiEvent(
                        NextClickEvent(label, actionId)
                    )
                },
                buttonState = isContinueButtonEnabled,
                scrollState = scrollState,
                isDropShadowEnabled = layoutConfig.hasEqualWeight,
                accessibilityTag = getContentDescriptionTag(this?.sections)
            )
        }
    }
}

@Composable
internal fun ColumnScope.SequentialPageComponentsView(
    modifier: Modifier = Modifier,
    layoutConfig: SequentialPageLayoutConfig,
    state: SequentialPageState,
    pageEvents: SequentialPageStateEvents
) {
    Column(
        modifier = Modifier
            .blueprintWindowSizePadding()
            .ifTrue(layoutConfig.hasEqualWeight) {
                Modifier
                    .weight(EqualWeight)
                    .verticalScroll(rememberScrollState())
            }
            .padding(bottom = LocalSpacing.current.large)
            .then(modifier),
        verticalArrangement = if (layoutConfig.buttonSpacer) Top else Center
    ) {
        SequentialPageHeaders(
            pageViewState = state
        )

        SequentialPageComponents(
            pageViewState = state,
            shouldSkipTopPadding = layoutConfig.shouldSkipTopPadding(),
            pageEvents = pageEvents
        )
    }
}
