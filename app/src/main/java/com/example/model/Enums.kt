package com.example.model

enum class UserRole(val displayName: String, val malayalamName: String) {
    TREASURER("Treasurer", "ട്രഷറർ"),
    MEMBER("Member / Guest", "അംഗം / ഗസ്റ്റ്")
}

enum class TransactionType(val malayalamName: String) {
    INCOME("വരുമാനം"),
    EXPENSE("ചെലവ്")
}

enum class PaymentMode(val malayalamName: String) {
    CASH("കാഷ് (Cash)"),
    BANK("ബാങ്ക് (Bank)")
}

enum class TransferType(val malayalamName: String, val fromLabel: String, val toLabel: String) {
    CASH_TO_BANK("കാഷ് ➔ ബാങ്ക്", "കാഷ് (Cash)", "ബാങ്ക് (Bank)"),
    BANK_TO_CASH("ബാങ്ക് ➔ കാഷ്", "ബാങ്ക് (Bank)", "കാഷ് (Cash)")
}

enum class SyncStatus {
    SYNCED,
    PENDING_SYNC,
    SYNC_FAILED
}

object TempleCategories {
    val incomeCategories = listOf(
        "മാസവരി സംഖ്യ",
        "കാണിക്ക",
        "വഴിപാട്",
        "സംഭാവന",
        "Sponsor",
        "Festival Income",
        "Other Income"
    )

    val expenseCategories = listOf(
        "പൂജാ സാധനങ്ങൾ",
        "Electricity",
        "Water",
        "Maintenance",
        "Salary/Wages",
        "Decoration",
        "Sound",
        "Food",
        "Advertisement",
        "Festival Expense",
        "Transportation",
        "Other Expense"
    )
}

object TempleConstants {
    const val TEMPLE_NAME = "ചിറയിൽ ശ്രീ മാടൻനട ക്ഷേത്രം"
    const val TEMPLE_LOCATION = "തോട്ടയ്ക്കാട്"
    const val APP_NAME = "Chirayil Temple Accounts"
    const val MAX_TREASURERS = 2
}
