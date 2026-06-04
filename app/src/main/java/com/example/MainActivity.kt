package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.CartItemEntity
import com.example.data.OrderEntity
import com.example.data.ProductEntity
import com.example.data.SubscriptionEntity
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.CheckoutState
import com.example.ui.viewmodel.CoffeeViewModel
import com.example.ui.viewmodel.SommelierState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: CoffeeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // Force elegant Persian RTL layout throughout the master app view
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        contentWindowInsets = WindowInsets.safeDrawing
                    ) { innerPadding ->
                        AromaMainApp(
                            viewModel = viewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}

// --- Custom Localized Price & Format Utilities ---

var globalUseRials by mutableStateOf(false)

fun Int.toPersianPrice(): String {
    val amount = if (globalUseRials) this * 10 else this
    val formatter = java.text.DecimalFormat("#,###")
    val formatted = formatter.format(amount)
    val unit = if (globalUseRials) " ریال" else " تومان"
    return formatted.toPersianDigits() + unit
}

fun String.toPersianDigits(): String {
    var result = this
    val farsiDigits = arrayOf("۰", "۱", "۲", "۳", "۴", "۵", "۶", "۷", "۸", "۹")
    for (i in 0..9) {
        result = result.replace(i.toString(), farsiDigits[i])
    }
    return result
}

// --- Master App Container ---

@Composable
fun AromaMainApp(
    viewModel: CoffeeViewModel,
    modifier: Modifier = Modifier
) {
    var currentTab by remember { mutableStateOf("catalog") } // "catalog", "quiz", "cart", "subscriptions", "orders"
    val cartCount by viewModel.cartItems.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showProfileDialog by remember { mutableStateOf(false) }

    LaunchedEffect(userProfile) {
        globalUseRials = userProfile?.preferredCurrency == "rial"
    }

    if (showProfileDialog) {
        UserProfileDialog(viewModel = viewModel, onDismiss = { showProfileDialog = false })
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // App Bar Title Block
            AromaHeader(
                tabId = currentTab,
                cartCount = cartCount.sumOf { it.quantity },
                onProfileClick = { showProfileDialog = true }
            )

            // Content Area with fluid tab transitions
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (currentTab) {
                    "catalog" -> CatalogTab(viewModel = viewModel)
                    "quiz" -> SommelierQuizTab(viewModel = viewModel)
                    "cart" -> CartTab(viewModel = viewModel, onTabRequested = { currentTab = it })
                    "subscriptions" -> SubscriptionClubTab(viewModel = viewModel, onCatalogRequest = { currentTab = "catalog" })
                    "orders" -> LogisticsOrdersTab(viewModel = viewModel)
                }
            }

            // Navigation Bar Safe container (RTL order is automatic)
            AromaBottomNavigation(
                currentTab = currentTab,
                onTabSelected = { currentTab = it },
                cartItemCount = cartCount.sumOf { it.quantity }
            )
        }
    }
}

// --- Header Component ---

@Composable
fun AromaHeader(tabId: String, cartCount: Int, onProfileClick: () -> Unit) {
    val titleFa = when (tabId) {
        "catalog" -> "رُستری طلایی"
        "quiz" -> "منوی طعم‌یاب هوشمند"
        "cart" -> "سبد سفارشات شما"
        "subscriptions" -> "پکیج اشتراک طلایی"
        "orders" -> "لجستیک و تحویل سفارش"
        else -> "رُستری طلایی"
    }

    val subtitleFa = when (tabId) {
        "catalog" -> "گلچین برترین دانه‌های تخصصی عربیکا با عطر تازه"
        "quiz" -> "مشاوره اختصاصی و طعم‌یابی مبتنی بر ابزار هوش مصنوعی"
        "cart" -> "پرداخت امن، سریع و تحویل کالا در سراسر شتاب"
        "subscriptions" -> "تامین مداوم دانه تازه با ۱۵٪ تخفیف طلایی"
        "orders" -> "رصد لجستیک دانه قهوه از کارگاه تا آدرس خانه شما"
        else -> "فروشگاه تخصصی و برشته‌کاری مدرن دانه قهوه"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Interactive Profile / Persona Icon based on the Sleek Theme template
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                    .clickable { onProfileClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "پروفایل و ترجیحات",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = titleFa,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitleFa,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 11.sp
                )
            }
        }

        // Round Shopping Bag Icon with Metallic Gold Badge for basket counts
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                .clickable { },
            contentAlignment = Alignment.Center
        ) {
            BadgedBox(
                badge = {
                    if (cartCount > 0) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.offset(x = 4.dp, y = (-4).dp)
                        ) {
                            Text(
                                text = cartCount.toString().toPersianDigits(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingBag,
                    contentDescription = "سبد خرید",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// --- Navigation Component ---

@Composable
fun AromaBottomNavigation(
    currentTab: String,
    onTabSelected: (String) -> Unit,
    cartItemCount: Int
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .border(width = 1.dp, color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)),
        color = MaterialTheme.colorScheme.background,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val navItems = listOf(
                NavigationItem("catalog", "ویترین", Icons.Default.Storefront, Icons.Outlined.Storefront),
                NavigationItem("quiz", "طعم‌یاب AI", Icons.Default.Psychology, Icons.Outlined.Psychology),
                NavigationItem("cart", "سبد خرید", Icons.Default.ShoppingBag, Icons.Outlined.ShoppingBag, badgeCount = cartItemCount),
                NavigationItem("subscriptions", "اشتراک", Icons.Default.CalendarMonth, Icons.Outlined.CalendarMonth),
                NavigationItem("orders", "لجستیک", Icons.Default.LocalShipping, Icons.Outlined.LocalShipping)
            )

            navItems.forEach { item ->
                val isSelected = currentTab == item.id
                Column(
                    modifier = Modifier
                        .testTag("nav_tab_${item.id}")
                        .clickable { onTabSelected(item.id) }
                        .padding(vertical = 4.dp, horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                else Color.Transparent
                            )
                            .padding(vertical = 6.dp, horizontal = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BadgedBox(
                            badge = {
                                if (item.badgeCount > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.tertiary,
                                        contentColor = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.offset(x = 4.dp, y = (-4).dp)
                                    ) {
                                        Text(
                                            text = item.badgeCount.toString().toPersianDigits(),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

data class NavigationItem(
    val id: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val badgeCount: Int = 0
)

// --- Catalog Screen (Vitreene) ---

@Composable
fun CatalogTab(viewModel: CoffeeViewModel) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val category by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val roastLevel by viewModel.selectedRoast.collectAsStateWithLifecycle()
    val origin by viewModel.selectedOrigin.collectAsStateWithLifecycle()
    val equipmentType by viewModel.selectedEquipmentType.collectAsStateWithLifecycle()
    val products by viewModel.filteredProducts.collectAsStateWithLifecycle()

    var selectedDetailProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var showPrismaSchemaDialog by remember { mutableStateOf(false) }

    if (showPrismaSchemaDialog) {
        PrismaSchemaDialog(onDismiss = { showPrismaSchemaDialog = false })
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search and Instant filters Panel
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Search field
                TextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("catalog_search_input"),
                    placeholder = { Text("جستجو میان دانه‌ها، آسیاب‌ها و وسایل تخصصی...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "جستجو") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Filled.Close, contentDescription = "پاک کردن")
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.background,
                        unfocusedContainerColor = MaterialTheme.colorScheme.background,
                        disabledContainerColor = MaterialTheme.colorScheme.background,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Basic categories (Beans vs Equipment)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryCapsule(
                        label = "همه محصولات",
                        isActive = category == "all",
                        onClick = { viewModel.setCategory("all") }
                    )
                    CategoryCapsule(
                        label = "دانه‌های قهوه ☕",
                        isActive = category == "beans",
                        onClick = { viewModel.setCategory("beans") }
                    )
                    CategoryCapsule(
                        label = "تجهیزات و ابزار خانگی ⚙️",
                        isActive = category == "equipment",
                        onClick = { viewModel.setCategory("equipment") }
                    )
                }

                // Smart Roast & Origin Filters (Only shown if looking at beans or all)
                if (category == "all" || category == "beans") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "تنظیمات باریستایی فیلتر قهوه:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // Roast Levels
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterPill(
                                label = "برشته‌کاری سبک (Light)",
                                isSelected = roastLevel == "لایت",
                                onClick = { viewModel.toggleRoastFilter("لایت") }
                            )
                        }
                        item {
                            FilterPill(
                                label = "برشته‌کاری متوسط (Medium)",
                                isSelected = roastLevel == "مدیوم",
                                onClick = { viewModel.toggleRoastFilter("مدیوم") }
                            )
                        }
                        item {
                            FilterPill(
                                label = "برشته‌کاری تیره (Dark)",
                                isSelected = roastLevel == "دارک",
                                onClick = { viewModel.toggleRoastFilter("دارک") }
                            )
                        }
                        item {
                            FilterPill(
                                label = "برشته‌کاری متوسط-دارک",
                                isSelected = roastLevel == "مدیوم-دارک",
                                onClick = { viewModel.toggleRoastFilter("مدیوم-دارک") }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Origins
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterPill(
                            label = "فرمول ترکیبی (Blend)",
                            isSelected = origin == "ترکیبی",
                            onClick = { viewModel.toggleOriginFilter("ترکیبی") }
                        )
                        FilterPill(
                            label = "تک‌خاستگاه (Single Origin)",
                            isSelected = origin == "تک خاستگاه",
                            onClick = { viewModel.toggleOriginFilter("تک خاستگاه") }
                        )
                    }
                }

                // Smart Equipment Filters (Only shown if looking at equipment or all)
                if (category == "all" || category == "equipment") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "تنظیمات ابزار و تجهیزات:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterPill(
                                label = "کتری برقی (Kettle)",
                                isSelected = equipmentType == "کتری",
                                onClick = { viewModel.toggleEquipmentTypeFilter("کتری") }
                            )
                        }
                        item {
                            FilterPill(
                                label = "دم‌آور تخصصی (Brewer)",
                                isSelected = equipmentType == "دم‌آور",
                                onClick = { viewModel.toggleEquipmentTypeFilter("دم‌آور") }
                            )
                        }
                        item {
                            FilterPill(
                                label = "آسیاب دستی (Grinder)",
                                isSelected = equipmentType == "آسیاب",
                                onClick = { viewModel.toggleEquipmentTypeFilter("آسیاب") }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "سینک دیتابیس همکار (روابط و اسکیما):",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.secondary,
                    )

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showPrismaSchemaDialog = true },
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = "اسکیما Prisma",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "اسکیما Prisma و روابط دیتابیس",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Product Catalog Grid
        if (products.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Inbox,
                        contentDescription = "بدون محصول",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "محصولی با این مشخصات یافت نشد!",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(160.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(products) { item ->
                    ProductCard(product = item, onClick = { selectedDetailProduct = item })
                }
            }
        }
    }

    // Product Detail Bottom Sheet Overlay
    selectedDetailProduct?.let { product ->
        ProductDetailDialog(
            product = product,
            onDismiss = { selectedDetailProduct = null },
            onAddToCart = { grind, qty ->
                viewModel.addToCart(product, grind, qty)
                selectedDetailProduct = null
            },
            onSubscribe = { freq, grind, qty ->
                viewModel.subscribeToCoffee(product, freq, grind, qty)
                selectedDetailProduct = null
            }
        )
    }
}

@Composable
fun CategoryCapsule(label: String, isActive: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() },
        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun FilterPill(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .border(
                1.dp,
                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                RoundedCornerShape(8.dp)
            ),
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(vertical = 5.dp, horizontal = 10.dp),
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ProductCard(product: ProductEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("product_card_${product.id}")
            .clickable { onClick() }
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(24.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            ) {
                // Product remote image
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = product.nameFa,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Specialty Badges
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Roast rating
                    if (product.roastLevelFa != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = product.roastLevelFa,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    // Score block
                    Surface(
                        color = Color.Black.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "امتیاز",
                                tint = MaterialTheme.colorScheme.tertiary, // Metallic Gold
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = product.rating.toString().toPersianDigits(),
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = product.nameFa,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Text(
                    text = product.roastingDateFa.toPersianDigits(),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = product.priceTomans.toPersianPrice(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "افزودن",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// --- Product Interactive Detail Dialog ---

@Composable
fun ProductDetailDialog(
    product: ProductEntity,
    onDismiss: () -> Unit,
    onAddToCart: (grind: String, qty: Int) -> Unit,
    onSubscribe: (freq: String, grind: String, qty: Int) -> Unit
) {
    var quantity by remember { mutableStateOf(1) }
    var selectedGrind by remember { mutableStateOf("") }
    
    val grindList = remember(product) {
        product.grindTypesFa.split(";")
    }

    if (selectedGrind.isEmpty() && grindList.isNotEmpty()) {
        selectedGrind = grindList.first()
    }

    var isSubMode by remember { mutableStateOf(false) }
    var selectedFrequency by remember { mutableStateOf("دو هفته یکبار") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .testTag("product_detail_dialog")
                .wrapContentHeight()
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = product.nameFa,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 18.sp
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "بستن")
                        }
                    }
                }

                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        AsyncImage(
                            model = product.imageUrl,
                            contentDescription = product.nameFa,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                item {
                    Text(
                        text = product.descriptionFa,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp
                    )
                }

                item {
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                }

                // Grind Level options Selection
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "انتخاب درجه آسیاب دانه:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            grindList.forEach { grind ->
                                val isChosen = selectedGrind == grind
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { selectedGrind = grind }
                                        .border(
                                            1.dp,
                                            if (isChosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                                            RoundedCornerShape(10.dp)
                                        ),
                                    color = if (isChosen) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent
                                ) {
                                    Text(
                                        text = grind,
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                                        fontSize = 11.sp,
                                        fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isChosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Purchase vs Subscription Mode Selector
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (!isSubMode) MaterialTheme.colorScheme.surface else Color.Transparent)
                                .clickable { isSubMode = false }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "خرید نقدی سبد خرید",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!isSubMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSubMode) MaterialTheme.colorScheme.surface else Color.Transparent)
                                .clickable { isSubMode = true }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.WorkspacePremium,
                                    contentDescription = "آروما کلاب",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp).padding(end = 2.dp)
                                )
                                Text(
                                    text = "عضویت کلوپ قهوه (اشتراکی)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSubMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Subscription customizable delivery schedule options
                if (isSubMode) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "برنامه زمان‌بندی ارسال خودکار (۱۵٪ تخفیف کلاب):",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                
                                val frequencies = listOf("هفتگی", "دو هفته یکبار", "ماهانه")
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    frequencies.forEach { freq ->
                                        val isChosen = selectedFrequency == freq
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { selectedFrequency = freq }
                                                .border(
                                                    1.dp,
                                                    if (isChosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                                    RoundedCornerShape(8.dp)
                                                ),
                                            color = if (isChosen) MaterialTheme.colorScheme.primary else Color.Transparent
                                        ) {
                                            Text(
                                                text = freq,
                                                modifier = Modifier.padding(vertical = 8.dp),
                                                fontSize = 11.sp,
                                                textAlign = TextAlign.Center,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isChosen) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Quantity and CTA Trigger Block
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Quantity selector
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { if (quantity > 1) quantity-- },
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "کم کردن", modifier = Modifier.size(16.dp))
                            }

                            Text(
                                text = quantity.toString().toPersianDigits(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(horizontal = 14.dp)
                            )

                            IconButton(
                                onClick = { quantity++ },
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "زیاد کردن", modifier = Modifier.size(16.dp))
                            }
                        }

                        // Final CTA Button
                        if (!isSubMode) {
                            Button(
                                onClick = { onAddToCart(selectedGrind, quantity) },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "افزودن به سبد: " + (product.priceTomans * quantity).toPersianPrice(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Button(
                                onClick = { onSubscribe(selectedFrequency, selectedGrind, quantity) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text(
                                    text = "برقراری اشتراک " + selectedFrequency,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- AI Taste Finder Sommelier Quiz (آزمون باریستای هوشمند) ---

@Composable
fun SommelierQuizTab(viewModel: CoffeeViewModel) {
    val context = LocalContext.current
    val sommelierState by viewModel.sommelierState.collectAsStateWithLifecycle()
    val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()

    var step by remember { mutableStateOf(1) } // 1: Taste, 2: Style, 3: Tool, 4: Caffeine, 5: Results

    var taste by remember { mutableStateOf("شکلاتی و شیرین (Sweet/Chocolatey)") }
    var milkStyle by remember { mutableStateOf("بدون شیر و خالص (Black Coffee)") }
    var brewTool by remember { mutableStateOf("دستگاه اسپرسو برقی (Espresso Machine)") }
    var caffeinePref by remember { mutableStateOf("کافئین معمولی (Regular)") }

    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (sommelierState is SommelierState.Loading) {
                // High-End Loading Animation: Pulsing Espresso cup
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.size(100.dp)) {
                        // Drawing an animated coffee extraction cup
                        val infiniteTransition = rememberInfiniteTransition()
                        val scale by infiniteTransition.animateFloat(
                            initialValue = 0.8f,
                            targetValue = 1.2f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            )
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape)
                        )
                        Icon(
                            imageVector = Icons.Default.HotTub,
                            contentDescription = "عصاره‌گیری",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(64.dp)
                                .align(Alignment.Center)
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "باریستای هوش مصنوعی آروما در حال بررسی ذائقه شما...",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "فرمول‌بندی نت‌های عطر، اسیدیته دانه و درجه برشته‌کاری متناسب با آسیاب...",
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (sommelierState is SommelierState.Success) {
                val recommendation = (sommelierState as SommelierState.Success).recommendation
                
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💡 نتیجه طعم‌سنجی باریستا",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { viewModel.resetSommelierQuiz(); step = 1 }) {
                            Icon(Icons.Default.Refresh, contentDescription = "آزمون مجدد")
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    // Card of recomendation
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                item {
                                    Text(
                                        text = recommendation,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontSize = 13.sp,
                                        lineHeight = 24.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Suggest buying the matched beans
                val matchedBean = remember(recommendation, allProducts) {
                    allProducts.find { product ->
                        recommendation.contains(product.nameEn.take(4)) || 
                        recommendation.contains(product.nameFa.take(4)) ||
                        (recommendation.contains("اتیوپی") && product.id == 1) ||
                        (recommendation.contains("کلمبیا") && product.id == 2) ||
                        (recommendation.contains("میکس") && product.id == 3) ||
                        (recommendation.contains("کنیا") && product.id == 4)
                    } ?: allProducts.firstOrNull()
                }

                matchedBean?.let { bean ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "خرید آسان فوری قهوه متناظر:",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = bean.nameFa,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Button(
                                onClick = {
                                    viewModel.addToCart(bean, bean.grindTypesFa.split(";").first(), 1)
                                    viewModel.resetSommelierQuiz()
                                    step = 1
                                    Toast.makeText(context, "قهوه به سبد شما افزوده شد!", Toast.LENGTH_LONG).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Text("خرید: " + bean.priceTomans.toPersianPrice(), fontSize = 11.sp)
                            }
                        }
                    }
                }
            } else {
                // Quiz steps layout
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        // Steps indicators
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "مرحله $step از ۴ آزمون باریستا".toPersianDigits(),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "کد کلید هوش مصنوعی فعال".toPersianDigits(),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }

                        LinearProgressIndicator(
                            progress = step / 4f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Render Step Content
                        when (step) {
                            1 -> QuizStepLayout(
                                title = "شما معمولاً چه نت‌های طعمی را ترجیح می‌دهید؟",
                                choices = listOf(
                                    "میوه‌ای، اسیدیته شاداب و گلی (مانند چای و مرکبات)",
                                    "شکلاتی، غنی، کاراملی و کارامل شده (ملایم و شیرین)",
                                    "تلخ، کاکائویی سنگین، ادویه‌ای و پرکرم (قوی و غلیظ)"
                                ),
                                selectedChoice = taste,
                                onSelect = { taste = it }
                            )

                            2 -> QuizStepLayout(
                                title = "آیا تمایل دارید قهوه خویش را با شیر ترکیب کنید یا خالص؟",
                                choices = listOf(
                                    "بدون شیر و خالص (قهوه سیاه فیلتری / شات اسپرسو خالص)",
                                    "همراه با افزودن شیر، خامه شکر (لاته، کاپوچینو سنگین)"
                                ),
                                selectedChoice = milkStyle,
                                onSelect = { milkStyle = it }
                            )

                            3 -> QuizStepLayout(
                                title = "محبوب‌ترین ابزار دم‌آوری قهوه در خانه شما چیست؟",
                                choices = listOf(
                                    "دستگاه اسپرسوساز برقی خانگی",
                                    "موکاپات روگازی ساده",
                                    "قهوه‌سازهای فیلتری دمی (V60، فرنچ چمکس، کالیتا)",
                                    "فرنچ پرس یا قهوه‌ساز فوری قطره‌ای"
                                ),
                                selectedChoice = brewTool,
                                onSelect = { brewTool = it }
                            )

                            4 -> QuizStepLayout(
                                title = "رابطه میانه شما با کافئین دانه قهوه چگونه است؟",
                                choices = listOf(
                                    "کافئین بسیار بالا لازم دارم (مخصوص کار شدید و انرژی صبحگاهی)",
                                    "میزان کافئین متعادل و لطیف (عربیکا ۱۰۰٪ خاستگاهی)",
                                    "بدون کافئین ترجیح می‌دهم (برای مهار استرس / نوشیدن عصرگاهی)"
                                ),
                                selectedChoice = caffeinePref,
                                onSelect = { caffeinePref = it }
                            )
                        }
                    }

                    // Step Navigation buttons
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (step > 1) {
                            OutlinedButton(
                                onClick = { step-- },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("بازگشت قبلی", fontSize = 12.sp)
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        Button(
                            onClick = {
                                if (step < 4) {
                                    step++
                                } else {
                                    // Submit to API or Local Sommelier
                                    viewModel.submitSommelierQuiz(
                                        tasteProfile = taste,
                                        consumeWithMilk = milkStyle,
                                        brewingMethod = brewTool,
                                        caffeineLevel = caffeinePref
                                    )
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (step < 4) "ادامه آزمون ➡️" else "باریستای هوش مصنوعی را خبر کن ☕",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuizStepLayout(
    title: String,
    choices: List<String>,
    selectedChoice: String,
    onSelect: (String) -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 15.sp,
            lineHeight = 24.sp
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        choices.forEach { choice ->
            val isSelected = selectedChoice == choice
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSelect(choice) }
                    .border(
                        1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
                        RoundedCornerShape(14.dp)
                    ),
                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { onSelect(choice) },
                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = choice.toPersianDigits(),
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

// --- Cart & Payment Gateway Integration Screen (سبد خرید) ---

@Composable
fun CartTab(viewModel: CoffeeViewModel, onTabRequested: (String) -> Unit) {
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val checkoutState by viewModel.checkoutState.collectAsStateWithLifecycle()
    var isGatewayOpen by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        if (cartItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ProductionQuantityLimits,
                        contentDescription = "سبد خالی",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "سبد خرید فوق‌العاده شما خالی است!",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "دانه‌های کلمبیا و لوازم تخصصی را به سبد خود اضافه کنید.",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { onTabRequested("catalog") },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("مشاهده محصولات ویترین ☕", fontSize = 12.sp)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(cartItems) { item ->
                    CartItemRow(item = item, viewModel = viewModel)
                }

                // Summary block
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("خلاصه فاکتور خرید شتاب:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            
                            val subtotal = cartItems.sumOf { it.priceTomans * it.quantity }
                            val shipping = 40000 // Fixed shipping in Tomans
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("جمع اقلام خرید قهوه:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(subtotal.toPersianPrice(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("پست سفارشی (تازه برشت بیمه‌شده):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(shipping.toPersianPrice(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("جمع نهایی پرداختی (تومان):", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text((subtotal + shipping).toPersianPrice(), fontSize = 14.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    // Complete Checkout Button
                    Button(
                        onClick = { isGatewayOpen = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("checkout_button"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("تکمیل خرید و پرداخت به درگاه بانک شتاب", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Modern Secure Payment Gateway (Representing local gateway/digital wallets)
    if (isGatewayOpen) {
        val totalToPay = cartItems.sumOf { it.priceTomans * it.quantity } + 40000
        PaymentGatewayDialog(
            amountTomans = totalToPay,
            onDismiss = { isGatewayOpen = false },
            onPaymentSubmit = {
                viewModel.executeCheckout()
                isGatewayOpen = false
                onTabRequested("orders") // Redirect to logistics instantly
            }
        )
    }
}

@Composable
fun CartItemRow(item: CartItemEntity, viewModel: CoffeeViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(10.dp))
            ) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.nameFa,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.nameFa,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "نوع آسیاب: " + item.grindTypeFa,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.priceTomans.toPersianPrice(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Quantity increments
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { viewModel.updateCartQuantity(item.id, item.quantity - 1) },
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "کم کردن", modifier = Modifier.size(12.dp))
                }

                Text(
                    text = item.quantity.toString().toPersianDigits(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                IconButton(
                    onClick = { viewModel.updateCartQuantity(item.id, item.quantity + 1) },
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                ) {
                    Icon(Icons.Default.Add, contentDescription = "زیاد کردن", modifier = Modifier.size(12.dp))
                }
            }
        }
    }
}

// --- Digital Wallet / Shaparak Card payment Simulation Dialog ---

@Composable
fun PaymentGatewayDialog(
    amountTomans: Int,
    onDismiss: () -> Unit,
    onPaymentSubmit: () -> Unit
) {
    var cardNumber by remember { mutableStateOf("") }
    var cvv2 by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var showSuccessToast by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .wrapContentHeight()
                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "💳 درگاه پرداخت امن شاپرک بانک",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 16.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "انصراف")
                    }
                }

                Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))

                // Price display banner
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("مبلغ قابل پرداخت:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = amountTomans.toPersianPrice(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // inputs fields
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("شماره کارت شتاب شمر:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = cardNumber,
                        onValueChange = { if (it.length <= 16) cardNumber = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("۶۰۳۷-۹۹۱۹-XXXX-XXXX", fontSize = 12.sp, textAlign = TextAlign.Left) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("کد CVV2:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = cvv2,
                                onValueChange = { cvv2 = it },
                                placeholder = { Text("XXXX", fontSize = 12.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("رمز اینترنتی (پویا):", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = pass,
                                onValueChange = { pass = it },
                                placeholder = { Text("رمز دوم", fontSize = 12.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onPaymentSubmit,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("پرداخت ایمن و نهایی‌سازی فاکتور", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = "🔒 اتصال رمزگذاری شده ۲۵۶ بیتی بانکی هماهنگ با کارت به کارت شیک آروما.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 9.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// --- Subscription Dashboard UI (کلوپ قهوه اشتراک) ---

@Composable
fun NextDeliveryProgressTimeline(subscriptionActive: Boolean) {
    val steps = listOf("تایید کلوپ", "برشته‌کاری", "بسته‌بندی نیتروژن", "ارسال سریع")
    val currentStep = if (subscriptionActive) 2 else -1

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "مرحله سفارش نوبت جاری:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
            if (!subscriptionActive) {
                Text(
                    text = "تعلیق موقت",
                    fontSize = 10.sp,
                    color = Color(0xFFC62828),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            steps.forEachIndexed { index, name ->
                val isActive = index <= currentStep && subscriptionActive
                val isCurrent = index == currentStep && subscriptionActive
                
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                if (isCurrent) MaterialTheme.colorScheme.primary
                                else if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isActive && !isCurrent) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                        } else {
                            Text(
                                text = (index + 1).toString().toPersianDigits(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = name,
                        fontSize = 9.sp,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }

                if (index < steps.size - 1) {
                    Box(
                        modifier = Modifier
                            .height(2.dp)
                            .weight(0.4f)
                            .background(
                                if (index < currentStep && subscriptionActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun UpcomingDeliveriesTimeline(subscription: SubscriptionEntity) {
    if (subscription.statusFa != "فعال") return
    
    val futureDates = com.example.data.JalaliCalendarHelper.getFutureDeliveries(
        subscription.nextDeliveryJalali,
        subscription.frequencyFa,
        count = 3
    )

    if (futureDates.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "تقویم تحویل‌های بعدی کلوپ (شامسی):",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    futureDates.forEachIndexed { i, dateString ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }

                            Text(
                                text = "نوبت ${i + 2}:".toPersianDigits(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.width(42.dp)
                            )

                            Text(
                                text = dateString.toPersianDigits(),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )

                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.07f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "ارسال رایگان کلاب",
                                    fontSize = 8.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun parseJalaliToFriendlyFullTextStr(dateString: String): String {
    return try {
        val parts = dateString.split("/")
        if (parts.size == 3) {
            val year = parts[0].toInt()
            val month = parts[1].toInt()
            val day = parts[2].toInt()
            val monthName = when (month) {
                1 -> "فروردین"
                2 -> "اردیبهشت"
                3 -> "خرداد"
                4 -> "تیر"
                5 -> "مرداد"
                6 -> "شهریور"
                7 -> "مهر"
                8 -> "آبان"
                9 -> "آذر"
                10 -> "دی"
                11 -> "بهمن"
                12 -> "اسفند"
                else -> ""
            }
            // Parse day of week
            val cal = com.example.data.JalaliCalendarHelper.jalaliToGregorian(year, month, day)
            val weekday = com.example.data.JalaliCalendarHelper.getPersianWeekdayNameForCal(cal)
            "$weekday، $day $monthName $year"
        } else {
            dateString
        }
    } catch (e: Exception) {
        dateString
    }
}

@Composable
fun SubscriptionClubTab(viewModel: CoffeeViewModel, onCatalogRequest: () -> Unit) {
    val subscriptions by viewModel.subscriptions.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Club Intro promo card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f)
            ),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = "کلاب لوکس",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "پیشخوان اشتراک طلایی قهوه آروما",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 16.sp
                    )
                }
                Text(
                    text = "ارائه منظم و دوره‌ای دانه‌های قهوه تازه‌برشت تخصصی مستقیماً درب منزل شما به همراه ۱۵٪ تخفیف انحصاری و ارسال رایگان کلاب.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                    lineHeight = 18.sp
                )
            }
        }

        // Statistics Overview Panel
        if (subscriptions.isNotEmpty()) {
            val activeCount = subscriptions.count { it.statusFa == "فعال" }
            val totalSaved = subscriptions.sumOf { (it.priceTomans * 0.15 * it.quantity).toInt() }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Stat 1: Active Subs
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = Icons.Default.Autorenew,
                                contentDescription = "فعال",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text("اشتراک فعال", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                        Text(
                            text = "$activeCount مورد".toPersianDigits(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Stat 2: Total Saved Budget
                Card(
                    modifier = Modifier.weight(1.2f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = Icons.Default.Savings,
                                contentDescription = "تخفیف",
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(16.dp)
                            )
                            Text("خدمات کلاب (صرفه‌جویی)", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                        Text(
                            text = "${totalSaved.toPersianPrice()}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }
        }

        Text(
            text = "برنامه‌های اشتراک با فرکانس تحویل شما:",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (subscriptions.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "بدون اشتراک",
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)
                    )
                    Text(
                        text = "هنوز اشتراک قهوه‌ای برقرار نکرده‌اید!",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "از گالری محصولات دانه‌ای موردعلاقه‌تان را انتخاب و حالت تحویل منظم را فعال کنید.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = onCatalogRequest,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Text("طعم یابی و اشتراک از ویترین قهوه", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(subscriptions) { sub ->
                    SubscriptionCard(subscription = sub, viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun SubscriptionCard(subscription: SubscriptionEntity, viewModel: CoffeeViewModel) {
    val context = LocalContext.current
    val subActive = subscription.statusFa == "فعال"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // First Row: Product Image + Title + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    AsyncImage(
                        model = subscription.imageUrl,
                        contentDescription = subscription.productNameFa,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subscription.productNameFa,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "کلوپ دوره‌ای: " + subscription.frequencyFa,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                        Text(
                            text = "قیمت: " + subscription.priceTomans.toPersianPrice(),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Active Badge
                Surface(
                    color = if (subActive) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = subscription.statusFa,
                        color = if (subActive) Color(0xFF2E7D32) else Color(0xFFC62828),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))

            // Progress Timeline Steps (only shown for tracking delivery phases visually)
            NextDeliveryProgressTimeline(subscriptionActive = subActive)

            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))

            // Subscriptions Detailed Dynamic Customizers Box (Interactive Dashboard settings)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "تنظیمات سفارش خودکار قهوه:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // 1. Quantity Picker (Stepper)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "تعداد بسته‌های درخواستی:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (subscription.quantity > 1) {
                                    val newQty = subscription.quantity - 1
                                    viewModel.updateSubscriptionQuantity(subscription.id, newQty)
                                    Toast.makeText(context, "تعداد دانه‌ها کاهش یافت", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .size(30.dp)
                                .background(MaterialTheme.colorScheme.surface, CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "کاهش تعداد",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        Text(
                            text = subscription.quantity.toString().toPersianDigits() + " بسته",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        IconButton(
                            onClick = {
                                val newQty = subscription.quantity + 1
                                viewModel.updateSubscriptionQuantity(subscription.id, newQty)
                                Toast.makeText(context, "تعداد دانه‌ها افزایش یافت", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .size(30.dp)
                                .background(MaterialTheme.colorScheme.surface, CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "افزایش تعداد",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                // 2. Grind Custom Choice Selection
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "نوع درجه آسیاب باریستا:",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val grinds = listOf("دانه کامل", "اسپرسو", "دم‌آور تخصصی", "فرانسه")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(grinds) { g ->
                            val isSelected = subscription.grindTypeFa == g
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        viewModel.updateSubscriptionParams(subscription.id, g, subscription.frequencyFa)
                                        Toast.makeText(context, "درجه آسیاب به $g تغییر یافت", Toast.LENGTH_SHORT).show()
                                    },
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            ) {
                                Text(
                                    text = g,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // 3. Delivery Frequency Adjustment
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "بازه ارسال هوشمند کلاب:",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val freqList = listOf("هفتگی", "دو هفته یکبار", "ماهانه")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(freqList) { f ->
                            val isSelected = subscription.frequencyFa == f
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        viewModel.updateSubscriptionParams(subscription.id, subscription.grindTypeFa, f)
                                        Toast.makeText(context, "بازه ارسال به $f تغییر یافت", Toast.LENGTH_SHORT).show()
                                    },
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            ) {
                                Text(
                                    text = f,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // 4. Postponent delay / acceleration buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "برنامه‌ریزی نوبت بعدی:",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Accelerate / Reschedule to 3 days
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.postponeSubscription(subscription.id, 3)
                                    Toast.makeText(context, "نوبت به ۳ روز آینده جلو انداخته شد", Toast.LENGTH_SHORT).show()
                                },
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        ) {
                            Text(
                                text = "تعجیل (۳ روز)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }

                        // Delay by 7 days
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.postponeSubscription(subscription.id, 14) // Adds 14 days from now as postpone
                                    Toast.makeText(context, "تحویل نوبت بعدی به مدت ۷ روز تمدید گردید", Toast.LENGTH_SHORT).show()
                                },
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                        ) {
                            Text(
                                text = "تمدید (+۷ روز)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))

            // Historical & Future Calendars Dates Tracking (Displays Upcoming Delivery sequences mapped to Jalali calendar)
            UpcomingDeliveriesTimeline(subscription = subscription)

            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))

            // Action row: Pause/Resume + Cancel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "تحویل بعدی (تقویم خورشیدی):",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = parseJalaliToFriendlyFullTextStr(subscription.nextDeliveryJalali).toPersianDigits(),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pause/resume action
                    OutlinedButton(
                        onClick = { viewModel.toggleSubscriptionActive(subscription.id) },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (subscription.statusFa == "فعال") "تعلیق موقت" else "فعال‌سازی مجدد",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Cancel
                    IconButton(
                        onClick = {
                            viewModel.cancelSubscription(subscription.id)
                            Toast.makeText(context, "اشتراک لغو گردید", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color(0xFFFFEBEE), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف اشتراک",
                            tint = Color(0xFFC62828),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// --- Logistics & Live Order Tracking Screen (پیگیری لجستیک و فاکتور) ---

@Composable
fun LogisticsOrdersTab(viewModel: CoffeeViewModel) {
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val checkoutState by viewModel.checkoutState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "کارگاه برشته‌کاری و لجستیک آروما",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "امروز: ۱۴۰۵/۰۳/۱۴".toPersianDigits(),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        // Live processing toast simulator if checking out
        if (checkoutState is CheckoutState.Processing) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "در حال پردازش تراکنش بانک مرکزی شتاب...",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (orders.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = "سفارش خالی",
                        modifier = Modifier.size(54.dp),
                        tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "تاریخچه سفارشات شما خالی است!",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(orders) { order ->
                    OrderTrackingCard(order = order)
                }
            }
        }
    }
}

@Composable
fun OrderTrackingCard(order: OrderEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Title order row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "سفارش " + order.orderNumber.toPersianDigits(),
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "تاریخ ثبت: " + order.orderDateFa.toPersianDigits(),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = order.totalAmountTomans.toPersianPrice(),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

            // Items summary
            Text(
                text = order.itemsSummaryFa.toPersianDigits(),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )

            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

            // Step timeline logistics indicators
            Text(
                text = "مرحله فیزیکی پردازش سفارش:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            val stages = listOf(
                "دم‌آوری" to "Brewing",
                "برشته‌شدن" to "Roasted",
                "ارسال شتاب" to "Delivering",
                "تحویل خانه" to "Delivered"
            )

            val activeIndex = when (order.statusEn) {
                "Brewing" -> 0
                "Roasted" -> 1
                "Delivering" -> 2
                "Delivered" -> 3
                else -> 0
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                stages.forEachIndexed { idx, stage ->
                    val isCompleted = idx <= activeIndex
                    val isCurrent = idx == activeIndex
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isCompleted) MaterialTheme.colorScheme.primary 
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCompleted && !isCurrent) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "کامل",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                            } else if (isCurrent) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = stage.first,
                            fontSize = 10.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileDialog(
    viewModel: CoffeeViewModel,
    onDismiss: () -> Unit
) {
    val userProfileState by viewModel.userProfile.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Local input states initialized with current database preferences or defaults
    val activeProfile = userProfileState
    val currentName = activeProfile?.fullName ?: "رضا شمس"
    val currentEmail = activeProfile?.email ?: "rezash.sh@gmail.com"
    val currentPhone = activeProfile?.phoneNumber ?: "۰۹۱۲۳۴۵۶۷۸۹"
    val currentAddress = activeProfile?.deliveryAddress ?: "تهران، خیابان ولیعصر، برج آروما، طبقه ۵"
    val currentCurrency = activeProfile?.preferredCurrency ?: "toman"
    val isLoggedIn = activeProfile?.isLoggedIn == true

    var nameInput by remember(activeProfile) { mutableStateOf(currentName) }
    var emailInput by remember(activeProfile) { mutableStateOf(currentEmail) }
    var phoneInput by remember(activeProfile) { mutableStateOf(currentPhone) }
    var addressInput by remember(activeProfile) { mutableStateOf(currentAddress) }
    var selectedCurrency by remember(activeProfile) { mutableStateOf(currentCurrency) }

    // Simulated NextAuth manual OTP state
    var verificationCodeInput by remember { mutableStateOf("") }
    var isSendingOtp by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(28.dp))
                .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f), RoundedCornerShape(28.dp)),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Symmetrical elegant dialog toolbar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "تنظیمات کاربری",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "تنظیمات پروفایل طلایی",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "مستندسازی و سینک محلی NextAuth",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 9.sp
                            )
                        }
                    }

                    // Elegant close badge
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "بستن",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Divider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    // Item 1: NextAuth / Auth.js Token Status indicator
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "وضعیت توکن احراز هویت (Auth.js Session)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    
                                    // Custom active state pill
                                    Surface(
                                        color = if (isLoggedIn) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = if (isLoggedIn) "احراز هویت شده (فعال)" else "مهمان غیرفعال",
                                            color = if (isLoggedIn) Color(0xFF2E7D32) else Color(0xFFC62828),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(6.dp))
                                
                                Text(
                                    text = if (isLoggedIn) {
                                        "شناسه سشن وب همگام‌سازی شده با آدرس پست الکترونیک: ​​$currentEmail"
                                    } else {
                                        "لطفاً جهت اتصال دائم به سیستم توزیع کشوری رستری طلائی آروما سینک نمایید."
                                    },
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Item 2: Address Preferences Form (with delivery address configuration requested)
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "آدرس تحویل اشتراک و سفارشات شتاب",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            
                            OutlinedTextField(
                                value = addressInput,
                                onValueChange = { addressInput = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("profile_address_input"),
                                placeholder = { Text("مثال: تهران، ونک، ملاصدرا، کوچه آروما، پلاک ۱۰", fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Home,
                                        contentDescription = "آدرس",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)
                                )
                            )
                        }
                    }

                    // Item 3: Preferred Currency Selector Form (Tomans vs Rials requested)
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "واحد پول دلخواه نمایش محصولات و سبد سفارش",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Toman Tab
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { selectedCurrency = "toman" },
                                    color = if (selectedCurrency == "toman") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (selectedCurrency == "toman") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                                    )
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "تومان",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (selectedCurrency == "toman") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                // Rial Tab
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { selectedCurrency = "rial" },
                                    color = if (selectedCurrency == "rial") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (selectedCurrency == "rial") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                                    )
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "ریال",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (selectedCurrency == "rial") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            
                            Text(
                                text = if (selectedCurrency == "rial") {
                                    "💡 واحد رسمی ریال انتخاب شد. تمام نرخ‌ها در کل سیستم موقتاً در مقیاس ×۱۰ ارائه خواهند شد."
                                } else {
                                    "💡 واحد تومان انتخاب شد. محاسبه فاکتورها بر اساس عرف رایج برشته‌کاری‌های کشور ثبت می‌شود."
                                },
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Item 4: Authentication Fields / Auth.js simulator
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "مدیریت حساب و احراز هویت",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                if (isLoggedIn) {
                                    // User details
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = (nameInput.firstOrNull() ?: "?").toString(),
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = nameInput,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = emailInput,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.logout()
                                            Toast.makeText(context, "با موفقیت از حساب خارج شدید (محدوده به کلاینت محلی)", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Logout,
                                            contentDescription = "خروج",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "خروج شتاب / باطل‌سازی سشن NextAuth",
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else {
                                    // Simulated sign in
                                    OutlinedTextField(
                                        value = nameInput,
                                        onValueChange = { nameInput = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("نام کامل", fontSize = 11.sp) },
                                        shape = RoundedCornerShape(10.dp),
                                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                                    )

                                    OutlinedTextField(
                                        value = emailInput,
                                        onValueChange = { emailInput = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("ایمیل (شناسه احراز هویت)", fontSize = 11.sp) },
                                        shape = RoundedCornerShape(10.dp),
                                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                                    )

                                    OutlinedTextField(
                                        value = phoneInput,
                                        onValueChange = { phoneInput = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("شماره تماس", fontSize = 11.sp) },
                                        shape = RoundedCornerShape(10.dp),
                                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                                    )

                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "کد ورود شتاب (پیامک تایید):",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = verificationCodeInput,
                                                onValueChange = { verificationCodeInput = it },
                                                modifier = Modifier.weight(1f),
                                                placeholder = { Text("کد آزمایشی: ۱۲۳۴", fontSize = 11.sp) },
                                                shape = RoundedCornerShape(10.dp),
                                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                                            )
                                            
                                            Button(
                                                onClick = {
                                                    if (emailInput.isEmpty() || !emailInput.contains("@")) {
                                                        Toast.makeText(context, "فرمت ایمیل نامعتبر است", Toast.LENGTH_SHORT).show()
                                                        return@Button
                                                    }
                                                    Toast.makeText(context, "کد تایید پیامکی ارسال شد (۱۲۳۴)", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 14.dp)
                                            ) {
                                                Text("ارسال کد", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            if (verificationCodeInput == "1234" || verificationCodeInput == "۱۲۳۴") {
                                                viewModel.simulateLogin(nameInput, emailInput, phoneInput)
                                                Toast.makeText(context, "با موفقیت احراز هویت شدید (سشن NextAuth با موفقیت ست شد!)", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "کد وارد شده صحیح نیست. لطفا از ۱۲۳۴ استفاده کنید.", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("تایید کد و ساخت توکن امن", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Item 5: Code Documentation regarding NextAuth Integration
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.03f)),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "دیباگ",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "مستندات و چیدمان فنی NextAuth Schema",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Text(
                                    text = "این فرم کلاینت مستقیماً با متد‌های ورود کلاینتی Auth.js تطابق دارد. الگو یا اسکیما طراحی شده برای دیتابیس همکار یا سرور این پروژه بدین صورت است:",
                                    fontSize = 10.sp,
                                    lineHeight = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Surface(
                                    color = Color.Black.copy(alpha = 0.05f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = """
                                                export const authOptions = {
                                                  providers: [
                                                    CredentialsProvider({
                                                      name: "Aroma Client OTP",
                                                      credentials: {
                                                        email: { label: "Email", type: "text" },
                                                        code: { label: "OTP Code", type: "text" }
                                                      },
                                                      async authorize(credentials) {
                                                        // Verify flow and return customized user instance
                                                        return {
                                                          id: "user-1",
                                                          name: "${nameInput}",
                                                          email: "${emailInput}",
                                                          address: "${addressInput}",
                                                          preferredCurrency: "${selectedCurrency}"
                                                        }
                                                      }
                                                    })
                                                  ]
                                                }
                                            """.trimIndent(),
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            lineHeight = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Dialog footer actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.updateDeliveryAddress(addressInput)
                            viewModel.updatePreferredCurrency(selectedCurrency)
                            Toast.makeText(context, "تمام ترجیحات تحویل و نوع پرداخت با موفقیت ذخیره شدند", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "ذخیره",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ذخیره ترجیحات", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("بازگشت", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrismaSchemaDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(28.dp))
                .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f), RoundedCornerShape(28.dp)),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Symmetrical toolbar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = "کد نویسی دیتابیس",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "اسکیما و روابط Prisma ⚡",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Products, Categories and Advanced Filters",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 9.sp
                            )
                        }
                    }

                    // Symmetrical close badge
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "بستن",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Divider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    // Feature Description Card
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "توضیحات طراحی وب‌سایت همکار و وب‌سرویس‌ها:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "جهت هماهنگی ۱۰۰٪ با لایه ذخیره‌سازی محلی (Room DB)، مدل طراحی شده ما در سرور وب براساس Prisma ORM تعریف شده است. این اسکیما حاوی مدل‌های ارتباطی کامل می‌باشد که فیلترهای درجه برشته‌کاری (Roast Level)، تک‌خاستگاه/ترکیبی (Origin) و نوع ابزار تخصصی (Equipment Type) را بر روی فیلدهای مجزا ایندکس می‌کند تا سرعت جستجو در فروشگاه فوق‌العاده سریع باشد.",
                                    fontSize = 11.sp,
                                    lineHeight = 17.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Prisma Schema Code Block
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "فایل اسکیما prisma.schema",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            Surface(
                                color = Color.Black.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = """
                                            // schema.prisma
                                            
                                            datasource db {
                                              provider = "postgresql"
                                              url      = env("DATABASE_URL")
                                            }

                                            generator client {
                                              provider = "prisma-client-js"
                                            }

                                            // دسته بندی محصولات (مثلا دانه‌ها، وسایل دم‌آوری)
                                            model Category {
                                              id          String    @id @default(uuid())
                                              nameEn      String    @unique
                                              nameFa      String    @unique
                                              slug        String    @unique
                                              products    Product[]
                                              createdAt   DateTime  @default(now())
                                              updatedAt   DateTime  @updatedAt
                                            }

                                            // محصولات برشته‌کاری طلایی و لوازم تخصصی
                                            model Product {
                                              id                String         @id @default(uuid())
                                              nameEn            String
                                              nameFa            String
                                              priceTomans       Int
                                              rating            Float          @default(0.0)
                                              imageUrl          String
                                              stock             Int            @default(0)
                                              descriptionFa     String
                                              descriptionEn     String
                                              roastingDateFa    String?
                                              
                                              // روابط با دسته بندی اصلی
                                              categoryId        String
                                              category          Category       @relation(fields: [categoryId], references: [id])
                                              
                                              // فیلترهای تخصصی باریستایی قهوه
                                              roastLevelEn      RoastLevel?
                                              roastLevelFa      String?       // "لایت", "مدیوم", "دارک", ...
                                              
                                              originEn          OriginType?
                                              originFa          String?       // "تک خاستگاه", "ترکیبی", ...
                                              
                                              equipmentTypeEn   EquipmentType?
                                              equipmentTypeFa   String?       // "کتری", "دم‌آور", "آسیاب", ...
                                              
                                              grindTypesFa      String        // جدا شده با نقطه ویرگول (;)
                                              
                                              createdAt         DateTime       @default(now())
                                              updatedAt         DateTime       @updatedAt

                                              // ساخت ایندکس‌های تخصصی برای فیلترینگ با سرعت بالا در بانک داده پستگرس
                                              @@index([categoryId])
                                              @@index([roastLevelEn])
                                              @@index([originEn])
                                              @@index([equipmentTypeEn])
                                            }

                                            enum RoastLevel {
                                              LIGHT
                                              LIGHT_MEDIUM
                                              MEDIUM
                                              MEDIUM_DARK
                                              DARK
                                            }

                                            enum OriginType {
                                              SINGLE_ORIGIN
                                              BLEND
                                            }

                                            enum EquipmentType {
                                              KETTLE
                                              BREWER
                                              GRINDER
                                              FILTER
                                            }
                                        """.trimIndent(),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    // REST / GraphQL fetching layer emulation documentation
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "پیاده سازی متد واکشی داده در وب (Data-Fetching Layer):",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            Surface(
                                color = Color.Black.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = """
                                            // api/products/route.ts (Next.js / Node.js)
                                            import { prisma } from "@/lib/prisma";
                                            import { NextResponse } from "next/server";

                                            export async function GET(request: Request) {
                                              const { searchParams } = new URL(request.url);
                                              
                                              const category      = searchParams.get("category");
                                              const roastLevel    = searchParams.get("roastLevel");
                                              const origin        = searchParams.get("origin");
                                              const equipmentType = searchParams.get("equipmentType");
                                              const query         = searchParams.get("q");

                                              const filters: any = {};

                                              if (category && category !== "all") {
                                                filters.category = { slug: category };
                                              }
                                              if (roastLevel) {
                                                filters.roastLevelFa = roastLevel;
                                              }
                                              if (origin) {
                                                filters.originFa = origin;
                                              }
                                              if (equipmentType) {
                                                filters.equipmentTypeFa = equipmentType;
                                              }
                                              if (query) {
                                                filters.OR = [
                                                  { nameFa: { contains: query, mode: "insensitive" } },
                                                  { nameEn: { contains: query, mode: "insensitive" } },
                                                  { descriptionFa: { contains: query, mode: "insensitive" } }
                                                ];
                                              }

                                              // واکشی داده با روابط و ایندکس پرسرعت
                                              const products = await prisma.product.findMany({
                                                where: filters,
                                                include: { category: true },
                                                orderBy: { rating: "desc" }
                                              });

                                              return NextResponse.json(products);
                                            }
                                        """.trimIndent(),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = Color(0xFF00796B), // Clean ocean dark teal
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Footer Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Text("متوجه شدم / بازگشت به ویترین", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

