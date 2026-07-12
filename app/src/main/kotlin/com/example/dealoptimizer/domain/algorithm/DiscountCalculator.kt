package com.example.dealoptimizer.domain.algorithm

import com.example.dealoptimizer.data.model.*
import kotlin.math.min

class DiscountCalculator {
    private companion object {
        const val MAX_AUTO_FILL_GAP = 40.0
        const val VIRTUAL_FILL_PRODUCT_ID = -1L
    }

    /**
     * 分组择优策略：
     * - NONE：只按总省钱最大（原行为，平局按枚举顺序）。
     * - FAIR：总省钱相同（不牺牲一分钱）时，偏好让「高价商品所属用户」保留更多优惠，
     *   即「大件配小件、大件拿大头」，解决多人凑单时贵重商品被 filler 稀释优惠的问题。
     */
    enum class FairnessStrategy { NONE, FAIR }

    private data class CouponCandidate(
        val mask: Int,
        val couponIndex: Int,
        val coupon: Coupon,
        val products: List<Product>,
        val total: Double,
        val discount: Double,
        val fillProducts: List<FillProduct> = emptyList(),
        // 公平分数：本组优惠中「归属最高价商品所属用户」的金额（越大越公平）。
        // 单人组 = 全额 discount（无跨用户补贴）；多人组 = discount × 最高价商品用户的原价占比。
        val ownerFairness: Double = discount
    ) {
        val fillTotal: Double get() = fillProducts.sumOf { it.price }
        val netSavings: Double get() = discount - fillTotal
    }

    private data class GroupingResult(
        val groups: List<CouponCandidate> = emptyList(),
        val discount: Double = 0.0,
        val groupedTotal: Double = 0.0,
        val fillTotal: Double = 0.0,
        val fairnessScore: Double = 0.0
    ) {
        val netSavings: Double get() = discount - fillTotal
    }

    private data class SearchKey(
        val availableMask: Int,
        val usagesLeft: List<Int>
    )

    fun calculateBestCombination(
        products: List<Product>,
        coupons: List<Coupon>,
        fillProducts: List<FillProduct> = emptyList(),
        useFillProducts: Boolean = fillProducts.isNotEmpty(),
        multiUserMode: Boolean = false,
        fairness: FairnessStrategy = FairnessStrategy.NONE
    ): BundleSolution {
        val enabledCoupons = coupons.filter { it.isEnabled }
        if (products.isEmpty()) {
            return BundleSolution(products, emptyList(), 0.0, 0.0)
        }
        val noCouponTotal = products.sumOf { it.originalPrice }
        val noCouponSolution = BundleSolution(
            products = products,
            couponUsages = emptyList(),
            originalTotal = noCouponTotal,
            finalPrice = noCouponTotal
        )

        if (enabledCoupons.isEmpty()) {
            return noCouponSolution
        }

        var bestSolution = noCouponSolution
        
        for (coupon in enabledCoupons) {
            val solution = if (coupon.isStackable) {
                calculateStackableSolution(products, coupon)
            } else {
                calculateNonStackableSolution(products, coupon, fillProducts, useFillProducts, multiUserMode, fairness)
            }
            if (solution.finalPrice < bestSolution.finalPrice) {
                bestSolution = solution
            }
        }

        if (enabledCoupons.size >= 2) {
            val solution = calculateMultiCouponSolution(products, enabledCoupons, fillProducts, useFillProducts, multiUserMode, fairness)
            if (solution.finalPrice < bestSolution.finalPrice) {
                bestSolution = solution
            }
        }

        return bestSolution
    }

    private fun calculateMultiCouponSolution(
        products: List<Product>,
        coupons: List<Coupon>,
        fillProducts: List<FillProduct>,
        useFillProducts: Boolean,
        multiUserMode: Boolean = false,
        fairness: FairnessStrategy = FairnessStrategy.NONE
    ): BundleSolution {
        val stackableCoupons = coupons.filter { it.isStackable }
        val nonStackableCoupons = coupons.filter { !it.isStackable }
        val originalTotal = products.totalPrice()
        val usages = mutableListOf<CouponUsage>()
        var totalDiscount = 0.0

        stackableCoupons.forEach { coupon ->
            val (count, discount) = calculateBestUsageCount(originalTotal, coupon)
            if (count > 0 && discount > 0.0) {
                usages.add(CouponUsage(coupon, count))
                totalDiscount += discount
            }
        }

        if (nonStackableCoupons.isNotEmpty()) {
            val grouping = if (products.size <= 16) {
                findBestExactGroups(products, nonStackableCoupons, fillProducts, useFillProducts, multiUserMode, fairness)
            } else {
                findGreedyGroups(products, nonStackableCoupons, fillProducts, useFillProducts, multiUserMode)
            }
            usages.addAll(grouping.groups.map { group ->
                CouponUsage(group.coupon, 1, group.products, group.fillProducts)
            })
            totalDiscount += grouping.discount
        }

        return BundleSolution(
            products = products,
            couponUsages = usages,
            originalTotal = originalTotal,
            finalPrice = maxOf(originalTotal + usages.flatMap { it.fillProducts }.sumOf { it.price } - totalDiscount, 0.0),
            fillProducts = usages.flatMap { it.fillProducts }
        )
    }

    private fun calculateStackableSolution(
        products: List<Product>,
        coupon: Coupon
    ): BundleSolution {
        val originalTotal = products.totalPrice()
        val (count, discount) = calculateBestUsageCount(originalTotal, coupon)

        return BundleSolution(
            products = products,
            couponUsages = if (count > 0) listOf(CouponUsage(coupon, count)) else emptyList(),
            originalTotal = originalTotal,
            finalPrice = maxOf(originalTotal - discount, 0.0)
        )
    }

    private fun calculateNonStackableSolution(
        products: List<Product>,
        coupon: Coupon,
        fillProducts: List<FillProduct>,
        useFillProducts: Boolean,
        multiUserMode: Boolean = false,
        fairness: FairnessStrategy = FairnessStrategy.NONE
    ): BundleSolution {
        val originalTotal = products.totalPrice()
        val grouping = if (products.size <= 16) {
            findBestExactGroups(products, listOf(coupon), fillProducts, useFillProducts, multiUserMode, fairness)
        } else {
            findGreedyGroups(products, listOf(coupon), fillProducts, useFillProducts, multiUserMode)
        }

        val usages = grouping.groups.map { group ->
            CouponUsage(group.coupon, 1, group.products, group.fillProducts)
        }

        return BundleSolution(
            products = products,
            couponUsages = usages,
            originalTotal = originalTotal,
            finalPrice = maxOf(originalTotal + grouping.fillTotal - grouping.discount, 0.0),
            fillProducts = usages.flatMap { it.fillProducts }
        )
    }

    fun calculateBestUsageCount(totalAmount: Double, coupon: Coupon): Pair<Int, Double> {
        var bestCount = 0
        var bestDiscount = 0.0

        when (coupon.type) {
            CouponType.FULL_REDUCTION -> {
                if (totalAmount >= coupon.threshold) {
                    val usageLimit = coupon.usageLimit()
                    bestCount = if (coupon.isStackable && coupon.threshold > 0.0) {
                        min((totalAmount / coupon.threshold).toInt(), usageLimit)
                    } else {
                        min(1, usageLimit)
                    }
                    bestDiscount = bestCount * coupon.discountValue
                }
            }

            CouponType.DISCOUNT -> {
                if (coupon.usageLimit() >= 1 && totalAmount >= coupon.threshold) {
                    bestCount = 1
                    bestDiscount = totalAmount * (coupon.discountValue / 100)
                }
            }

            CouponType.NO_THRESHOLD -> {
                if (coupon.usageLimit() >= 1) {
                    bestCount = 1
                    bestDiscount = minOf(coupon.discountValue, totalAmount)
                }
            }
        }

        return Pair(bestCount, bestDiscount)
    }

    fun calculateFillSuggestions(
        currentTotal: Double,
        coupons: List<Coupon>,
        fillProducts: List<FillProduct>
    ): List<FillSuggestion> {
        val suggestions = mutableListOf<FillSuggestion>()

        val fullReductionCoupons = coupons
            .filter { it.isEnabled && it.type == CouponType.FULL_REDUCTION && it.threshold > currentTotal }
            .sortedBy { it.threshold }

        if (fullReductionCoupons.isEmpty()) {
            return emptyList()
        }

        val targetCoupon = fullReductionCoupons.first()
        val candidates = fillProducts
            .filter { it.price in 1.0..20.0 }
            .sortedBy { it.price }

        var bestCombination: List<FillProduct> = emptyList()
        var bestSaving = 0.0

        for (product in candidates) {
            val fillTotal = product.price
            val newTotal = currentTotal + fillTotal
            if (newTotal >= targetCoupon.threshold) {
                val (_, discount) = calculateBestUsageCount(newTotal, targetCoupon)
                val finalPrice = newTotal - discount
                val saving = currentTotal - finalPrice + fillTotal

                if (saving > bestSaving) {
                    bestSaving = saving
                    bestCombination = listOf(product)
                }
            }
        }

        if (bestCombination.isNotEmpty()) {
            val fillTotal = bestCombination.sumOf { it.price }
            val newTotal = currentTotal + fillTotal
            val (_, discount) = calculateBestUsageCount(newTotal, targetCoupon)
            val finalPrice = newTotal - discount
            val saving = currentTotal - finalPrice + fillTotal

            suggestions.add(FillSuggestion(
                fillProducts = bestCombination,
                originalTotal = currentTotal,
                newTotal = newTotal,
                discount = discount,
                finalPrice = finalPrice,
                saving = saving
            ))
        }

        return suggestions
    }

    private fun findBestExactGroups(
        products: List<Product>,
        coupons: List<Coupon>,
        fillProducts: List<FillProduct>,
        useFillProducts: Boolean,
        multiUserMode: Boolean = false,
        fairness: FairnessStrategy = FairnessStrategy.NONE
    ): GroupingResult {
        val candidates = coupons.flatMapIndexed { index, coupon ->
            buildCandidates(products, coupon, index, fillProducts, useFillProducts, multiUserMode)
        }
        if (candidates.isEmpty()) return GroupingResult()

        val fullMask = (1 shl products.size) - 1
        val usageLimits = coupons.map { min(it.usageLimit(), products.size) }
        val memo = mutableMapOf<SearchKey, GroupingResult>()

        fun solve(availableMask: Int, usagesLeft: List<Int>): GroupingResult {
            if (availableMask == 0 || usagesLeft.all { it == 0 }) return GroupingResult()
            val key = SearchKey(availableMask, usagesLeft)
            memo[key]?.let { return it }

            var best = GroupingResult()
            for (candidate in candidates) {
                if (candidate.mask and availableMask != candidate.mask) continue
                if (usagesLeft[candidate.couponIndex] <= 0) continue

                val nextUsagesLeft = usagesLeft.toMutableList().also {
                    it[candidate.couponIndex] = it[candidate.couponIndex] - 1
                }
                val next = solve(availableMask xor candidate.mask, nextUsagesLeft)
                val current = GroupingResult(
                    groups = listOf(candidate) + next.groups,
                    discount = candidate.discount + next.discount,
                    groupedTotal = candidate.total + next.groupedTotal,
                    fillTotal = candidate.fillTotal + next.fillTotal,
                    fairnessScore = candidate.ownerFairness + next.fairnessScore
                )

                if (current.isBetterThan(best, fairness)) {
                    best = current
                }
            }

            memo[key] = best
            return best
        }

        return solve(fullMask, usageLimits)
    }

    private fun findGreedyGroups(
        products: List<Product>,
        coupons: List<Coupon>,
        fillProducts: List<FillProduct>,
        useFillProducts: Boolean,
        multiUserMode: Boolean = false
    ): GroupingResult {
        val remaining = products.sortedBy { it.originalPrice }.toMutableList()
        val groups = mutableListOf<CouponCandidate>()
        var discount = 0.0
        var groupedTotal = 0.0
        var fillTotal = 0.0
        val usagesLeft = coupons.associateWith { min(it.usageLimit(), products.size) }.toMutableMap()

        while (remaining.isNotEmpty() && usagesLeft.any { it.value > 0 }) {
            val coupon = coupons
                .filter { (usagesLeft[it] ?: 0) > 0 }
                .maxByOrNull { it.discountValue }
                ?: break
            val group = mutableListOf<Product>()
            var groupTotal = 0.0

            while (remaining.isNotEmpty() && groupTotal < coupon.threshold) {
                val next = remaining.first()
                if (multiUserMode) {
                    val ownersIfAdded = (group + next).groupBy { it.ownerId }
                        .mapValues { (_, ps) -> ps.sumOf { it.originalPrice } }
                    if (ownersIfAdded.size >= 2 && ownersIfAdded.values.any { it >= coupon.threshold }) {
                        break
                    }
                }
                group.add(remaining.removeAt(0))
                groupTotal += next.originalPrice
            }

            val groupFillProducts = if (groupTotal < coupon.threshold) {
                if (!useFillProducts) break
                findBestFillForGap(coupon.threshold - groupTotal, fillProducts) ?: break
            } else {
                emptyList()
            }

            val groupFillTotal = groupFillProducts.sumOf { it.price }
            val filledTotal = groupTotal + groupFillTotal
            val groupDiscount = calculateSingleUseDiscount(filledTotal, coupon)
            if (groupDiscount <= groupFillTotal) break
            groups.add(CouponCandidate(0, 0, coupon, group, filledTotal, groupDiscount, groupFillProducts))
            discount += groupDiscount
            groupedTotal += filledTotal
            fillTotal += groupFillTotal
            usagesLeft[coupon] = (usagesLeft[coupon] ?: 0) - 1
        }

        return GroupingResult(groups, discount, groupedTotal, fillTotal)
    }

    private fun buildCandidates(
        products: List<Product>,
        coupon: Coupon,
        couponIndex: Int,
        fillProducts: List<FillProduct>,
        useFillProducts: Boolean,
        multiUserMode: Boolean = false
    ): List<CouponCandidate> {
        val candidates = mutableListOf<CouponCandidate>()
        val fullMask = (1 shl products.size) - 1

        for (mask in 1..fullMask) {
            val group = mutableListOf<Product>()
            var productTotal = 0.0

            products.forEachIndexed { index, product ->
                if (mask and (1 shl index) != 0) {
                    group.add(product)
                    productTotal += product.originalPrice
                }
            }

            if (multiUserMode) {
                val ownerTotals = group.groupBy { it.ownerId }
                    .mapValues { (_, ps) -> ps.sumOf { it.originalPrice } }
                if (ownerTotals.size >= 2 && ownerTotals.values.any { it >= coupon.threshold }) {
                    continue
                }
            }

            if (productTotal >= coupon.threshold) {
                val discount = calculateSingleUseDiscount(productTotal, coupon)
                if (discount > 0.0) {
                    candidates.add(CouponCandidate(mask, couponIndex, coupon, group, productTotal, discount,
                        ownerFairness = ownerFairness(group, discount)))
                }
            } else if (useFillProducts) {
                val groupFillProducts = findBestFillForGap(coupon.threshold - productTotal, fillProducts)
                if (groupFillProducts != null) {
                    val fillTotal = groupFillProducts.sumOf { it.price }
                    val filledTotal = productTotal + fillTotal
                    val discount = calculateSingleUseDiscount(filledTotal, coupon)
                    if (discount > fillTotal) {
                        candidates.add(CouponCandidate(mask, couponIndex, coupon, group, filledTotal, discount, groupFillProducts,
                            ownerFairness = ownerFairness(group, discount)))
                    }
                }
            }
        }

        return candidates.sortedWith(
            compareByDescending<CouponCandidate> { it.netSavings }
                .thenBy { it.fillTotal }
                .thenBy { it.total }
                .thenBy { it.products.size }
        )
    }

    private fun calculateSingleUseDiscount(totalAmount: Double, coupon: Coupon): Double {
        val discount = when (coupon.type) {
            CouponType.FULL_REDUCTION -> coupon.discountValue
            CouponType.DISCOUNT -> totalAmount * (coupon.discountValue / 100)
            CouponType.NO_THRESHOLD -> coupon.discountValue
        }
        return minOf(discount, totalAmount)
    }

    private fun GroupingResult.isBetterThan(
        other: GroupingResult,
        fairness: FairnessStrategy = FairnessStrategy.NONE
    ): Boolean {
        if (netSavings != other.netSavings) return netSavings > other.netSavings
        if (fillTotal != other.fillTotal) return fillTotal < other.fillTotal
        if (discount != other.discount) return discount > other.discount
        // 公平优先：总省钱完全相同的前提下，偏好高价商品用户多留优惠的分组
        if (fairness == FairnessStrategy.FAIR && fairnessScore != other.fairnessScore) {
            return fairnessScore > other.fairnessScore
        }
        if (groups.size != other.groups.size) return groups.size > other.groups.size
        return groupedTotal < other.groupedTotal
    }

    /**
     * 计算一个分组的公平分数：本组优惠中归属「最高价商品所属用户」的金额。
     * 单人组（或空/无归属）返回全额 discount（无跨用户补贴，天然公平）。
     * 多人组返回 discount × 最高价商品用户的组内原价占比 —— 占比越高说明大件确实拿了大头。
     */
    private fun ownerFairness(group: List<Product>, discount: Double): Double {
        if (group.isEmpty()) return discount
        val owners = group.map { it.ownerId }.distinct()
        if (owners.size <= 1) return discount
        val topOwner = group.maxByOrNull { it.originalPrice }?.ownerId ?: return discount
        val groupTotal = group.sumOf { it.originalPrice }
        if (groupTotal <= 0.0) return discount
        val topOwnerTotal = group.filter { it.ownerId == topOwner }.sumOf { it.originalPrice }
        return discount * (topOwnerTotal / groupTotal)
    }

    private fun findBestFillForGap(gap: Double, fillProducts: List<FillProduct>): List<FillProduct>? {
        if (gap <= 0.0 || gap > MAX_AUTO_FILL_GAP) return null

        val realFillProduct = fillProducts
            .filter { it.price >= gap && it.price <= MAX_AUTO_FILL_GAP }
            .minWithOrNull(compareBy<FillProduct> { it.price - gap }.thenBy { it.price })

        return listOf(
            realFillProduct ?: FillProduct(
                id = VIRTUAL_FILL_PRODUCT_ID,
                name = "差价小物",
                price = gap,
                category = "自动凑单",
                isHot = false
            )
        )
    }

    private fun Coupon.usageLimit(): Int {
        return if (maxUsages == Int.MAX_VALUE) Int.MAX_VALUE else maxOf(maxUsages, 0)
    }

    private fun List<Product>.totalPrice(): Double {
        return sumOf { it.originalPrice }
    }

    fun calculateComparisonResult(
        products: List<Product>,
        coupons: List<Coupon>,
        fillProducts: List<FillProduct>,
        useFillProducts: Boolean = fillProducts.isNotEmpty()
    ): ComparisonResult {
        val originalPrice = products.sumOf { it.originalPrice }
        val effectiveFillProducts = if (useFillProducts) fillProducts else emptyList()
        val directSolution = calculateBestCombination(products, coupons, effectiveFillProducts, useFillProducts)
        val fillSuggestions = if (useFillProducts) {
            calculateFillSuggestions(originalPrice, coupons, fillProducts)
        } else {
            emptyList()
        }

        return ComparisonResult(
            originalPrice = originalPrice,
            directDiscountPrice = directSolution.finalPrice,
            fillSuggestions = fillSuggestions
        )
    }

    fun calculateWithOptionalProducts(
        products: List<Product>,
        coupons: List<Coupon>
    ): List<BundleSolution> {
        val solutions = mutableListOf<BundleSolution>()

        val requiredProducts = products.filter { it.isRequired }
        val optionalProducts = products.filter { !it.isRequired }

        val fullProductIdSet = products.map { it.id }.toSet()

        val subsets = generateSubsets(optionalProducts)

        for (subset in subsets) {
            val currentProducts = requiredProducts + subset
            if (currentProducts.isEmpty()) continue
            if (currentProducts.map { it.id }.toSet() == fullProductIdSet) continue

            val solution = calculateBestCombination(currentProducts, coupons)
            val duplicate = solutions.any { existing ->
                existing.products.map { it.id }.toSet() == solution.products.map { it.id }.toSet()
            }
            if (!duplicate) {
                solutions.add(solution)
            }
        }

        return solutions.sortedBy { it.finalPrice }
    }

    private fun generateSubsets(products: List<Product>): List<List<Product>> {
        if (products.size > 12) {
            val result = mutableListOf<List<Product>>()
            result.add(emptyList())
            for (product in products) {
                val newSubsets = result.map { it + product }
                result.addAll(newSubsets)
                if (result.size > 1024) break
            }
            return result
        }

        val subsets = mutableListOf<List<Product>>()
        subsets.add(emptyList())

        for (product in products) {
            val newSubsets = subsets.map { it + product }
            subsets.addAll(newSubsets)
        }

        return subsets
    }

}
