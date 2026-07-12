package com.example.dealoptimizer.domain.algorithm

import com.example.dealoptimizer.data.model.Coupon
import com.example.dealoptimizer.data.model.CouponType
import com.example.dealoptimizer.data.model.FillProduct
import com.example.dealoptimizer.data.model.Product
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscountCalculatorTest {
    private val calculator = DiscountCalculator()

    @Test
    fun stackableFullReductionUsesWholeOrderThresholdMultiples() {
        val products = listOf(
            Product(id = 1, name = "A", originalPrice = 49.0),
            Product(id = 2, name = "B", originalPrice = 159.0)
        )
        val coupon = Coupon(
            name = "满100减20",
            type = CouponType.FULL_REDUCTION,
            threshold = 100.0,
            discountValue = 20.0,
            isStackable = true
        )

        val solution = calculator.calculateBestCombination(products, listOf(coupon))

        assertEquals(168.0, solution.finalPrice, 0.001)
        assertEquals(1, solution.couponUsages.size)
        assertEquals(2, solution.couponUsages.first().count)
    }

    @Test
    fun nonStackableFullReductionUsesEachCouponForOneQualifiedGroupOnly() {
        val products = listOf(
            Product(id = 1, name = "A", originalPrice = 49.0),
            Product(id = 2, name = "B", originalPrice = 159.0)
        )
        val coupon = Coupon(
            name = "满100减20",
            type = CouponType.FULL_REDUCTION,
            threshold = 100.0,
            discountValue = 20.0,
            isStackable = false
        )

        val solution = calculator.calculateBestCombination(products, listOf(coupon))

        assertEquals(188.0, solution.finalPrice, 0.001)
        assertEquals(1, solution.couponUsages.size)
        assertEquals(1, solution.couponUsages.first().count)
        assertTrue(solution.couponUsages.first().productGroup.sumOf { it.originalPrice } >= 100.0)
    }

    @Test
    fun singleUseNonStackableCouponCanBeUsedByMultipleQualifiedGroupsWhenCountIsUnlimited() {
        val products = listOf(
            Product(id = 1, name = "1", originalPrice = 129.0),
            Product(id = 2, name = "2", originalPrice = 49.0),
            Product(id = 3, name = "3", originalPrice = 159.0)
        )
        val coupon = Coupon(
            name = "100",
            type = CouponType.FULL_REDUCTION,
            threshold = 100.0,
            purchasePrice = 69.0,
            discountValue = 31.0,
            isStackable = false,
            isSingleUse = true
        )

        val solution = calculator.calculateBestCombination(products, listOf(coupon))

        assertEquals(275.0, solution.finalPrice, 0.001)
        assertEquals(2, solution.couponUsages.size)
        assertTrue(solution.couponUsages.all { it.count == 1 })
    }

    @Test
    fun optionalProductSolutionsAlwaysKeepRequiredProducts() {
        val required = Product(id = 1, name = "必买", originalPrice = 80.0, isRequired = true)
        val optionalA = Product(id = 2, name = "可选A", originalPrice = 30.0)
        val optionalB = Product(id = 3, name = "可选B", originalPrice = 60.0)
        val coupon = Coupon(
            name = "满100减20",
            type = CouponType.FULL_REDUCTION,
            threshold = 100.0,
            discountValue = 20.0,
            isStackable = false
        )

        val solutions = calculator.calculateWithOptionalProducts(
            listOf(required, optionalA, optionalB),
            listOf(coupon)
        )

        assertTrue(solutions.isNotEmpty())
        assertTrue(solutions.all { solution -> solution.products.any { it.id == required.id } })
    }

    @Test
    fun combinesLimitedAndUnlimitedNonStackableCouponsForLowestFinalPrice() {
        val products = listOf(
            Product(id = 1, name = "1", originalPrice = 110.0),
            Product(id = 2, name = "2", originalPrice = 120.0),
            Product(id = 3, name = "3", originalPrice = 130.0)
        )
        val limitedBetterCoupon = Coupon(
            name = "满100减50",
            type = CouponType.FULL_REDUCTION,
            threshold = 100.0,
            purchasePrice = 50.0,
            discountValue = 50.0,
            maxUsages = 1,
            isStackable = false,
            isSingleUse = true
        )
        val unlimitedCoupon = Coupon(
            name = "满100减31",
            type = CouponType.FULL_REDUCTION,
            threshold = 100.0,
            purchasePrice = 69.0,
            discountValue = 31.0,
            isStackable = false,
            isSingleUse = true
        )

        val solution = calculator.calculateBestCombination(
            products,
            listOf(limitedBetterCoupon, unlimitedCoupon)
        )

        assertEquals(248.0, solution.finalPrice, 0.001)
        assertEquals(1, solution.couponUsages.count { it.coupon.name == "满100减50" })
        assertEquals(2, solution.couponUsages.count { it.coupon.name == "满100减31" })
    }

    @Test
    fun stackableCouponsCanCombineWithSingleUseGroupsWithoutOwningAGroup() {
        val products = listOf(
            Product(id = 1, name = "1", originalPrice = 129.0),
            Product(id = 2, name = "2", originalPrice = 57.0),
            Product(id = 3, name = "3", originalPrice = 150.0),
            Product(id = 4, name = "4", originalPrice = 98.0)
        )
        val stackableCoupon = Coupon(
            name = "叠满100减50",
            type = CouponType.FULL_REDUCTION,
            threshold = 100.0,
            discountValue = 50.0,
            maxUsages = 1,
            isStackable = true
        )
        val singleUseCoupon = Coupon(
            name = "单满100减50",
            type = CouponType.FULL_REDUCTION,
            threshold = 100.0,
            discountValue = 50.0,
            maxUsages = 1,
            isStackable = false,
            isSingleUse = true
        )

        val solution = calculator.calculateBestCombination(
            products,
            listOf(stackableCoupon, singleUseCoupon)
        )

        assertTrue(solution.couponUsages.any { it.coupon.isStackable && it.productGroup.isEmpty() })
        assertTrue(solution.couponUsages.any { !it.coupon.isStackable && it.productGroup.isNotEmpty() })
    }

    @Test
    fun stackableBetterCouponIsUsedAlongsideSingleUseCoupons() {
        val products = listOf(
            Product(id = 1, name = "1", originalPrice = 129.0),
            Product(id = 2, name = "2", originalPrice = 57.0),
            Product(id = 3, name = "3", originalPrice = 150.0),
            Product(id = 4, name = "4", originalPrice = 98.0)
        )
        val stackableBetterCoupon = Coupon(
            name = "叠·满100减50",
            type = CouponType.FULL_REDUCTION,
            threshold = 100.0,
            discountValue = 50.0,
            maxUsages = 1,
            isStackable = true
        )
        val singleUseCoupon = Coupon(
            name = "单·满100减31",
            type = CouponType.FULL_REDUCTION,
            threshold = 100.0,
            discountValue = 31.0,
            isStackable = false,
            isSingleUse = true
        )

        val solution = calculator.calculateBestCombination(
            products,
            listOf(stackableBetterCoupon, singleUseCoupon)
        )

        assertTrue(solution.couponUsages.any { it.coupon.name == "叠·满100减50" })
        assertTrue(solution.couponUsages.any { it.coupon.name == "单·满100减31" })
        assertTrue(solution.finalPrice < products.sumOf { it.originalPrice } - 31.0)
    }

    @Test
    fun disabledCouponsDoNotParticipateInBestPriceCalculation() {
        val products = listOf(
            Product(id = 1, name = "1", originalPrice = 120.0)
        )
        val disabledBetterCoupon = Coupon(
            name = "单·满100减100",
            type = CouponType.FULL_REDUCTION,
            threshold = 100.0,
            discountValue = 100.0,
            isStackable = false,
            isSingleUse = true,
            isEnabled = false
        )
        val enabledCoupon = Coupon(
            name = "单·满100减10",
            type = CouponType.FULL_REDUCTION,
            threshold = 100.0,
            discountValue = 10.0,
            isStackable = false,
            isSingleUse = true,
            isEnabled = true
        )

        val solution = calculator.calculateBestCombination(
            products,
            listOf(disabledBetterCoupon, enabledCoupon)
        )

        assertEquals(110.0, solution.finalPrice, 0.001)
        assertEquals(listOf("单·满100减10"), solution.couponUsages.map { it.coupon.name })
    }

    @Test
    fun nearThresholdProductsUseGapFillItemsForMoreSingleUseCoupons() {
        val products = listOf(
            Product(id = 1, name = "衣服1", originalPrice = 99.0),
            Product(id = 2, name = "衣服2", originalPrice = 99.0),
            Product(id = 3, name = "衣服3", originalPrice = 99.0)
        )
        val coupon = Coupon(
            name = "单·满100减31",
            type = CouponType.FULL_REDUCTION,
            threshold = 100.0,
            discountValue = 31.0,
            isStackable = false,
            isSingleUse = true
        )
        val fillProduct = FillProduct(
            id = 1,
            name = "小物件",
            price = 1.0,
            category = "凑单"
        )

        val solution = calculator.calculateBestCombination(products, listOf(coupon), listOf(fillProduct))

        assertEquals(3, solution.couponUsages.size)
        assertEquals(207.0, solution.finalPrice, 0.001)
        assertTrue(solution.couponUsages.all { it.productGroup.size == 1 })
        assertTrue(solution.couponUsages.all { it.fillProducts.sumOf { fill -> fill.price } == 1.0 })
    }

    @Test
    fun disabledGapFillDoesNotCreateVirtualOrRealFillItems() {
        val products = listOf(
            Product(id = 1, name = "衣服1", originalPrice = 99.0),
            Product(id = 2, name = "衣服2", originalPrice = 99.0),
            Product(id = 3, name = "衣服3", originalPrice = 99.0)
        )
        val coupon = Coupon(
            name = "单·满100减31",
            type = CouponType.FULL_REDUCTION,
            threshold = 100.0,
            discountValue = 31.0,
            isStackable = false,
            isSingleUse = true
        )
        val fillProduct = FillProduct(
            id = 1,
            name = "小物件",
            price = 1.0,
            category = "凑单"
        )

        val solution = calculator.calculateBestCombination(
            products = products,
            coupons = listOf(coupon),
            fillProducts = listOf(fillProduct),
            useFillProducts = false
        )

        assertEquals(1, solution.couponUsages.size)
        assertTrue(solution.couponUsages.all { it.fillProducts.isEmpty() })
        assertEquals(266.0, solution.finalPrice, 0.001)
    }

    @Test
    fun productGroupsWinWhenTheySaveMoreThanGapFillItems() {
        val products = listOf(
            Product(id = 1, name = "A", originalPrice = 60.0),
            Product(id = 2, name = "B", originalPrice = 60.0)
        )
        val coupon = Coupon(
            name = "单·满100减31",
            type = CouponType.FULL_REDUCTION,
            threshold = 100.0,
            discountValue = 31.0,
            isStackable = false,
            isSingleUse = true
        )

        val solution = calculator.calculateBestCombination(products, listOf(coupon))

        assertEquals(1, solution.couponUsages.size)
        assertEquals(89.0, solution.finalPrice, 0.001)
        assertEquals(2, solution.couponUsages.first().productGroup.size)
        assertTrue(solution.couponUsages.first().fillProducts.isEmpty())
    }

    @Test
    fun greedyPathUsedForMoreThan16ProductsPicksValidCheapestPrice() {
        // 17 件商品触发贪心分支（>16），验证不崩溃且结果合理：
        // 每件 30，合计 510；满100减20，贪心每组 4 件(120) 减 20 → 4 组减 80，finalPrice=430
        val products = (1..17).map { i ->
            Product(id = i.toLong(), name = "P$i", originalPrice = 30.0)
        }
        val coupon = Coupon(
            name = "满100减20",
            type = CouponType.FULL_REDUCTION,
            threshold = 100.0,
            discountValue = 20.0,
            isStackable = false
        )

        val solution = calculator.calculateBestCombination(products, listOf(coupon))

        assertEquals(430.0, solution.finalPrice, 0.001)
        assertEquals(4, solution.couponUsages.size)
    }

    @Test
    fun greedyPathWithFairnessParamDoesNotCrash() {
        // >16 件 + 公平策略：贪心路径接收 fairness 参数且不崩溃，结果不超过原价
        val products = (1..17).map { i ->
            Product(id = i.toLong(), name = "P$i", originalPrice = 30.0, ownerId = if (i <= 8) 1L else 2L)
        }
        val coupon = Coupon(
            name = "满100减20",
            type = CouponType.FULL_REDUCTION,
            threshold = 100.0,
            discountValue = 20.0,
            isStackable = false
        )

        val solution = calculator.calculateBestCombination(
            products, listOf(coupon), multiUserMode = true,
            fairness = DiscountCalculator.FairnessStrategy.FAIR
        )

        assertTrue(solution.finalPrice <= 510.0)
        assertTrue(solution.couponUsages.isNotEmpty())
    }
}
