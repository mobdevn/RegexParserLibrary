@file:OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalTextApi::class,
    ExperimentalMaterial3Api::class
)

package com.example.sequentialpagecapture

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext

@Composable
internal fun SequentialPageInputTextField(
    modifier: Modifier = Modifier,
    item: SequentialPageContent,
    pageEvents: SequentialPageStateEvents,
    state: SequentialPageState,
    onFocusRequesterAction: ((List<FocusRequester>) -> Unit) -> Unit
) {
    item.inputFieldState.fieldValue?.let { fieldValue ->
        with(state.sequentialPageSections) {
            SequentialPageInputField(
                modifier = modifier,
                item = item,
                onValueChanged = { itemKey, value, transformation ->
                    pageEvents.onUiEvent(
                        FieldChanged(
                            key = itemKey,
                            value = value,
                            contentType = transformation
                        )
                    )
                },
                onViewFocusChanged = { },
                outputTransformation = getProductTransformationType(
                    contentType = item.pageContentType,
                    context = LocalContext.current
                ),
                inputViewState = SequentialPageInputFieldState(
                    fieldValue = fieldValue,
                    contextualTitleFallback = contextualInformationForInputField(
                        item = item,
                        heading = this?.heading ?: DEFAULT_TEXT_LABEL,
                        subheading = this?.subheading ?: DEFAULT_TEXT_LABEL
                    ),
                    componentLabel = item.fieldContent.fieldLabel.takeUnless { it.isNullOrEmpty() }
                ),
                onClickHandler = {
                    pageEvents.onLogInputViewFocusChange(
                        it ?: state.sequentialPageSections?.heading
                    )
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        isOnlyInputField(state.inputViewFieldCount)?.let {
            onFocusRequesterAction { focusRequesters ->
                focusRequesters[FIRST_FIELD_INDEX].requestFocus()
            }
        }
    }
}
