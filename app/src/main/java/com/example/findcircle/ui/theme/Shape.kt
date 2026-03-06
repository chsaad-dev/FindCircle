package com.example.findcircle.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ─── FindCircle Shape System ───────────────────────────────────────────
// small  → Chips, Tags, Small badges        (8dp)
// medium → Buttons, Inputs, Small cards     (12dp)
// large  → Cards, Modals, Bottom Sheets     (16dp)

val Shapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp)
)
