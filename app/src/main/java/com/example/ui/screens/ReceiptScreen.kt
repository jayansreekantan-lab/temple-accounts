package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.entities.TransactionEntity
import com.example.model.TempleConstants
import com.example.ui.theme.*
import com.example.util.ExportUtil
import java.util.*

@Composable
fun ReceiptScreen(
    transaction: TransactionEntity,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isIncome = transaction.type == "INCOME"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Receipt Document Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 500.dp)
                .border(2.dp, TempleGoldDark, RoundedCornerShape(16.dp))
                .testTag("receipt_document_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_temple_arch),
                    contentDescription = null,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = TempleConstants.TEMPLE_NAME,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TempleMaroon
                    ),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = TempleConstants.TEMPLE_LOCATION,
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.outline),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    color = if (isIncome) CashGreen else ExpenseRed,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = if (isIncome) "രസീത് (RECEIPT)" else "പെയ്‌മെന്റ് വൗച്ചർ (VOUCHER)",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = TempleGoldLight)
                Spacer(modifier = Modifier.height(16.dp))

                // Detail Rows
                ReceiptRow("നമ്പർ (Number):", transaction.voucherOrReceiptNo)
                ReceiptRow("തീയതി (Date):", transaction.dateFormatted)
                ReceiptRow(if (isIncome) "ലഭിച്ചത് (Received From):" else "നൽകിയത് (Paid To):", transaction.partyName)
                ReceiptRow("വിഭാഗം (Category):", transaction.category)
                if (!transaction.festivalName.isNullOrBlank()) {
                    ReceiptRow("ഉത്സവം (Festival):", transaction.festivalName)
                }
                ReceiptRow("പേയ്‌മെന്റ് രീതി (Payment Mode):", transaction.paymentMode)
                if (transaction.description.isNotBlank()) {
                    ReceiptRow("വിവരണം (Description):", transaction.description)
                }
                if (transaction.notes.isNotBlank()) {
                    ReceiptRow("കുറിപ്പ് (Notes):", transaction.notes)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Amount Highlight Box
                Surface(
                    color = TempleGoldLight.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ആകെ തുക (Amount):",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "₹${String.format(Locale.US, "%,.2f", transaction.amount)}",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TempleMaroon
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Signature Lines
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("തയ്യാറാക്കിയത്", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.outline))
                        Text(transaction.createdBy.ifEmpty { "Treasurer" }, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("അംഗീകാരം", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.outline))
                        Text("അധികൃത ഒപ്പ്", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 500.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { ExportUtil.generateAndShareReceiptPdf(context, transaction) },
                colors = ButtonDefaults.buttonColors(containerColor = TempleMaroon),
                modifier = Modifier.weight(1f).testTag("receipt_share_pdf_button")
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share PDF")
            }

            OutlinedButton(
                onClick = {
                    val shareText = buildString {
                        append("${TempleConstants.TEMPLE_NAME} - ${TempleConstants.TEMPLE_LOCATION}\n")
                        append("${if (isIncome) "രസീത്" else "വൗച്ചർ"}: ${transaction.voucherOrReceiptNo}\n")
                        append("തീയതി: ${transaction.dateFormatted}\n")
                        append("${if (isIncome) "ലഭിച്ചത്" else "നൽകിയത്"}: ${transaction.partyName}\n")
                        append("വിഭാഗം: ${transaction.category}\n")
                        append("തുക: ₹${String.format(Locale.US, "%,.2f", transaction.amount)}\n")
                        append("പേയ്‌മെന്റ് രീതി: ${transaction.paymentMode}\n")
                    }
                    ExportUtil.shareText(context, "Receipt ${transaction.voucherOrReceiptNo}", shareText)
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share Text")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        TextButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("മുകളിലേക്ക് മടങ്ങുക (Back)")
        }
    }
}

@Composable
private fun ReceiptRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.outline)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}
