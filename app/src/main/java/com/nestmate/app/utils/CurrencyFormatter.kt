package com.nestmate.app.utils

import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {

    private val inrFormatter: NumberFormat = NumberFormat.getNumberInstance(Locale("en", "IN"))

    fun formatPaise(paise: Long): String {
        val rupees = paise / 100L
        val remainder = (paise % 100L).let { if (it < 0) -it else it }
        val base = inrFormatter.format(rupees)
        return if (remainder == 0L) "Rs $base"
        else "Rs $base.${remainder.toString().padStart(2, '0')}"
    }

    fun formatRupees(rupees: Long): String = "Rs ${inrFormatter.format(rupees)}"
}
