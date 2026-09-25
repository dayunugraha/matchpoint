package com.matchpoint.app.ui.registration

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matchpoint.app.backend.RegistrationRepository
import com.matchpoint.app.ui.theme.GhostButton
import com.matchpoint.app.ui.theme.GlassCard
import com.matchpoint.app.ui.theme.GoogleSans
import com.matchpoint.app.ui.theme.PrimaryButton
import com.matchpoint.app.ui.theme.SmoothCorner
import com.matchpoint.app.ui.theme.Theme
import com.matchpoint.app.ui.theme.ThemedScreenBackground
import com.matchpoint.app.ui.theme.themeTitle

/**
 * First-launch onboarding, not a login screen — no password, no verification step. Shown by
 * MainActivity in place of [com.matchpoint.app.ui.navigation.RootNavGraph] whenever
 * [RegistrationRepository.isRegistered] is false; disappears on its own once registration
 * succeeds, since that flips the StateFlow the screen underneath is watching.
 */
@Composable
fun RegistrationScreen(viewModel: RegistrationViewModel) {
    val state by viewModel.uiState.collectAsState()
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }

    ThemedScreenBackground {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            when {
                state.isCheckingAuth -> {
                    CircularProgressIndicator(color = Theme.accent)
                }
                state.authFailed -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Couldn't connect", style = themeTitle(20.sp), color = Theme.textPrimary, textAlign = TextAlign.Center)
                        Text(
                            "Check your connection and try again.",
                            fontFamily = GoogleSans,
                            fontSize = 13.sp,
                            color = Theme.textSecondary,
                            textAlign = TextAlign.Center
                        )
                        GhostButton(text = "Retry", onClick = { viewModel.ensureAuth() }, modifier = Modifier.fillMaxWidth())
                    }
                }
                else -> {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Welcome to Match Point", style = themeTitle(24.sp), color = Theme.textPrimary)
                            Text(
                                "Tell us who's playing before your first mabar.",
                                fontFamily = GoogleSans,
                                fontSize = 13.sp,
                                color = Theme.textSecondary
                            )
                        }

                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text("Name", fontFamily = GoogleSans, fontSize = 12.sp, color = Theme.textSecondary)
                            RegistrationField(
                                value = name,
                                onValueChange = { name = it },
                                placeholder = "Your name",
                                capitalization = KeyboardCapitalization.Words,
                                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                            )
                            Text("Email (optional)", fontFamily = GoogleSans, fontSize = 12.sp, color = Theme.textSecondary)
                            RegistrationField(
                                value = email,
                                onValueChange = { email = it },
                                placeholder = "you@example.com",
                                keyboardType = KeyboardType.Email,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        state.errorMessage?.let {
                            Text(it, fontFamily = GoogleSans, fontSize = 12.sp, color = Theme.danger)
                        }

                        PrimaryButton(
                            text = if (state.isSubmitting) "Saving..." else "Continue",
                            enabled = !state.isSubmitting && name.isNotBlank(),
                            onClick = { viewModel.register(name, email) }
                        )
                    }
                }
            }
        }
    }
}

/** Same light "themed text field" fill used across the app's forms (see SelectPlayersScreen's
 * ThemedField) — readable against the dark court background. */
@Composable
private fun RegistrationField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(SmoothCorner(10.dp))
            .background(Theme.fieldFill)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        if (value.isEmpty()) {
            Text(placeholder, fontFamily = GoogleSans, fontSize = 14.sp, color = Theme.fieldText.copy(alpha = 0.45f))
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(fontFamily = GoogleSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Theme.fieldText),
            cursorBrush = SolidColor(Theme.fieldText),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType, capitalization = capitalization),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
