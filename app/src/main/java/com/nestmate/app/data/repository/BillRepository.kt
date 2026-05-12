package com.nestmate.app.data.repository

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.nestmate.app.data.model.BillGroup
import com.nestmate.app.data.model.PaymentStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) {

    private val billsRef
        get() = firestore.collection("finance").document("root").collection("bills")

    // Note: Firestore path is `finance/root/bills/{billId}` (4 segments, valid doc path).
    // Rules mirror this in `firestore.rules`.

    suspend fun createBill(bill: BillGroup): Result<String> = runCatching {
        val ref = billsRef.document()
        val now = System.currentTimeMillis()
        val toWrite = bill.copy(id = ref.id, createdAt = now, updatedAt = now)
        ref.set(toWrite).await()
        ref.id
    }

    suspend fun updateBill(bill: BillGroup): Result<Unit> = runCatching {
        billsRef.document(bill.id)
            .set(bill.copy(updatedAt = System.currentTimeMillis()))
            .await()
    }

    suspend fun deleteBill(billId: String): Result<Unit> = runCatching {
        billsRef.document(billId).delete().await()
    }

    fun getBills(userId: String): Flow<List<BillGroup>> = callbackFlow {
        val reg = billsRef
            .whereEqualTo("creatorId", userId)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val list = snap?.toObjects(BillGroup::class.java).orEmpty()
                    .sortedByDescending { it.createdAt }
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    fun getBillById(billId: String): Flow<BillGroup?> = callbackFlow {
        val reg = billsRef.document(billId).addSnapshotListener { snap, err ->
            if (err != null) { close(err); return@addSnapshotListener }
            trySend(snap?.toObject(BillGroup::class.java))
        }
        awaitClose { reg.remove() }
    }

    suspend fun markParticipantPaid(
        billId: String,
        participantId: String,
        newPaidAmount: Long,
    ): Result<Unit> = runCatching {
        firestore.runTransaction { tx ->
            val ref = billsRef.document(billId)
            val snap = tx.get(ref)
            val current = snap.toObject(BillGroup::class.java) ?: error("Bill not found")
            val updatedParticipants = current.participants.map { p ->
                if (p.id != participantId) p
                else {
                    val status = when {
                        newPaidAmount >= p.shareAmount -> PaymentStatus.PAID
                        newPaidAmount > 0L -> PaymentStatus.PARTIALLY_PAID
                        else -> PaymentStatus.UNPAID
                    }
                    p.copy(paidAmount = newPaidAmount, paymentStatus = status)
                }
            }
            val allPaid = updatedParticipants.isNotEmpty() &&
                updatedParticipants.all { it.paymentStatus == PaymentStatus.PAID }
            tx.update(
                ref,
                mapOf(
                    "participants" to updatedParticipants,
                    "isSettled" to allPaid,
                    "updatedAt" to System.currentTimeMillis(),
                ),
            )
        }.await()
    }

    fun exportBillAsCsv(bill: BillGroup, context: Context): Result<Uri> = runCatching {
        val dir = File(context.cacheDir, "bills").apply { mkdirs() }
        val file = File(dir, "bill_${bill.id.ifBlank { "draft" }}.csv")
        val lines = buildList {
            add("Name,Share (paise),Paid (paise),Remaining (paise),Status")
            bill.participants.forEach { p ->
                val remaining = (p.shareAmount - p.paidAmount).coerceAtLeast(0L)
                add("${escape(p.name)},${p.shareAmount},${p.paidAmount},$remaining,${p.paymentStatus.name}")
            }
        }
        file.writeText(lines.joinToString("\n"))

        val authority = "${context.packageName}.fileprovider"
        FileProvider.getUriForFile(context, authority, file)
    }

    private fun escape(value: String): String =
        if (value.contains(',') || value.contains('"')) "\"${value.replace("\"", "\"\"")}\""
        else value
}
