package com.nestmate.app.utils.bill

import com.nestmate.app.data.model.BillGroup
import com.nestmate.app.data.model.BillItem
import com.nestmate.app.data.model.Participant
import com.nestmate.app.data.model.SettlementTransfer
import com.nestmate.app.data.model.SplitMethod

data class ParticipantShare(
    val participantId: String,
    val name: String,
    val share: Long,
)

sealed interface SplitResult {
    data class Success(val shares: List<ParticipantShare>) : SplitResult
    data class Invalid(val message: String) : SplitResult
}

object BillCalculator {

    fun computeShares(bill: BillGroup, customShares: Map<String, Long> = emptyMap()): SplitResult {
        if (bill.participants.isEmpty()) {
            return SplitResult.Invalid("Add at least one participant")
        }
        if (bill.totalAmount < 0L || bill.taxAmount < 0L ||
            bill.serviceChargeAmount < 0L || bill.tipAmount < 0L
        ) {
            return SplitResult.Invalid("Amounts cannot be negative")
        }

        return when (bill.splitMethod) {
            SplitMethod.EQUAL -> equalSplit(bill)
            SplitMethod.CUSTOM -> customSplit(bill, customShares)
            SplitMethod.ITEM_WISE -> itemWiseSplit(bill)
        }
    }

    private fun equalSplit(bill: BillGroup): SplitResult {
        val base = bill.totalAmount + bill.taxAmount + bill.serviceChargeAmount
        val tip = bill.tipAmount
        val count = bill.participants.size

        val baseShares = distributeWithRemainder(base, count)
        val tipShares = distributeWithRemainder(tip, count)

        return SplitResult.Success(
            bill.participants.mapIndexed { i, p ->
                ParticipantShare(
                    participantId = p.id,
                    name = p.name,
                    share = baseShares[i] + tipShares[i],
                )
            },
        )
    }

    private fun customSplit(bill: BillGroup, customShares: Map<String, Long>): SplitResult {
        val expectedTotal = bill.totalAmount + bill.taxAmount +
            bill.serviceChargeAmount + bill.tipAmount
        val providedTotal = bill.participants.sumOf { customShares[it.id] ?: 0L }
        if (providedTotal != expectedTotal) {
            val diff = expectedTotal - providedTotal
            return SplitResult.Invalid(
                if (diff > 0) "Remaining: ${diff}p unassigned"
                else "Over by ${-diff}p",
            )
        }
        return SplitResult.Success(
            bill.participants.map { p ->
                ParticipantShare(
                    participantId = p.id,
                    name = p.name,
                    share = customShares[p.id] ?: 0L,
                )
            },
        )
    }

    private fun itemWiseSplit(bill: BillGroup): SplitResult {
        if (bill.items.isEmpty()) {
            return SplitResult.Invalid("Add at least one item")
        }
        val perParticipantItemTotal = mutableMapOf<String, Long>()
        bill.participants.forEach { perParticipantItemTotal[it.id] = 0L }

        for (item in bill.items) {
            if (item.assignedParticipantIds.isEmpty()) {
                return SplitResult.Invalid("Item '${item.name}' has no assignees")
            }
            val perHead = distributeWithRemainder(item.price, item.assignedParticipantIds.size)
            item.assignedParticipantIds.forEachIndexed { idx, pid ->
                perParticipantItemTotal[pid] =
                    (perParticipantItemTotal[pid] ?: 0L) + perHead[idx]
            }
        }

        val totalItems = bill.items.sumOf { it.price }
        val extrasTotal = bill.taxAmount + bill.serviceChargeAmount + bill.tipAmount
        // Proportional extras per participant based on their item subtotal.
        val shares = bill.participants.map { p ->
            val itemShare = perParticipantItemTotal[p.id] ?: 0L
            val extras = if (totalItems > 0L) {
                proportionalShare(extrasTotal, itemShare, totalItems)
            } else 0L
            ParticipantShare(
                participantId = p.id,
                name = p.name,
                share = itemShare + extras,
            )
        }

        // Extras rounding: redistribute remainder onto the first participants who had items.
        val expectedSum = totalItems + extrasTotal
        val actualSum = shares.sumOf { it.share }
        val remainder = expectedSum - actualSum
        val adjusted = if (remainder == 0L) shares else adjustRemainder(shares, remainder)

        return SplitResult.Success(adjusted)
    }

    private fun proportionalShare(total: Long, numerator: Long, denominator: Long): Long {
        if (denominator == 0L) return 0L
        return (total.toDouble() * numerator.toDouble() / denominator.toDouble()).toLong()
    }

    private fun adjustRemainder(
        shares: List<ParticipantShare>,
        remainder: Long,
    ): List<ParticipantShare> {
        if (remainder == 0L || shares.isEmpty()) return shares
        val result = shares.toMutableList()
        val step = if (remainder > 0) 1L else -1L
        var remaining = remainder
        var i = 0
        while (remaining != 0L && i < result.size) {
            result[i] = result[i].copy(share = result[i].share + step)
            remaining -= step
            i++
        }
        return result
    }

    /**
     * Split [amount] among [count] participants, distributing the remainder by adding 1 paise
     * to the first `remainder` participants. Returns a list of length [count].
     */
    fun distributeWithRemainder(amount: Long, count: Int): List<Long> {
        require(count > 0) { "count must be > 0" }
        if (amount == 0L) return List(count) { 0L }
        val base = amount / count
        val remainder = amount - base * count
        return List(count) { i ->
            base + if (i < remainder) 1L else 0L
        }
    }

    /**
     * Greedy simplification: pair largest debtor with largest creditor until all balances zero.
     * Returns list of transfers from debtor → creditor.
     */
    fun simplifySettlement(participants: List<Participant>): List<SettlementTransfer> {
        data class Balance(val participant: Participant, var net: Long)

        val balances = participants.map { Balance(it, it.paidAmount - it.shareAmount) }
            .filter { it.net != 0L }
            .toMutableList()

        val transfers = mutableListOf<SettlementTransfer>()
        while (balances.any { it.net != 0L }) {
            val debtor = balances.filter { it.net < 0L }.minByOrNull { it.net } ?: break
            val creditor = balances.filter { it.net > 0L }.maxByOrNull { it.net } ?: break
            val amount = minOf(-debtor.net, creditor.net)
            if (amount <= 0L) break
            transfers += SettlementTransfer(
                fromParticipantId = debtor.participant.id,
                fromName = debtor.participant.name,
                toParticipantId = creditor.participant.id,
                toName = creditor.participant.name,
                amount = amount,
            )
            debtor.net += amount
            creditor.net -= amount
        }
        return transfers
    }
}
