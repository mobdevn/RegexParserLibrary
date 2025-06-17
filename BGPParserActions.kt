// SequentialPageActions.kt
package com.example.refactored

data class SequentialPageActions(
    val onFieldChanged: (key: String, value: String) -> Unit,
    val onProductSelected: (productId: Int) -> Unit,
    val onInvestmentProductSelected: (productInfo: SequentialPageInvestmentProductInfo) -> Unit,
    val onInvestmentAccountChanged: (accountNumber: String) -> Unit,
    val onNextClicked: () -> Unit,
    val onBackPressed: (id: String) -> Unit,
    val onAccordionToggled: (item: SequentialPageFieldCaptureItem, isExpanded: Boolean) -> Unit,
    val onHyperlinkClicked: (action: MvcAction) -> Unit
)
