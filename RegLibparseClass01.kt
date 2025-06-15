import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(instrumentedPackages = ["androidx.loader.content"])
@RunWith(RobolectricTestRunner::class)
class SequentialPageInputFieldRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun onViewFocusChanged_called_whenKeyNotNull() {
        var focusChangedKey: String? = null
        val item = SequentialPageFieldCaptureItem(
            key = "testKey",
            keyBoardType = KeyboardType.Text,
            errorState = ErrorState(isError = false, errorMessage = null)
        )

        composeTestRule.setContent {
            SequentialPageInputField(
                modifier = Modifier.testTag("InputField"),
                item = item,
                onValueChanged = { _, _, _ -> },
                outputTransformation = null,
                onViewFocusChanged = { key -> focusChangedKey = key },
                fieldValue = "",
                contextualTitleFallback = null,
                componentLabel = null
            )
        }

        composeTestRule.onNodeWithTag("InputField").performSemanticsAction(SemanticsActions.OnFocusChanged)
        assert(focusChangedKey == "testKey")
    }

    @Test
    fun onViewFocusChanged_notCalled_whenKeyNull() {
        var focusChangedKey: String? = null
        val item = SequentialPageFieldCaptureItem(
            key = null,
            keyBoardType = KeyboardType.Text,
            errorState = ErrorState(isError = false, errorMessage = null)
        )

        composeTestRule.setContent {
            SequentialPageInputField(
                modifier = Modifier.testTag("InputField"),
                item = item,
                onValueChanged = { _, _, _ -> },
                outputTransformation = null,
                onViewFocusChanged = { key -> focusChangedKey = key },
                fieldValue = "",
                contextualTitleFallback = null,
                componentLabel = null
            )
        }

        composeTestRule.onNodeWithTag("InputField").performSemanticsAction(SemanticsActions.OnFocusChanged)
        assert(focusChangedKey == null)
    }

    @Test
    fun onValueChanged_called_whenKeyNotNull() {
        var valueChangedKey: String? = null
        var valueChangedValue: String? = null
        var valueChangedTransformation: OutputTransformation? = null
        val item = SequentialPageFieldCaptureItem(
            key = "testKey",
            keyBoardType = KeyboardType.Text,
            errorState = ErrorState(isError = false, errorMessage = null)
        )

        composeTestRule.setContent {
            SequentialPageInputField(
                modifier = Modifier.testTag("InputField"),
                item = item,
                onValueChanged = { key, value, transformation ->
                    valueChangedKey = key
                    valueChangedValue = value
                    valueChangedTransformation = transformation
                },
                outputTransformation = OutputTransformation(),
                onViewFocusChanged = null,
                fieldValue = "",
                contextualTitleFallback = null,
                componentLabel = null
            )
        }

        composeTestRule.onNodeWithTag("InputField").performTextInput("hello")
        assert(valueChangedKey == "testKey")
        assert(valueChangedValue == "hello")
        assert(valueChangedTransformation != null)
    }

    @Test
    fun onValueChanged_notCalled_whenKeyNull() {
        var valueChangedKey: String? = null
        val item = SequentialPageFieldCaptureItem(
            key = null,
            keyBoardType = KeyboardType.Text,
            errorState = ErrorState(isError = false, errorMessage = null)
        )

        composeTestRule.setContent {
            SequentialPageInputField(
                modifier = Modifier.testTag("InputField"),
                item = item,
                onValueChanged = { key, _, _ -> valueChangedKey = key },
                outputTransformation = null,
                onViewFocusChanged = null,
                fieldValue = "",
                contextualTitleFallback = null,
                componentLabel = null
            )
        }

        composeTestRule.onNodeWithTag("InputField").performTextInput("hello")
        assert(valueChangedKey == null)
    }

    @Test
    fun displaysErrorAndLabel() {
        val item = SequentialPageFieldCaptureItem(
            key = "testKey",
            keyBoardType = KeyboardType.Text,
            errorState = ErrorState(isError = true, errorMessage = "Error!")
        )

        composeTestRule.setContent {
            SequentialPageInputField(
                modifier = Modifier.testTag("InputField"),
                item = item,
                onValueChanged = { _, _, _ -> },
                outputTransformation = null,
                onViewFocusChanged = null,
                fieldValue = "",
                contextualTitleFallback = null,
                componentLabel = "Label"
            )
        }

        composeTestRule.onNodeWithTag("InputField").assertTextContains("Label")
        composeTestRule.onNodeWithTag("InputField").assertTextContains("Error!")
    }
}
