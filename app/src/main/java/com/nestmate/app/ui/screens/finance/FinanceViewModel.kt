package com.nestmate.app.ui.screens.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.nestmate.app.data.model.Expense
import com.nestmate.app.data.repository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class FinanceState {
    object Loading : FinanceState()
    data class Success(
        val personalExpenses: List<Expense>,
        val owedToMe: List<Expense>,
        val iOwe: List<Expense>,
        val rentPayments: List<Map<String, Any>>
    ) : FinanceState()
    data class Error(val message: String) : FinanceState()
}

@HiltViewModel
class FinanceViewModel @Inject constructor(
    private val financeRepository: FinanceRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow<FinanceState>(FinanceState.Loading)
    val uiState: StateFlow<FinanceState> = _uiState.asStateFlow()

    init {
        loadAllData()
    }

    fun loadAllData() {
        viewModelScope.launch {
            _uiState.value = FinanceState.Loading
            if (auth.currentUser?.uid == null) {
                _uiState.value = FinanceState.Error("Please log in to view finances.")
                return@launch
            }

            // Run all four Firestore fetches concurrently instead of sequentially
            coroutineScope {
                val personalDeferred = async { financeRepository.getUserExpenses() }
                val owedToMeDeferred = async { financeRepository.getExpensesOwedToMe() }
                val iOweDeferred     = async { financeRepository.getExpensesIOwe() }
                val rentDeferred     = async { financeRepository.getRentPayments() }

                val personalRes  = personalDeferred.await()
                val owedToMeRes  = owedToMeDeferred.await()
                val iOweRes      = iOweDeferred.await()
                val rentRes      = rentDeferred.await()

                if (personalRes.isSuccess && owedToMeRes.isSuccess && iOweRes.isSuccess && rentRes.isSuccess) {
                    _uiState.value = FinanceState.Success(
                        personalExpenses = personalRes.getOrNull() ?: emptyList(),
                        owedToMe         = owedToMeRes.getOrNull() ?: emptyList(),
                        iOwe             = iOweRes.getOrNull() ?: emptyList(),
                        rentPayments     = rentRes.getOrNull() ?: emptyList()
                    )
                } else {
                    _uiState.value = FinanceState.Error("Failed to load some finance data.")
                }
            }
        }
    }

    fun addPersonalExpense(desc: String, amount: Int, category: String, note: String) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            val userName = auth.currentUser?.displayName ?: "You"
            val exp = Expense(
                payerId = userId,
                payerName = userName,
                description = desc,
                amount = amount,
                category = category,
                note = note,
                isPersonal = true
            )
            financeRepository.addExpense(exp)
            loadAllData()
        }
    }

    fun addSplitExpense(desc: String, amount: Int, category: String, flatmates: List<String>) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            val userName = auth.currentUser?.displayName ?: "You"
            
            // Calculate equal splits
            // Total people = flatmates + me
            val totalPeople = flatmates.size + 1
            val splitAmount = amount / totalPeople
            
            val splits = flatmates.associateWith { splitAmount }
            
            val exp = Expense(
                payerId = userId,
                payerName = userName,
                description = desc,
                amount = amount,
                category = category,
                isPersonal = false,
                splits = splits,
                splitUserIds = flatmates
            )
            financeRepository.addSplitExpense(exp)
            loadAllData()
        }
    }

    fun markSettled(expenseId: String, settledUserId: String) {
        viewModelScope.launch {
            financeRepository.markSettled(expenseId, settledUserId)
            loadAllData()
        }
    }

    fun saveMonthlyBudget(amount: Int) {
        viewModelScope.launch {
            financeRepository.saveMonthlyBudget(amount)
        }
    }

    fun saveRentInfo(amount: Int, dueDay: Int, landlord: String) {
        viewModelScope.launch {
            financeRepository.saveRentInfo(amount, dueDay, landlord)
        }
    }

    fun markRentPaid(amount: Int) {
        viewModelScope.launch {
            financeRepository.addRentPayment(amount)
            loadAllData()
        }
    }
}
