package com.vscodroid

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Guards the chat-only WebView selection and clipboard injection. */
class CopilotChatTextSelectionTest {

    private fun mainActivity(): String =
        SourceScan.withoutComments(SourceScan.read("src/main/kotlin/com/vscodroid/MainActivity.kt"))

    @Test
    fun `chat selection injection is installed for every workbench document`() {
        val source = mainActivity()
        val wiring = SourceScan.body(source, "private fun injectBridgeToken(")

        assertTrue(
            wiring.contains("injectCopilotChatTextSelection()"),
            "The chat feature must be installed with the per-document injections, including " +
                "after workbench navigation.",
        )
        assertTrue(
            source.contains("private fun injectCopilotChatTextSelection()"),
            "The Android-side chat selection script is missing.",
        )
    }

    @Test
    fun `native selection and clipboard actions are scoped to chat`() {
        val script = SourceScan.body(mainActivity(), "private fun injectCopilotChatTextSelection(")

        for (marker in listOf(
            "user-select: text",
            "-webkit-user-select: text",
            ".interactive-session",
            ".chat-input-container",
            "contextmenu",
            "}, 550);",
            "pointercancel",
            "label: 'Select All'",
            "label: 'Copy'",
            "label: 'Paste'",
            "AndroidBridge.copyToClipboard(token, value)",
            "AndroidBridge.readFromClipboard(token)",
            "ec.updateText",
            "new MutationObserver(markChatText)",
        )) {
            assertTrue(script.contains(marker), "Chat selection script no longer includes `$marker`.")
        }

        assertTrue(
            script.contains("if (node.closest(COMPOSER)) return false"),
            "Composer/input elements must not be treated as response text.",
        )
    }
}