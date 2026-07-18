package com.smartspends.app.data.parser

import org.junit.Assert.*
import org.junit.Test

class SmsParserTest {

    @Test
    fun testIsTransactionSms_ValidTransaction() {
        val sms = "Your A/c ending 1234 has been debited by Rs. 250.00 on 18-07-26 via UPI to STARBUCKS."
        assertTrue(SmsParser.isTransactionSms(sms))
    }

    @Test
    fun testIsTransactionSms_IgnoreOTP() {
        val sms = "Dear customer, your OTP for transaction of Rs.500 is 123456. Do not share this with anyone."
        assertFalse(SmsParser.isTransactionSms(sms))
    }

    @Test
    fun testParse_HdfcDebit() {
        val sms = "Alert: Rs 450.00 debited from A/c X1234 on 18-07-26 by UPI to SWIGGY. Ref: 231456."
        val parsed = SmsParser.parse(sms, "AD-HDFCBK", 1784382000000L)
        
        assertNotNull(parsed)
        assertEquals(450.00, parsed!!.amount, 0.0)
        assertEquals("EXPENSE", parsed.type)
        assertEquals("HDFC Bank", parsed.bank)
        assertEquals("XX1234", parsed.accountNumberMasked)
        assertEquals("UPI", parsed.transactionMode)
        assertEquals("SWIGGY", parsed.description)
        assertEquals("Food", parsed.category)
    }

    @Test
    fun testParse_SbiCredit() {
        val sms = "Dear Customer, your A/c XX4321 has been Credited with INR 5,000.00 on 18/07/26. Ref: interest payout."
        val parsed = SmsParser.parse(sms, "VK-SBIINB", 1784382000000L)
        
        assertNotNull(parsed)
        assertEquals(5000.00, parsed!!.amount, 0.0)
        assertEquals("INCOME", parsed.type)
        assertEquals("SBI", parsed.bank)
        assertEquals("XX4321", parsed.accountNumberMasked)
        assertEquals("Interest", parsed.category)
    }

    @Test
    fun testParse_EsafDebit() {
        val sms = "Dear customer, Rs 450.00 debited from your A/c XX1234 on 18-07-26 by UPI. Ref: Swiggy."
        val parsed = SmsParser.parse(sms, "VK-ESAFOT-S", 1784382000000L)

        assertNotNull(parsed)
        assertEquals(450.00, parsed!!.amount, 0.0)
        assertEquals("EXPENSE", parsed.type)
        assertEquals("ESAF Bank", parsed.bank)
        assertEquals("XX1234", parsed.accountNumberMasked)
        assertEquals("Food", parsed.category)
    }

    @Test
    fun testParse_EsafCredit() {
        val sms = "Dear customer, Rs 1200.00 credited to your A/c XX1234 on 18-07-26 by Interest."
        val parsed = SmsParser.parse(sms, "VM-ESAFSF-S", 1784382000000L)

        assertNotNull(parsed)
        assertEquals(1200.00, parsed!!.amount, 0.0)
        assertEquals("INCOME", parsed.type)
        assertEquals("ESAF Bank", parsed.bank)
        assertEquals("Interest", parsed.category)
    }
}
