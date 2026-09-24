package com.matchpoint.app.ui.matches

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matchpoint.app.data.SessionEntity
import com.matchpoint.app.domain.Side
import com.matchpoint.app.ui.theme.GoogleSans
import com.matchpoint.app.ui.theme.LocalCourtTheme
import com.matchpoint.app.ui.theme.SmoothCorner
import com.matchpoint.app.ui.theme.Theme
import com.matchpoint.app.ui.theme.themeTitle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Static, non-interactive recap card for a past mabar — standings and every
 * completed match's result — rendered off-screen at a fixed 1080px width and
 * captured to a bitmap by [ShareCardRenderer]. Ported 1:1 from iOS's
 * SessionShareCardView; rendered at natural height, then fit onto the exact
 * 1080x1920 story canvas by the renderer, so this composable doesn't need to
 * worry about how many players/matches fit.
 */
const val ShareCardRenderWidthPx = 1080

@Composable
fun ShareCardContent(session: SessionEntity, standings: List<HistoryPlayerUi>, completedMatches: List<HistoryMatchUi>) {
    Column(
        modifier = Modifier
            .width(ShareCardRenderWidthPx.dp)
            .wrapContentHeight()
            .background(Theme.backgroundGradient)
            .padding(48.dp)
    ) {
        ShareCardHeader(session)

        if (standings.isNotEmpty()) {
            Spacer(Modifier.wrapContentHeight().padding(top = 36.dp))
            ShareCardSectionLabel("STANDINGS")
            Spacer(Modifier.padding(top = 8.dp))
            ShareCardStandings(standings)
        }

        if (completedMatches.isNotEmpty()) {
            Spacer(Modifier.padding(top = 36.dp))
            ShareCardSectionLabel("MATCHES")
            Spacer(Modifier.padding(top = 8.dp))
            ShareCardMatches(completedMatches)
        }

        Spacer(Modifier.padding(top = 36.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text("Generated with Match Point", fontFamily = GoogleSans, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = Theme.textSecondary)
        }
    }
}

@Composable
private fun ShareCardHeader(session: SessionEntity) {
    Column {
        Image(
            painter = painterResource(LocalCourtTheme.current.logoImage),
            contentDescription = null,
            modifier = Modifier.size(width = 200.dp, height = 56.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(Modifier.padding(top = 20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            ShareCardBadge(session)
            Column(modifier = Modifier.padding(start = 20.dp)) {
                Text(session.name, style = themeTitle(40.sp), color = Theme.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date(session.date)),
                    fontFamily = GoogleSans, fontSize = 24.sp, color = Theme.textSecondary
                )
                if (session.startTime != null && session.endTime != null) {
                    val fmt = SimpleDateFormat("h:mm a", Locale.getDefault())
                    Text(
                        "${fmt.format(Date(session.startTime))} – ${fmt.format(Date(session.endTime))}",
                        fontFamily = GoogleSans, fontSize = 22.sp, color = Theme.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun ShareCardBadge(session: SessionEntity) {
    val customBitmap = remember(session.customBadgeImageData) {
        session.customBadgeImageData?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
    }
    if (customBitmap != null) {
        Image(
            bitmap = customBitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(96.dp)
        )
    } else {
        Image(
            painter = painterResource(LocalCourtTheme.current.pastMabarBadge),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(96.dp)
        )
    }
}

@Composable
private fun ShareCardSectionLabel(text: String) {
    Text(text, fontFamily = GoogleSans, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Theme.textSecondary, letterSpacing = 1.6.sp)
}

@Composable
private fun ShareCardCardBackground(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SmoothCorner(28.dp))
            .background(Theme.cardFill)
            .border(1.dp, Theme.cardBorder, SmoothCorner(28.dp))
            .padding(24.dp)
    ) { content() }
}

@Composable
private fun ShareCardStandings(standings: List<HistoryPlayerUi>) {
    ShareCardCardBackground {
        standings.forEachIndexed { index, entry ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                Text("${index + 1}", fontFamily = GoogleSans, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Theme.textSecondary, modifier = Modifier.width(34.dp))
                Text(
                    entry.name,
                    fontFamily = GoogleSans,
                    fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 26.sp,
                    color = Theme.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${entry.sessionPlayer.wins}–${entry.sessionPlayer.losses}",
                    fontFamily = GoogleSans, fontSize = 22.sp, color = Theme.textSecondary,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Text(
                    "${entry.sessionPlayer.points} pts",
                    fontFamily = GoogleSans, fontWeight = FontWeight.Bold, fontSize = 26.sp,
                    color = if (index == 0) Theme.accent else Theme.textSecondary,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(110.dp)
                )
            }
            if (index < standings.size - 1) HorizontalDivider(color = Theme.cardBorder)
        }
    }
}

/** Split into left/right columns rather than one long vertical list — halves the height a
 * growing match count costs, matching iOS's two-column recap layout. */
@Composable
private fun ShareCardMatches(matches: List<HistoryMatchUi>) {
    val splitPoint = (matches.size + 1) / 2
    val left = matches.take(splitPoint)
    val right = matches.drop(splitPoint)

    Row(horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.fillMaxWidth()) {
        ShareCardMatchColumn(left, startIndex = 0, placeholderRows = 0, modifier = Modifier.weight(1f))
        if (right.isNotEmpty()) {
            ShareCardMatchColumn(right, startIndex = left.size, placeholderRows = left.size - right.size, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ShareCardMatchColumn(matches: List<HistoryMatchUi>, startIndex: Int, placeholderRows: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier.clip(SmoothCorner(28.dp)).background(Theme.cardFill).border(1.dp, Theme.cardBorder, SmoothCorner(28.dp)).padding(24.dp)) {
        matches.forEachIndexed { position, m ->
            ShareCardMatchRow(m, number = startIndex + position + 1)
            if (position < matches.size - 1 || placeholderRows > 0) HorizontalDivider(color = Theme.cardBorder)
        }
        repeat(placeholderRows) {
            Spacer(Modifier.padding(vertical = 34.dp))
        }
    }
}

@Composable
private fun ShareCardMatchRow(m: HistoryMatchUi, number: Int) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text("M$number", fontFamily = GoogleSans, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Theme.textSecondary, modifier = Modifier.width(44.dp))
        Column(modifier = Modifier.weight(1f)) {
            ShareCardTeamRow(m.sideANames, m.match.finalScoreA, isWinner = m.match.winnerSide == Side.A)
            Spacer(Modifier.padding(top = 5.dp))
            ShareCardTeamRow(m.sideBNames, m.match.finalScoreB, isWinner = m.match.winnerSide == Side.B)
        }
    }
}

@Composable
private fun ShareCardTeamRow(names: List<String>, score: Int?, isWinner: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            names.joinToString(" & "),
            fontFamily = GoogleSans,
            fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal,
            fontSize = 21.sp,
            color = Theme.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        score?.let {
            Text("$it", style = themeTitle(22.sp), color = if (isWinner) Theme.accent else Theme.textSecondary)
        }
    }
}
