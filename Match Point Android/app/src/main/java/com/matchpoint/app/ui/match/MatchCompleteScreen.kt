package com.matchpoint.app.ui.match

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matchpoint.app.R
import com.matchpoint.app.repository.SessionRepository
import com.matchpoint.app.ui.theme.GoogleSans
import com.matchpoint.app.ui.theme.LocalCourtTheme
import com.matchpoint.app.ui.theme.PrimaryButton
import com.matchpoint.app.ui.theme.Theme
import com.matchpoint.app.ui.theme.themeScore
import com.matchpoint.app.ui.theme.themeTitle
import java.util.UUID

@Composable
fun MatchCompleteScreen(
    repository: SessionRepository,
    matchId: UUID,
    onContinue: () -> Unit
) {
    val viewModel: MatchCompleteViewModel = viewModel(factory = MatchCompleteViewModel.Factory(repository, matchId))
    val state by viewModel.uiState.collectAsState()
    var sessionEndedError by remember { mutableStateOf(false) }

    val match = state.match ?: return

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(LocalCourtTheme.current.matchCompleteIllustration),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().heightIn(max = 305.dp).padding(bottom = 8.dp)
        )
        Text(
            state.winnerNames.joinToString(" & "),
            style = themeTitle(22.sp),
            color = Theme.accent,
            textAlign = TextAlign.Center
        )
        Text("WINNER", fontFamily = GoogleSans, fontSize = 11.sp, color = Theme.textSecondary, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(14.dp))
        Text("${match.finalScoreA} — ${match.finalScoreB}", style = themeScore(44.sp), color = Theme.textPrimary)
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = Theme.cardBorder)
        Spacer(Modifier.height(8.dp))
        Text(
            state.pointsSummary.joinToString(" · ") { "${it.name} +${it.points}" },
            fontFamily = GoogleSans,
            fontSize = 13.sp,
            color = Theme.textSecondary,
            textAlign = TextAlign.Center
        )

        if (sessionEndedError) {
            Text(
                "This mabar has ended — head back to Home to start a new one.",
                fontFamily = GoogleSans,
                fontSize = 12.sp,
                color = Theme.danger,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 14.dp)
            )
        }

        Spacer(Modifier.height(28.dp))
        PrimaryButton(
            text = "Continue to Next Match",
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                viewModel.continueToNextMatch { ended ->
                    if (ended) sessionEndedError = true else onContinue()
                }
            }
        )
    }
}
