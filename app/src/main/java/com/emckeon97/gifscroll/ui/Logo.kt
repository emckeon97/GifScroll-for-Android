package com.emckeon97.gifscroll.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/** Instagram-style gradient used for the "Gif" half of the wordmark. */
val GifScrollGradient = Brush.linearGradient(
    listOf(
        Color(0xFF833AB4), // purple
        Color(0xFFFD1D1D), // pink-red
        Color(0xFFFCB045)  // orange
    )
)

/**
 * The GifScroll wordmark: "Gif" in gradient, "Scroll" in white.
 * Bold, clean, dark-mode first — Instagram-style.
 */
@Composable
fun GifScrollLogo(
    fontSize: TextUnit = 28.sp,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Gif",
            style = TextStyle(
                brush = GifScrollGradient,
                fontSize = fontSize,
                fontWeight = FontWeight.Black
            )
        )
        Text(
            text = "Scroll",
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
    }
}
