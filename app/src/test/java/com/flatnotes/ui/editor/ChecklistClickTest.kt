package com.flatnotes.ui.editor

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.flatnotes.data.NoteBlock
import com.flatnotes.data.sampleNotes
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ChecklistClickTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun tappingTheBoxTogglesThatItem() {
        val toggled = mutableListOf<String>()
        compose.setContent {
            EditorBody(
                blocks = listOf(
                    NoteBlock.Checklist(id = "milk", text = "牛奶"),
                    NoteBlock.Checklist(id = "bread", text = "面包", done = true),
                ),
                mediaFile = { File("missing") },
                loadBitmap = { null },
                focusRequesters = mutableMapOf(),
                mediaMenuId = null,
                captionEditing = false,
                onFocus = {},
                onTextChange = { _, _ -> },
                onRemoveEmpty = {},
                onToggle = { toggled += it },
                onMediaMenu = {},
                onEnlarge = {},
                onShrink = {},
                onCaptionEdit = {},
                onCaptionChange = { _, _ -> },
                onRemoveMedia = {},
                onAppendText = {},
            )
        }

        compose.onNodeWithContentDescription("未完成").assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription("已完成").assertIsDisplayed().performClick()
        assertEquals(listOf("milk", "bread"), toggled)
    }

    @Test
    fun spacingPanelListsTheThreeControls() {
        compose.setContent {
            EditorScreen(
                note = sampleNotes().first(),
                mediaFile = { File("missing") },
                loadBitmap = { null },
                onTitleChange = {},
                onBlocksChange = {},
                onImportImage = { null },
                onSaveDoodle = { "doodle.png" },
                onDeleteMedia = {},
                onDelete = {},
                onBack = {},
            )
        }
        compose.onNodeWithContentDescription("更多").performClick()
        compose.onNodeWithText("间距").performClick()
        compose.onNodeWithText("正文间距").assertIsDisplayed()
        compose.onNodeWithText("清单间距").assertIsDisplayed()
        compose.onNodeWithText("块间距（正文与清单/图片/涂鸦）").assertIsDisplayed()
    }
}
