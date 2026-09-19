package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.chess.engine.ChessEngine
import com.example.chess.ui.BoardTheme
import com.example.chess.ui.components.ChessBoardView
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
  fun chess_board_screenshot() {
    val state = ChessEngine.createInitialState()
    composeTestRule.setContent {
      MyApplicationTheme {
        ChessBoardView(
          state = state,
          selectedSquare = null,
          legalMoves = emptyList(),
          lastMove = null,
          isFlipped = false,
          boardTheme = BoardTheme.NAVY,
          onSquareClick = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/chess_board.png")
  }
}
