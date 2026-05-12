package com.nestmate.app.utils.bill

import com.nestmate.app.data.model.BillGroup
import com.nestmate.app.data.model.BillItem
import com.nestmate.app.data.model.Participant
import com.nestmate.app.data.model.SplitMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BillCalculatorTest {

    private fun participant(id: String, name: String = id, paid: Long = 0L, share: Long = 0L) =
        Participant(id = id, name = name, paidAmount = paid, shareAmount = share)

    @Test
    fun `equal split with no remainder`() {
        val bill = BillGroup(
            totalAmount = 30000L,
            splitMethod = SplitMethod.EQUAL,
            participants = listOf(
                participant("a"), participant("b"), participant("c"),
            ),
        )
        val result = BillCalculator.computeShares(bill) as SplitResult.Success
        assertEquals(listOf(10000L, 10000L, 10000L), result.shares.map { it.share })
    }

    @Test
    fun `equal split with remainder distributes extra paisa to first participants`() {
        val bill = BillGroup(
            totalAmount = 10000L,
            splitMethod = SplitMethod.EQUAL,
            participants = listOf(
                participant("a"), participant("b"), participant("c"),
            ),
        )
        val result = BillCalculator.computeShares(bill) as SplitResult.Success
        // 10000 / 3 = 3333 r 1 — first participant gets +1.
        assertEquals(listOf(3334L, 3333L, 3333L), result.shares.map { it.share })
    }

    @Test
    fun `equal split with tip remainder distributed independently`() {
        val bill = BillGroup(
            totalAmount = 30000L,
            tipAmount = 100L,
            splitMethod = SplitMethod.EQUAL,
            participants = listOf(
                participant("a"), participant("b"), participant("c"),
            ),
        )
        val result = BillCalculator.computeShares(bill) as SplitResult.Success
        // Base: 30000 / 3 = 10000 each. Tip: 100 / 3 = 33 r 1 → first gets +1.
        assertEquals(listOf(10034L, 10033L, 10033L), result.shares.map { it.share })
    }

    @Test
    fun `custom split with valid balance returns success`() {
        val bill = BillGroup(
            totalAmount = 30000L,
            splitMethod = SplitMethod.CUSTOM,
            participants = listOf(
                participant("a"), participant("b"), participant("c"),
            ),
        )
        val custom = mapOf("a" to 12000L, "b" to 10000L, "c" to 8000L)
        val result = BillCalculator.computeShares(bill, custom) as SplitResult.Success
        assertEquals(listOf(12000L, 10000L, 8000L), result.shares.map { it.share })
    }

    @Test
    fun `custom split with unbalanced entry returns invalid`() {
        val bill = BillGroup(
            totalAmount = 30000L,
            splitMethod = SplitMethod.CUSTOM,
            participants = listOf(
                participant("a"), participant("b"), participant("c"),
            ),
        )
        val custom = mapOf("a" to 10000L, "b" to 10000L, "c" to 5000L) // 25000, short by 5000
        val result = BillCalculator.computeShares(bill, custom)
        assertTrue(result is SplitResult.Invalid)
        assertTrue((result as SplitResult.Invalid).message.contains("Remaining"))
    }

    @Test
    fun `item-wise split where one item is shared by 2 of 4 people`() {
        val bill = BillGroup(
            splitMethod = SplitMethod.ITEM_WISE,
            participants = listOf(
                participant("a"), participant("b"), participant("c"), participant("d"),
            ),
            items = listOf(
                BillItem(id = "i1", name = "Pizza", price = 10000L, assignedParticipantIds = listOf("a", "b")),
                BillItem(id = "i2", name = "Coke A", price = 5000L, assignedParticipantIds = listOf("a")),
                BillItem(id = "i3", name = "Coke B", price = 5000L, assignedParticipantIds = listOf("b")),
                BillItem(id = "i4", name = "Dessert", price = 4000L, assignedParticipantIds = listOf("c", "d")),
            ),
        )
        val result = BillCalculator.computeShares(bill) as SplitResult.Success
        val byId = result.shares.associateBy { it.participantId }
        // A: half pizza 5000 + coke 5000 = 10000
        // B: half pizza 5000 + coke 5000 = 10000
        // C: half dessert 2000
        // D: half dessert 2000
        assertEquals(10000L, byId["a"]!!.share)
        assertEquals(10000L, byId["b"]!!.share)
        assertEquals(2000L, byId["c"]!!.share)
        assertEquals(2000L, byId["d"]!!.share)
    }

    @Test
    fun `item-wise split distributes tax proportionally to item subtotals`() {
        val bill = BillGroup(
            taxAmount = 2000L,
            splitMethod = SplitMethod.ITEM_WISE,
            participants = listOf(participant("a"), participant("b")),
            items = listOf(
                BillItem(id = "i1", name = "Food A", price = 6000L, assignedParticipantIds = listOf("a")),
                BillItem(id = "i2", name = "Food B", price = 4000L, assignedParticipantIds = listOf("b")),
            ),
        )
        val result = BillCalculator.computeShares(bill) as SplitResult.Success
        val byId = result.shares.associateBy { it.participantId }
        // Item totals: a=6000, b=4000. Total items = 10000. Tax 2000 proportional: a gets 1200, b gets 800.
        // Sum must equal 12000.
        val total = result.shares.sumOf { it.share }
        assertEquals(12000L, total)
        // Individual checks tolerate +/- 1 due to remainder redistribution.
        assertTrue((byId["a"]!!.share - 7200L).let { it == 0L || it == 1L })
        assertTrue((byId["b"]!!.share - 4800L).let { it == 0L || it == -1L })
    }

    @Test
    fun `simplify settlement 3-person cycle collapses to minimum transfers`() {
        // A paid 100, B paid 0, C paid 0. Each owes 33/34 share.
        val participants = listOf(
            participant("a", paid = 10000L, share = 3334L),
            participant("b", paid = 0L, share = 3333L),
            participant("c", paid = 0L, share = 3333L),
        )
        val transfers = BillCalculator.simplifySettlement(participants)
        // Net: a=+6666, b=-3333, c=-3333 → exactly 2 transfers, both to A.
        assertEquals(2, transfers.size)
        assertTrue(transfers.all { it.toParticipantId == "a" })
        assertEquals(6666L, transfers.sumOf { it.amount })
    }

    @Test
    fun `simplify settlement 4-person multiple creditors and debtors`() {
        // A paid 80, B paid 0, C paid 0, D paid 40. Each owes 30 share.
        val participants = listOf(
            participant("a", paid = 8000L, share = 3000L),
            participant("b", paid = 0L, share = 3000L),
            participant("c", paid = 0L, share = 3000L),
            participant("d", paid = 4000L, share = 3000L),
        )
        val transfers = BillCalculator.simplifySettlement(participants)
        // Net: a=+5000, b=-3000, c=-3000, d=+1000.
        // Greedy: largest debtor (b or c -3000) → largest creditor (a +5000): 3000 (a → +2000)
        // Next: largest debtor (c -3000) → largest creditor (a +2000): 2000 (a → 0) + 1000 from d
        // Total 3 transfers.
        assertEquals(3, transfers.size)
        assertEquals(6000L, transfers.sumOf { it.amount })
    }

    @Test
    fun `simplify settlement everyone paid equal produces zero transfers`() {
        val participants = listOf(
            participant("a", paid = 3000L, share = 3000L),
            participant("b", paid = 3000L, share = 3000L),
            participant("c", paid = 3000L, share = 3000L),
        )
        val transfers = BillCalculator.simplifySettlement(participants)
        assertTrue(transfers.isEmpty())
    }
}
