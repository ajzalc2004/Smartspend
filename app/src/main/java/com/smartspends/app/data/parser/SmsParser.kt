package com.smartspends.app.data.parser

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

object SmsParser {

    private val TRANSACTION_KEYWORDS = listOf(
        "debited", "credited", "spent", "received", "sent", "withdrawal",
        "deposited", "transfer", "transaction of", "payment of", "paid to",
        "withdrawn", "deducted", "salary", "refunded", "declined"
    )

    private val OTP_KEYWORDS = listOf("otp", "one time password", "verification code", "verification otp", "code is")

    // RegEx for Amount extraction
    // Matches: Rs 500, Rs. 500, Rs.500.00, INR 5,00.00, INR 12,000.50, Rs. 1,23,456.78, ₹ 100, etc.
    private val AMOUNT_REGEX = Pattern.compile(
        "(?i)(?:rs\\.?|inr|₹|payment of|amt)\\s*([0-9,]+\\.[0-9]{2}|[0-9,]+)"
    )

    // RegEx for Account number
    // Matches: A/c X1234, acct ending in 4321, A/c ending 1234, XX1234, xx4321, card ending 3421
    private val ACCOUNT_REGEX = Pattern.compile(
        "(?i)(?:a/c|acct|account|card|chq|ending(?:\\s+with)?|xx)\\s*(?:no\\.?\\s*)?([xX\\d]*\\d{4})"
    )

    fun isTransactionSms(body: String): Boolean {
        val lowerBody = body.lowercase(Locale.ROOT)
        
        // Ignore OTPs and verification messages
        if (OTP_KEYWORDS.any { lowerBody.contains(it) }) {
            return false
        }

        // Must contain at least one transaction keyword
        return TRANSACTION_KEYWORDS.any { lowerBody.contains(it) }
    }

    fun parse(body: String, senderAddress: String?, smsTimestamp: Long): ParsedTransaction? {
        if (!isTransactionSms(body)) return null

        val cleanBody = body.replace("\n", " ").replace("\r", " ")
        val lowerBody = cleanBody.lowercase(Locale.ROOT)

        // 1. Amount Extraction
        val amount = extractAmount(cleanBody) ?: return null

        // 2. Type (Credit vs Debit)
        val type = extractType(lowerBody)

        // 3. Bank Name
        val bankName = extractBankName(senderAddress, cleanBody)

        // 4. Account Number (Masked)
        val accountNumber = extractAccountNumber(cleanBody)

        // 5. Transaction Mode
        val transactionMode = extractTransactionMode(lowerBody)

        // 6. Category mapping based on content keywords
        val category = autoCategorize(lowerBody, type)

        // 7. Date & Time parsing from message timestamp
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateStr = sdfDate.format(Date(smsTimestamp))
        val timeStr = sdfTime.format(Date(smsTimestamp))

        // 8. Description extraction
        val description = extractDescription(cleanBody, type, transactionMode, bankName)

        return ParsedTransaction(
            amount = amount,
            type = type,
            category = category,
            bank = bankName,
            accountNumberMasked = accountNumber,
            date = dateStr,
            time = timeStr,
            transactionMode = transactionMode,
            description = description,
            smsBody = body
        )
    }

    private fun extractAmount(body: String): Double? {
        val matcher = AMOUNT_REGEX.matcher(body)
        while (matcher.find()) {
            val amountStr = matcher.group(1)?.replace(",", "")
            val parsed = amountStr?.toDoubleOrNull()
            if (parsed != null && parsed > 0.0) {
                return parsed
            }
        }
        
        // Fallback for cases like "spent 100.00" without RS / INR prefix
        val fallbackRegex = Pattern.compile("(?i)(?:spent|debited|credited|withdrawn|paid|received)\\s+([0-9,]+\\.[0-9]{2}|[0-9,]+)")
        val fallbackMatcher = fallbackRegex.matcher(body)
        if (fallbackMatcher.find()) {
            val amountStr = fallbackMatcher.group(1)?.replace(",", "")
            return amountStr?.toDoubleOrNull()
        }
        return null
    }

    private fun extractType(lowerBody: String): String {
        val creditWords = listOf("credited", "received", "added", "deposited", "salary", "refund", "cashback", "interest")
        val debitWords = listOf("debited", "spent", "withdrawn", "sent", "paid", "transferred", "deducted")

        val creditIndex = creditWords.map { lowerBody.indexOf(it) }.filter { it >= 0 }.minOrNull() ?: Int.MAX_VALUE
        val debitIndex = debitWords.map { lowerBody.indexOf(it) }.filter { it >= 0 }.minOrNull() ?: Int.MAX_VALUE

        return if (creditIndex < debitIndex) "INCOME" else "EXPENSE"
    }

    private fun extractBankName(senderAddress: String?, body: String): String {
        if (!senderAddress.isNullOrEmpty()) {
            // Address formats: "VK-ESAFOT-S", "JM-ESAFOT-S", "AD-HDFCBK" etc.
            val parts = senderAddress.split("-")
            if (parts.size >= 2) {
                val identity = parts[1].uppercase(Locale.ROOT)
                if (identity.length >= 3) {
                    return cleanBankName(identity)
                }
            }
            if (senderAddress.length >= 3 && !senderAddress.contains("-")) {
                return cleanBankName(senderAddress.uppercase(Locale.ROOT))
            }
        }

        // Try extracting from body if sender address is unavailable or generic
        val bankKeywords = listOf("SBI", "HDFC", "ICICI", "AXIS", "KOTAK", "PNB", "BOB", "CANARA", "UNION", "PAYTM", "ESAF")
        for (bank in bankKeywords) {
            if (body.contains(bank, ignoreCase = true)) {
                return if (bank == "ESAF") "ESAF Bank" else bank
            }
        }

        return "Bank"
    }

    private fun cleanBankName(name: String): String {
        return when {
            name.contains("HDFCBK") || name.contains("HDFC") -> "HDFC Bank"
            name.contains("SBIINB") || name.contains("SBICARD") || name.contains("SBI") -> "SBI"
            name.contains("ICICIB") || name.contains("ICICI") -> "ICICI Bank"
            name.contains("AXISBK") || name.contains("AXIS") -> "Axis Bank"
            name.contains("KOTAK") -> "Kotak Bank"
            name.contains("PNBSMS") || name.contains("PNB") -> "PNB"
            name.contains("BOBTXN") || name.contains("BOB") -> "Bank of Baroda"
            name.contains("CANARA") || name.contains("CNRB") -> "Canara Bank"
            name.contains("UNIONB") || name.contains("UBIN") -> "Union Bank"
            name.contains("PAYTM") -> "Paytm Bank"
            name.contains("ESAF") -> "ESAF Bank"
            else -> name
        }
    }

    private fun extractAccountNumber(body: String): String? {
        val matcher = ACCOUNT_REGEX.matcher(body)
        if (matcher.find()) {
            val matched = matcher.group(1) ?: return null
            val cleanDigits = matched.filter { it.isDigit() }
            return if (cleanDigits.length >= 4) {
                "XX" + cleanDigits.takeLast(4)
            } else {
                "XX" + matched.takeLast(4)
            }
        }
        
        // Simple fallback to locate any 4-digit sequence marked with 'x'
        val fallbackPattern = Pattern.compile("(?i)[x*]+(\\d{4})")
        val fallbackMatcher = fallbackPattern.matcher(body)
        if (fallbackMatcher.find()) {
            return "XX" + fallbackMatcher.group(1)
        }
        return null
    }

    private fun extractTransactionMode(lowerBody: String): String {
        return when {
            lowerBody.contains("upi") || lowerBody.contains("gpay") || lowerBody.contains("phonepe") || lowerBody.contains("paytm") -> "UPI"
            lowerBody.contains("atm") || lowerBody.contains("cash withdrawal") || lowerBody.contains("withdrawn at") -> "ATM"
            lowerBody.contains("imps") -> "IMPS"
            lowerBody.contains("neft") -> "NEFT"
            lowerBody.contains("rtgs") -> "RTGS"
            lowerBody.contains("salary") -> "Salary"
            lowerBody.contains("interest") -> "Interest"
            lowerBody.contains("card") || lowerBody.contains("pos") || lowerBody.contains("swipe") -> "Card"
            else -> "Transfer"
        }
    }

    private fun autoCategorize(lowerBody: String, type: String): String {
        if (type == "INCOME") {
            return when {
                lowerBody.contains("salary") || lowerBody.contains("payroll") || lowerBody.contains("stipend") -> "Salary"
                lowerBody.contains("interest") || lowerBody.contains("dividend") -> "Interest"
                lowerBody.contains("freelance") || lowerBody.contains("gigs") -> "Freelance"
                lowerBody.contains("gift") -> "Gift"
                lowerBody.contains("refund") -> "Refund"
                lowerBody.contains("investment") || lowerBody.contains("groww") || lowerBody.contains("zerodha") -> "Investment"
                else -> "Other"
            }
        } else {
            return when {
                lowerBody.contains("zomato") || lowerBody.contains("swiggy") || lowerBody.contains("food") || 
                lowerBody.contains("cafe") || lowerBody.contains("restaurant") || lowerBody.contains("eats") || 
                lowerBody.contains("pizza") || lowerBody.contains("kfc") || lowerBody.contains("mcdonald") -> "Food"
                
                lowerBody.contains("petrol") || lowerBody.contains("diesel") || lowerBody.contains("fuel") || 
                lowerBody.contains("hpcl") || lowerBody.contains("bpcl") || lowerBody.contains("iocl") || 
                lowerBody.contains("shell") || lowerBody.contains("cng") -> "Fuel"
                
                lowerBody.contains("amazon") || lowerBody.contains("flipkart") || lowerBody.contains("myntra") || 
                lowerBody.contains("ajio") || lowerBody.contains("shopping") || lowerBody.contains("retail") || 
                lowerBody.contains("blinkit") || lowerBody.contains("zepto") || lowerBody.contains("grocery") || 
                lowerBody.contains("groceries") || lowerBody.contains("mart") -> "Shopping"
                
                lowerBody.contains("uber") || lowerBody.contains("ola") || lowerBody.contains("rapido") || 
                lowerBody.contains("metro") || lowerBody.contains("irctc") || lowerBody.contains("railway") || 
                lowerBody.contains("flight") || lowerBody.contains("makemytrip") || lowerBody.contains("cab") || 
                lowerBody.contains("travel") -> "Travel"
                
                lowerBody.contains("electricity") || lowerBody.contains("bescom") || lowerBody.contains("water") || 
                lowerBody.contains("broadband") || lowerBody.contains("recharge") || lowerBody.contains("jio") || 
                lowerBody.contains("airtel") || lowerBody.contains("vi") || lowerBody.contains("bill") || 
                lowerBody.contains("insurance") || lowerBody.contains("premium") -> "Bills"
                
                lowerBody.contains("netflix") || lowerBody.contains("prime") || lowerBody.contains("hotstar") || 
                lowerBody.contains("spotify") || lowerBody.contains("youtube") || lowerBody.contains("pvr") || 
                lowerBody.contains("inox") || lowerBody.contains("bookmyshow") || lowerBody.contains("movie") || 
                lowerBody.contains("gaming") || lowerBody.contains("steam") || lowerBody.contains("entertainment") -> "Entertainment"
                
                lowerBody.contains("pharmacy") || lowerBody.contains("hospital") || lowerBody.contains("clinic") || 
                lowerBody.contains("apollo") || lowerBody.contains("pharmeasy") || lowerBody.contains("doctor") || 
                lowerBody.contains("medical") || lowerBody.contains("healthcare") -> "Healthcare"
                
                lowerBody.contains("school") || lowerBody.contains("college") || lowerBody.contains("udemy") || 
                lowerBody.contains("coursera") || lowerBody.contains("fees") || lowerBody.contains("tuition") || 
                lowerBody.contains("education") -> "Education"
                
                lowerBody.contains("rent") || lowerBody.contains("landlord") || lowerBody.contains("society") -> "Rent"
                
                else -> "Other"
            }
        }
    }

    private fun extractDescription(body: String, type: String, mode: String, bank: String): String {
        // Try extracting receiver/sender info. E.g. "by UPI to John Doe" or "credited by Salary"
        val lowerBody = body.lowercase(Locale.ROOT)
        val upiToRegex = Pattern.compile("(?i)to\\s+([^\\d\\s]+(?:\\s+[^\\d\\s]+){0,2})")
        val upiMatcher = upiToRegex.matcher(body)
        if (upiMatcher.find()) {
            val merchant = upiMatcher.group(1)?.trim()
            if (!merchant.isNullOrEmpty() && merchant.length > 2 && !merchant.equals("rs", ignoreCase = true) && !merchant.equals("inr", ignoreCase = true)) {
                return merchant
            }
        }

        val infoKeywords = listOf("transfer to", "paid to", "sent to", "spent at", "spent on")
        for (kw in infoKeywords) {
            val idx = lowerBody.indexOf(kw)
            if (idx != -1) {
                val start = idx + kw.length
                val text = body.substring(start).trim()
                val spaceIdx = text.indexOf(" ")
                val word = if (spaceIdx != -1) text.substring(0, spaceIdx) else text
                if (word.length > 2) return word
            }
        }

        return if (type == "INCOME") "Incoming $mode ($bank)" else "Outgoing $mode ($bank)"
    }
}

data class ParsedTransaction(
    val amount: Double,
    val type: String,
    val category: String,
    val bank: String?,
    val accountNumberMasked: String?,
    val date: String,
    val time: String,
    val transactionMode: String,
    val description: String,
    val smsBody: String
)
