@file:OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalTextApi::class,
    ExperimentalMaterial3Api::class
)

package com.example.sequentialpagecapture

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester

@Composable
fun SequentialPageNameFields(
    modifier: Modifier = Modifier,
    item: SequentialPageContent,
    state: SequentialPageState,
    pageEvents: SequentialPageStateEvents,
    onFocusRequesterAction: ((List<FocusRequester>) -> Unit) -> Unit
) {
    val value by remember { mutableStateOf(item.inputFieldState.fieldValue) }
    
    with(pageEvents) {
        when (item.pageComponentType) {
            DATE -> {
                SequentialPageDatePicker(
                    modifier = modifier,
                    item = item,
                    value = value ?: DEFAULT_TEXT_LABEL,
                    onFocusChange = { onLogInputViewFocusChange(it) },
                    onDateChanged = { onUiEvent(it) }
                )
            }

            INPUT_FIELD -> {
                SequentialPageInputTextField(
                    modifier = modifier,
                    item = item,
                    state = state,
                    pageEvents = pageEvents,
                    onFocusRequesterAction = onFocusRequesterAction
                )
            }

            REVIEW_FIELD -> {
                SequentialPageReviewPanel(
                    spcSections = state.sequentialPageSections,
                    onEditButtonClicked = { id, label ->
                        onUiEvent(
                            PageBackEvent(id, label)
                        )
                    }
                )
            }

            INFO_PANEL -> {
                SequentialPageCaptureInfoPanel(
                    modifier = modifier,
                    sequentialPageSection = state.sequentialPageSections,
                    onDelayLoadingComplete = { progressState ->
                        onUiEvent(
                            NextClickEvent(
                                actionLabel = item.pageActionLabel ?: NEXT,
                                actionId = NEXT,
                                showProgress = progressState
                            )
                        )
                    }
                )
            }

            RADIO_INPUT -> {
                SequentialPageInvestmentAccountView(
                    modifier = modifier,
                    item = item,
                    sequentialPageState = state,
                    onSelectedInvestmentAccount = { onUiEvent(it) },
                    onValueChanged = { onUiEvent(it) },
                    onClickEvent = { onLogInputViewFocusChange(it) }
                )
            }

            RADIO_OPTION -> {
                SequentialPageProductPickerPanel(
                    productList = state.productPickerState.products,
                    onProductSelected = { event, _ ->
                        onUiEvent(event)
                    }
                )
            }

            HYPERLINK -> {
                SequentialPageHyperLink(
                    item = item,
                    onEventHandler = { event, _ ->
                        onUiEvent(event)
                    }
                )
            }
        }
    }
}
