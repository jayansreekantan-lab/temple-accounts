package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.data.local.entities.TransactionEntity
import com.example.model.TempleConstants
import com.example.ui.navigation.TempleNavRoute
import com.example.ui.theme.*
import com.example.ui.viewmodel.TempleViewModel
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: TempleViewModel,
    onNavigate: (TempleNavRoute) -> Unit,
    onViewReceipt: (TransactionEntity) -> Unit
) {
    val balances by viewModel.balances.collectAsState()
    val isTreasurer by viewModel.isTreasurer.collectAsState()
    val recentTransactions by viewModel.allTransactions.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Temple Header Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_header_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = TempleMaroon)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_temple_arch),
                        contentDescription = "Temple Emblem",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = TempleConstants.TEMPLE_NAME,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Accounts Dashboard (അക്കൗണ്ട്സ്)",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TempleGoldLight,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = TempleConstants.TEMPLE_LOCATION,
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.8f))
                        )
                    }
                }
            }
        }

        // Summary Cards Section
        item {
            Text(
                text = "സാമ്പത്തിക സ്ഥിതിവിവരം (Financial Summary)",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        // Summary Cards Grid: Cash & Bank Balance
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryCard(
                    title = "💵 കാഷ് ബാലൻസ്",
                    subtitle = "Cash in Hand",
                    amount = balances.cashBalance,
                    color = CashGreen,
                    icon = Icons.Default.Payments,
                    modifier = Modifier.weight(1f).testTag("card_cash_balance")
                ) {
                    onNavigate(TempleNavRoute.CASH_BOOK)
                }

                SummaryCard(
                    title = "🏦 ബാങ്ക് ബാലൻസ്",
                    subtitle = "Bank Balance",
                    amount = balances.bankBalance,
                    color = BankBlue,
                    icon = Icons.Default.AccountBalance,
                    modifier = Modifier.weight(1f).testTag("card_bank_balance")
                ) {
                    onNavigate(TempleNavRoute.BANK_BOOK)
                }
            }
        }

        // Summary Cards Grid: Total Income & Total Expense
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryCard(
                    title = "💰 ആകെ വരുമാനം",
                    subtitle = "Total Income",
                    amount = balances.totalIncome,
                    color = IncomeGreen,
                    icon = Icons.Default.TrendingUp,
                    modifier = Modifier.weight(1f).testTag("card_total_income")
                ) {
                    onNavigate(TempleNavRoute.INCOME)
                }

                SummaryCard(
                    title = "💸 ആകെ ചെലവ്",
                    subtitle = "Total Expense",
                    amount = balances.totalExpense,
                    color = ExpenseRed,
                    icon = Icons.Default.TrendingDown,
                    modifier = Modifier.weight(1f).testTag("card_total_expense")
                ) {
                    onNavigate(TempleNavRoute.EXPENSE)
                }
            }
        }

        // Festival Balance Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate(TempleNavRoute.FESTIVALS) }
                    .testTag("card_festival_balance"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(TempleGoldDark.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Celebration,
                                contentDescription = null,
                                tint = TempleGoldDark
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "📊 ഉത്സവ ബാലൻസ് (Festival Balance)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = balances.currentFestivalName.ifEmpty { "തൃക്കൊടിയേറ്റ് മകരവിളക്ക് മഹോത്സവം 2027" },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    Text(
                        text = "₹${String.format(Locale.US, "%,.2f", balances.currentFestivalBalance)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TempleMaroon
                        )
                    )
                }
            }
        }

        // Quick Actions for Treasurer / Members
        item {
            Text(
                text = if (isTreasurer) "ദ്രുത പ്രവർത്തനങ്ങൾ (Quick Actions)" else "റിപ്പോർട്ടുകളും അന്വേഷണങ്ങളും (Reports & Views)",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        item {
            if (isTreasurer) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionButton(
                        label = "Add Income\nവരുമാനം",
                        icon = Icons.Default.AddCircle,
                        color = IncomeGreen,
                        modifier = Modifier.weight(1f).testTag("quick_action_add_income")
                    ) {
                        onNavigate(TempleNavRoute.INCOME)
                    }

                    QuickActionButton(
                        label = "Add Expense\nചെലവ്",
                        icon = Icons.Default.RemoveCircle,
                        color = ExpenseRed,
                        modifier = Modifier.weight(1f).testTag("quick_action_add_expense")
                    ) {
                        onNavigate(TempleNavRoute.EXPENSE)
                    }

                    QuickActionButton(
                        label = "Transfer\nട്രാൻസ്ഫർ",
                        icon = Icons.Default.SwapHoriz,
                        color = BankBlue,
                        modifier = Modifier.weight(1f).testTag("quick_action_transfer")
                    ) {
                        onNavigate(TempleNavRoute.TRANSFERS)
                    }

                    QuickActionButton(
                        label = "Reports\nറിപ്പോർട്ട്",
                        icon = Icons.Default.Assessment,
                        color = TempleMaroon,
                        modifier = Modifier.weight(1f).testTag("quick_action_reports")
                    ) {
                        onNavigate(TempleNavRoute.REPORTS)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionButton(
                        label = "Cash Book\nകാഷ് ബുക്ക്",
                        icon = Icons.Default.MenuBook,
                        color = CashGreen,
                        modifier = Modifier.weight(1f)
                    ) {
                        onNavigate(TempleNavRoute.CASH_BOOK)
                    }
                    QuickActionButton(
                        label = "Bank Book\nബാങ്ക് ബുക്ക്",
                        icon = Icons.Default.AccountBalance,
                        color = BankBlue,
                        modifier = Modifier.weight(1f)
                    ) {
                        onNavigate(TempleNavRoute.BANK_BOOK)
                    }
                    QuickActionButton(
                        label = "Reports\nറിപ്പോർട്ടുകൾ",
                        icon = Icons.Default.Assessment,
                        color = TempleMaroon,
                        modifier = Modifier.weight(1f)
                    ) {
                        onNavigate(TempleNavRoute.REPORTS)
                    }
                }
            }
        }

        // Recent Transactions Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "സമീപകാല ഇടപാടുകൾ (Recent Transactions)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                TextButton(onClick = { onNavigate(TempleNavRoute.INCOME) }) {
                    Text("എല്ലാം കാണുക (View All)", fontSize = 12.sp)
                }
            }
        }

        // Recent Transactions List
        if (recentTransactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "ഇടപാടുകൾ ഒന്നും രേഖപ്പെടുത്തിയിട്ടില്ല.\n'Add Income' അല്ലെങ്കിൽ 'Add Expense' ക്ലിക്ക് ചെയ്യുക.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        } else {
            items(recentTransactions.take(8)) { tx ->
                val isIncome = tx.type == "INCOME"
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onViewReceipt(tx) }
                        .testTag("recent_tx_${tx.voucherOrReceiptNo}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (isIncome) CashGreen.copy(alpha = 0.15f) else ExpenseRed.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = if (isIncome) CashGreen else ExpenseRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = tx.partyName.ifEmpty { tx.description },
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "${tx.category} • ${tx.voucherOrReceiptNo} • ${tx.paymentMode}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.outline,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${if (isIncome) "+" else "-"}₹${String.format(Locale.US, "%,.2f", tx.amount)}",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isIncome) CashGreen else ExpenseRed
                                )
                            )
                            Text(
                                text = tx.dateFormatted,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.outline,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryCard(
    title: String,
    subtitle: String,
    amount: Double,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "₹${String.format(Locale.US, "%,.2f", amount)}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            )
        }
    }
}

@Composable
fun QuickActionButton(
    label: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2
            )
        }
    }
}
