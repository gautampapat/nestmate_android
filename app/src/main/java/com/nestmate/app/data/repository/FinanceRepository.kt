package com.nestmate.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.nestmate.app.data.model.Expense
import com.nestmate.app.utils.FirebaseConstants
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FinanceRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    /**
     * Gets all personal expenses where the current user is the payer.
     */
    suspend fun getUserExpenses(): Result<List<Expense>> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        return try {
            val snapshot = firestore.collection(FirebaseConstants.COLLECTION_EXPENSES)
                .whereEqualTo("payerId", userId)
                .get()
                .await()
            Result.success(snapshot.documents.mapNotNull { it.toObject(Expense::class.java) })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addExpense(expense: Expense): Result<Unit> {
        return try {
            val docRef = firestore.collection(FirebaseConstants.COLLECTION_EXPENSES).document()
            docRef.set(expense.copy(expenseId = docRef.id)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addSplitExpense(expense: Expense): Result<Unit> {
        return addExpense(expense)
    }

    suspend fun getExpensesOwedToMe(): Result<List<Expense>> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        return try {
            val snapshot = firestore.collection(FirebaseConstants.COLLECTION_EXPENSES)
                .whereEqualTo("payerId", userId)
                .get()
                .await()
            
            // Filter locally to only include non-personal expenses where the splits map is not empty
            val expenses = snapshot.documents
                .mapNotNull { it.toObject(Expense::class.java) }
                .filter { !it.isPersonal && it.splits.isNotEmpty() }
            
            Result.success(expenses)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getExpensesIOwe(): Result<List<Expense>> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        return try {
            val snapshot = firestore.collection(FirebaseConstants.COLLECTION_EXPENSES)
                .whereArrayContains("splitUserIds", userId)
                .get()
                .await()
            Result.success(snapshot.documents.mapNotNull { it.toObject(Expense::class.java) })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markSettled(expenseId: String, settledUserId: String): Result<Unit> {
        return try {
            val docRef = firestore.collection(FirebaseConstants.COLLECTION_EXPENSES).document(expenseId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val expense = snapshot.toObject(Expense::class.java) ?: return@runTransaction
                
                // Remove the user from the splitUserIds array
                transaction.update(docRef, "splitUserIds", FieldValue.arrayRemove(settledUserId))
                // Remove the amount owed from the splits map
                transaction.update(docRef, "splits.$settledUserId", FieldValue.delete())
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveMonthlyBudget(amount: Int): Result<Unit> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        return try {
            firestore.collection(FirebaseConstants.COLLECTION_USERS)
                .document(userId)
                .update("budget", amount)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveRentInfo(amount: Int, dueDay: Int, landlord: String): Result<Unit> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        return try {
            firestore.collection(FirebaseConstants.COLLECTION_USERS)
                .document(userId)
                .update(
                    mapOf(
                        "rentAmount" to amount,
                        "rentDueDay" to dueDay,
                        "rentLandlord" to landlord
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addRentPayment(amount: Int): Result<Unit> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        return try {
            val docRef = firestore.collection(FirebaseConstants.COLLECTION_USERS)
                .document(userId)
                .collection("rentPayments")
                .document()
            docRef.set(
                mapOf(
                    "paymentId" to docRef.id,
                    "amount" to amount,
                    "timestamp" to System.currentTimeMillis()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRentPayments(): Result<List<Map<String, Any>>> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        return try {
            val snapshot = firestore.collection(FirebaseConstants.COLLECTION_USERS)
                .document(userId)
                .collection("rentPayments")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val payments = snapshot.documents.mapNotNull { it.data }
            Result.success(payments)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
