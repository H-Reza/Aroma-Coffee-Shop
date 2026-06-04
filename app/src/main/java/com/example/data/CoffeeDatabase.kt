package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- 1. Entities ---

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: Int,
    val nameEn: String,
    val nameFa: String,
    val category: String, // "beans" or "equipment"
    val roastLevelFa: String?, // "لایت", "مدیوم", "دارک"
    val roastLevelEn: String?, // "Light", "Medium", "Dark"
    val originFa: String?, // "تک خاستگاه", "ترکیبی"
    val originEn: String?, // "Single Origin", "Blend"
    val grindTypesFa: String, // Semi-colon separated, e.g. "دانه کامل;اسپرسو;فرانسه"
    val priceTomans: Int,
    val rating: Float,
    val imageUrl: String,
    val stock: Int,
    val descriptionFa: String,
    val descriptionEn: String,
    val roastingDateFa: String, // e.g. "برشته‌کاری: ۲ روز پیش"
    val equipmentTypeFa: String? = null,
    val equipmentTypeEn: String? = null
)

@Entity(tableName = "cart_items")
data class CartItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productId: Int,
    val nameFa: String,
    val priceTomans: Int,
    val quantity: Int,
    val grindTypeFa: String,
    val roastLevelFa: String?,
    val imageUrl: String
)

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productId: Int,
    val productNameFa: String,
    val frequencyFa: String, // "هفتگی", "دو هفته یکبار", "ماهانه"
    val quantity: Int,
    val grindTypeFa: String,
    val roastLevelFa: String?,
    val nextDeliveryJalali: String, // e.g. "۱۴۰۵/۰۳/۱۸"
    val statusFa: String, // "فعال", "متوقف شده"
    val priceTomans: Int,
    val imageUrl: String
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val orderNumber: String, // e.g., "AR-2394"
    val orderDateFa: String, // e.g., "۱۴۰۵/۰۳/۱۴"
    val totalAmountTomans: Int,
    val statusFa: String, // "در حال دم‌آوری", "برشته‌شده", "در حال ارسال", "تحویل‌شده"
    val statusEn: String, // "Brewing", "Roasted", "Delivering", "Delivered"
    val itemsSummaryFa: String // e.g., "قهوه اتیوپی (مدیوم) - ۲ عدد"
)

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val fullName: String,
    val email: String,
    val phoneNumber: String,
    val deliveryAddress: String,
    val preferredCurrency: String, // "toman" or "rial"
    val isLoggedIn: Boolean
)

// --- 2. DAO (Data Access Object) ---

@Dao
interface CoffeeDao {
    // Products
    @Query("SELECT * FROM products")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    // Cart Items
    @Query("SELECT * FROM cart_items")
    fun getCartItems(): Flow<List<CartItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartItem(item: CartItemEntity)

    @Update
    suspend fun updateCartItem(item: CartItemEntity)

    @Query("DELETE FROM cart_items WHERE id = :id")
    suspend fun deleteCartItem(id: Int)

    @Query("DELETE FROM cart_items")
    suspend fun clearCart()

    // Subscriptions
    @Query("SELECT * FROM subscriptions")
    fun getSubscriptions(): Flow<List<SubscriptionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(sub: SubscriptionEntity)

    @Update
    suspend fun updateSubscription(sub: SubscriptionEntity)

    @Query("DELETE FROM subscriptions WHERE id = :id")
    suspend fun deleteSubscription(id: Int)

    // Orders
    @Query("SELECT * FROM orders ORDER BY id DESC")
    fun getOrders(): Flow<List<OrderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    // User Profile
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getUserProfile(): Flow<UserProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserProfile(profile: UserProfileEntity)

    @Query("DELETE FROM user_profile")
    suspend fun clearUserProfile()
}

// --- 3. Database ---

@Database(
    entities = [
        ProductEntity::class,
        CartItemEntity::class,
        SubscriptionEntity::class,
        OrderEntity::class,
        UserProfileEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class CoffeeDatabase : RoomDatabase() {
    abstract fun coffeeDao(): CoffeeDao
}
