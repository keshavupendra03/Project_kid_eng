package com.samjhoenglish.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.samjhoenglish.R

// Baloo 2: rounded, child-friendly, covers both Devanagari and Latin letters (SIL Open Font License).
// It is a variable font; Android 8+ uses the exact weight, Android 7 shows the regular weight.
@OptIn(ExperimentalTextApi::class)
private fun baloo(weight: FontWeight) = Font(
    resId = R.font.baloo2,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val Baloo = FontFamily(
    baloo(FontWeight.Normal),
    baloo(FontWeight.Medium),
    baloo(FontWeight.SemiBold),
    baloo(FontWeight.Bold),
    baloo(FontWeight.ExtraBold),
)

private val base = Typography()

// Sizes are a step larger than Material defaults so young readers can read comfortably.
val SamjhoTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = Baloo, fontWeight = FontWeight.ExtraBold),
    displayMedium = base.displayMedium.copy(fontFamily = Baloo, fontWeight = FontWeight.ExtraBold),
    displaySmall = base.displaySmall.copy(fontFamily = Baloo, fontWeight = FontWeight.Bold),
    headlineLarge = base.headlineLarge.copy(fontFamily = Baloo, fontWeight = FontWeight.Bold),
    headlineMedium = base.headlineMedium.copy(fontFamily = Baloo, fontWeight = FontWeight.Bold),
    headlineSmall = base.headlineSmall.copy(fontFamily = Baloo, fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 24.sp),
    titleMedium = base.titleMedium.copy(fontFamily = Baloo, fontWeight = FontWeight.SemiBold, fontSize = 19.sp),
    titleSmall = base.titleSmall.copy(fontFamily = Baloo, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = base.bodyLarge.copy(fontFamily = Baloo, fontSize = 19.sp, lineHeight = 26.sp),
    bodyMedium = base.bodyMedium.copy(fontFamily = Baloo, fontSize = 17.sp, lineHeight = 23.sp),
    bodySmall = base.bodySmall.copy(fontFamily = Baloo, fontSize = 15.sp),
    labelLarge = base.labelLarge.copy(fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 18.sp),
    labelMedium = base.labelMedium.copy(fontFamily = Baloo, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    labelSmall = base.labelSmall.copy(fontFamily = Baloo, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
)
