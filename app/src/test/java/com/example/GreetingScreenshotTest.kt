package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.model.FrameStats
import com.example.omt.ConnectionStatus
import com.example.ui.components.OmtTopBar
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        OmtTopBar(
          selectedStream = null,
          discoveredStreams = emptyList(),
          connectionStatus = ConnectionStatus.IDLE,
          frameStats = FrameStats(),
          isScanning = false,
          isFrozen = false,
          showHud = true,
          onSelectStream = {},
          onRefreshScan = {},
          onOpenManualDialog = {},
          onToggleFreeze = {},
          onToggleHud = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}

