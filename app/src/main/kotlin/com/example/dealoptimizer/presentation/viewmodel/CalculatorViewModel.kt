package com.example.dealoptimizer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dealoptimizer.data.model.*
import com.example.dealoptimizer.data.repository.CouponModeRepository
import com.example.dealoptimizer.data.repository.CouponRepository
import com.example.dealoptimizer.data.repository.FillProductRepository
import com.example.dealoptimizer.data.repository.ProductRepository
import com.example.dealoptimizer.data.repository.UserRepository
import com.example.dealoptimizer.domain.algorithm.DiscountCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CalculatorViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val couponRepository: CouponRepository,
    private val fillProductRepository: FillProductRepository,
    private val userRepository: UserRepository,
    private val couponModeRepository: CouponModeRepository,
    private val discountCalculator: DiscountCalculator
) : ViewModel() {

    // 当前券模式：true=叠加券，false=单用券（跨页持久化，默认叠加）
    val couponMode: StateFlow<Boolean> = couponModeRepository.isStackableMode

    private val _solution = MutableStateFlow<BundleSolution?>(null)
    val solution: StateFlow<BundleSolution?> = _solution

    private val _comparisonResult = MutableStateFlow<ComparisonResult?>(null)
    val comparisonResult: StateFlow<ComparisonResult?> = _comparisonResult

    private val _allProducts = MutableStateFlow<List<Product>>(emptyList())
    val allProducts: StateFlow<List<Product>> = _allProducts

    private val _allCoupons = MutableStateFlow<List<Coupon>>(emptyList())
    val allCoupons: StateFlow<List<Coupon>> = _allCoupons

    // ---- 多用户凑单（按用户分框 + 合并分摊） ----
    private val _checkedUsers = MutableStateFlow<List<User>>(emptyList())
    val checkedUsers: StateFlow<List<User>> = _checkedUsers

    private val _perUserSolutions = MutableStateFlow<Map<Long, BundleSolution>>(emptyMap())
    val perUserSolutions: StateFlow<Map<Long, BundleSolution>> = _perUserSolutions

    private val _combinedSolution = MutableStateFlow<BundleSolution?>(null)
    val combinedSolution: StateFlow<BundleSolution?> = _combinedSolution

    private val _shares = MutableStateFlow<Map<Long, MergedShare>>(emptyMap())
    val shares: StateFlow<Map<Long, MergedShare>> = _shares

    // 合并相对各人单独算的「增量优惠」：>0 才有跨用户分摊意义；=0 则合并无额外收益
    private val _incrementalDiscount = MutableStateFlow(0.0)
    val incrementalDiscount: StateFlow<Double> = _incrementalDiscount

    // 用户手动勾选的商品ID（跨导航持久化，避免切页后重置）
    private val _selectedProductIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedProductIds: StateFlow<Set<Long>> = _selectedProductIds

    // 计算异常（DB 读取 / 算法异常）暴露给 UI，避免协程静默失败导致界面无反馈
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun updateSelectedProductIds(ids: Set<Long>) {
        _selectedProductIds.value = ids
    }

    fun clearError() {
        _error.value = null
    }

    // ---- 多用户凑单方案对比：现状(两阶段) vs 全局(公平优先) ----
    data class MultiUserSolution(
        val key: String,
        val title: String,
        val combined: BundleSolution,
        val shares: Map<Long, MergedShare>,
        val perUser: Map<Long, BundleSolution>
    )

    private val _candidateOptions = MutableStateFlow<List<MultiUserSolution>>(emptyList())
    val candidateOptions: StateFlow<List<MultiUserSolution>> = _candidateOptions

    private val _selectedCandidateKey = MutableStateFlow<String?>(null)
    val selectedCandidateKey: StateFlow<String?> = _selectedCandidateKey

    /** 用户在「现状 / 全局」两种方案间手动切换应用 */
    fun selectCandidate(key: String) {
        val target = _candidateOptions.value.firstOrNull { it.key == key } ?: return
        _selectedCandidateKey.value = key
        _combinedSolution.value = target.combined
        _shares.value = target.shares
    }

    // 计算结果缓存：输入签名未变时跳过重复计算（跨页面切换有效，ViewModel 在导航期间存活）
    private data class SingleCalcKey(
        val products: List<Product>,
        val coupons: List<Coupon>,
        val useFill: Boolean
    )
    private data class MultiUserCalcKey(
        val products: List<Product>,
        val coupons: List<Coupon>,
        val users: List<User>,
        val selectedIds: Set<Long>,
        val useFill: Boolean
    )
    private var lastSingleKey: SingleCalcKey? = null
    private var lastMultiUserKey: MultiUserCalcKey? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            productRepository.allProducts.collect { products ->
                _allProducts.value = products
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            couponRepository.allCoupons.collect { coupons ->
                _allCoupons.value = coupons
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.allUsers.collect { users ->
                _checkedUsers.value = users.filter { it.isSelected }
            }
        }
    }

    fun clearCalculation() {
        clearAllResults()
    }

    /** 清空全部方案展示态：单/多用户方案、对比候选、分摊等一并复位，避免清空商品后旧方案残留 */
    private fun clearAllResults() {
        _solution.value = null
        _comparisonResult.value = null
        _perUserSolutions.value = emptyMap()
        _combinedSolution.value = null
        _shares.value = emptyMap()
        _incrementalDiscount.value = 0.0
        _candidateOptions.value = emptyList()
        _selectedCandidateKey.value = null
    }

    fun calculateBestPrice(useFillProducts: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
            val products = productRepository.allProducts.first()
            val coupons = couponRepository.allCoupons.first()
            // 当前仅使用单一模式券（叠加或单用）。
            // 混合场景（两类券同时参与）算法层 DiscountCalculator 已支持，
            // 如需启用，去掉下方 isStackable 过滤即可。
            val enabledCoupons = coupons.filter { it.isEnabled && it.isStackable == couponModeRepository.isStackableMode.value }
            val fillProducts = if (useFillProducts) fillProductRepository.allFillProducts.first() else emptyList()
            _allProducts.value = products
            _allCoupons.value = coupons

            if (products.isEmpty() || enabledCoupons.isEmpty()) {
                _solution.value = null
                _comparisonResult.value = null
                return@launch
            }

            val bestSolution = discountCalculator.calculateBestCombination(products, enabledCoupons, fillProducts, useFillProducts)
            _solution.value = bestSolution

            val comparison = discountCalculator.calculateComparisonResult(products, enabledCoupons, fillProducts, useFillProducts)
            _comparisonResult.value = comparison
            } catch (e: Exception) {
                _error.value = e.message ?: "计算失败"
            }
        }
    }

    fun calculateForProducts(selectedProducts: List<Product>, useFillProducts: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
            val coupons = _allCoupons.value.ifEmpty { couponRepository.allCoupons.first() }
            // 当前仅使用单一模式券（叠加或单用）。
            // 混合场景（两类券同时参与）算法层 DiscountCalculator 已支持，
            // 如需启用，去掉下方 isStackable 过滤即可。
            val enabledCoupons = coupons.filter { it.isEnabled && it.isStackable == couponModeRepository.isStackableMode.value }
            // 输入签名未变且已有结果：直接复用，避免切页/重组导致的重复重算
            val key = SingleCalcKey(selectedProducts, enabledCoupons, useFillProducts)
            if (key == lastSingleKey && _solution.value != null) {
                return@launch
            }
            lastSingleKey = key
            val fillProducts = if (useFillProducts) fillProductRepository.allFillProducts.first() else emptyList()

            if (selectedProducts.isEmpty() || enabledCoupons.isEmpty()) {
                _solution.value = null
                _comparisonResult.value = null
                return@launch
            }

            _solution.value = discountCalculator.calculateBestCombination(selectedProducts, enabledCoupons, fillProducts, useFillProducts)
            _comparisonResult.value = discountCalculator.calculateComparisonResult(selectedProducts, enabledCoupons, fillProducts, useFillProducts)
            } catch (e: Exception) {
                _error.value = e.message ?: "计算失败"
            }
        }
    }

    // 多用户：每人单独算 + 合并跨用户算 + 按原价占比分摊（仅用勾选商品）
    fun calculateMultiUser(useFillProducts: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
            val allProducts = productRepository.allProducts.first()
            val coupons = couponRepository.allCoupons.first()
            // 当前仅使用单一模式券（叠加或单用）。
            // 混合场景（两类券同时参与）算法层 DiscountCalculator 已支持，
            // 如需启用，去掉下方 isStackable 过滤即可。
            val enabledCoupons = coupons.filter { it.isEnabled && it.isStackable == couponModeRepository.isStackableMode.value }
            val fillProducts = if (useFillProducts) fillProductRepository.allFillProducts.first() else emptyList()
            val users = userRepository.allUsers.first().filter { it.isSelected }
            val selectedIds = _selectedProductIds.value

            // 输入签名未变且已有结果：直接复用，避免切页/重组导致的重复重算（跨页面有效）
            val key = MultiUserCalcKey(allProducts, enabledCoupons, users, selectedIds, useFillProducts)
            if (key == lastMultiUserKey && _combinedSolution.value != null) {
                return@launch
            }
            lastMultiUserKey = key

            // 仅保留勾选商品（必买商品始终保留）
            val scoped = allProducts.filter { p ->
                (p.id in selectedIds || p.isRequired) && users.any { it.id == p.ownerId }
            }

            if (users.isEmpty() || scoped.isEmpty() || enabledCoupons.isEmpty()) {
                clearAllResults()
                return@launch
            }

            // 按用户分组，每人单独算（仅用自己的勾选商品）
            val perUser = users.mapNotNull { u ->
                val userScoped = scoped.filter { it.ownerId == u.id }
                if (userScoped.isEmpty()) null
                else u.id to discountCalculator.calculateBestCombination(
                    userScoped, enabledCoupons, fillProducts, useFillProducts
                )
            }.toMap()

            val isStackable = enabledCoupons.firstOrNull()?.isStackable ?: false

            // ================================================================
            //  两阶段分组策略：「先自组，后凑单」
            //  阶段1：每人先用 perUser 自组达标（锁定已用券商品，不再参与跨人凑单）
            //  阶段2：剩余未达标商品 → 跨用户凑单
            // ================================================================
            val lockedProductIds = mutableSetOf<Long>()   // 阶段1已锁定的商品ID
            val perUserLockedUsages = mutableMapOf<Long, List<CouponUsage>>()  // userId → 单人组券使用记录

            for ((userId, sol) in perUser) {
                if (sol.couponUsages.isNotEmpty()) {
                    // 该用户在单独计算中成功用了券 → 锁定这些商品为"单人组"
                    val usedIds = sol.couponUsages.flatMap { it.productGroup }.map { it.id }.toSet()
                    lockedProductIds.addAll(usedIds)
                    perUserLockedUsages[userId] = sol.couponUsages
                }
            }

            // 阶段2：剩余商品用于跨用户凑单
            val poolProducts = scoped.filter { it.id !in lockedProductIds }
            val pooledUsers = poolProducts.map { it.ownerId }.distinct()
            // 只有多人参与且有足够商品时才跑跨用户凑单；否则空解
            val crossPool: BundleSolution = if (pooledUsers.size >= 2 && poolProducts.isNotEmpty() &&
                enabledCoupons.any { !it.isStackable }) {
                discountCalculator.calculateBestCombination(poolProducts, enabledCoupons, fillProducts, useFillProducts, multiUserMode = true)
            } else {
                BundleSolution(poolProducts, emptyList(), poolProducts.sumOf { it.originalPrice }, poolProducts.sumOf { it.originalPrice })
            }

            // 合并解（供 UI 展示）
            val combined: BundleSolution = if (isStackable) {
                // 叠加券：在合并后的总价上整量重算（count=floor(合并总价/阈值)，通常多于各人单独之和）。
                // 这才是真正的「合并凑单」优惠——合并的实惠正来自此处，不能用各人单独用券简单相加
                // （否则 combined 优惠恒等于各人之和，incrementalDiscount 永远为 0，合并永无额外节省）。
                discountCalculator.calculateBestCombination(scoped, enabledCoupons, fillProducts, useFillProducts)
            } else {
                // 单用券：先自组后凑单，合并两组用券
                val mergedUsages = perUserLockedUsages.values.flatten() + crossPool.couponUsages
                BundleSolution(
                    products = scoped,
                    couponUsages = mergedUsages,
                    originalTotal = scoped.sumOf { it.originalPrice },
                    finalPrice = scoped.sumOf { it.originalPrice } - mergedUsages.sumOf { usage -> usageDiscount(usage, includeFill = false) },
                    fillProducts = (perUser.values.flatMap { it.fillProducts } + crossPool.fillProducts).distinctBy { it.name }
                )
            }

            // 分摊计算：叠加券用增量优惠+余量占比；单用券按「先自组后凑单」逐组聚合
            val incrementalDiscount = if (isStackable) {
                val sumPerUserDiscount = perUser.values.sumOf { it.totalDiscount }
                combined.totalDiscount - sumPerUserDiscount
            } else {
                0.0
            }

            val shares = if (isStackable) {
                // ---- 叠加券：增量优惠 + 余量占比（原有逻辑）----
                val totalRemaining = users.sumOf { u ->
                    val sol = perUser[u.id]
                    val ownOriginal = scoped.filter { it.ownerId == u.id }.sumOf { it.originalPrice }
                    val usedThreshold = sol?.couponUsages?.sumOf { it.count * it.coupon.threshold } ?: 0.0
                    maxOf(ownOriginal - usedThreshold, 0.0)
                }
                users.associate { u ->
                    val ownOriginal = scoped.filter { it.ownerId == u.id }.sumOf { it.originalPrice }
                    val perUserSol = perUser[u.id]
                    val basePayable = perUserSol?.finalPrice ?: ownOriginal
                    val baseDiscount = perUserSol?.totalDiscount ?: 0.0
                    val usedThreshold = perUserSol?.couponUsages?.sumOf { it.count * it.coupon.threshold } ?: 0.0
                    val remaining = maxOf(ownOriginal - usedThreshold, 0.0)
                    val w = if (totalRemaining > 0) remaining / totalRemaining else 0.0
                    val incremental = incrementalDiscount * w
                    u.id to MergedShare(
                        ownOriginal = ownOriginal,
                        remaining = remaining,
                        discount = baseDiscount + incremental,
                        payable = basePayable - incremental
                    )
                }
            } else {
                buildNonStackableShares(users, scoped, perUserLockedUsages, crossPool)
            }

            // ===== 候选方案2：全局最优 + 公平优先 =====
            // 所有勾选商品一起参与（multiUserMode=false 允许自组+跨组），仅在总省钱平局时
            // 用公平 tie-break 让高价商品所属用户多留优惠，绝不牺牲总省钱。
            val globalSolution = discountCalculator.calculateBestCombination(
                scoped, enabledCoupons, fillProducts, useFillProducts,
                multiUserMode = false,
                fairness = DiscountCalculator.FairnessStrategy.FAIR
            )
            val globalShares = if (isStackable) shares else buildNonStackableShares(users, scoped, emptyMap(), globalSolution)

            val statusQuo = MultiUserSolution(
                key = "status_quo",
                title = "现状（先自组后凑单）",
                combined = combined,
                shares = shares,
                perUser = perUser
            )
            val globalFair = MultiUserSolution(
                key = "global_fair",
                title = "全局（公平优先）",
                combined = globalSolution,
                shares = globalShares,
                perUser = perUser
            )

            val candidates = listOf(statusQuo, globalFair)
            val selectedKey = _selectedCandidateKey.value ?: "status_quo"
            val selected = candidates.firstOrNull { it.key == selectedKey } ?: statusQuo

            _perUserSolutions.value = perUser
            _combinedSolution.value = selected.combined
            _shares.value = selected.shares
            _incrementalDiscount.value = incrementalDiscount
            _candidateOptions.value = candidates
            _selectedCandidateKey.value = selected.key
            } catch (e: Exception) {
                _error.value = e.message ?: "计算失败"
            }
        }
    }

    /**
     * 单条用券折扣的统一算法（与 DiscountCalculator 的折扣口径一致，单一真相源）。
     * ViewModel 内分摊/合并多处需要单条用券折扣，集中此处避免手抄公式分叉。
     * includeFill=true 时 DISCOUNT 类型以「商品原价 + 凑单小物」为基数（分摊场景），
     * false 时仅用商品原价（合并方案 finalPrice 场景）。
     */
    private fun usageDiscount(usage: CouponUsage, includeFill: Boolean): Double {
        val groupTotal = if (includeFill) {
            usage.productGroup.sumOf { it.originalPrice } + usage.fillProducts.sumOf { it.price }
        } else {
            usage.productGroup.sumOf { it.originalPrice }
        }
        return when (usage.coupon.type) {
            CouponType.FULL_REDUCTION -> usage.count * usage.coupon.discountValue
            CouponType.DISCOUNT -> groupTotal * (usage.coupon.discountValue / 100) * usage.count
            CouponType.NO_THRESHOLD -> usage.count * usage.coupon.discountValue
        }
    }

    /**
     * 单用券分摊：先自组（perUserLockedUsages）+ 后凑单（crossPool）逐组聚合。
     * 全局方案复用本函数时传 emptyMap() 作为 perUserLockedUsages、传 globalSolution 作为 crossPool，
     * 此时所有分组（含自组单人或跨用户）统一按「原价占比」分摊，逻辑一致。
     */
    private fun buildNonStackableShares(
        users: List<User>,
        scoped: List<Product>,
        perUserLockedUsages: Map<Long, List<CouponUsage>>,
        crossPool: BundleSolution
    ): Map<Long, MergedShare> {
        val allGroupedProductIds = (perUserLockedUsages.values.flatten().flatMap { it.productGroup } +
            crossPool.couponUsages.flatMap { it.productGroup }).map { it.id }.toSet()
        return users.associate { u ->
            val baseOriginal = scoped.filter { it.ownerId == u.id }.sumOf { it.originalPrice }
            var userDiscount = 0.0
            var userPayable = 0.0
            var allocatedFillCost = 0.0

            // 阶段1：perUser 单人组（该用户已锁定的自组商品）
            val lockedUsages = perUserLockedUsages[u.id]
            if (lockedUsages != null) {
                for (usage in lockedUsages) {
                    val groupProductTotal = usage.productGroup.sumOf { it.originalPrice }
                    val fillTotal = usage.fillProducts.sumOf { it.price }
                    val groupDiscount = usageDiscount(usage, includeFill = true)
                    // 单人组：全额享受优惠 + 承担全部凑单小物费用
                    userDiscount += groupDiscount
                    userPayable += groupProductTotal - groupDiscount + fillTotal
                    allocatedFillCost += fillTotal
                }
            }

            // 阶段2：跨用户凑单组（crossPool 中含该用户的组）
            for (usage in crossPool.couponUsages) {
                val userProducts = usage.productGroup.filter { it.ownerId == u.id }
                if (userProducts.isEmpty()) continue
                val ownerOriginal = userProducts.sumOf { it.originalPrice }
                val fillTotal = usage.fillProducts.sumOf { it.price }
                val groupProductTotal = usage.productGroup.sumOf { it.originalPrice }
                val groupDiscount = usageDiscount(usage, includeFill = true)
                val ownersInGroup = usage.productGroup.map { it.ownerId }.distinct()

                if (ownersInGroup.size >= 2) {
                    // 凑单组：按原价占比分摊优惠 + 凑单小物按原价占比分摊成本
                    if (ownerOriginal > 0 || fillTotal > 0) {
                        val w = if (groupProductTotal > 0) ownerOriginal / groupProductTotal else 0.0
                        val myDiscount = groupDiscount * w
                        val myFillCost = fillTotal * w
                        userDiscount += myDiscount
                        userPayable += ownerOriginal - myDiscount + myFillCost
                        allocatedFillCost += myFillCost
                    }
                } else if (userProducts.isNotEmpty() || ownersInGroup.size == 1) {
                    // 跨用户池中的单人组（某人剩余商品恰好够自己达标）
                    userDiscount += groupDiscount
                    userPayable += ownerOriginal - groupDiscount + fillTotal
                    allocatedFillCost += fillTotal
                } else if (fillTotal > 0 && ownersInGroup.isEmpty()) {
                    val avgFill = fillTotal / users.size
                    userPayable += avgFill
                    allocatedFillCost += avgFill
                }
            }

            // 未达标商品（未进入任何组）：原价付
            val remainingOriginal = scoped
                .filter { it.ownerId == u.id && it.id !in allGroupedProductIds }
                .sumOf { it.originalPrice }
            userPayable += remainingOriginal

            u.id to MergedShare(
                ownOriginal = baseOriginal + allocatedFillCost,
                remaining = baseOriginal + allocatedFillCost - userDiscount,
                discount = userDiscount,
                payable = userPayable
            )
        }
    }
}
