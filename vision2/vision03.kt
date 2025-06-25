fun handleFieldChange(key: String, value: String) {
        val currentItems = _pageState.value.uiState.items
        val itemToUpdate = currentItems.firstOrNull { it.key == key } ?: return
        
        // Enforce max length constraint using the pre-calculated value from the state.
        val cappedValue = value.take(itemToUpdate.maxLength)

        val result = validationDelegator.validateField(
            value = cappedValue,
            item = itemToUpdate,
            productData = _pageState.value.productPickerState.selectedProductData
        )
        
        val newItems = currentItems.map { item ->
            if (item.key == key) {
                val showError = result is FieldValidationResult.Failure && showErrorsOnInvalidFields
                val errorMessage = (result as? FieldValidationResult.Failure)?.message
                item.copy(
                    value = cappedValue, // Use the capped value
                    errorState = SequentialPageUiErrorState(
                        isError = showError,
                        message = if (showError) errorMessage else null
                    )
                )
            } else {
                item
            }
        }
        _pageState.update { it.copy(uiState = it.uiState.copy(items = newItems)) }
    }

    fun onContinueClicked() {
        val result = validationDelegator.validateAllFields(
            _pageState.value.uiState.items,
            _pageState.value.productPickerState.selectedProductData
        )

        _pageState.update {
            it.copy(uiState = it.uiState.copy(items = result.updatedItems))
        }

        if (result.isAllValid) {
            // Proceed to the next step...
        } else {
            // Enable instant error feedback for subsequent edits.
            showErrorsOnInvalidFields = true
        }
    }
