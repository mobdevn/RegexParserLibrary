@file:OptIn(ExperimentalCoroutinesApi::class)
package com.your.pkg

import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flowSharedFlow
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.*
import kotlin.test.*

class SequentialPageCaptureViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScheduler get() = testDispatcher.scheduler
    private val testScope = TestScope(testDispatcher)

    private val fieldDataProcessor: SequentialPageDataProcessor = mockk(relaxUnitFun = true)
    private val productHandler: SequentialPageSelectedProductProcessor = mockk(relaxUnitFun = true)
    private val investmentAccountHandler: SequentialPageInvestmentAccountProcessor = mockk(relaxUnitFun = true)
    private val payLoadProcessor: SequentialPagePayLoadGenerator = mockk(relaxUnitFun = true)
    private val analyticsManager: SequentialPageAnalyticsEvent = mockk(relaxUnitFun = true)
    private val validationDelegator: SequentialPageValidationDelegator = mockk(relaxUnitFun = true)
    private val journeyActionBuilder: JourneyFrameworkGenericHandleAction.Builder = mockk(relaxed = true)
    private val moduleDataProcessor: SequentialPageCaptureModuleProcessor = mockk(relaxUnitFun = true)

    private val moduleFlow = MutableSharedFlow<SequentialPageCaptureModule?>(replay = 1)

    private val personalDetailsModule = mockk<SequentialPageCaptureModule>(relaxed = true) {
        every { sequentialPageDataList } returns listOf(
            mockk<SequentialPageModuleResponse>(relaxed = true) {
                every { id } returns "emailAddress"
                every { type } returns "INPUT_FIELD"
                every { value } returns "john.doe@example.com"
            },
            mockk<SequentialPageModuleResponse>(relaxed = true) {
                every { id } returns "phoneNumber"
                every { type } returns "INPUT_FIELD"
                every { value } returns "07123456789"
            }
        )
        every { sequentialPageSections } returns mockk(relaxed = true) {
            every { analyticsPageTag } returns "personal_details_page"
        }
        every { getAction(any<String>()) } returns mockk(relaxed = true) {
            every { stepId } returns "PERSONAL_DETAILS_SAVE"
        }
        every { id } returns "personal-details-module"
    }

    private val emailFieldItem = SequentialPageFieldCaptureItem(
        key = "emailAddress",
        type = SequentialPageCaptureComponentType.INPUT_FIELD,
        value = "john.doe@example.com",
        regex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    )

    private val phoneFieldItem = SequentialPageFieldCaptureItem(
        key = "phoneNumber",
        type = SequentialPageCaptureComponentType.INPUT_FIELD,
        value = "07123456789",
        regex = "^07[0-9]{9}$"
    )

    private val savingsAccount = SequentialPageInvestmentAccount(
        radioTitle = "Savings Account",
        isProductSelected = true,
        regex = "^[0-9]{8}$",
        maxLength = 8,
        invalidInputText = "Please enter a valid 8-digit account number"
    )

    private val currentAccount = SequentialPageInvestmentAccount(
        radioTitle = "Current Account",
        isProductSelected = false,
        regex = "^[0-9]{8}$",
        maxLength = 8,
        invalidInputText = "Please enter a valid 8-digit account number"
    )

    private fun createViewModel() = SequentialPageCaptureViewModel(
        fieldDataProcessor,
        productHandler,
        investmentAccountHandler,
        payLoadProcessor,
        analyticsManager,
        validationDelegator,
        journeyActionBuilder,
        moduleDataProcessor
    ).apply { Dispatchers.setMain(testDispatcher) }

    @BeforeEach
    fun setUp() {
        every { moduleDataProcessor.dataStateFlow } returns moduleFlow
        every { validationDelegator.validateAllFields(any(), any(), any()) } returns
            SequentialPageValidationResult(emptyList(), true)
        every { fieldDataProcessor.processModuleData(any()) } returns
            SequentialPageDataProcessor.PageModuleData(listOf(emailFieldItem, phoneFieldItem), 2, true)
        every { fieldDataProcessor.getAccordionData(any()) } returns emptyMap()
        every { fieldDataProcessor.getInvestmentProducts(any()) } returns emptyList()
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    @Test
    fun `testViewModelInitialization_setsUpDependenciesCorrectly`() = testScope.runTest {
        val viewModel = createViewModel()
        assertNotNull(viewModel)
        verify { moduleDataProcessor.dataStateFlow }
    }

    @Test
    fun `testViewModelInitialization_collectsModuleDataFlow`() = testScope.runTest {
        val viewModel = createViewModel()
        moduleFlow.emit(personalDetailsModule)
        testScheduler.advanceUntilIdle()
        assertEquals(personalDetailsModule, viewModel.getModule())
    }

    @Test
    fun `testViewModelInitialization_setsInitialStates`() = testScope.runTest {
        val viewModel = createViewModel()
        assertTrue(viewModel.isContinueButtonEnabled.value)
        assertTrue(viewModel.pageState.value.sequentialPageState.isLoading)
        assertNull(viewModel.pageState.value.sequentialPageState.error)
    }

    @Test
    fun `testViewModelInitialization_handlesNullJourneyActionBuilder`() = testScope.runTest {
        val viewModel = SequentialPageCaptureViewModel(
            fieldDataProcessor, productHandler, investmentAccountHandler,
            payLoadProcessor, analyticsManager, validationDelegator,
            null,
            moduleDataProcessor
        ).apply { Dispatchers.setMain(testDispatcher) }
        assertNotNull(viewModel)
    }

    @Test
    fun `testProcessStartModule_updatesModuleReference`() = testScope.runTest {
        val viewModel = createViewModel()
        viewModel.onUiUpdateEvent(SequentialPageEvent.ModuleStart(personalDetailsModule))
        testScheduler.advanceUntilIdle()
        assertEquals(personalDetailsModule, viewModel.getModule())
    }

    @Test
    fun `testProcessStartModule_logsScreenAnalytics`() = testScope.runTest {
        val viewModel = createViewModel()
        viewModel.onUiUpdateEvent(SequentialPageEvent.ModuleStart(personalDetailsModule))
        testScheduler.advanceUntilIdle()
        verify { analyticsManager.logScreenLoad("personal_details_page") }
    }

    @Test
    fun `testProcessStartModule_processesModuleData`() = testScope.runTest {
        val viewModel = createViewModel()
        viewModel.onUiUpdateEvent(SequentialPageEvent.ModuleStart(personalDetailsModule))
        testScheduler.advanceUntilIdle()
        verify { fieldDataProcessor.processModuleData(personalDetailsModule.sequentialPageDataList) }
        verify { fieldDataProcessor.getAccordionData(any()) }
        verify { fieldDataProcessor.getInvestmentProducts(any()) }
    }

    @Test
    fun `testProcessStartModule_updatesPageState`() = testScope.runTest {
        val viewModel = createViewModel()
        viewModel.onUiUpdateEvent(SequentialPageEvent.ModuleStart(personalDetailsModule))
        testScheduler.advanceUntilIdle()
        assertFalse(viewModel.pageState.value.sequentialPageState.isLoading)
        assertEquals(listOf(emailFieldItem, phoneFieldItem), viewModel.pageState.value.sequentialPageState.items)
    }

    @Test
    fun `testProcessStartModule_setsButtonState`() = testScope.runTest {
        every { fieldDataProcessor.processModuleData(any()) } returns
            SequentialPageDataProcessor.PageModuleData(emptyList(), 0, false)
        val viewModel = createViewModel()
        viewModel.onUiUpdateEvent(SequentialPageEvent.ModuleStart(personalDetailsModule))
        testScheduler.advanceUntilIdle()
        assertFalse(viewModel.isContinueButtonEnabled.value)
    }

    @Test
    fun `testProcessStartModule_handlesEmptyDataList`() = testScope.runTest {
        every { personalDetailsModule.sequentialPageDataList } returns emptyList()
        every { fieldDataProcessor.processModuleData(any()) } returns
            SequentialPageDataProcessor.PageModuleData(emptyList(), 0, true)
        val viewModel = createViewModel()
        viewModel.onUiUpdateEvent(SequentialPageEvent.ModuleStart(personalDetailsModule))
        testScheduler.advanceUntilIdle()
        assertTrue(viewModel.pageState.value.sequentialPageState.items.isEmpty())
    }

    @Test
    fun `testProcessStartModule_handlesNullAnalyticsPageTag`() = testScope.runTest {
        every { personalDetailsModule.sequentialPageSections.analyticsPageTag } returns null
        val viewModel = createViewModel()
        viewModel.onUiUpdateEvent(SequentialPageEvent.ModuleStart(personalDetailsModule))
        testScheduler.advanceUntilIdle()
        verify { analyticsManager.logScreenLoad("") }
    }

    @Test
    fun `testProcessInputStream_updatesEmailFieldValue`() = testScope.runTest {
        val viewModel = createViewModel()
        setupModuleForFieldTesting(viewModel)
        every { fieldDataProcessor.getCappedValue(any(), any(), any(), any(), any()) } returns
            SequentialPageDataProcessor.PageInputFieldData("updated.email@example.com", emailFieldItem)
        viewModel.onUiUpdateEvent(SequentialPageEvent.FieldChanged("emailAddress", "updated.email@example.com", null))
        testScheduler.advanceUntilIdle()
        verify { fieldDataProcessor.getCappedValue(any(), any(), "emailAddress", "updated.email@example.com", null) }
    }

    @Test
    fun `testProcessInputStream_handlesPhoneNumberCapping`() = testScope.runTest {
        val viewModel = createViewModel()
        setupModuleForFieldTesting(viewModel)
        val cappedPhoneItem = phoneFieldItem.copy(value = "07123456789")
        every { fieldDataProcessor.getCappedValue(any(), any(), any(), any(), any()) } returns
            SequentialPageDataProcessor.PageInputFieldData("07123456789", cappedPhoneItem)
        viewModel.onUiUpdateEvent(SequentialPageEvent.FieldChanged("phoneNumber", "071234567890123", null))
        testScheduler.advanceUntilIdle()
        verify { fieldDataProcessor.getCappedValue(any(), any(), "phoneNumber", "071234567890123", null) }
    }

    @Test
    fun `testProcessInputStream_handlesContentTransformation`() = testScope.runTest {
        val viewModel = createViewModel()
        setupModuleForFieldTesting(viewModel)
        val sortCodeTransformation = mockk<OutputTransformation>()
        every { fieldDataProcessor.getCappedValue(any(), any(), any(), any(), any()) } returns
            SequentialPageDataProcessor.PageInputFieldData("12-34-56", emailFieldItem)
        viewModel.onUiUpdateEvent(SequentialPageEvent.FieldChanged("sortCode", "123456", sortCodeTransformation))
        testScheduler.advanceUntilIdle()
        verify { fieldDataProcessor.getCappedValue(any(), any(), "sortCode", "123456", sortCodeTransformation) }
    }

    @Test
    fun `testProcessInputStream_handlesInvestmentAccountTransformation`() = testScope.runTest {
        val viewModel = createViewModel()
        setupModuleForFieldTesting(viewModel)
        viewModel.pageState.value.copy(
            investmentAccountUiState = viewModel.pageState.value.investmentAccountUiState.copy(
                selectedInvestmentAccount = savingsAccount
            )
        )
        every { fieldDataProcessor.getCappedValue(any(), any(), any(), any(), any()) } returns
            SequentialPageDataProcessor.PageInputFieldData("12345678", emailFieldItem)
        viewModel.onUiUpdateEvent(SequentialPageEvent.FieldChanged("accountNumber", "12345678", null))
        testScheduler.advanceUntilIdle()
        verify { fieldDataProcessor.getCappedValue(any(), any(), "accountNumber", "12345678", null) }
    }

    @Test
    fun `testProcessInputStream_handlesNullKey`() = testScope.runTest {
        val viewModel = createViewModel()
        viewModel.onUiUpdateEvent(SequentialPageEvent.FieldChanged(null, "someValue", null))
        testScheduler.advanceUntilIdle()
        verify(exactly = 0) { fieldDataProcessor.getCappedValue(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `testProcessInputStream_handlesEmptyValue`() = testScope.runTest {
        val viewModel = createViewModel()
        setupModuleForFieldTesting(viewModel)
        every { fieldDataProcessor.getCappedValue(any(), any(), any(), any(), any()) } returns
            SequentialPageDataProcessor.PageInputFieldData("", emailFieldItem)
        viewModel.onUiUpdateEvent(SequentialPageEvent.FieldChanged("emailAddress", "", null))
        testScheduler.advanceUntilIdle()
        verify { fieldDataProcessor.getCappedValue(any(), any(), "emailAddress", "", null) }
    }

    @Test
    fun `testProcessInputStream_handlesMaxLengthExceeded`() = testScope.runTest {
        val viewModel = createViewModel()
        setupModuleForFieldTesting(viewModel)
        val longEmailValue = "${"a".repeat(100)}@example.com"
        every { fieldDataProcessor.getCappedValue(any(), any(), any(), any(), any()) } returns
            SequentialPageDataProcessor.PageInputFieldData("truncated@example.com", emailFieldItem)
        viewModel.onUiUpdateEvent(SequentialPageEvent.FieldChanged("emailAddress", longEmailValue, null))
        testScheduler.advanceUntilIdle()
        verify { fieldDataProcessor.getCappedValue(any(), any(), "emailAddress", longEmailValue, null) }
    }

    @Test
    fun `testProcessInputStream_validatesFieldOnInput`() = testScope.runTest {
        val viewModel = createViewModel()
        setupModuleForFieldTesting(viewModel)
        every { fieldDataProcessor.getCappedValue(any(), any(), any(), any(), any()) } returns
            SequentialPageDataProcessor.PageInputFieldData("valid@example.com", emailFieldItem)
        viewModel.onUiUpdateEvent(SequentialPageEvent.FieldChanged("emailAddress", "valid@example.com", null))
        testScheduler.advanceUntilIdle()
        verify { fieldDataProcessor.getCappedValue(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `testProcessSelectedProduct_updatesSavingsAccountState`() = testScope.runTest {
        val viewModel = createViewModel()
        setupModuleForFieldTesting(viewModel)
        val updatedState = viewModel.pageState.value.copy(
            productPickerState = viewModel.pageState.value.productPickerState.copy(selectedProductId = 0)
        )
        every { productHandler.processSelectedProduct(any(), any(), any()) } returns updatedState
        viewModel.onUiUpdateEvent(SequentialPageEvent.InvestmentProductSelected(0))
        testScheduler.advanceUntilIdle()
        verify { productHandler.processSelectedProduct(any(), 0, any()) }
    }

    @Test
    fun `testProcessSelectedProduct_deselectsPreviousProduct`() = testScope.runTest {
        val viewModel = createViewModel()
        setupModuleForFieldTesting(viewModel)
        val stateWithCurrentAccountSelected = viewModel.pageState.value.copy(
            productPickerState = viewModel.pageState.value.productPickerState.copy(selectedProductId = 1)
        )
        every { productHandler.processSelectedProduct(any(), any(), any()) } returns stateWithCurrentAccountSelected
        viewModel.onUiUpdateEvent(SequentialPageEvent.InvestmentProductSelected(0))
        testScheduler.advanceUntilIdle()
        verify { productHandler.processSelectedProduct(any(), 0, any()) }
    }

    @Test
    fun `testProcessSelectedProduct_handlesInvalidProductId`() = testScope.runTest {
        val viewModel = createViewModel()
        setupModuleForFieldTesting(viewModel)
        every { productHandler.processSelectedProduct(any(), any(), any()) } returns viewModel.pageState.value
        viewModel.onUiUpdateEvent(SequentialPageEvent.InvestmentProductSelected(-1))
        testScheduler.advanceUntilIdle()
        verify { productHandler.processSelectedProduct(any(), -1, any()) }
    }

    @Test
    fun `testProcessSelectedProduct_updatesProductDescription`() = testScope.runTest {
        val viewModel = createViewModel()
        setupModuleForFieldTesting(viewModel)
        val productWithDescription = SequentialPageProductPickerView(
            title = "Premium Savings Account",
            description = "Earn higher interest rates with our premium savings account"
        )
        val updatedState = viewModel.pageState.value.copy(
            productPickerState = viewModel.pageState.value.productPickerState.copy(
                products = listOf(productWithDescription)
            )
        )
        every { productHandler.processSelectedProduct(any(), any(), any()) } returns updatedState
        viewModel.onUiUpdateEvent(SequentialPageEvent.InvestmentProductSelected(0))
        testScheduler.advanceUntilIdle()
        verify { productHandler.processSelectedProduct(any(), 0, any()) }
    }

    @Test
    fun `testProcessSelectedProduct_handlesEmptyProductList`() = testScope.runTest {
        val viewModel = createViewModel()
        setupModuleForFieldTesting(viewModel)
        every { productHandler.processSelectedProduct(any(), any(), any()) } returns viewModel.pageState.value
        viewModel.onUiUpdateEvent(SequentialPageEvent.InvestmentProductSelected(0))
        testScheduler.advanceUntilIdle()
        verify { productHandler.processSelectedProduct(any(), 0, any()) }
    }

    @Test
    fun `testProcessSelectedInvestmentAccount_updatesSavingsAccountState`() = testScope.runTest {
        val viewModel = createViewModel()
        setupModuleForFieldTesting(viewModel)
        val updatedState = viewModel.pageState.value.copy(
            investmentAccountUiState = viewModel.pageState.value.investmentAccountUiState.copy(
                selectedInvestmentAccount = savingsAccount
            )
        )
        every { investmentAccountHandler.processSelectedInvestmentAccount(any(), any(), any()) } returns updatedState
        viewModel.onUiUpdateEvent(SequentialPageEvent.InvestmentAccountSelected(savingsAccount, emailFieldItem))
        testScheduler.advanceUntilIdle()
        verify { investmentAccountHandler.processSelectedInvestmentAccount(any(), savingsAccount, emailFieldItem) }
    }

    @Test
    fun `testProcessSelectedInvestmentAccount_updatesTransformation`() = testScope.runTest {
        val viewModel = createViewModel()
        setupModuleForFieldTesting(viewModel)
        val accountNumberTransformation = mockk<SequentialPageTransformationConfig>()
        val updatedState = viewModel.pageState.value.copy(
            investmentAccountUiState = viewModel.pageState.value.investmentAccountUiState.copy(
                invAccountTransformation = accountNumberTransformation
            )
        )
        every { investmentAccountHandler.processSelectedInvestmentAccount(any(), any(), any()) } returns updatedState
        viewModel.onUiUpdateEvent(SequentialPageEvent.InvestmentAccountSelected(savingsAccount, emailFieldItem))
        testScheduler.advanceUntilIdle()
        verify { investmentAccountHandler.processSelectedInvestmentAccount(any(), savingsAccount, emailFieldItem) }
    }

    @Test
    fun `testProcessSelectedInvestmentAccount_handlesNullAccount`() = testScope.runTest {
        val viewModel = createViewModel()
        setupModuleForFieldTesting(viewModel)
        val nullTitleAccount = savingsAccount.copy(radioTitle = null)
        every { investmentAccountHandler.processSelectedInvestmentAccount(any(), any(), any()) } returns viewModel.pageState.value
        viewModel.onUiUpdateEvent(SequentialPageEvent.InvestmentAccountSelected(nullTitleAccount, emailFieldItem))
        testScheduler.advanceUntilIdle()
        verify { investmentAccountHandler.processSelectedInvestmentAccount(any(), nullTitleAccount, emailFieldItem) }
    }

    @Test
    fun `testProcessSelectedInvestmentAccount_updatesInputVisibility`() = testScope.runTest {
        val viewModel = createViewModel()
        setupModuleForFieldTesting(viewModel)
        val updatedState = viewModel.pageState.value.copy(
            investmentAccountUiState = viewModel.pageState.value.investmentAccountUiState.copy(
                isInputViewVisible = true
            )
        )
        every { investmentAccountHandler.processSelectedInvestmentAccount(any(), any(), any()) } returns updatedState
        viewModel.onUiUpdateEvent(SequentialPageEvent.InvestmentAccountSelected(savingsAccount, emailFieldItem))
        testScheduler.advanceUntilIdle()
        verify { investmentAccountHandler.processSelectedInvestmentAccount(any(), savingsAccount, emailFieldItem) }
    }

    @Test
    fun `testProcessContinueEvent_logsContinueButtonClick`() = testScope.runTest {
        val viewModel = createViewModel()
        setupModuleForFieldTesting(viewModel)
        viewModel.onUiUpdateEvent(SequentialPageEvent.NextClickEvent("Continue", "next", true))
        testScheduler.advanceUntilIdle()
        verify { analyticsManager.logButtonClick("Continue") }
    }

    @Test
    fun `testProcessContinueEvent_validatesAllFields`() = testScope.runTest {
        val viewModel = createViewModel()
        setupModuleForFieldTesting(viewModel)
        viewModel.onUiUpdateEvent(SequentialPageEvent.NextClickEvent("Continue", "next", true))
        testScheduler.advanceUntilIdle()
        verify { validationDelegator.validateAllFields(any(), any(), any()) }
    }

    @Test
    fun `testProcessContinueEvent_launchesJourneyOnValidInput`() = testScope.runTest {
        val viewModel = createViewModel()
        setupModuleForFieldTesting(viewModel)
        val continueAction = mockk<MvcAction>(relaxed = true)
        every { personalDetailsModule.getAction("next") } returns continueAction
        every { payLoadProcessor.createPayLoad(any<List<SequentialPageFieldCaptureItem>>()) } returns mockk()
        viewModel.onUiUpdateEvent(SequentialPageEvent.NextClickEvent("Continue", "next", true))
        testScheduler.advanceUntilIdle()
        verify { journeyActionBuilder.build().executeAction(continueAction, "personal-details-module", any()) }
    }

    @Test
    fun `testProcessContinueEvent_showsErrorsOnInvalidInput`() = testScope.runTest {
        val invalidEmailItem = emailFieldItem.copy(
            errorState = SequentialPageUiErrorState(isError = true, errorMessage = "Invalid email format")
        )
        every { validationDelegator.validateAllFields(any(), any(), any()) } returns
            SequentialPageValidationResult(listOf(invalidEmailItem), false)
        val viewModel = createViewModel()
        setupModuleForFieldTesting(viewModel)
        viewModel.onUiUpdateEvent(SequentialPageEvent.NextClickEvent("Continue", "next", true))
        testScheduler.advanceUntilIdle()
        verify(exactly = 0) { journeyActionBuilder.build().executeAction(any(), any(), any()) }
        assertTrue(viewModel.pageState.value.sequentialPageState.items.any { it.errorState?.isError == true })
    }

    @Test
    fun `testProcessContinueEvent_handlesOtpVerificationStep`() = testScope.runTest {
        val viewModel = createViewModel()
        setupModuleForFieldTesting(viewModel)
        val otpVerificationAction = mockk<MvcAction>(relaxed = true) {
            every { stepId } returns "OTP_VERIFICATION_STATUS_CHECK"
        }
        every { personalDetailsModule.getAction("next") } returns otpVerificationAction
        viewModel.onUiUpdateEvent(SequentialPageEvent.NextClickEvent("Continue", "next", true))
        testScheduler.advanceUntilIdle()
        verify { journeyActionBuilder.build().executeAction(otpVerificationAction, "personal-details-module", any()) }
    }

    @Test
    fun `testProcessContinueEvent_handlesInvestmentMassAffluentStep`() = testScope.runTest {
        val viewModel = createViewModel()
        setupModuleForFieldTesting(viewModel)
        val massAffluentAction = mockk<MvcAction>(relaxed = true) {
            every { stepId } returns "INVESTMENT_MASS_AFFLUENT_CALL_HELPDESK"
        }
        every { personalDetailsModule.getAction("next") } returns massAffluentAction
        viewModel.onUiUpdateEvent(SequentialPageEvent.NextClickEvent("Continue", "next", true))
        testScheduler.advanceUntilIdle()
        verify { journeyActionBuilder.build().executeAction(massAffluentAction, "personal-details-module", any()) }
    }

    @Test
    fun `testProcessContinueEvent_handlesInvestmentInitialiseStep`() = testScope.runTest {
        val viewModel = createViewModel()
        setupModuleForFieldTesting(viewModel)
        val investmentInitAction = mockk<MvcAction>(relaxed = true) {
            every { stepId } returns "INVESTMENT_INITIALISE"
        }
        every { personalDetailsModule.getAction("next") } returns investmentInitAction
        viewModel.onUiUpdateEvent(SequentialPageEvent.NextClickEvent("Continue", "next", true))
        testScheduler.advanceUntilIdle()
        verify { journeyActionBuilder.build().executeAction(investmentInitAction, "personal-details-module", any()) }
    }

    @Test
    fun `testProcessContinueEvent_handlesPendingActivationStep`() = testScope.runTest {
        val viewModel = createViewModel()
        setupModuleForFieldTesting(viewModel)
        val pendingActivationAction = mockk<MvcAction>(relaxed = true) {
            every { stepId } returns "PENDING_ACTIVATION_CODE_INITIAL"
        }
        every { personalDetailsModule.getAction("next") } returns pendingActivationAction
        viewModel.onUiUpdateEvent(SequentialPageEvent.NextClickEvent("Continue", "next", true))
        testScheduler.advanceUntilIdle()
        verify { journeyActionBuilder.build().executeAction(pendingActivationAction, "personal-details-module", any()) }
    }

    @Test
    fun `testProcessContinueEvent_handlesProductSelectionSave`() = testScope.runTest {
        val viewModel = createViewModel()
        setupModuleForFieldTesting(viewModel)
        val productSelectionAction = mockk<MvcAction>(relaxed = true) {
            every { stepId } returns "PRODUCT_SELECTION_SAVE"
        }
        every { personalDetailsModule.getAction("next") } returns productSelectionAction
        val productSelectionResponse = mockk<SequentialPageModuleResponse>(relaxed = true) {
            every { id } returns "selectedProduct"
        }
        every { personalDetailsModule.sequentialPageDataList } returns listOf(productSelectionResponse)
        val selectedSavingsProduct = mockk<SequentialPageInvestmentProduct>(relaxed = true) {
            every { value } returns "savings_account"
        }
        val stateWithSelectedProduct = viewModel.pageState.value.copy(
            productPickerState = viewModel.pageState.value.productPickerState.copy(
                selectedProductData = selectedSavingsProduct
            )
        )
        every { payLoadProcessor.createPayLoad("selectedProduct", "savings_account") } returns mockk()
        viewModel.onUiUpdateEvent(SequentialPageEvent.NextClickEvent("Continue", "next", true))
        testScheduler.advanceUntilIdle()
        verify { payLoadProcessor.createPayLoad("selectedProduct", "savings_account") }
    }

    @Test
    fun `testProcessContinueEvent_handlesDefaultPayloadCreation`() = testScope.runTest {
        val viewModel = createViewModel()
        setupModuleForFieldTesting(viewModel)
        val submitPersonalDetailsAction = mockk<MvcAction>(relaxed = true) {
            every { stepId } returns "PERSONAL_DETAILS_SUBMIT"
        }
        every { personalDetailsModule.getAction("next") } returns submitPersonalDetailsAction
        every { payLoadProcessor.createPayLoad(any<List<SequentialPageFieldCaptureItem>>()) } returns mockk()
        viewModel.onUiUpdateEvent(SequentialPageEvent.NextClickEvent("Continue", "next", true))
        testScheduler.advanceUntilIdle()
        verify { payLoadProcessor.createPayLoad(any<List<SequentialPageFieldCaptureItem>>()) }
    }

    @Test
    fun `testProcessContinueEvent_handlesNullAction`() = testScope.runTest {
        val viewModel = createViewModel()
        setupModuleForFieldTesting(viewModel)
        every { personalDetailsModule.getAction("next") } returns null
        viewModel.onUiUpdateEvent(SequentialPageEvent.NextClickEvent("Continue", "next", true))
        testScheduler.advanceUntilIdle()
        verify(exactly = 0) { journeyActionBuilder.build().executeAction(any(), any(), any()) }
    }

    @Test
    fun `testProcessContinueEvent_updatesProgressState`() = testScope.runTest {
        val viewModel = createViewModel()
        setupModuleForFieldTesting(viewModel)
        val submitAction = mockk<MvcAction>(relaxed = true)
        every { personalDetailsModule.getAction("next") } returns submitAction
        every { payLoadProcessor.createPayLoad(any<List<SequentialPageFieldCaptureItem>>()) } returns mockk()
        viewModel.onUiUpdateEvent(SequentialPageEvent.NextClickEvent("Continue", "next", false))
        testScheduler.advanceUntilIdle()
        verify { submitAction.showProgress = false }
    }

    @Test
    fun `testProcessOnBackEvent_logsBackButtonClick`() = testScope.runTest {
        val viewModel = createViewModel()
        setupModuleForFieldTesting(viewModel)
        viewModel.onUiUpdateEvent(SequentialPageEvent.PageBackEvent("back", "Back"))
        testScheduler.advanceUntilIdle()
        verify { analyticsManager.logButtonClick("back") }
    }

    @Test
    fun `testProcessOnBackEvent_launchesJourney`() = testScope.runTest {
        val viewModel = createViewModel()
        setupModuleForFieldTesting(viewModel)
        val backNavigationAction = mockk<MvcAction>(relaxed = true)
        every { personalDetailsModule.getAction("back") } returns backNavigationAction
        viewModel.onUiUpdateEvent(SequentialPageEvent.PageBackEvent("back", "Back"))
        testScheduler.advanceUntilIdle()
        verify { journeyActionBuilder.build().executeAction(backNavigationAction, "personal-details-module", any()) }
    }

    @Test
    fun `testProcessOnBackEvent_handlesNullAction`() = testScope.runTest {
        val viewModel = createViewModel()
        setupModuleForFieldTesting(viewModel)
        every { personalDetailsModule.getAction("back") } returns null
        viewModel.onUiUpdateEvent(SequentialPageEvent.PageBackEvent("back", "Back"))
        testScheduler.advanceUntilIdle()
        verify(exactly = 0) { journeyActionBuilder.build().executeAction(any(), any(), any()) }
    }

    @Test
    fun `testProcessOnBackEvent_passesEmptyRequest`() = testScope.runTest {
        val viewModel = createViewModel()
        setupModuleForFieldTesting(viewModel)
        val backNavigationAction = mockk<MvcAction>(relaxed = true)
        every { personalDetailsModule.getAction("back") } returns backNavigationAction
        viewModel.onUiUpdateEvent(SequentialPageEvent.PageBackEvent("back", "Back"))
        testScheduler.advanceUntilIdle()
        verify { personalDetailsModule.attachPayLoadToMvcAction(any<JourneyFrameworkGenericHandleAction.EmptyRequest>(), backNavigationAction) }
    }

    @Test
    fun `testProcessHyperlinkClickEvent_logsNoneOfTheseForProductSelection`() = testScope.runTest {
        val viewModel = createViewModel()
        setupModuleForFieldTesting(viewModel)
        val productSelectionAction = mockk<MvcAction>(relaxed = true) {
            every { stepId } returns "PRODUCT_SELECTION_SAVE"
        }
        every { personalDetailsModule.getAction("NEXT") } returns productSelectionAction
        every { personalDetailsModule.getAction("NONE_OF_THESE") } returns mockk(relaxed = true)
        viewModel.onUiUpdateEvent(SequentialPageEvent.HyperlinkClicked)
        testScheduler.advanceUntilIdle()
        verify { analyticsManager.logHyperLink("NONE_OF_THESE") }
    }

    @Test
    fun `testProcessHyperlinkClickEvent_logsResendForOtherSteps`() = testScope.runTest {
        val viewModel = createViewModel()
        setupModuleForFieldTesting(viewModel)
        val otpVerificationAction = mockk<MvcAction>(relaxed = true) {
            every { stepId } returns "OTP_VERIFICATION_STATUS_CHECK"
        }
        every { personalDetailsModule.getAction("NEXT") } returns otpVerificationAction
        every { personalDetailsModule.getAction("RESEND") } returns mockk(relaxed = true)
        viewModel.onUiUpdateEvent(SequentialPageEvent.HyperlinkClicked)
        testScheduler.advanceUntilIdle()
        verify { analyticsManager.logHyperLink("RESEND") }
    }

    @Test
    fun `testProcessHyperlinkClickEvent_launchesCorrectJourney`() = testScope.runTest {
        val viewModel = createViewModel()
        setupModuleForFieldTesting(viewModel)
        val resendOtpAction = mockk<MvcAction>(relaxed = true)
        every { personalDetailsModule.getAction("NEXT") } returns mockk(relaxed = true) {
            every { stepId } returns "OTP_VERIFICATION_STATUS_CHECK"
        }
        every { personalDetailsModule.getAction("RESEND") } returns resendOtpAction
        viewModel.onUiUpdateEvent(SequentialPageEvent.HyperlinkClicked)
        testScheduler.advanceUntilIdle()
        verify { journeyActionBuilder.build().executeAction(resendOtpAction, "personal-details-module", any()) }
    }

    @Test
    fun `testProcessHyperlinkClickEvent_handlesNullAction`() = testScope.runTest {
        val viewModel = createViewModel()
        setupModuleForFieldTesting(viewModel)
        every { personalDetailsModule.getAction("NEXT") } returns mockk(relaxed = true) {
            every { stepId } returns "OTP_VERIFICATION_STATUS_CHECK"
        }
        every { personalDetailsModule.getAction("RESEND") } returns null
        viewModel.onUiUpdateEvent(SequentialPageEvent.HyperlinkClicked)
        testScheduler.advanceUntilIdle()
        verify(exactly = 0) { journeyActionBuilder.build().executeAction(any(), any(), any()) }
    }

    @Test
    fun `testClearError_resetsErrorState`() = testScope.runTest {
        val viewModel = createViewModel()
        viewModel.pageState.value.copy(
            sequentialPageState = viewModel.pageState.value.sequentialPageState.copy(error = "Validation failed")
        )
        viewModel.onUiUpdateEvent(SequentialPageEvent.ClearError)
        testScheduler.advanceUntilIdle()
        assertNull(viewModel.pageState.value.sequentialPageState.error)
    }

    @Test
    fun `testClearError_maintainsOtherStates`() = testScope.runTest {
        val viewModel = createViewModel()
        setupModuleForFieldTesting(viewModel)
        val originalItems = viewModel.pageState.value.sequentialPageState.items
        viewModel.onUiUpdateEvent(SequentialPageEvent.ClearError)
        testScheduler.advanceUntilIdle()
        assertEquals(originalItems, viewModel.pageState.value.sequentialPageState.items)
    }

    @Test
    fun `testErrorState_updatesCorrectly`() = testScope.runTest {
        val invalidEmailItem = emailFieldItem.copy(
            errorState = SequentialPageUiErrorState(isError = true, errorMessage = "Invalid email format")
        )
        every { validationDelegator.validateAllFields(any(), any(), any()) } returns
            SequentialPageValidationResult(listOf(invalidEmailItem), false)
        val viewModel = createViewModel()
        setupModuleForFieldTesting(viewModel)
        viewModel.onUiUpdateEvent(SequentialPageEvent.NextClickEvent("Continue", "next", true))
        testScheduler.advanceUntilIdle()
        assertTrue(viewModel.pageState.value.sequentialPageState.items.any { it.errorState?.isError == true })
    }

    @Test
    fun `testErrorState_showsFirstErrorIndex`() = testScope.runTest {
        val validEmailItem = emailFieldItem.copy(key = "emailAddress")
        val invalidPhoneItem = phoneFieldItem.copy(
            key = "phoneNumber",
            errorState = SequentialPageUiErrorState(isError = true, errorMessage = "Invalid phone number")
        )
        every { validationDelegator.validateAllFields(any(), any(), any()) } returns
            SequentialPageValidationResult(listOf(validEmailItem, invalidPhoneItem), false)
        val viewModel = createViewModel()
        setupModuleForFieldTesting(viewModel)
        viewModel.onUiUpdateEvent(SequentialPageEvent.NextClickEvent("Continue", "next", true))
        testScheduler.advanceUntilIdle()
        assertEquals(1, viewModel.pageState.value.firstErrorInputViewIndex)
    }

    @Test
    fun `testValidation_allFieldsValid`() = testScope.runTest {
        every { validationDelegator.validateAllFields(any(), any(), any()) } returns
            SequentialPageValidationResult(listOf(emailFieldItem, phoneFieldItem), true)
        val viewModel = createViewModel()
        setupModuleForFieldTesting(viewModel)
        every { payLoadProcessor.createPayLoad(any<List<SequentialPageFieldCaptureItem>>()) } returns mockk()
        viewModel.onUiUpdateEvent(SequentialPageEvent.NextClickEvent("Continue", "next", true))
        testScheduler.advanceUntilIdle()
        verify { journeyActionBuilder.build().executeAction(any(), any(), any()) }
    }

    @Test
    fun `testValidation_someFieldsInvalid`() = testScope.runTest {
        val invalidEmailItem = emailFieldItem.copy(
            errorState = SequentialPageUiErrorState(isError = true)
        )
        every { validationDelegator.validateAllFields(any(), any(), any()) } returns
            SequentialPageValidationResult(listOf(invalidEmailItem), false)
        val viewModel = createViewModel()
        setupModuleForFieldTesting(viewModel)
        viewModel.onUiUpdateEvent(SequentialPageEvent.NextClickEvent("Continue", "next", true))
        testScheduler.advanceUntilIdle()
        verify(exactly = 0) { journeyActionBuilder.build().executeAction(any(), any(), any()) }
    }

    @Test
    fun `testValidation_emptyRequiredFields`() = testScope.runTest {
        val emptyEmailItem = emailFieldItem.copy(
            value = "",
            errorState = SequentialPageUiErrorState(isError = true, errorMessage = "Email address is required")
        )
        every { validationDelegator.validateAllFields(any(), any(), any()) } returns
            SequentialPageValidationResult(listOf(emptyEmailItem), false)
        val viewModel = createViewModel()
        setupModuleForFieldTesting(viewModel)
        viewModel.onUiUpdateEvent(SequentialPageEvent.NextClickEvent("Continue", "next", true))
        testScheduler.advanceUntilIdle()
        assertTrue(viewModel.pageState.value.sequentialPageState.items.any {
            it.errorState?.errorMessage == "Email address is required"
        })
    }

    @Test
    fun `testValidation_regexFailure`() = testScope.runTest {
        val invalidFormatEmailItem = emailFieldItem.copy(
            regex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
            value = "invalid-email",
            errorState = SequentialPageUiErrorState(isError = true, errorMessage = "Please enter a valid email address")
        )
        every { validationDelegator.validateAllFields(any(), any(), any()) } returns
            SequentialPageValidationResult(listOf(invalidFormatEmailItem), false)
        val viewModel = createViewModel()
        setupModuleForFieldTesting(viewModel)
        viewModel.onUiUpdateEvent(SequentialPageEvent.NextClickEvent("Continue", "next", true))
        testScheduler.advanceUntilIdle()
        assertTrue(viewModel.pageState.value.sequentialPageState.items.any {
            it.errorState?.errorMessage == "Please enter a valid email address"
        })
    }

    @Test
    fun `testValidation_dateValidation`() = testScope.runTest {
        val dateOfBirthItem = emailFieldItem.copy(
            type = SequentialPageCaptureComponentType.DATE,
            minDate = "1900-01-01",
            maxDate = "2005-12-31",
            value = "2010-01-01",
            errorState = SequentialPageUiErrorState(isError = true, errorMessage = "Date must be before 2006")
        )
        every { validationDelegator.validateAllFields(any(), any(), any()) } returns
            SequentialPageValidationResult(listOf(dateOfBirthItem), false)
        val viewModel = createViewModel()
        setupModuleForFieldTesting(viewModel)
        viewModel.onUiUpdateEvent(SequentialPageEvent.NextClickEvent("Continue", "next", true))
        testScheduler.advanceUntilIdle()
        assertTrue(viewModel.pageState.value.sequentialPageState.items.any {
            it.errorState?.errorMessage == "Date must be before 2006"
        })
    }

    @Test
    fun `testValidation_investmentAccountValidation`() = testScope.runTest {
        val invalidAccountItem = emailFieldItem.copy(
            value = "1234567",
            errorState = SequentialPageUiErrorState(isError = true, errorMessage = "Please enter a valid 8-digit account number")
        )
        every { validationDelegator.validateAllFields(any(), any(), any()) } returns
            SequentialPageValidationResult(listOf(invalidAccountItem), false)
        val viewModel = createViewModel()
        setupModuleForFieldTesting(viewModel)
        viewModel.onUiUpdateEvent(SequentialPageEvent.NextClickEvent("Continue", "next", true))
        testScheduler.advanceUntilIdle()
        verify { validationDelegator.validateAllFields(any(), any(), any()) }
    }

    @Test
    fun `testValidation_updatesErrorStates`() = testScope.runTest {
        val validEmailItem = emailFieldItem.copy(key = "emailAddress", errorState = SequentialPageUiErrorState(isError = false))
        val invalidPhoneItem = phoneFieldItem.copy(key = "phoneNumber", errorState = SequentialPageUiErrorState(isError = true))
        every { validationDelegator.validateAllFields(any(), any(), any()) } returns
            SequentialPageValidationResult(listOf(validEmailItem, invalidPhoneItem), false)
        val viewModel = createViewModel()
        setupModuleForFieldTesting(viewModel)
        viewModel.onUiUpdateEvent(SequentialPageEvent.NextClickEvent("Continue", "next", true))
        testScheduler.advanceUntilIdle()
        val updatedItems = viewModel.pageState.value.sequentialPageState.items
        assertEquals(listOf(validEmailItem, invalidPhoneItem).size, updatedItems.size)
        assertEquals(validEmailItem.errorState?.isError, updatedItems[0].errorState?.isError)
        assertEquals(invalidPhoneItem.errorState?.isError, updatedItems[1].errorState?.isError)
    }

    @Test
    fun `testAnalytics_screenLoadLogged`() = testScope.runTest {
        val viewModel = createViewModel()
        viewModel.onUiUpdateEvent(SequentialPageEvent.ModuleStart(personalDetailsModule))
        testScheduler.advanceUntilIdle()
        verify { analyticsManager.logScreenLoad("personal_details_page") }
    }

    @Test
    fun `testAnalytics_buttonClickLogged`() = testScope.runTest {
        val viewModel = createViewModel()
        setupModuleForFieldTesting(viewModel)
        viewModel.onUiUpdateEvent(SequentialPageEvent.NextClickEvent("Submit Application", "submit", true))
        testScheduler.advanceUntilIdle()
        verify { analyticsManager.logButtonClick("Submit Application") }
    }

    @Test
    fun `testAnalytics_accordionInteractionLogged`() = testScope.runTest {
        val viewModel = createViewModel()
        viewModel.logAccordionStateChange(true, "Account Information Help")
        verify { analyticsManager.logAccordionInteraction("Account Information Help", "ACCORDION_EXPAND") }
    }

    @Test
    fun `testAnalytics_inputFocusChangeLogged`() = testScope.runTest {
        val viewModel = createViewModel()
        viewModel.logInputViewFocusChange("Email Address Field")
        verify { analyticsManager.logInputViewChangeFocus("Email Address Field") }
    }

    @Test
    fun `testAnalytics_hyperlinkClickLogged`() = testScope.runTest {
        val viewModel = createViewModel()
        setupModuleForFieldTesting(viewModel)
        every { personalDetailsModule.getAction("NEXT") } returns mockk(relaxed = true) {
            every { stepId } returns "PRODUCT_SELECTION_SAVE"
        }
        every { personalDetailsModule.getAction("NONE_OF_THESE") } returns mockk(relaxed = true)
        viewModel.onUiUpdateEvent(SequentialPageEvent.HyperlinkClicked)
        testScheduler.advanceUntilIdle()
        verify { analyticsManager.logHyperLink("NONE_OF_THESE") }
    }

    @Test
    fun `testAnalytics_inputErrorLogged`() = testScope.runTest {
        val viewModel = createViewModel()
        viewModel.logInputViewFocusChange("Phone Number Field")
        verify { analyticsManager.logInputViewChangeFocus("Phone Number Field") }
    }

    @Test
    fun `testJourneyAction_executesWithCorrectParameters`() = testScope.runTest {
        val viewModel = createViewModel()
        setupModuleForFieldTesting(viewModel)
        val submitAction = mockk<MvcAction>(relaxed = true)
        every { personalDetailsModule.getAction("next") } returns submitAction
        every { payLoadProcessor.createPayLoad(any<List<SequentialPageFieldCaptureItem>>()) } returns mockk()
        viewModel.onUiUpdateEvent(SequentialPageEvent.NextClickEvent("Continue", "next", true))
        testScheduler.advanceUntilIdle()
        verify { journeyActionBuilder.build().executeAction(submitAction, "personal-details-module", "CONTROLLER_ID") }
    }

    @Test
    fun `testJourneyAction_attachesPayloadCorrectly`() = testScope.runTest {
        val viewModel = createViewModel()
        setupModuleForFieldTesting(viewModel)
        val submitAction = mockk<MvcAction>(relaxed = true)
        val personalDetailsPayload = mockk<SequentialPageCaptureRequestData>()
        every { personalDetailsModule.getAction("next") } returns submitAction
        every { payLoadProcessor.createPayLoad(any<List<SequentialPageFieldCaptureItem>>()) } returns personalDetailsPayload
        viewModel.onUiUpdateEvent(SequentialPageEvent.NextClickEvent("Continue", "next", true))
        testScheduler.advanceUntilIdle()
        verify { personalDetailsModule.attachPayLoadToMvcAction(personalDetailsPayload, submitAction) }
    }

    @Test
    fun `testJourneyAction_handlesNullBuilder`() = testScope.runTest {
        val viewModelWithNullBuilder = SequentialPageCaptureViewModel(
            fieldDataProcessor, productHandler, investmentAccountHandler,
            payLoadProcessor, analyticsManager, validationDelegator,
            null, moduleDataProcessor
        ).apply { Dispatchers.setMain(testDispatcher) }
        setupModuleForFieldTesting(viewModelWithNullBuilder)
        viewModelWithNullBuilder.onUiUpdateEvent(SequentialPageEvent.NextClickEvent("Continue", "next", true))
        testScheduler.advanceUntilIdle()
        verify(exactly = 0) { journeyActionBuilder.build() }
    }

    @Test
    fun `testJourneyAction_handlesNullAction`() = testScope.runTest {
        val viewModel = createViewModel()
        setupModuleForFieldTesting(viewModel)
        every { personalDetailsModule.getAction("next") } returns null
        viewModel.onUiUpdateEvent(SequentialPageEvent.NextClickEvent("Continue", "next", true))
        testScheduler.advanceUntilIdle()
        verify(exactly = 0) { journeyActionBuilder.build().executeAction(any(), any(), any()) }
    }

    private suspend fun setupModuleForFieldTesting(viewModel: SequentialPageCaptureViewModel) {
        viewModel.onUiUpdateEvent(SequentialPageEvent.ModuleStart(personalDetailsModule))
        testScheduler.advanceUntilIdle()
    }

    companion object {
        private const val CONTROLLER_ID = "CONTROLLER_ID"
    }
}
