package pro.sihao.jarvis.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pro.sihao.jarvis.permission.PermissionManager
import pro.sihao.jarvis.ui.model.InputMode
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class MessageInputTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val grantedPermissions = PermissionManager.MediaPermissionsSummary(
        voiceRecordingStatus = PermissionManager.PermissionStatus.GRANTED,
        cameraStatus = PermissionManager.PermissionStatus.GRANTED,
        galleryStatus = PermissionManager.PermissionStatus.GRANTED,
        hasMicrophoneHardware = true,
        hasCameraHardware = true
    )

    @Test
    fun modeToggleSwitchesBetweenTextAndVoice() {
        var mode = InputMode.TEXT
        composeRule.setContent {
            MessageInput(
                message = "",
                inputMode = mode,
                isLoading = false,
                isRecording = false,
                recordingDuration = 0,
                onMessageChange = {},
                onSendClick = {},
                onInputModeChange = { mode = it },
                onEmojiSelected = {},
                onVoiceRecordStart = {},
                onVoiceRecordStop = {},
                onVoiceRecordCancel = {},
                onCameraClick = {},
                onGalleryClick = {},
                permissionStatus = grantedPermissions
            )
        }

        composeRule.onNodeWithTag("input_mode_toggle").performClick()
        composeRule.runOnIdle { assertEquals(InputMode.VOICE, mode) }
    }

    @Test
    fun holdToTalkReleaseSendsWhenNotCancelled() {
        var started = false
        var stopped = false
        var cancelled = false

        composeRule.setContent {
            MessageInput(
                message = "",
                inputMode = InputMode.VOICE,
                isLoading = false,
                isRecording = true,
                recordingDuration = 1500,
                onMessageChange = {},
                onSendClick = {},
                onInputModeChange = {},
                onEmojiSelected = {},
                onVoiceRecordStart = { started = true },
                onVoiceRecordStop = { stopped = true },
                onVoiceRecordCancel = { cancelled = true },
                onCameraClick = {},
                onGalleryClick = {},
                permissionStatus = grantedPermissions
            )
        }

        composeRule.onNodeWithTag("hold_to_talk").performTouchInput {
            down(center)
            moveBy(Offset(5f, 0f))
            up()
        }

        composeRule.runOnIdle {
            assertTrue(started)
            assertTrue(stopped)
            assertFalse(cancelled)
        }
    }

    @Test
    fun holdToTalkSlideLeftCancelsRecording() {
        var started = false
        var stopped = false
        var cancelled = false

        composeRule.setContent {
            MessageInput(
                message = "",
                inputMode = InputMode.VOICE,
                isLoading = false,
                isRecording = true,
                recordingDuration = 1500,
                onMessageChange = {},
                onSendClick = {},
                onInputModeChange = {},
                onEmojiSelected = {},
                onVoiceRecordStart = { started = true },
                onVoiceRecordStop = { stopped = true },
                onVoiceRecordCancel = { cancelled = true },
                onCameraClick = {},
                onGalleryClick = {},
                permissionStatus = grantedPermissions
            )
        }

        composeRule.onNodeWithTag("hold_to_talk").performTouchInput {
            down(center)
            moveBy(Offset(-400f, 0f))
            up()
        }

        composeRule.runOnIdle {
            assertTrue(started)
            assertTrue(cancelled)
            assertFalse(stopped)
        }
    }

    @Test
    fun attachMenuInvokesCameraAndGallery() {
        var cameraTapped = false
        var galleryTapped = false

        composeRule.setContent {
            MessageInput(
                message = "",
                inputMode = InputMode.TEXT,
                isLoading = false,
                isRecording = false,
                recordingDuration = 0,
                onMessageChange = {},
                onSendClick = {},
                onInputModeChange = {},
                onEmojiSelected = {},
                onVoiceRecordStart = {},
                onVoiceRecordStop = {},
                onVoiceRecordCancel = {},
                onCameraClick = { cameraTapped = true },
                onGalleryClick = { galleryTapped = true },
                permissionStatus = grantedPermissions
            )
        }

        composeRule.onNodeWithTag("attach_button").performClick()
        composeRule.onNodeWithTag("attach_camera").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("attach_button").performClick()
        composeRule.onNodeWithTag("attach_gallery").assertIsDisplayed().performClick()

        composeRule.runOnIdle {
            assertTrue(cameraTapped)
            assertTrue(galleryTapped)
        }
    }
}
