package com.nestmate.app.ui.screens.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nestmate.app.data.model.Clinic
import com.nestmate.app.data.repository.HealthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class HealthState {
    object Loading : HealthState()
    data class Success(val clinics: List<Clinic>) : HealthState()
    data class Error(val message: String) : HealthState()
}

@HiltViewModel
class HealthViewModel @Inject constructor(
    private val healthRepository: HealthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HealthState>(HealthState.Loading)
    val uiState: StateFlow<HealthState> = _uiState.asStateFlow()

    init {
        loadClinics()
    }

    fun loadClinics() {
        viewModelScope.launch {
            _uiState.value = HealthState.Loading
            val result = healthRepository.getNearbyClinics()
            if (result.isSuccess) {
                _uiState.value = HealthState.Success(result.getOrNull() ?: emptyList())
            } else {
                _uiState.value = HealthState.Error("Could not load nearest clinics.")
            }
        }
    }
}
