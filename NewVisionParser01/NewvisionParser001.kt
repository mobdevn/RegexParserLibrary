package ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import model.*
import viewmodel.SequentialPageViewModel
import viewmodel.UiEvent

/**
 * A map-based component renderer. This is a scalable pattern that avoids large `when`
 * blocks, adhering to the "max 5 conditions" rule. It maps a component type
 * to its Composable function.
 */
val componentRenderer: Map<ComponentType, @Composable (SequentialPageFieldItem, (UiEvent) -> Unit) -> Unit> = mapOf(
    ComponentType.TEXT_INPUT to { item, onEvent -> TextInputComponent(item, onEvent) },
    ComponentType.PRODUCT_PICKER to { item, onEvent -> ProductPickerComponent(item, onEvent) },
    ComponentType.ACCORDION_INFO to { item, onEvent -> AccordionInfoComponent(item, onEvent) },
    ComponentType.HYPERLINK to { item, onEvent -> HyperlinkComponent(item, onEvent) }
)

/**
 * The main stateful screen. It collects state and navigation events from the ViewModel
 * and handles the navigation logic.
 */
@Composable
fun SequentialPageScreen(viewModel: SequentialPageViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current // For handling back press or opening URLs

    // Side-effect to handle one-time navigation events
    LaunchedEffect(state.navigationEvent) {
        when (val event = state.navigationEvent) {
            is NavigationEvent.ToUrl -> { /* launch custom tab or intent */ }
            is NavigationEvent.ToRoute -> { /* navigate with NavController */ }
            is NavigationEvent.Back -> { /* activity.onBackPressedDispatcher.onBackPressed() */ }
            else -> Unit
        }
        // Consume the event so it doesn't fire again on recomposition
        if (state.navigationEvent !is NavigationEvent.Consumed) {
            viewModel.onEvent(UiEvent.NavigationConsumed)
        }
    }

    Scaffold(
        bottomBar = {
            Button(
                onClick = { viewModel.onEvent(UiEvent.NextClicked) },
                enabled = state.isContinueButtonEnabled,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) { Text("Continue") }
        }
    ) { paddingValues ->
        SequentialPageContent(state, viewModel::onEvent, Modifier.padding(paddingValues))
    }
}

/**
 * A stateless content Composable that renders the list of components.
 */
@Composable
fun SequentialPageContent(
    state: SequentialPageState,
    onEvent: (UiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.isLoading) {
        CircularProgressIndicator(modifier = Modifier.fillMaxSize().wrapContentSize())
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(state.items, key = { it.id }) { item ->
                // Look up the appropriate Composable in the map and render it
                componentRenderer[item.type]?.invoke(item, onEvent)
            }
        }
    }
}

// region Component Composables
@Composable
fun TextInputComponent(item: SequentialPageFieldItem, onEvent: (UiEvent) -> Unit) {
    Column {
        OutlinedTextField(
            value = item.value ?: "",
            onValueChange = { onEvent(UiEvent.FieldChanged(item.id, it)) },
            label = { Text(item.label ?: "") },
            isError = item.error != null,
            modifier = Modifier.fillMaxWidth().onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    onEvent(UiEvent.FieldFocused(item.id))
                }
            }
        )
        item.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
fun ProductPickerComponent(item: SequentialPageFieldItem, onEvent: (UiEvent) -> Unit) {
    Column {
        item.label?.let { Text(it, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp)) }
        item.options?.forEach { option ->
            Row(modifier = Modifier.fillMaxWidth().clickable { onEvent(UiEvent.ProductSelected(item.id, option.id)) }) {
                RadioButton(selected = item.selectedOptionId == option.id, onClick = { onEvent(UiEvent.ProductSelected(item.id, option.id)) })
                Spacer(Modifier.width(8.dp))
                Text(option.title, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
fun AccordionInfoComponent(item: SequentialPageFieldItem, onEvent: (UiEvent) -> Unit) {
    if (item.isAccordionExpanded) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                item.accordionTitle?.let { Text(it, fontWeight = FontWeight.Bold) }
                item.accordionContent?.forEach { content -> Text(content) }
            }
        }
    }
}

@Composable
fun HyperlinkComponent(item: SequentialPageFieldItem, onEvent: (UiEvent) -> Unit) {
    Text(
        text = item.hyperlinkText ?: "Learn More",
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.clickable { item.hyperlinkUrl?.let { onEvent(UiEvent.HyperlinkClicked(it)) } }
    )
}
// endregion
