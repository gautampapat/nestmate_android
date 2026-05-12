package com.nestmate.app.data.repository

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.nestmate.app.data.model.SpendingBudget
import com.nestmate.app.data.model.SpendingCategory
import com.nestmate.app.data.model.SpendingTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpendingRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context,
) {
    private val txRef = firestore.collection("spendingTransactions")
    private val catRef = firestore.collection("spendingCategories")
    private val budgetRef = firestore.collection("spendingBudgets")

    // ── Transactions ──────────────────────────────────────────────────────────

    suspend fun addTransaction(t: SpendingTransaction): Result<String> = runCatching {
        val ref = txRef.document()
        txRef.document(ref.id).set(t.copy(id = ref.id)).await()
        ref.id
    }

    suspend fun updateTransaction(t: SpendingTransaction): Result<Unit> = runCatching {
        txRef.document(t.id).set(t).await()
    }

    suspend fun deleteTransaction(id: String): Result<Unit> = runCatching {
        txRef.document(id).delete().await()
    }

    fun getTransactions(userId: String, startDate: Long, endDate: Long): Flow<List<SpendingTransaction>> = callbackFlow {
        val listener = txRef
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val list = snap?.documents?.mapNotNull {
                    it.toObject(SpendingTransaction::class.java)?.copy(id = it.id)
                } ?: emptyList()
                
                // Filter and sort locally to avoid Firestore composite index requirements
                val filtered = list.filter { it.date in startDate..endDate }
                    .sortedByDescending { it.date }
                
                trySend(filtered)
            }
        awaitClose { listener.remove() }
    }

    // ── Categories ────────────────────────────────────────────────────────────

    fun getCategories(userId: String): Flow<List<SpendingCategory>> = callbackFlow {
        val listener = catRef.whereEqualTo("userId", userId)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                trySend(snap?.documents?.mapNotNull {
                    it.toObject(SpendingCategory::class.java)?.copy(id = it.id)
                } ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    suspend fun addCategory(c: SpendingCategory): Result<String> = runCatching {
        val ref = catRef.document()
        catRef.document(ref.id).set(c.copy(id = ref.id)).await()
        ref.id
    }

    suspend fun deleteCategory(id: String): Result<Unit> = runCatching {
        catRef.document(id).delete().await()
    }

    suspend fun seedDefaultCategories(userId: String) {
        val batch = firestore.batch()
        val expenseDefaults = listOf("Food", "Rent", "Travel", "Laundry", "Entertainment",
            "Shopping", "Books", "Medical", "Subscriptions", "Miscellaneous")
        val incomeDefaults = listOf("Stipend", "Allowance", "Freelance", "Part-time Work", "Other Income")

        expenseDefaults.forEach { name ->
            val ref = catRef.document()
            batch.set(ref, SpendingCategory(id = ref.id, userId = userId, name = name, isDefault = true, transactionType = "EXPENSE"))
        }
        incomeDefaults.forEach { name ->
            val ref = catRef.document()
            batch.set(ref, SpendingCategory(id = ref.id, userId = userId, name = name, isDefault = true, transactionType = "INCOME"))
        }
        batch.commit().await()
    }

    suspend fun hasCategories(userId: String): Boolean {
        return catRef.whereEqualTo("userId", userId).limit(1).get().await().isEmpty.not()
    }

    // ── Budgets ───────────────────────────────────────────────────────────────

    fun getBudgets(userId: String, month: Int, year: Int): Flow<List<SpendingBudget>> = callbackFlow {
        val listener = budgetRef
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val list = snap?.documents?.mapNotNull {
                    it.toObject(SpendingBudget::class.java)?.copy(id = it.id)
                } ?: emptyList()
                
                // Filter locally to avoid composite index requirement
                val filtered = list.filter { it.month == month && it.year == year }
                
                trySend(filtered)
            }
        awaitClose { listener.remove() }
    }

    suspend fun setBudget(b: SpendingBudget): Result<Unit> = runCatching {
        val docId = "${b.userId}_${b.category}_${b.year}_${b.month}"
        budgetRef.document(docId).set(b.copy(id = docId)).await()
    }

    // ── CSV Export ────────────────────────────────────────────────────────────

    suspend fun exportTransactionsAsCsv(transactions: List<SpendingTransaction>): Result<Uri> = runCatching {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sb = StringBuilder("Date,Type,Category,Amount (Rs),Notes\n")
        transactions.forEach { t ->
            val date = sdf.format(Date(t.date))
            val amountRs = t.amount / 100.0
            val notes = t.notes?.replace(",", ";") ?: ""
            sb.appendLine("$date,${t.type},${t.category},$amountRs,$notes")
        }
        val file = File(context.cacheDir, "nestmate_spending_export.csv")
        file.writeText(sb.toString())
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}
