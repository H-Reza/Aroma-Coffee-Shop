package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface SommelierState {
    object Idle : SommelierState
    object Loading : SommelierState
    data class Success(val recommendation: String) : SommelierState
    data class Error(val message: String) : SommelierState
}

sealed interface CheckoutState {
    object Idle : CheckoutState
    object Processing : CheckoutState
    data class Success(val orderNumber: String) : CheckoutState
    data class Error(val message: String) : CheckoutState
}

class CoffeeViewModel(application: Application) : AndroidViewModel(application) {

    private val database: CoffeeDatabase = Room.databaseBuilder(
        application,
        CoffeeDatabase::class.java,
        "aroma_coffee_database"
    )
        .fallbackToDestructiveMigration()
        .build()

    private val repository = CoffeeRepository(application, database.coffeeDao())

    // --- State Observables ---
    val allProducts: StateFlow<List<ProductEntity>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cartItems: StateFlow<List<CartItemEntity>> = repository.cartItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subscriptions: StateFlow<List<SubscriptionEntity>> = repository.subscriptions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val orders: StateFlow<List<OrderEntity>> = repository.orders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userProfile: StateFlow<UserProfileEntity?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // --- Search & Filtering States ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("all") // "all", "beans", "equipment"
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _selectedRoast = MutableStateFlow<String?>(null) // "لایت", "مدیوم", "دارک"
    val selectedRoast = _selectedRoast.asStateFlow()

    private val _selectedOrigin = MutableStateFlow<String?>(null) // "تک خاستگاه", "ترکیبی"
    val selectedOrigin = _selectedOrigin.asStateFlow()

    private val _selectedEquipmentType = MutableStateFlow<String?>(null) // "کتری", "دم‌آور", "آسیاب"
    val selectedEquipmentType = _selectedEquipmentType.asStateFlow()

    // --- Computed Filtered Products ---
    val filteredProducts: StateFlow<List<ProductEntity>> = combine(
        allProducts, _searchQuery, _selectedCategory, _selectedRoast, _selectedOrigin, _selectedEquipmentType
    ) { arrayOfFlows ->
        @Suppress("UNCHECKED_CAST")
        val products = arrayOfFlows[0] as List<ProductEntity>
        val query = arrayOfFlows[1] as String
        val cat = arrayOfFlows[2] as String
        val roast = arrayOfFlows[3] as String?
        val origin = arrayOfFlows[4] as String?
        val eqType = arrayOfFlows[5] as String?

        products.filter { product ->
            val matchesQuery = product.nameFa.contains(query, ignoreCase = true) || 
                               product.nameEn.contains(query, ignoreCase = true) ||
                               product.descriptionFa.contains(query, ignoreCase = true)
            
            val matchesCategory = cat == "all" || product.category == cat
            
            val matchesRoast = cat == "equipment" || roast == null || product.roastLevelFa == roast
            
            val matchesOrigin = cat == "equipment" || origin == null || product.originFa == origin

            val matchesEquipmentType = cat == "beans" || eqType == null || product.equipmentTypeFa == eqType

            matchesQuery && matchesCategory && matchesRoast && matchesOrigin && matchesEquipmentType
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- UI/UX Interactive states ---
    private val _checkoutState = MutableStateFlow<CheckoutState>(CheckoutState.Idle)
    val checkoutState = _checkoutState.asStateFlow()

    private val _sommelierState = MutableStateFlow<SommelierState>(SommelierState.Idle)
    val sommelierState = _sommelierState.asStateFlow()

    // Quiz inputs
    val quizAnswers = mutableMapOf<String, String>()

    init {
        // Trigger Seeding automatically on startup
        viewModelScope.launch {
            repository.seedDatabase()
        }
    }

    // --- Search & Filter Setters ---
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun toggleRoastFilter(roast: String) {
        _selectedRoast.value = if (_selectedRoast.value == roast) null else roast
    }

    fun toggleOriginFilter(origin: String) {
        _selectedOrigin.value = if (_selectedOrigin.value == origin) null else origin
    }

    fun toggleEquipmentTypeFilter(eqType: String) {
        _selectedEquipmentType.value = if (_selectedEquipmentType.value == eqType) null else eqType
    }

    fun clearAllFilters() {
        _selectedRoast.value = null
        _selectedOrigin.value = null
        _selectedEquipmentType.value = null
        _searchQuery.value = ""
    }

    // --- Cart Execution ---
    fun addToCart(product: ProductEntity, grind: String, quantity: Int = 1) {
        viewModelScope.launch {
            repository.addToCart(product, grind, quantity)
        }
    }

    fun updateCartQuantity(itemId: Int, quantity: Int) {
        viewModelScope.launch {
            repository.updateCartQuantity(itemId, quantity)
        }
    }

    fun removeCartItem(itemId: Int) {
        viewModelScope.launch {
            repository.deleteCartItem(itemId)
        }
    }

    // --- Subscription (Coffee Club) Execution ---
    fun subscribeToCoffee(product: ProductEntity, frequencyFa: String, grind: String, quantity: Int = 1) {
        viewModelScope.launch {
            // Generate a future Jalali date dynamically based on frequency (e.g. 7, 14, or 30 days)
            val days = when (frequencyFa) {
                "هفتگی" -> 7
                "دو هفته یکبار" -> 14
                else -> 30
            }
            val jalaliDate = com.example.data.JalaliCalendarHelper.getFutureJalaliDate(days).format()
            repository.addSubscription(product, frequencyFa, grind, quantity, jalaliDate)
        }
    }

    fun toggleSubscriptionActive(subId: Int) {
        viewModelScope.launch {
            repository.toggleSubscriptionState(subId)
        }
    }

    fun cancelSubscription(subId: Int) {
        viewModelScope.launch {
            repository.cancelSubscription(subId)
        }
    }

    fun postponeSubscription(subId: Int, daysToPostpone: Int) {
        viewModelScope.launch {
            val sList = subscriptions.value
            val match = sList.find { it.id == subId }
            if (match != null) {
                val newDate = com.example.data.JalaliCalendarHelper.getFutureJalaliDate(daysToPostpone).format()
                repository.updateSubscription(match.copy(nextDeliveryJalali = newDate))
            }
        }
    }

    fun updateSubscriptionParams(subId: Int, grindTypeFa: String, frequencyFa: String) {
        viewModelScope.launch {
            val sList = subscriptions.value
            val match = sList.find { it.id == subId }
            if (match != null) {
                val days = when (frequencyFa) {
                    "هفتگی" -> 7
                    "دو هفته یکبار" -> 14
                    else -> 30
                }
                val newDate = com.example.data.JalaliCalendarHelper.getFutureJalaliDate(days).format()
                repository.updateSubscription(match.copy(
                    grindTypeFa = grindTypeFa,
                    frequencyFa = frequencyFa,
                    nextDeliveryJalali = newDate
                ))
            }
        }
    }

    fun updateSubscriptionQuantity(subId: Int, newQty: Int) {
        viewModelScope.launch {
            val sList = subscriptions.value
            val match = sList.find { it.id == subId }
            if (match != null) {
                repository.updateSubscription(match.copy(quantity = newQty))
            }
        }
    }

    // --- Checkout Process ---
    fun executeCheckout() {
        viewModelScope.launch {
            _checkoutState.value = CheckoutState.Processing
            
            // Generate items summary
            val currentCart = cartItems.value
            if (currentCart.isEmpty()) {
                _checkoutState.value = CheckoutState.Error("سبد خرید شما خالی است")
                return@launch
            }

            val summaryBuilder = StringBuilder()
            var totalPrice = 0
            currentCart.forEachIndexed { index, item ->
                totalPrice += item.priceTomans * item.quantity
                summaryBuilder.append(item.nameFa)
                if (item.roastLevelFa != null) {
                    summaryBuilder.append(" (${item.roastLevelFa})")
                }
                summaryBuilder.append(" [آسیاب: ${item.grindTypeFa}]")
                summaryBuilder.append(" - ${item.quantity} عدد")
                if (index < currentCart.size - 1) summaryBuilder.append("\n")
            }

            try {
                // Simulate payment gateway delay (e.g. Shaparak) for a genuine premium feel
                kotlinx.coroutines.delay(2000)
                
                val orderNo = repository.checkout(totalPrice, summaryBuilder.toString())
                _checkoutState.value = CheckoutState.Success(orderNo)
            } catch (e: Exception) {
                _checkoutState.value = CheckoutState.Error(e.localizedMessage ?: "خطایی در درگاه شتاب رخ داد")
            }
        }
    }

    fun resetCheckoutState() {
        _checkoutState.value = CheckoutState.Idle
    }

    // --- AI Coffee Sommelier Quiz Solver ---
    fun submitSommelierQuiz(
        tasteProfile: String,
        consumeWithMilk: String,
        brewingMethod: String,
        caffeineLevel: String
    ) {
        viewModelScope.launch {
            _sommelierState.value = SommelierState.Loading
            
            // Record responses
            quizAnswers["taste"] = tasteProfile
            quizAnswers["milk"] = consumeWithMilk
            quizAnswers["method"] = brewingMethod
            quizAnswers["caffeine"] = caffeineLevel

            try {
                val recommendation = repository.getAICoffeeSOMMELIERRecommendation(
                    tasteProfile = tasteProfile,
                    consumeWithMilk = consumeWithMilk,
                    brewingMethod = brewingMethod,
                    caffeineLevel = caffeineLevel
                )
                _sommelierState.value = SommelierState.Success(recommendation)
            } catch (e: Exception) {
                _sommelierState.value = SommelierState.Error(e.localizedMessage ?: "موفق به ارتباط با باریستای هوش مصنوعی نشدیم")
            }
        }
    }

    fun resetSommelierQuiz() {
        _sommelierState.value = SommelierState.Idle
    }

    // --- User Profile CRUD & NextAuth Emulation ---
    fun saveUserProfile(profile: UserProfileEntity) {
        viewModelScope.launch {
            repository.saveUserProfile(profile)
        }
    }

    fun updateDeliveryAddress(address: String) {
        viewModelScope.launch {
            val current = userProfile.value
            if (current != null) {
                repository.saveUserProfile(current.copy(deliveryAddress = address))
            }
        }
    }

    fun updatePreferredCurrency(currency: String) {
        viewModelScope.launch {
            val current = userProfile.value
            if (current != null) {
                repository.saveUserProfile(current.copy(preferredCurrency = currency))
            }
        }
    }

    fun simulateLogin(fullName: String, email: String, phoneNumber: String) {
        viewModelScope.launch {
            val updatedProfile = UserProfileEntity(
                id = 1,
                fullName = fullName,
                email = email,
                phoneNumber = phoneNumber,
                deliveryAddress = userProfile.value?.deliveryAddress ?: "تهران، خیابان ولیعصر، برج آروما، طبقه ۵",
                preferredCurrency = userProfile.value?.preferredCurrency ?: "toman",
                isLoggedIn = true
            )
            repository.saveUserProfile(updatedProfile)
        }
    }

    fun logout() {
        viewModelScope.launch {
            val updatedProfile = UserProfileEntity(
                id = 1,
                fullName = "کاربر مهمان",
                email = "",
                phoneNumber = "",
                deliveryAddress = "آدرس ثبت نشده است",
                preferredCurrency = "toman",
                isLoggedIn = false
            )
            repository.saveUserProfile(updatedProfile)
        }
    }
}
