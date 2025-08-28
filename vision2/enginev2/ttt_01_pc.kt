@file:OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalTextApi::class,
    ExperimentalMaterial3Api::class
)

package com.example.sequentialpagecapture

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.requestFocus
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSpacing
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalTextApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
internal fun SequentialPageCapture(
    viewModel: SequentialPageCaptureViewModel,
    modifier: Modifier = Modifier,
) {
    val stateHolder = rememberSequentialPageStateHolder(viewModel)
    
    with(viewModel) {
        val state by stateHolder.pageState
        val isContinueButtonEnabled by stateHolder.isContinueButtonEnabled
        val scrollState = stateHolder.scrollState
        val pageLayoutConfig = stateHolder.pageLayoutConfig
        val pageStateEvents = stateHolder.pageStateEvents

        BlueprintTheme(
            isDarkTheme = isBlueprintDarkTheme(),
            app = configureBlueprintTheme()
        ) {
            BlueprintScaffold(
                containerColor = getContainerColor(state.sequentialPageSections),
                top = {
                    SequentialPageNavigation(
                        modifier = modifier,
                        sequentialPageModule = module,
                        pageStateEvents = pageStateEvents
                    )
                },
                content = {
                    pageLayoutConfig?.let { layoutConfig ->
                        SequentialPageContent(
                            modifier = modifier,
                            state = state,
                            pageStateEvents = pageStateEvents,
                            layoutConfig = layoutConfig,
                            isContinueButtonEnabled = isContinueButtonEnabled,
                            scrollState = scrollState
                        )
                    }
                }
            )
        }
    }
}
