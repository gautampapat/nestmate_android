package com.nestmate.app.ui.screens.bill

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nestmate.app.data.model.BillGroup
import com.nestmate.app.data.model.BillItem
import com.nestmate.app.data.model.ConnectionStatus
import com.nestmate.app.data.model.Participant
import com.nestmate.app.data.model.PaymentStatus
import com.nestmate.app.data.model.RoommateProfile
import com.nestmate.app.data.model.SettlementTransfer
import com.nestmate.app.data.model.SplitMethod
import com.nestmate.app.data.repository.AuthRepository
import com.nestmate.app.data.repository.BillRepository
import com.nestmate.app.data.repository.ConnectionRepository
import com.nestmate.app.data.repository.RoommateRepository
import com.nestmate.app.utils.bill.BillCalculator
import com.nestmate.app.utils.bill.ParticipantShare
import com.nestmate.app.utils.bill.SplitResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BillSplitterViewModel @Inject constructor(
    private val repository: BillRepository,
    private val authRepository: AuthRepository,
    private val roommateRepository: RoommateRepository,
    private val connectionRepository: ConnectionRepository,
) : ViewModel() {

    val currentUserId: String? get() = authRepository.getCurrentUserId()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val bills: StateFlow<List<BillGroup>> = (
        currentUserId?.let { repository.getBills(it) } ?: flowOf(emptyList())
        )
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    /**
     * Connected roommates (ACCEPTED connections) resolved as [RoommateProfile] list.
     * Loaded once on first observation, refreshed when connections change.
     */
    private val _connectedRoommates = MutableStateFlow<List<RoommateProfile>>(emptyList())
    val connectedRoommates: StateFlow<List<RoommateProfile>> = _connectedRoommates.asStateFlow()

    init {
        loadConnectedRoommates()
    }

    /**
     * Fetches accepted roommate connections and resolves each other-user's RoommateProfile.
     * If a profile is not found (e.g., user hasn't set up a roommate profile), a minimal
     * placeholder profile with just the userId is included so the name falls back gracefully.
     */
    private fun loadConnectedRoommates() {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            connectionRepository.getConnections(uid)
                .catch { emit(emptyList()) }
                .collect { connections ->
                val acceptedOtherIds = connections
                    .filter { it.status == ConnectionStatus.ACCEPTED }
                    .map { conn -> if (conn.requesterId == uid) conn.receiverId else conn.requesterId }
                    .filter { it.isNotBlank() }

                val profiles = acceptedOtherIds.mapNotNull { otherId ->
                    runCatching { roommateRepository.getProfile(otherId).first() }.getOrNull()
                        ?: RoommateProfile(userId = otherId, name = "Roommate") // fallback
                }
                _connectedRoommates.value = profiles
            }
        }
    }

    fun getBill(billId: String): Flow<BillGroup?> = repository.getBillById(billId)

    fun previewShares(
        bill: BillGroup,
        customShares: Map<String, Long> = emptyMap(),
    ): SplitResult = BillCalculator.computeShares(bill, customShares)

    fun simplify(participants: List<Participant>): List<SettlementTransfer> =
        BillCalculator.simplifySettlement(participants)

    fun createBill(
        input: BillInput,
        customShares: Map<String, Long> = emptyMap(),
        onDone: (Result<String>) -> Unit,
    ) {
        val uid = currentUserId ?: run {
            onDone(Result.failure(IllegalStateException("Sign in first"))); return
        }
        val draft = input.toBillGroup(creatorId = uid)
        when (val result = BillCalculator.computeShares(draft, customShares)) {
            is SplitResult.Invalid -> {
                _error.value = result.message
                onDone(Result.failure(IllegalStateException(result.message)))
            }
            is SplitResult.Success -> {
                val withShares = draft.copy(
                    participants = draft.participants.map { p ->
                        val assigned = result.shares.firstOrNull { it.participantId == p.id }
                        p.copy(
                            shareAmount = assigned?.share ?: 0L,
                            paymentStatus = PaymentStatus.UNPAID,
                        )
                    },
                )
                viewModelScope.launch {
                    val creation = repository.createBill(withShares)
                    creation.onFailure { _error.value = it.message ?: "Could not create bill" }
                    onDone(creation)
                }
            }
        }
    }

    fun markPaid(billId: String, participantId: String, amountPaise: Long) {
        viewModelScope.launch {
            repository.markParticipantPaid(billId, participantId, amountPaise)
                .onFailure { _error.value = it.message ?: "Could not mark paid" }
        }
    }

    fun deleteBill(billId: String, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.deleteBill(billId)
            result.onFailure { _error.value = it.message ?: "Could not delete bill" }
            onDone(result.isSuccess)
        }
    }

    fun exportCsv(bill: BillGroup, context: Context): Uri? {
        val result = repository.exportBillAsCsv(bill, context)
        result.onFailure { _error.value = it.message ?: "Could not export CSV" }
        return result.getOrNull()
    }

    fun clearError() { _error.value = null }

    data class BillInput(
        val title: String,
        val description: String?,
        val totalAmount: Long,
        val taxAmount: Long,
        val serviceChargeAmount: Long,
        val tipAmount: Long,
        val notes: String?,
        val splitMethod: SplitMethod,
        val participants: List<Participant>,
        val items: List<BillItem>,
    )

    private fun BillInput.toBillGroup(creatorId: String): BillGroup =
        BillGroup(
            creatorId = creatorId,
            title = title,
            description = description,
            totalAmount = totalAmount,
            taxAmount = taxAmount,
            serviceChargeAmount = serviceChargeAmount,
            tipAmount = tipAmount,
            splitMethod = splitMethod,
            participants = participants,
            items = items,
            notes = notes,
        )
}
