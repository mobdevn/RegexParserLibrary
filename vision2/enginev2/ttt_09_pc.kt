@file:OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalTextApi::class,
    ExperimentalMaterial3Api::class
)

package com.example.sequentialpagecapture

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.*

/**
 * State holder class for managing SequentialPageCapture state and events
 * Follows the unidirectional data flow pattern
 */
class SequentialPageStateHolder(
    private val viewModel: SequentialPageCaptureViewModel
) {
    val pageState: State<SequentialPageState> 
        @Composable get() = viewModel.pageState.collectAsState()
    
    val isContinueButtonEnabled: State<Boolean>
        @Composable get() = viewModel.isContinueButtonEnabled.collectAsState()
    
    val scrollState: ScrollState
        @Composable get() = rememberScrollState()
    
    val pageLayoutConfig: SequentialPageLayoutConfig?
        @Composable get() {
            val state by pageState
            return remember(state.viewType) {
                state.viewType?.let { fromViewType(it) }
            }
        }
    
    val pageStateEvents: SequentialPageStateEvents
        get() = SequentialPageStateEvents(
            onUiEvent = viewModel::onUiUpdateEvent,
            onLogInputViewFocusChange = viewModel::LogInputViewFocusChange,
            onLogAccordionStateChange = viewModel::LogAccordionStateChange
        )
}

/**
 * Composable function to create and remember the state holder
 */
@Composable
fun rememberSequentialPageStateHolder(
    viewModel: SequentialPageCaptureViewModel
): SequentialPageStateHolder {
    return remember(viewModel) {
        SequentialPageStateHolder(viewModel)
    }
}
