object SequentialPageFieldProcessor {
    
    fun createFieldCaptureItem(response: SequentialPageCaptureData): SequentialPageFieldCaptureItem {
        val inputFieldType = SequentialPageUtilities.addSPCComponentType(response.type)
        
        return SequentialPageFieldCaptureItem(
            key = response.id,
            regex = response.regex,
            value = response.value,
            heading = response.heading,
            minDate = response.minDate,
            maxDate = response.maxDate,
            keyboardType = SequentialPageUtilities.getKeyBoardType(response.keyboard),
            inputFieldType = inputFieldType,
            errorState = SequentialPageErrorState(),
            title = response.title,
            helpInformation = response.helpInformation,
            contentType = response.contentType,
            radioInputs = response.radioInputs,
            noRadioOptionSelectedText = response.noRadioOptionSelectedText,
            text = response.text,
            action = response.action,
            radioOptions = response.radioOptions
        )
    }
    
    fun processModuleData(dataList: List<SequentialPageCaptureData>): Pair<List<SequentialPageFieldCaptureItem>, Int> {
        var fieldCount = 0
        val items = dataList.mapNotNull { response ->
            createFieldCaptureItem(response).also { item ->
                if (item.inputFieldType == INPUT_FIELD || item.inputFieldType == DATE) {
                    fieldCount++
                }
            }
        }
        return Pair(items, fieldCount)
    }
    
    fun processItemForPayload(
        item: SequentialPageFieldCaptureItem,
        requestDataBuilder: SequentialPageCaptureRequestData.Builder?
    ) {
        with(item) {
            when (type) {
                DATE -> SequentialPageUtilities.formatDateForPayload(value)
                RADIO_OPTION -> value?.let { /* Analytics logging handled elsewhere */ }
            }
            
            key?.let { itemKey ->
                value?.let { itemValue ->
                    if (!SequentialPageUtilities.shouldExcludeFromPayload(itemKey)) {
                        requestDataBuilder?.add(itemKey, itemValue)
                    }
                }
            }
        }
    }
}
