package com.nestmate.app.ui.screens.marketplace

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nestmate.app.data.model.ItemCategory
import com.nestmate.app.data.model.ItemCondition
import com.nestmate.app.data.model.ItemStatus
import com.nestmate.app.data.model.ItemTag
import com.nestmate.app.data.model.MarketplaceFilters
import com.nestmate.app.data.model.MarketplaceItem
import com.nestmate.app.data.model.WantedPost
import com.nestmate.app.data.model.User
import com.nestmate.app.data.repository.AuthRepository
import com.nestmate.app.data.repository.MarketplaceRepository
import com.nestmate.app.utils.imageupload.ImageCompressor
import com.nestmate.app.utils.marketplace.PriceComparison
import com.nestmate.app.utils.marketplace.SearchFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class MarketplaceViewModel @Inject constructor(
    private val repository: MarketplaceRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _serverFilters = MutableStateFlow(MarketplaceFilters())
    val filters: StateFlow<MarketplaceFilters> = _serverFilters.asStateFlow()

    private val _searchQuery = MutableStateFlow<String?>(null)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val currentUser: StateFlow<User?> = authRepository.observeCurrentUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentUserId: String? get() = authRepository.getCurrentUserId()

    private val serverItems: Flow<List<MarketplaceItem>> = _serverFilters
        .flatMapLatest { f -> repository.getActiveListings(f) }

    val activeListings: StateFlow<List<MarketplaceItem>> = combine(
        serverItems,
        _searchQuery.debounce(250L),
    ) { items, query -> SearchFilter.apply(items, query) }
        .onStart { _isLoading.value = true }
        .onEach { _isLoading.value = false }
        .catch { t ->
            _error.value = t.message ?: "Failed to load listings"
            _isLoading.value = false
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    val trendingItems: StateFlow<List<MarketplaceItem>> = activeListings
        .map { items -> items.sortedByDescending { it.viewCount + it.saveCount }.take(8) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    val belowAverageIds: StateFlow<Set<String>> = activeListings
        .map { PriceComparison.buildBelowAverageSet(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptySet())

    val movingOutItems: StateFlow<List<MarketplaceItem>> = activeListings
        .map { items -> items.filter { ItemTag.MOVING_OUT_SALE in it.tags } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    val wantedPosts: StateFlow<List<WantedPost>> = repository.getWantedPosts()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    val savedItemIds: StateFlow<Set<String>> = (
        currentUserId?.let { repository.getSavedItemIds(it) } ?: flowOf(emptySet())
        )
        .catch { emit(emptySet()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptySet())

    val myListings: StateFlow<List<MarketplaceItem>> = (
        currentUserId?.let { repository.getListingsBySeller(it) } ?: flowOf(emptyList())
        )
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    fun setCategory(category: ItemCategory?) {
        _serverFilters.value = _serverFilters.value.copy(category = category)
    }

    fun setCondition(condition: ItemCondition?) {
        _serverFilters.value = _serverFilters.value.copy(condition = condition)
    }

    fun setPriceRange(minPaise: Long?, maxPaise: Long?) {
        _serverFilters.value = _serverFilters.value.copy(minPrice = minPaise, maxPrice = maxPaise)
    }

    fun setSearchQuery(query: String?) {
        // Client-side only — don't touch _serverFilters or we re-subscribe to Firestore per keystroke.
        _searchQuery.value = query?.takeIf { it.isNotBlank() }
    }

    fun clearFilters() {
        _serverFilters.value = MarketplaceFilters()
        _searchQuery.value = null
    }

    fun toggleSave(itemId: String) {
        val uid = currentUserId ?: run {
            _error.value = "Sign in to save listings"; return
        }
        viewModelScope.launch {
            val currentlySaved = itemId in savedItemIds.value
            val result = if (currentlySaved) repository.unsaveItem(uid, itemId)
            else repository.saveItem(uid, itemId)
            result.onFailure { t ->
                _error.value = t.message ?: "Could not update saved status"
            }
        }
    }

    fun markSold(itemId: String, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.markAsSold(itemId)
            result.onFailure { _error.value = it.message ?: "Could not mark sold" }
            onDone(result.isSuccess)
        }
    }

    fun deleteListing(itemId: String, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.deleteListing(itemId)
            result.onFailure { _error.value = it.message ?: "Could not delete listing" }
            onDone(result.isSuccess)
        }
    }

    fun incrementView(itemId: String) {
        repository.incrementViewCount(itemId)
    }

    fun getListing(itemId: String): Flow<MarketplaceItem?> = repository.getListingById(itemId)

    fun getItemsByIds(ids: List<String>): Flow<List<MarketplaceItem>> =
        repository.getListingsByIds(ids)

    fun createListing(
        context: Context,
        input: ListingInput,
        photoUris: List<Uri>,
        onDone: (Result<String>) -> Unit,
    ) {
        val uid = currentUserId ?: run {
            onDone(Result.failure(IllegalStateException("Sign in first"))); return
        }
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val sellerName = runCatching { authRepository.getUserData(uid) }
                    .getOrNull()?.getOrNull()?.name.orEmpty()

                val tempItemId = java.util.UUID.randomUUID().toString()
                val photoUrls = uploadPhotosOrThrow(context, uid, tempItemId, photoUris)

                val item = MarketplaceItem(
                    sellerId = uid,
                    sellerName = sellerName,
                    title = input.title,
                    description = input.description,
                    price = input.pricePaise,
                    isNegotiable = input.isNegotiable,
                    condition = input.condition,
                    category = input.category,
                    photoUrls = photoUrls,
                    tags = input.tags,
                    status = ItemStatus.ACTIVE,
                )
                val result = repository.createListing(item)
                onDone(result)
            } catch (t: Throwable) {
                _error.value = t.message ?: "Failed to create listing"
                onDone(Result.failure(t))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateListing(
        context: Context,
        existing: MarketplaceItem,
        input: ListingInput,
        newPhotoUris: List<Uri>,
        onDone: (Result<Unit>) -> Unit,
    ) {
        val uid = currentUserId ?: run {
            onDone(Result.failure(IllegalStateException("Sign in first"))); return
        }
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val photoUrls = if (newPhotoUris.isEmpty()) existing.photoUrls
                else uploadPhotosOrThrow(context, uid, existing.id, newPhotoUris)

                val updated = existing.copy(
                    title = input.title,
                    description = input.description,
                    price = input.pricePaise,
                    isNegotiable = input.isNegotiable,
                    condition = input.condition,
                    category = input.category,
                    photoUrls = photoUrls,
                    tags = input.tags,
                )
                val result = repository.updateListing(updated)
                onDone(result)
            } catch (t: Throwable) {
                _error.value = t.message ?: "Failed to update listing"
                onDone(Result.failure(t))
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun uploadPhotosOrThrow(
        context: Context,
        userId: String,
        itemId: String,
        uris: List<Uri>,
    ): List<String> {
        val urls = mutableListOf<String>()
        uris.forEachIndexed { index, uri ->
            val bytes = ImageCompressor.compress(uri, context, targetMaxBytes = 500_000L)
            val url = repository.uploadPhoto(userId, itemId, index, bytes).getOrThrow()
            urls += url
        }
        return urls
    }

    fun createWantedPost(
        itemDescription: String,
        maxBudgetPaise: Long,
        category: ItemCategory?,
        onDone: (Result<String>) -> Unit,
    ) {
        val uid = currentUserId ?: run {
            onDone(Result.failure(IllegalStateException("Sign in first"))); return
        }
        viewModelScope.launch {
            val buyerName = runCatching { authRepository.getUserData(uid) }
                .getOrNull()?.getOrNull()?.name.orEmpty()
            val post = WantedPost(
                buyerId = uid,
                buyerName = buyerName,
                itemDescription = itemDescription,
                maxBudget = maxBudgetPaise,
                category = category,
            )
            val result = repository.createWantedPost(post)
            result.onFailure { _error.value = it.message ?: "Could not create post" }
            onDone(result)
        }
    }

    fun createBundle(
        title: String,
        description: String,
        pricePaise: Long,
        selectedItemIds: List<String>,
        bundleExpiryDate: Long?,
        onDone: (Result<String>) -> Unit,
    ) {
        val uid = currentUserId ?: run {
            onDone(Result.failure(IllegalStateException("Sign in first"))); return
        }
        if (selectedItemIds.isEmpty()) {
            onDone(Result.failure(IllegalArgumentException("Select at least one item")))
            return
        }
        viewModelScope.launch {
            val sellerName = runCatching { authRepository.getUserData(uid) }
                .getOrNull()?.getOrNull()?.name.orEmpty()
            val item = MarketplaceItem(
                sellerId = uid,
                sellerName = sellerName,
                title = title,
                description = description,
                price = pricePaise,
                isNegotiable = false,
                condition = ItemCondition.USED,
                category = ItemCategory.MISCELLANEOUS,
                photoUrls = emptyList(),
                tags = listOf(ItemTag.MOVING_OUT_SALE),
                status = ItemStatus.ACTIVE,
                isBundleListing = true,
                bundleItemIds = selectedItemIds,
                bundleExpiryDate = bundleExpiryDate,
            )
            val result = repository.createListing(item)
            result.onFailure { _error.value = it.message ?: "Could not create bundle" }
            onDone(result)
        }
    }

    suspend fun openItemChat(item: MarketplaceItem): Result<String> {
        val uid = currentUserId
            ?: return Result.failure(IllegalStateException("Sign in first"))
        if (uid == item.sellerId) {
            return Result.failure(IllegalStateException("Cannot start a chat with yourself"))
        }
        return repository.getOrCreateItemChat(
            itemId = item.id,
            itemTitle = item.title,
            buyerId = uid,
            sellerId = item.sellerId,
        )
    }

    suspend fun openWantedChat(post: WantedPost): Result<String> {
        val uid = currentUserId
            ?: return Result.failure(IllegalStateException("Sign in first"))
        if (uid == post.buyerId) {
            return Result.failure(IllegalStateException("Cannot respond to your own post"))
        }
        return repository.getOrCreateWantedChat(
            wantedPostId = post.id,
            itemDescription = post.itemDescription,
            buyerId = post.buyerId,
            responderId = uid,
        )
    }

    fun clearError() { _error.value = null }

    data class ListingInput(
        val title: String,
        val description: String,
        val pricePaise: Long,
        val isNegotiable: Boolean,
        val category: ItemCategory,
        val condition: ItemCondition,
        val tags: List<ItemTag>,
    )
}
