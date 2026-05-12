package com.nestmate.app.ui.screens.restaurant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nestmate.app.data.model.Restaurant
import com.nestmate.app.data.model.RestaurantRating
import com.nestmate.app.data.repository.AuthRepository
import com.nestmate.app.data.repository.RestaurantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RestaurantFilters(
    val foodType: String? = null,
    val category: String? = null,
    val priceLevel: String? = null,
    val mealTime: String? = null,
    val minRating: Float? = null,
)

@HiltViewModel
class RestaurantViewModel @Inject constructor(
    private val restaurantRepository: RestaurantRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _activeFilters = MutableStateFlow(RestaurantFilters())
    val activeFilters: StateFlow<RestaurantFilters> = _activeFilters.asStateFlow()

    val allRestaurants: StateFlow<List<Restaurant>> = restaurantRepository.getAllRestaurants()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedRestaurantIds: StateFlow<Set<String>> = run {
        val userId = authRepository.getCurrentUserId() ?: ""
        restaurantRepository.getSavedRestaurantIds(userId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
    }

    val filteredRestaurants: StateFlow<List<Restaurant>> =
        combine(allRestaurants, _activeFilters) { restaurants, filters ->
            restaurants.filter { r ->
                (filters.mealTime == null || filters.mealTime in r.mealTimes) &&
                (filters.foodType == null || r.foodType == filters.foodType || r.foodType == "BOTH") &&
                (filters.category == null || r.category == filters.category) &&
                (filters.priceLevel == null || r.priceLevel == filters.priceLevel) &&
                (filters.minRating == null || r.overallRating >= filters.minRating)
            }.sortedByDescending { it.overallRating }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trendingRestaurants: StateFlow<List<Restaurant>> =
        allRestaurants.combine(MutableStateFlow(Unit)) { restaurants, _ ->
            restaurants.sortedByDescending { it.visitCount }.take(5)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgetRestaurants: StateFlow<List<Restaurant>> =
        allRestaurants.combine(MutableStateFlow(Unit)) { restaurants, _ ->
            restaurants.filter { it.priceLevel == "BUDGET" }.sortedByDescending { it.overallRating }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val promotedAndTrusted: StateFlow<List<Restaurant>> =
        allRestaurants.combine(MutableStateFlow(Unit)) { restaurants, _ ->
            restaurants.filter { it.isPromoted || it.isTrusted }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val imHungryPicks: StateFlow<List<Restaurant>> =
        filteredRestaurants.combine(MutableStateFlow(Unit)) { restaurants, _ ->
            restaurants.filter { it.priceLevel != "PREMIUM" }.sortedByDescending { it.overallRating }.take(3)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateFilter(update: RestaurantFilters.() -> RestaurantFilters) {
        _activeFilters.value = _activeFilters.value.update()
    }

    fun clearFilters() {
        _activeFilters.value = RestaurantFilters()
    }

    fun logVisit(restaurantId: String) {
        restaurantRepository.logVisit(restaurantId)
    }

    fun submitRating(restaurantId: String, rating: Float) {
        val userId = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            restaurantRepository.submitRating(
                RestaurantRating(
                    restaurantId = restaurantId,
                    userId = userId,
                    rating = rating,
                    createdAt = System.currentTimeMillis(),
                )
            ).onFailure { _error.value = it.message }
        }
    }

    fun toggleSave(restaurantId: String) {
        val userId = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            if (restaurantId in savedRestaurantIds.value) {
                restaurantRepository.unsaveRestaurant(userId, restaurantId)
            } else {
                restaurantRepository.saveRestaurant(userId, restaurantId)
            }
        }
    }

    fun getUserRating(restaurantId: String): StateFlow<RestaurantRating?> {
        val userId = authRepository.getCurrentUserId() ?: ""
        return restaurantRepository.getUserRatingForRestaurant(userId, restaurantId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    }
}
