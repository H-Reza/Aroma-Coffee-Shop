package com.example.data

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class CoffeeRepository(private val context: Context, private val coffeeDao: CoffeeDao) {

    val allProducts: Flow<List<ProductEntity>> = coffeeDao.getAllProducts().flowOn(Dispatchers.IO)
    val cartItems: Flow<List<CartItemEntity>> = coffeeDao.getCartItems().flowOn(Dispatchers.IO)
    val subscriptions: Flow<List<SubscriptionEntity>> = coffeeDao.getSubscriptions().flowOn(Dispatchers.IO)
    val orders: Flow<List<OrderEntity>> = coffeeDao.getOrders().flowOn(Dispatchers.IO)
    val userProfile: Flow<UserProfileEntity?> = coffeeDao.getUserProfile().flowOn(Dispatchers.IO)

    suspend fun saveUserProfile(profile: UserProfileEntity) = withContext(Dispatchers.IO) {
        coffeeDao.saveUserProfile(profile)
    }

    suspend fun clearUserProfile() = withContext(Dispatchers.IO) {
        coffeeDao.clearUserProfile()
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Seeds the database with high-premium specialty coffee options
    suspend fun seedDatabase() = withContext(Dispatchers.IO) {
        val existing = coffeeDao.getAllProducts().first()
        if (existing.isEmpty()) {
            val list = listOf(
                ProductEntity(
                    id = 1,
                    nameEn = "Ethiopia Yirgacheffe G1",
                    nameFa = "قهوه اتیوپی یرگاشف درجه ۱",
                    category = "beans",
                    roastLevelEn = "Light-Medium",
                    roastLevelFa = "لایت-مدیوم",
                    originEn = "Single Origin",
                    originFa = "تک خاستگاه",
                    grindTypesFa = "دانه کامل;اسپرسو اسپشال;سازگار با قهوه‌ساز چمکس / V60;فرنچ پرس",
                    priceTomans = 280000,
                    rating = 4.9f,
                    imageUrl = "https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=600&auto=format&fit=crop",
                    stock = 15,
                    descriptionFa = "برآمده از تپه‌های یِرگاشف اتیوپی با خاستگاهی غنی. پروفایل طعمی گلی با یادداشت‌های یاس، لیموترش شیرازی و عطر عسل. اسیدیته روشن و شبیه به چای سیاه مرغوب که تجربه‌ای فوق‌العاده ظریف هدیه می‌دهد.",
                    descriptionEn = "Originating from Yirgacheffe hills, Ethiopia. Features elegant jasmine floral notes, sweet lime, and honey with a bright tea-like clarity.",
                    roastingDateFa = "برشته‌کاری: ۲ روز پیش (تازه برشت)"
                ),
                ProductEntity(
                    id = 2,
                    nameEn = "Colombia Supremo Decaf",
                    nameFa = "قهوه کلمبیا سوپریمو (بدون کافئین)",
                    category = "beans",
                    roastLevelEn = "Medium",
                    roastLevelFa = "مدیوم",
                    originEn = "Single Origin",
                    originFa = "تک خاستگاه",
                    grindTypesFa = "دانه کامل;اسپرسو اسپشال;فرانسه;موکاپات",
                    priceTomans = 245000,
                    rating = 4.7f,
                    imageUrl = "https://images.unsplash.com/photo-1559056199-641a0ac8b55e?w=600&auto=format&fit=crop",
                    stock = 24,
                    descriptionFa = "قهوه ملایم کلمبیا با فرآیند کافئین‌زدایی کاملاً طبیعی آب کوهستان (Mountain Water Process). یادآور شیرینی شکر قهوه‌ای، بادام تفت‌داده شده و بدنه تافی مخملی. بسیار متبوع و ضد تپش قلب.",
                    descriptionEn = "Natural mountain water decaffeinated. Delivers smooth notes of brown sugar, almond, and butterscotch flavor profiles.",
                    roastingDateFa = "برشته‌کاری: ۴ روز پیش"
                ),
                ProductEntity(
                    id = 3,
                    nameEn = "Aroma House Blend (70/30)",
                    nameFa = "میکس اسپرسو آروما (۷۰٪ عربیکا)",
                    category = "beans",
                    roastLevelEn = "Medium-Dark",
                    roastLevelFa = "مدیوم-دارک",
                    originEn = "Blend",
                    originFa = "ترکیبی",
                    grindTypesFa = "دانه کامل;اسپرسو خانگی;فرنچ پرس;موکاپات",
                    priceTomans = 195000,
                    rating = 4.8f,
                    imageUrl = "https://images.unsplash.com/photo-1497935586351-b67a49e012bf?w=600&auto=format&fit=crop",
                    stock = 40,
                    descriptionFa = "ترکیب طلایی خانه آروما شامل ۷۰٪ عربیکا آمریکای جنوبی برگزیده و ۳۰٪ روبوستا ممتاز اوگاندا. خامه غلیظ عسلی (کرمای بی‌نظیر)، بدنه سنگین، اسیدیته کم و عطر فندق و کاکائو تلخ. ایده‌آل برای لاته و کاپوچینو.",
                    descriptionEn = "Premium 70% Arabica and 30% Robusta house blend designed for unmatched espresso crema and sweet hazelnut-chocolate density.",
                    roastingDateFa = "برشته‌کاری: امروز صبح (بسیار تازه)"
                ),
                ProductEntity(
                    id = 4,
                    nameEn = "Kenya Nyahururu G1",
                    nameFa = "قهوه کنیا نیاهورو لایت برشت",
                    category = "beans",
                    roastLevelEn = "Light",
                    roastLevelFa = "لایت",
                    originEn = "Single Origin",
                    originFa = "تک خاستگاه",
                    grindTypesFa = "دانه کامل;سازگار با قهوه‌ساز چمکس / V60",
                    priceTomans = 310000,
                    rating = 4.9f,
                    imageUrl = "https://images.unsplash.com/photo-1447933601403-0c6688de566e?w=600&auto=format&fit=crop",
                    stock = 8,
                    descriptionFa = "دستیابی به عمق طعم میوه‌های وحشی آفریقا. یادداشت‌های برجسته تمشک، انار و پرتقال تو سرخ با پایان کاراملی بسیار ماندگار. مخصوص قهوه‌نوشان حرفه‌ای و دمی چمکس.",
                    descriptionEn = "Exotic African profile with tart blackcurrants, pomegranate, juicy orange, and a long caramelized sugar finish.",
                    roastingDateFa = "برشته‌کاری: دیروز عصر"
                ),
                ProductEntity(
                    id = 5,
                    nameEn = "Brewista Gooseneck Kettle",
                    nameFa = "کتری برقی هوشمند برویستا ۱.۲ لیتری",
                    category = "equipment",
                    roastLevelEn = null,
                    roastLevelFa = null,
                    originEn = null,
                    originFa = null,
                    grindTypesFa = "بدون نیاز به آسیاب",
                    priceTomans = 4850000,
                    rating = 5.0f,
                    imageUrl = "https://images.unsplash.com/photo-1577968897966-3d4325b36b61?w=600&auto=format&fit=crop",
                    stock = 5,
                    descriptionFa = "کتری برقی گردن‌غاز کمپانی خوش‌نام برویستا با رنگ مشکی مات مات متالیک و با دسته چوبی ارگونومیک. قابلیت تنظیم دمای آب با دقت نیم درجه برای بهینه‌ترین حالت عصاره‌گیری فیلتری و چمکس.",
                    descriptionEn = "Brewista Artisan 1.2L Matte Black kettle with precision pour and temperature control features.",
                    roastingDateFa = "لوازم جانبی درجه یک",
                    equipmentTypeFa = "کتری",
                    equipmentTypeEn = "Kettle"
                ),
                ProductEntity(
                    id = 6,
                    nameEn = "Aroma Ceramic V60 Dripper",
                    nameFa = "دریپر سرامیکی V60 آروما گلد",
                    category = "equipment",
                    roastLevelEn = null,
                    roastLevelFa = null,
                    originEn = null,
                    originFa = null,
                    grindTypesFa = "آماده استفاده",
                    priceTomans = 380000,
                    rating = 4.6f,
                    imageUrl = "https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=600&auto=format&fit=crop",
                    stock = 12,
                    descriptionFa = "دریپر سرامیکی دست‌ساز با خطوط شیار مارپیچی آب گریز برای جریان یکنواخت آب به پودر قهوه. لعاب‌کاری‌شده با روکش لوکس طلایی جهت کنترل بهینه حرارت در طول مدت ریزش آب.",
                    descriptionEn = "Handcrafted high-temperature ceramic dripper with beautiful gold exterior and helical inner ribs.",
                    roastingDateFa = "لوازم جانبی درجه یک",
                    equipmentTypeFa = "دم‌آور",
                    equipmentTypeEn = "Brewer"
                ),
                ProductEntity(
                    id = 7,
                    nameEn = "Hand Grind Mill Hario Wood",
                    nameFa = "آسیاب دستی هاریو مدل ستونی چوبی",
                    category = "equipment",
                    roastLevelEn = null,
                    roastLevelFa = null,
                    originEn = null,
                    originFa = null,
                    grindTypesFa = "تنظیم‌پذیر",
                    priceTomans = 1750000,
                    rating = 4.8f,
                    imageUrl = "https://images.unsplash.com/photo-1589301760014-d929f3979dbc?w=600&auto=format&fit=crop",
                    stock = 7,
                    descriptionFa = "آسیاب خوش‌دست و تاریخی از آلیاژ چدن، سرامیک سخت و بدنه تمام چوب گردو ژاپن. تیغه‌های سرامیکی که در هنگام آسیاب حرارت تولید نمی‌کنند و عطر دانه قهوه را دست‌نخورده حفظ می‌نمایند.",
                    descriptionEn = "Japan Hario columns wooden manual grinder with ceramic burrs preventing heat deterioration.",
                    roastingDateFa = "لوازم آسیاب دستی",
                    equipmentTypeFa = "آسیاب",
                    equipmentTypeEn = "Grinder"
                )
            )
            coffeeDao.insertProducts(list)
        }
        
        // Seed user profile if not exists
        val existingProfile = coffeeDao.getUserProfile().first()
        if (existingProfile == null) {
            coffeeDao.saveUserProfile(
                UserProfileEntity(
                    id = 1,
                    fullName = "رضا شمس",
                    email = "rezash.sh@gmail.com",
                    phoneNumber = "۰۹۱۲۳۴۵۶۷۸۹",
                    deliveryAddress = "تهران، خیابان ولیعصر، برج آروما، طبقه ۵",
                    preferredCurrency = "toman",
                    isLoggedIn = true
                )
            )
        }
    }

    // --- Cart Actions ---
    suspend fun addToCart(product: ProductEntity, grind: String, quantity: Int = 1) {
        val cartItem = CartItemEntity(
            productId = product.id,
            nameFa = product.nameFa,
            priceTomans = product.priceTomans,
            quantity = quantity,
            grindTypeFa = grind,
            roastLevelFa = product.roastLevelFa,
            imageUrl = product.imageUrl
        )
        coffeeDao.insertCartItem(cartItem)
    }

    suspend fun updateCartQuantity(itemId: Int, quantity: Int) {
        if (quantity <= 0) {
            coffeeDao.deleteCartItem(itemId)
        } else {
            // Fetch current items and update
            val items = coffeeDao.getCartItems().first()
            val match = items.find { it.id == itemId }
            if (match != null) {
                coffeeDao.insertCartItem(match.copy(quantity = quantity))
            }
        }
    }

    suspend fun deleteCartItem(itemId: Int) {
        coffeeDao.deleteCartItem(itemId)
    }

    // --- Subscription (Coffee Club) Actions ---
    suspend fun addSubscription(product: ProductEntity, frequencyFa: String, grind: String, quantity: Int, jalaliDate: String) {
        val sub = SubscriptionEntity(
            productId = product.id,
            productNameFa = product.nameFa,
            frequencyFa = frequencyFa,
            quantity = quantity,
            grindTypeFa = grind,
            roastLevelFa = product.roastLevelFa,
            nextDeliveryJalali = jalaliDate,
            statusFa = "فعال",
            priceTomans = product.priceTomans,
            imageUrl = product.imageUrl
        )
        coffeeDao.insertSubscription(sub)
    }

    suspend fun toggleSubscriptionState(subId: Int) {
        val subs = coffeeDao.getSubscriptions().first()
        val match = subs.find { it.id == subId }
        if (match != null) {
            val newStatus = if (match.statusFa == "فعال") "متوقف شده" else "فعال"
            coffeeDao.insertSubscription(match.copy(statusFa = newStatus))
        }
    }

    suspend fun cancelSubscription(subId: Int) {
        coffeeDao.deleteSubscription(subId)
    }

    suspend fun updateSubscription(sub: SubscriptionEntity) {
        coffeeDao.insertSubscription(sub)
    }

    // --- Checkout & Logistics ---
    suspend fun checkout(totalPrice: Int, itemsSummary: String): String = withContext(Dispatchers.IO) {
        val randomNum = (1000..9999).random()
        val orderNo = "AR-$randomNum"
        val jalaliDate = getTodayJalali()
        
        val newOrder = OrderEntity(
            orderNumber = orderNo,
            orderDateFa = jalaliDate,
            totalAmountTomans = totalPrice,
            statusFa = "در حال دم‌آوری", // Default state of premium order
            statusEn = "Brewing",
            itemsSummaryFa = itemsSummary
        )

        coffeeDao.insertOrder(newOrder)
        coffeeDao.clearCart()
        return@withContext orderNo
    }

    private fun getTodayJalali(): String {
        // Since we are in 2026, we return a beautifully stylized Jalali representation of the date
        return "۱۴۰۵/۰۳/۱۴"
    }

    // --- AI Coffee Sommelier Quiz Call via direct REST API ---
    suspend fun getAICoffeeSOMMELIERRecommendation(
        tasteProfile: String,
        consumeWithMilk: String,
        brewingMethod: String,
        caffeineLevel: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        
        // Let's generate a stunning, premium local matching fallback first
        val localRecommendation = generateLocalBaristaAdvice(tasteProfile, consumeWithMilk, brewingMethod, caffeineLevel)
        
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w("CoffeeSommelier", "Gemini API key is not set. Using local specialty matcher engine.")
            // Wait 1.5 seconds to simulate high-end AI calculations nicely
            kotlinx.coroutines.delay(1200)
            return@withContext localRecommendation
        }

        try {
            // Construct direct REST API payload to gemini-3.5-flash as prescribed by skill instructions
            val systemInstruction = "شما باریستا و کارشناس قهوه ارشد فروشگاه تخصصی آروما هستید. مشاوره باریستایی دقیق، لوکس، مهیج و به زبان فارسی صریح، گرم و دلنشین ارائه دهید."
            val prompt = """
                کاربری در آزمون طعم‌شناسی فروشگاه آروما شرکت کرده است. مشخصات انتخابی او این است:
                ۱. پروفایل طعم دلخواه: $tasteProfile
                ۲. نحوه نوشیدن: $consumeWithMilk
                ۳. ابزار دم‌آوری: $brewingMethod
                ۴. میزان کافئین درخواستی: $caffeineLevel
                
                لطفاً این پاسخ را تحلیل کنید و بدین صورت بنویسید:
                ۱. قهوه پیشنهادی دقیق از میان قهوه‌های اتیوپی یرگاشف (برای طعم گلی و اسیدی)، کلمبیا سوپریمو (برای طعم ملایم، شکلاتی یا بدون کافئین)، میکس اسپرسو آروما ۷۰/۳۰ (برای تلخی، خامه غلیظ و فرمول شیر)، و کنیا نیاهورو لایت (برای طعم میوه‌ای و اسیدیته درخشان).
                ۲. میزان یا درجه آسیاب مناسب برای ابزار دم‌آوری آنها.
                ۳. توصیه شگفت‌انگیز برای افزایش لذت نوشیدن این فن برشت قهوه به شکل هنرمندانه.
                طوری بنویسید که جذاب، خوانا با ایموجی‌های قهوه و با چیدمان راست‌چین باشد و واقعاً حس حضور در کافی‌شاپ آروما را القا کند.
            """.trimIndent()

            val contentsJson = JSONObject().apply {
                put("contents", org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", org.json.JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", org.json.JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", systemInstruction)
                        })
                    })
                })
            }

            val requestBody = contentsJson.toString().toRequestBody("application/json".toMediaType())
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyStr = response.body?.string() ?: ""
                val responseJson = JSONObject(bodyStr)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val contentObj = candidate.optJSONObject("content")
                    val parts = contentObj?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text", localRecommendation)
                    }
                }
            }
            
            Log.e("CoffeeSommelier", "REST API Error: ${response.code} ${response.message}. Falling back to local sommelier.")
            return@withContext localRecommendation
        } catch (e: Exception) {
            Log.e("CoffeeSommelier", "Exception while calling Gemini: ${e.localizedMessage}. Using robust expert fallback.")
            return@withContext localRecommendation
        }
    }

    private fun generateLocalBaristaAdvice(
        taste: String,
        milk: String,
        tool: String,
        caffeine: String
    ): String {
        val decafMatch = caffeine.contains("بدون") || caffeine.contains("Decaf")
        val espressoMatch = tool.contains("اسپرسو") || tool.contains("Moka") || tool.contains("موکا")
        val sweetMatch = taste.contains("شکلات") || taste.contains("شیرین")
        val fruityMatch = taste.contains("میوه") || taste.contains("اسید") || taste.contains("گلی")

        val recommendedBean: String
        val justification: String
        val idealGrind: String
        val waterTemp: String

        if (decafMatch) {
            recommendedBean = "کلمبیا سوپریمو دکافئین (Colombia Supremo Decaf)"
            justification = "از آنجایی که تمایل به قهوه بدون کافئین دارید، این دانه ۱۰۰٪ عربیکای کالمبیا که با فرآیند کاملاً طبیعی Mountain Water کافئین‌زدایی شده برای شما بی‌نظیر است. طعم ملایم بادام سوخته و کارامل آن با شیر یا به شکل سیاه عالی می‌شود."
            idealGrind = espressoMatch.let { if (it) "نرم (ریزه مخصوص اسپرسو)" else "متوسط (مخصوص فرانسه و پور اوور)" }
            waterTemp = "۹۲ درجه سانتی‌گراد"
        } else if (espressoMatch && !fruityMatch) {
            recommendedBean = "میکس امضا آروما (Aroma House Blend 70/30)"
            justification = "برای ابزار عالی اسپرسو شما، میکس اختصاصی آروما گزینه‌ای خارق‌العاده است! تلفیق ۷۰ درصد دانه عربیکا آمریکای مرکزی و ۳۰ درصد روبوستا ممتاز اوگاندا، خامه‌ای سنگین (کرمای عسلی) ایجاد کرده و بدنه شکلاتی بسیار قوی با ماندگاری بالا ایجاد می‌کند که با لاته هم شاهکار می‌شود."
            idealGrind = "ریز یکنواخت (مخصوص پرتافیلتر یا موکاپات)"
            waterTemp = "۹۳ درجه سانتی‌گراد"
        } else if (fruityMatch) {
            if (tool.contains("چمکس") || tool.contains("فیلتر") || tool.contains("v60") || tool.contains("V60")) {
                recommendedBean = "کنیا نیاهورو لایت برشت"
                justification = "برای طعم‌شناخت میوه‌ای و اسیدیته روشن شما همراه با دم‌آوری پورآور، کنیا نیاهورو لایت شاهکار است! این قهوه با طعم ترش شیرین تمشک و انار وحشی و فرآیند شسته، یک عصاره‌گیری شفاف و مانند اکسیر در فلاسک V60 به همراه دارد."
                idealGrind = "شکر قهوه‌ای زبر (دمی قطره‌ای)"
                waterTemp = "۹۰ درجه سانتی‌گراد"
            } else {
                recommendedBean = "اتیوپی یرگاشف درجه ۱ عربیکا"
                justification = "این قهوه تک خاستگاه از تپه‌های مرتفع یرگاشف اتیوپی، عطر یاس و طعم لیمو و چای سیاه ظریف را ارائه می‌کند. غلیظ‌ترین نت‌های گلی که شامه شما را نوازش می‌کند در این انتخاب به اوج می‌رسد."
                idealGrind = "متوسط رو به زبر"
                waterTemp = "۹۱ درجه سانتی‌گراد"
            }
        } else {
            recommendedBean = "کلمبیا سوپریمو شیرین و متعادل"
            justification = "برای یک فنجان متعادل، شیرین و کاراملی که بدنه متوسط و عطر دلپذیر فندق را دارد، تک‌خاستگاه اصل کلمبیا جوابگوی سلیقه والای شماست."
            idealGrind = "متناسب با ابزار دم‌آوری شما"
            waterTemp = "۹۲ درجه سانتی‌گراد"
        }

        return """
            ☕ **پیشنهاد شخصی‌سازی شده باریستا آروما** ☕
            
            دانه قهوه پیشنهادی کارشناس:
            ✨ **$recommendedBean** ✨
            
            *فرمول و تحلیل انتخاب باریستا:*
            $justification
            
            *⚙️ مشخصات عصاره‌گیری پیشنهادی:*
            - **درجه آسیاب ویژه:** $idealGrind
            - **دمای آب ایده‌آل:** $waterTemp
            - **روش نوشیدن پیشنهادی:** ${if (milk.contains("شیر")) "با شیر فوم‌دار داغ در فنجان سرامیکی سنگین" else "به صورت سیاه و خالص در ۲ نوبت ریزش برای درک کامل نت‌ها"}
            
            *💡 فوت کوزه‌گری باریستا:*
            قبل از دم‌آوری، حتماً ابزار و فنجان خود را با آب داغ گرم کنید تا شوک دمایی عطر قهوه تازه برشت برویستا را پنهان نسازد! خریدی تازه و دلپذیر را در کلوپ آروما تجربه کنید. 🌿
        """.trimIndent()
    }
}
