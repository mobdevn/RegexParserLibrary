@file:OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalTextApi::class,
    ExperimentalMaterial3Api::class
)

package com.example.sequentialpagecapture

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSpacing
import kotlinx.coroutines.launch

@Composable
internal fun SequentialPageComponents(
    pageViewState: SequentialPageState,
    shouldSkipTopPadding: Boolean,
    pageEvents: SequentialPageStateEvents
) {
    with(pageViewState) {
        val focusRequesters =
            remember { List(sequentialPageState.items.size) { FocusRequester() } }
        
        LaunchedEffect(firstErrorInputViewIndex) {
            firstErrorInputViewIndex?.let {
                launch {
                    focusRequesters[it].requestFocus()
                }
            }
        }

        Column(
            modifier = Modifier
                .ifFalse(shouldSkipTopPadding) {
                    Modifier.padding(top = LocalSpacing.current.small)
                },
            verticalArrangement = spacedBy(LocalSpacing.current.large)
        ) {
            sequentialPageState.items.forEachIndexed { index, item ->
                SequentialPageNameFields(
                    modifier = Modifier.focusRequester(focusRequesters[index]),
                    item = item,
                    state = pageViewState,
                    pageEvents = pageEvents,
                    onFocusRequesterAction = { action ->
                        action(focusRequesters)
                    }
                )
            }
        }
    }

    AccordionView(
        state = pageViewState,
        onStateChange = { state, label ->
            pageEvents.onLogAccordionStateChange(state, label)
        }
    )
}

@Composable
internal fun AccordionView(
    modifier: Modifier = Modifier,
    state: SequentialPageState,
    onStateChange: (Boolean, String) -> Unit
) {
    with(state.sequentialPageState.accordionState) {
        val accordionKey = accordionPair.firstNotNullOf { it.key }
        accordionPair[accordionKey]?.forEach {
            SequentialPageAccordion(
                modifier = modifier,
                title = it.title ?: ACCORDION_DEFAULT_TITLE,
                description = it.text ?: ACCORDION_DEFAULT_DESCRIPTION,
                onStateChange = onStateChange
            )
        }
    }
}
