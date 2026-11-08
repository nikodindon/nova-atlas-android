package com.niko.novaatlas

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.material3.MaterialTheme
import org.junit.Rule
import org.junit.Test

/** Small isolated components: no MainActivity, ads, service, API or radio connection. */
class CompanionComposeTest {
    @get:Rule val compose = createComposeRule()
    @Test fun unicodeAndInertHostileTitle() {
        compose.setContent { MaterialTheme { ArticleCard(Article("h","","日本語","<script>alert(1)</script> العربية 🎙️",link="javascript:alert(1)",summary="Résumé")) } }
        compose.onNodeWithText("<script>alert(1)</script> العربية 🎙️").assertIsDisplayed()
    }
    @Test fun unknownRightsRemainFallback() {
        compose.setContent { MaterialTheme { ApprovedImage(PublicImage("https://example.invalid/private.jpg",false)) } }
        compose.onNodeWithText("◈ Illustration indisponible").assertIsDisplayed()
    }
}
