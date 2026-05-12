package com.nestmate.app.ui.screens.services

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nestmate.app.data.model.ProviderListing
import com.nestmate.app.data.repository.ProviderRepository
import com.nestmate.app.utils.FirebaseConstants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ServicesState {
    object Loading : ServicesState()
    data class Success(val services: List<ProviderListing>) : ServicesState()
    data class Error(val message: String) : ServicesState()
}

@HiltViewModel
class ServicesViewModel @Inject constructor(
    private val providerRepository: ProviderRepository,
) : ViewModel() {

    private val collegeId = FirebaseConstants.DEFAULT_COLLEGE_ID

    private val _servicesState = MutableStateFlow<ServicesState>(ServicesState.Loading)
    val servicesState: StateFlow<ServicesState> = _servicesState.asStateFlow()

    init {
        viewModelScope.launch {
            providerRepository.getServiceListings(collegeId).collect { listings ->
                _servicesState.value = ServicesState.Success(listings)
            }
        }
    }
}
