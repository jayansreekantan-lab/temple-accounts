package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.entities.UserEntity
import com.example.model.SyncStatus
import com.example.model.TempleConstants
import com.example.model.UserRole
import com.example.ui.navigation.TempleNavRoute
import com.example.ui.theme.*

data class NavDrawerItem(
    val route: TempleNavRoute,
    val icon: ImageVector,
    val treasurerOnly: Boolean = false
)

val drawerItems = listOf(
    NavDrawerItem(TempleNavRoute.DASHBOARD, Icons.Default.Dashboard, false),
    NavDrawerItem(TempleNavRoute.INCOME, Icons.Default.TrendingUp, false),
    NavDrawerItem(TempleNavRoute.EXPENSE, Icons.Default.TrendingDown, false),
    NavDrawerItem(TempleNavRoute.CASH_BOOK, Icons.Default.MenuBook, false),
    NavDrawerItem(TempleNavRoute.BANK_BOOK, Icons.Default.AccountBalance, false),
    NavDrawerItem(TempleNavRoute.TRANSFERS, Icons.Default.SwapHoriz, true),
    NavDrawerItem(TempleNavRoute.FESTIVALS, Icons.Default.Celebration, false),
    NavDrawerItem(TempleNavRoute.SPONSORS, Icons.Default.VolunteerActivism, false),
    NavDrawerItem(TempleNavRoute.REPORTS, Icons.Default.Assessment, false),
    NavDrawerItem(TempleNavRoute.USERS, Icons.Default.People, true),
    NavDrawerItem(TempleNavRoute.BACKUP_RESTORE, Icons.Default.Backup, true),
    NavDrawerItem(TempleNavRoute.AUDIT_LOG, Icons.Default.History, true),
    NavDrawerItem(TempleNavRoute.SETTINGS, Icons.Default.Settings, false)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TempleTopAppBar(
    title: String,
    currentUser: UserEntity?,
    isTreasurer: Boolean,
    syncStatus: SyncStatus,
    onMenuClick: () -> Unit,
    onSyncClick: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = TempleConstants.TEMPLE_NAME,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = TempleGoldLight
                )
            }
        },
        navigationIcon = {
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier.testTag("nav_menu_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Open Navigation Menu",
                    tint = Color.White
                )
            }
        },
        actions = {
            // Sync status badge button
            val syncColor = when (syncStatus) {
                SyncStatus.SYNCED -> Color(0xFF4CAF50)
                SyncStatus.PENDING_SYNC -> Color(0xFFFFB300)
                SyncStatus.SYNC_FAILED -> Color(0xFFEF5350)
            }
            IconButton(
                onClick = onSyncClick,
                modifier = Modifier.testTag("sync_action_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "Sync Cloud",
                    tint = syncColor
                )
            }

            // Role indicator chip
            Surface(
                color = if (isTreasurer) TempleGoldDark else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(
                    text = if (isTreasurer) "Treasurer" else "Member",
                    color = if (isTreasurer) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = TempleMaroon
        )
    )
}

@Composable
fun TempleNavDrawerContent(
    currentRoute: TempleNavRoute,
    currentUser: UserEntity?,
    isTreasurer: Boolean,
    syncStatus: SyncStatus,
    onNavigate: (TempleNavRoute) -> Unit,
    onSyncClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    ModalDrawerSheet(
        modifier = Modifier.width(310.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Drawer Header with Temple Sanctum Emblem
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TempleMaroon)
                    .padding(20.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_temple_arch),
                        contentDescription = "Temple Emblem",
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = TempleConstants.TEMPLE_NAME,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = TempleConstants.TEMPLE_LOCATION,
                        style = MaterialTheme.typography.bodySmall.copy(color = TempleGoldLight)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // User Info Card
                    Surface(
                        color = TempleMaroonDark,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isTreasurer) Icons.Default.Shield else Icons.Default.Person,
                                contentDescription = null,
                                tint = TempleGoldLight,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = currentUser?.name ?: "Guest User",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = if (isTreasurer) "ട്രഷറർ (Full Access)" else "അംഗം (Read Only)",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TempleGoldLight,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Sync Status banner
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSyncClick() }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val (syncText, color) = when (syncStatus) {
                            SyncStatus.SYNCED -> "ക്ലൗഡ് സമന്വയിപ്പിച്ചു (Synced)" to CashGreen
                            SyncStatus.PENDING_SYNC -> "സമന്വയിപ്പിക്കുന്നു... (Pending)" to TempleGold
                            SyncStatus.SYNC_FAILED -> "ഓഫ്‌ലൈൻ / പരാജയപ്പെട്ടു" to ExpenseRed
                        }
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = syncText,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                        )
                    }
                    Text(
                        text = "Sync",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Navigation Items
            drawerItems.forEach { item ->
                // Show item if it is public OR user is Treasurer
                if (!item.treasurerOnly || isTreasurer) {
                    val isSelected = currentRoute == item.route
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.route.titleEnglish,
                                tint = if (isSelected) TempleMaroon else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        label = {
                            Column {
                                Text(
                                    text = item.route.titleMalayalam,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) TempleMaroon else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = item.route.titleEnglish,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                )
                            }
                        },
                        selected = isSelected,
                        onClick = { onNavigate(item.route) },
                        modifier = Modifier
                            .padding(NavigationDrawerItemDefaults.ItemPadding)
                            .testTag("nav_item_${item.route.name.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f, fill = false))
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Logout Item
            NavigationDrawerItem(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = "Logout",
                        tint = ExpenseRed
                    )
                },
                label = {
                    Text(
                        text = "ലോഗ് ഔട്ട് (Logout)",
                        style = MaterialTheme.typography.bodyMedium.copy(color = ExpenseRed, fontWeight = FontWeight.Bold)
                    )
                },
                selected = false,
                onClick = onLogoutClick,
                modifier = Modifier
                    .padding(NavigationDrawerItemDefaults.ItemPadding)
                    .testTag("nav_item_logout")
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
