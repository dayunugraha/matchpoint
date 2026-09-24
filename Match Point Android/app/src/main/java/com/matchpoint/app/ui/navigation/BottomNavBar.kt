package com.matchpoint.app.ui.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matchpoint.app.ui.theme.GoogleSans
import com.matchpoint.app.ui.theme.Theme

data class BottomNavItem(val route: String, val label: String, val activeIcon: Int, val inactiveIcon: Int)

/** Plain solid bottom nav bar — icon + label per tab, tight custom spacing. */
@Composable
fun BottomNavBar(
    items: List<BottomNavItem>,
    selectedRoute: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Theme.backgroundBottom)
            .navigationBarsPadding()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        items.forEach { item ->
            val selected = item.route == selectedRoute
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = { onSelect(item.route) }
                    )
                    .padding(horizontal = 12.dp, vertical = 2.dp)
            ) {
                Image(
                    painter = painterResource(if (selected) item.activeIcon else item.inactiveIcon),
                    contentDescription = item.label,
                    modifier = Modifier.size(30.dp)
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    item.label,
                    fontFamily = GoogleSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = if (selected) Theme.accent else Theme.textSecondary
                )
            }
        }
    }
}
