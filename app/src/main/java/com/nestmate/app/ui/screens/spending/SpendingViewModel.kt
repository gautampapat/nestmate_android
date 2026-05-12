package com.nestmate.app.ui.screens.spending

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nestmate.app.data.model.SpendingBudget
import com.nestmate.app.data.model.SpendingCategory
import com.nestmate.app.data.model.SpendingTransaction
import com.nestmate.app.data.repository.AuthRepository
import com.nestmate.app.data.repository.SpendingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

// UI-layer data classes
data class CategorySpend(val category: String, val total: Long, val percentage: Float)
data class DailySpend(val date: Long, val total: Long)
data class BudgetProgress(val category: String, val limit: Long, val spent: Long, val isWarning: Boolean)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SpendingViewModel @Inject constructor(
    private val spendingRepository: SpendingRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val userId: String get() = authRepository.getCurrentUserId() ?: ""

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val selectedPeriod = MutableStateFlow("MONTHLY") // DAILY | WEEKLY | MONTHLY

    private val _selectedYearMonth = MutableStateFlow(currentYearMonth())
    val selectedYearMonth: StateFlow<Pair<Int, Int>> = _selectedYearMonth.asStateFlow()

    // ── Transactions ──────────────────────────────────────────────────────────

    val transactions: StateFlow<List<SpendingTransaction>> =
        combine(selectedPeriod, _selectedYearMonth) { period, ym ->
            periodDateRange(period, ym.first, ym.second)
        }.flatMapLatest { (start, end) ->
            spendingRepository.getTransactions(userId, start, end)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Categories ────────────────────────────────────────────────────────────

    val categories: StateFlow<List<SpendingCategory>> =
        spendingRepository.getCategories(userId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Budgets ───────────────────────────────────────────────────────────────

    val budgets: StateFlow<List<SpendingBudget>> =
        _selectedYearMonth.flatMapLatest { (year, month) ->
            spendingRepository.getBudgets(userId, month, year)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Derived aggregates ────────────────────────────────────────────────────

    val totalIncome: StateFlow<Long> = transactions.combine(MutableStateFlow(Unit)) { txs, _ ->
        txs.filter { it.type == "INCOME" }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val totalExpenses: StateFlow<Long> = transactions.combine(MutableStateFlow(Unit)) { txs, _ ->
        txs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val totalBalance: StateFlow<Long> = combine(totalIncome, totalExpenses) { income, expenses ->
        income - expenses
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val categoryBreakdown: StateFlow<List<CategorySpend>> = transactions.combine(MutableStateFlow(Unit)) { txs, _ ->
        val expenses = txs.filter { it.type == "EXPENSE" }
        val total = expenses.sumOf { it.amount }.toFloat().coerceAtLeast(1f)
        expenses.groupBy { it.category }
            .map { (cat, items) ->
                val catTotal = items.sumOf { it.amount }
                CategorySpend(cat, catTotal, catTotal / total * 100f)
            }
            .sortedByDescending { it.total }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val timeSeriesData: StateFlow<List<DailySpend>> = transactions.combine(MutableStateFlow(Unit)) { txs, _ ->
        txs.filter { it.type == "EXPENSE" }
            .groupBy { it.date / 86_400_000 * 86_400_000 } // truncate to day
            .map { (day, items) -> DailySpend(day, items.sumOf { it.amount }) }
            .sortedBy { it.date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgetProgress: StateFlow<List<BudgetProgress>> =
        combine(budgets, categoryBreakdown) { budgetList, breakdown ->
            budgetList.map { budget ->
                val spent = breakdown.firstOrNull { it.category == budget.category }?.total ?: 0L
                BudgetProgress(
                    category = budget.category,
                    limit = budget.monthlyLimit,
                    spent = spent,
                    isWarning = budget.monthlyLimit > 0 && spent.toFloat() / budget.monthlyLimit >= 0.8f
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Init ──────────────────────────────────────────────────────────────────

    init {
        viewModelScope.launch {
            if (!spendingRepository.hasCategories(userId)) {
                spendingRepository.seedDefaultCategories(userId)
            }
        }
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    fun addTransaction(t: SpendingTransaction) {
        viewModelScope.launch {
            spendingRepository.addTransaction(t.copy(userId = userId)).onFailure { _error.value = it.message }
        }
    }

    fun updateTransaction(t: SpendingTransaction) {
        viewModelScope.launch {
            spendingRepository.updateTransaction(t).onFailure { _error.value = it.message }
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            spendingRepository.deleteTransaction(id).onFailure { _error.value = it.message }
        }
    }

    fun addCategory(name: String, type: String) {
        viewModelScope.launch {
            spendingRepository.addCategory(
                SpendingCategory(userId = userId, name = name, isDefault = false, transactionType = type)
            ).onFailure { _error.value = it.message }
        }
    }

    fun deleteCategory(id: String) {
        viewModelScope.launch {
            spendingRepository.deleteCategory(id).onFailure { _error.value = it.message }
        }
    }

    fun setBudget(category: String, limitPaise: Long) {
        val (year, month) = _selectedYearMonth.value
        viewModelScope.launch {
            spendingRepository.setBudget(
                SpendingBudget(userId = userId, category = category, monthlyLimit = limitPaise, month = month, year = year)
            ).onFailure { _error.value = it.message }
        }
    }

    fun exportCsv(onResult: (Uri?) -> Unit) {
        viewModelScope.launch {
            val result = spendingRepository.exportTransactionsAsCsv(transactions.value)
            onResult(result.getOrNull())
        }
    }

    fun setSelectedPeriod(period: String) { selectedPeriod.value = period }

    fun navigateMonth(delta: Int) {
        val (year, month) = _selectedYearMonth.value
        val cal = Calendar.getInstance().apply { set(year, month - 1, 1); add(Calendar.MONTH, delta) }
        _selectedYearMonth.value = cal.get(Calendar.YEAR) to (cal.get(Calendar.MONTH) + 1)
    }

    fun clearError() { _error.value = null }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun currentYearMonth(): Pair<Int, Int> {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.YEAR) to (cal.get(Calendar.MONTH) + 1)
    }

    private fun periodDateRange(period: String, year: Int, month: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        return when (period) {
            "MONTHLY" -> {
                cal.set(year, month - 1, 1, 0, 0, 0); cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.set(year, month - 1, cal.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
                cal.set(Calendar.MILLISECOND, 999)
                start to cal.timeInMillis
            }
            "WEEKLY" -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.add(Calendar.DAY_OF_WEEK, 6)
                cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59)
                start to cal.timeInMillis
            }
            else -> { // DAILY
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59)
                start to cal.timeInMillis
            }
        }
    }
}
