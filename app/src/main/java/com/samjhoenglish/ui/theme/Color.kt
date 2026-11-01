package com.samjhoenglish.ui.theme

import androidx.compose.ui.graphics.Color

// Brand
val Saffron = Color(0xFFE05A14) // buttons; dark enough for white bold text
val SaffronLight = Color(0xFFFF8A3D)
val Teal = Color(0xFF14858A)
val Sunny = Color(0xFFFFC93C)
val Cream = Color(0xFFFFF8EE)
val Peach = Color(0xFFFFEBD6)
val Ink = Color(0xFF2D2A32)
val InkSoft = Color(0xFF6B6474)
val Line = Color(0xFFEBDCC8)
val Leaf = Color(0xFF3E9B46)

// Tinku
val Sky = Color(0xFF4FB3FF)
val SkyDark = Color(0xFF2E8FDB)
val Navy = Color(0xFF1E2A4A)
val Glow = Color(0xFF7DF9FF)
val Blush = Color(0xFFFF8FA3)

// Sentence word roles: the same colour in Hindi and English shows which word does which job.
val RoleWho = Color(0xFFBFE3FF) // s: who
val RoleAction = Color(0xFFFFD2A8) // v: action / is-am-are
val RoleRest = Color(0xFFC8EBC4) // o: the rest
val RoleQuestion = Color(0xFFE2D4FF) // q: question words
val RoleHelper = Color(0xFFEDE6DA) // x: small helper words

fun roleColor(role: String): Color = when (role) {
    "s" -> RoleWho
    "v" -> RoleAction
    "o" -> RoleRest
    "q" -> RoleQuestion
    else -> RoleHelper
}

// Level colours on the journey map
val LevelColors = listOf(
    Color(0xFFFFC93C),
    Color(0xFF7ED7C1),
    Color(0xFFFFA8A8),
    Color(0xFFA9C8FF),
)
