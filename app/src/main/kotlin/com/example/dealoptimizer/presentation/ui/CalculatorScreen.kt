package com.example.dealoptimizer.presentation.ui

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dealoptimizer.data.model.BundleSolution
import com.example.dealoptimizer.data.model.ComparisonResult
import com.example.dealoptimizer.data.model.Coupon
import com.example.dealoptimizer.data.model.CouponType
import com.example.dealoptimizer.data.model.CouponUsage
import com.example.dealoptimizer.data.model.FillSuggestion
import com.example.dealoptimizer.data.model.MergedShare
import com.example.dealoptimizer.data.model.Product
import com.example.dealoptimizer.data.model.User
import com.example.dealoptimizer.presentation.viewmodel.CalculatorViewModel
import kotlin.math.roundToInt

private data class DiscountSummaryRow(
    val couponId: Long,
    val name: String,
    val count: Int,
    val discount: Double
)

private data class CalculationInputSignature(
    val products: List<Product>,
    val coupons: List<Coupon>,
    val selectedProductIds: Set<Long>,
    val useFillProducts: Boolean
)

private data class MultiUserInputSignature(
    val products: List<Product>,
    val coupons: List<Coupon>,
    val selectedIds: Set<Long>,
    val checkedUserIds: Set<Long>,
    val useFillProducts: Boolean
)

@Composable
fun CalculatorScreen() {
    val viewModel: CalculatorViewModel = hiltViewModel()
    val solution = viewModel.solution.collectAsState().value
    val comparisonResult = viewModel.comparisonResult.collectAsState().value
    val allProducts = viewModel.allProducts.collectAsState().value
    val allCoupons = viewModel.allCoupons.collectAsState().value
    val checkedUsers = viewModel.checkedUsers.collectAsState().value
    val combinedSolution = viewModel.combinedSolution.collectAsState().value
    val shares = viewModel.shares.collectAsState().value
    val incrementalDiscount = viewModel.incrementalDiscount.collectAsState().value
    val perUserSolutions = viewModel.perUserSolutions.collectAsState().value
    val candidateOptions = viewModel.candidateOptions.collectAsState().value
    val selectedCandidateKey = viewModel.selectedCandidateKey.collectAsState().value
    val couponMode = viewModel.couponMode.collectAsState().value
    val modeCoupons = allCoupons.filter { it.isEnabled && it.isStackable == couponMode }
    val vmSelectedIds = viewModel.selectedProductIds.collectAsState().value
    var lastCalculationInput by remember { mutableStateOf<CalculationInputSignature?>(null) }
    var lastMultiUserInput by remember { mutableStateOf<MultiUserInputSignature?>(null) }
    var useFillProducts by remember { mutableStateOf(false) }
    val selectedProductIds = vmSelectedIds
    val selectedProducts = allProducts.filter { it.id in selectedProductIds || it.isRequired }
    // 归属人映射：userId → nickname（用于商品列表展示）
    val ownerNames = checkedUsers.associate { it.id to it.nickname }

    // 仅在从未设置过选中状态且当前有商品时，执行首次全选。
    // 之后用户手动取消/勾选的状态由 ViewModel StateFlow 持久化，切页不丢失、不复盖。
    LaunchedEffect(allProducts) {
        if (vmSelectedIds.isEmpty() && allProducts.isNotEmpty()) {
            viewModel.updateSelectedProductIds(allProducts.map { it.id }.toSet())
        }
    }

    // 单用户场景（无勾选用户）：仅计算单用户方案，避免与多用户计算重复
    LaunchedEffect(selectedProductIds, allProducts, allCoupons, useFillProducts, checkedUsers) {
        if (checkedUsers.isNotEmpty()) return@LaunchedEffect
        val calculationInput = CalculationInputSignature(
            products = allProducts,
            coupons = allCoupons,
            selectedProductIds = selectedProducts.map { it.id }.toSet(),
            useFillProducts = useFillProducts
        )
        if (calculationInput == lastCalculationInput) {
            return@LaunchedEffect
        }
        lastCalculationInput = calculationInput

        if (allProducts.isEmpty() || allCoupons.isEmpty() || selectedProducts.isEmpty()) {
            viewModel.clearCalculation()
        } else {
            viewModel.calculateForProducts(selectedProducts, useFillProducts)
        }
    }

    // 多用户场景（有勾选用户）：仅计算多用户方案
    LaunchedEffect(selectedProductIds, allProducts, allCoupons, useFillProducts, checkedUsers) {
        if (checkedUsers.isEmpty()) return@LaunchedEffect
        val multiUserInput = MultiUserInputSignature(
            products = allProducts,
            coupons = allCoupons,
            selectedIds = selectedProductIds,
            checkedUserIds = checkedUsers.map { it.id }.toSet(),
            useFillProducts = useFillProducts
        )
        if (multiUserInput == lastMultiUserInput) {
            return@LaunchedEffect
        }
        lastMultiUserInput = multiUserInput
        viewModel.calculateMultiUser(useFillProducts)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("凑单计算") },
                backgroundColor = MaterialTheme.colors.surface,
                contentColor = AppInk,
                elevation = 1.dp,
                actions = {
                    IconButton(onClick = { viewModel.calculateForProducts(selectedProducts, useFillProducts) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "重新计算")
                    }
                }
            )
        }
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            val density = LocalDensity.current
            val buttonSize = 56.dp
            val buttonEdgePadding = 16.dp
            val buttonSizePx = with(density) { buttonSize.toPx() }
            val maxButtonX = (with(density) { maxWidth.toPx() } - buttonSizePx).coerceAtLeast(0f)
            val maxButtonY = (with(density) { maxHeight.toPx() } - buttonSizePx).coerceAtLeast(0f)
            val startButtonOffset = with(density) {
                Offset(
                    x = (maxWidth.toPx() - buttonSizePx - buttonEdgePadding.toPx()).coerceIn(0f, maxButtonX),
                    y = (maxHeight.toPx() / 3f - buttonSizePx / 2f).coerceIn(0f, maxButtonY)
                )
            }
            var fillButtonOffset by remember { mutableStateOf<Offset?>(null) }

            LaunchedEffect(maxButtonX, maxButtonY) {
                val hasValidButtonBounds = maxButtonX > 0f && maxButtonY > 0f
                if (!hasValidButtonBounds) return@LaunchedEffect

                fillButtonOffset = fillButtonOffset
                    ?.coerceInBounds(maxButtonX, maxButtonY)
                    ?: startButtonOffset
            }

            var tabIndex by remember { mutableStateOf(0) }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (checkedUsers.isNotEmpty()) {
                    items(checkedUsers) { user ->
                        val userProducts = allProducts.filter { it.ownerId == user.id }
                        ProductSelectionPanel(
                            user = user,
                            products = userProducts,
                            coupons = allCoupons,
                            selectedProductIds = selectedProductIds,
                            onToggle = { product ->
                                if (product.isRequired) return@ProductSelectionPanel
                                viewModel.updateSelectedProductIds(
                                    if (product.id in selectedProductIds) selectedProductIds - product.id
                                    else selectedProductIds + product.id
                                )
                            },
                            onToggleSelectAll = { checked ->
                                val optionalIds = userProducts.filter { !it.isRequired }.map { it.id }.toSet()
                                viewModel.updateSelectedProductIds(
                                    if (checked) selectedProductIds + optionalIds
                                    else selectedProductIds - optionalIds
                                )
                            }
                        )
                    }
                } else if (allProducts.isNotEmpty()) {
                    item {
                        ProductSelectionPanel(
                            user = null,
                            products = allProducts,
                            coupons = allCoupons,
                            selectedProductIds = selectedProductIds,
                            onToggle = { product ->
                                if (product.isRequired) return@ProductSelectionPanel
                                viewModel.updateSelectedProductIds(
                                    if (product.id in selectedProductIds) selectedProductIds - product.id
                                    else selectedProductIds + product.id
                                )
                            },
                            onToggleSelectAll = { checked ->
                                val optionalIds = allProducts.filter { !it.isRequired }.map { it.id }.toSet()
                                viewModel.updateSelectedProductIds(
                                    if (checked) selectedProductIds + optionalIds
                                    else selectedProductIds - optionalIds
                                )
                            }
                        )
                    }
                }

                // 多用户方案对比：现状 vs 全局，二选一应用（单用券场景才展示，叠加券不展示）
                if (!couponMode && checkedUsers.isNotEmpty() && candidateOptions.size >= 2) {
                    item {
                        CandidateCompareCard(
                            candidates = candidateOptions,
                            selectedKey = selectedCandidateKey,
                            users = checkedUsers,
                            onSelect = { viewModel.selectCandidate(it) }
                        )
                    }
                }

                // 多用户场景：Tab 切换（汇总 / 分摊说明），叠加券与单用券统一布局
                if (checkedUsers.isNotEmpty() && combinedSolution != null) {
                    val merged = combinedSolution
                    item {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            TabLabel(
                                text = "汇总",
                                selected = tabIndex == 0,
                                onClick = { tabIndex = 0 },
                                modifier = Modifier.weight(1f)
                            )
                            TabLabel(
                                text = "分摊说明",
                                selected = tabIndex == 1,
                                onClick = { tabIndex = 1 },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    if (tabIndex == 0) {
                        // 汇总：只显示合并主卡（含每人应付）
                        item {
                            SummaryCard(
                                combined = merged,
                                incremental = incrementalDiscount,
                                originalColor = if (couponMode) {
                                    val og = priceGapColor(merged.originalTotal, modeCoupons)
                                    if (og == AppInk) Color.White else og
                                } else Color.White,
                                priceColor = Color.White,
                                users = checkedUsers,
                                shares = shares
                            )
                        }
                    } else {
                        // 分摊说明
                        if (couponMode) {
                            // 叠加券：浅蓝色说明区
                            item {
                                PerUserShareCard(
                                    users = checkedUsers,
                                    shares = shares,
                                    perUserSolutions = perUserSolutions,
                                    combined = merged,
                                    incremental = incrementalDiscount
                                )
                            }
                        }
                        if (!couponMode) {
                            // 单用券：按用户视角展示分摊说明
                            // 每个勾选用户一张块（浅蓝）：仅含自达标组 + 单独付
                            // 凑单组（多人拼券）统一下沉到最底部绿色区
                            val groupedUsages = merged.couponUsages.filter { !it.coupon.isStackable }
                            items(checkedUsers.size) { idx ->
                                PerUserCouponShareCard(
                                    user = checkedUsers[idx],
                                    allUsages = groupedUsages,
                                    allProducts = merged.products,
                                    coupons = allCoupons
                                )
                            }
                            // 底部：凑单组合（多人拼券）
                            val poolOrNoOwnerUsages = groupedUsages.filter { usage ->
                                val owners = usage.productGroup.map { it.ownerId }.distinct()
                                owners.size >= 2 || owners.isEmpty()
                            }
                            if (poolOrNoOwnerUsages.isNotEmpty()) {
                                item {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 14.dp)
                                            .border(1.dp, Color(0xFFA7E0C0), RoundedCornerShape(10.dp)),
                                        color = Color(0xFFE8F8EE),
                                        shape = RoundedCornerShape(10.dp),
                                        elevation = 1.dp
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "凑单组合（多人拼券）",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF1F9254)
                                                )
                                                Spacer(modifier = Modifier.weight(1f))
                                                Text(text = "按组合看谁和谁凑", fontSize = 11.sp, color = AppMuted)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            poolOrNoOwnerUsages.forEachIndexed { pIdx, usage ->
                                                GroupCombinationCard(
                                                    groupIndex = pIdx,
                                                    usage = usage,
                                                    coupons = allCoupons,
                                                    ownerNames = ownerNames,
                                                    backgroundColor = Color(0xFFF2FCF6)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (solution != null) {
                    item {
                        SolutionCard(
                            solution = solution,
                            isBest = true,
                            coupons = allCoupons,
                            priceColor = AppInk,
                            ownerNames = ownerNames
                        )
                    }

                    if (comparisonResult != null && comparisonResult.fillSuggestions.isNotEmpty()) {
                        item {
                            Text(
                                text = "凑单建议",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        items(comparisonResult.fillSuggestions) { suggestion ->
                            FillSuggestionCard(suggestion)
                        }
                    }
                }

                if (solution == null) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            val emptyMessage = when {
                                allProducts.isEmpty() && allCoupons.isEmpty() -> "请添加商品和优惠券"
                                allProducts.isEmpty() -> "请添加商品"
                                allCoupons.isEmpty() -> "请添加优惠券"
                                selectedProducts.isEmpty() -> "请至少选择一件商品"
                                else -> "暂无可用方案"
                            }
                            Text(text = emptyMessage, color = AppMuted)
                        }
                    }
                }
            }

            fillButtonOffset?.let { currentOffset ->
                FillProductFloatingButton(
                    checked = useFillProducts,
                    onClick = {
                        useFillProducts = !useFillProducts
                    },
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                currentOffset.x.roundToInt(),
                                currentOffset.y.roundToInt()
                            )
                        }
                        .pointerInput(maxButtonX, maxButtonY) {
                            detectDragGesturesAfterLongPress(
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val latestOffset = fillButtonOffset ?: currentOffset
                                    fillButtonOffset = Offset(
                                        x = latestOffset.x + dragAmount.x,
                                        y = latestOffset.y + dragAmount.y
                                    ).coerceInBounds(maxButtonX, maxButtonY)
                                }
                            )
                        }
                )
            }
        }
    }
}

@Composable
private fun CandidateCompareCard(
    candidates: List<CalculatorViewModel.MultiUserSolution>,
    selectedKey: String?,
    users: List<User>,
    onSelect: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "多用户凑单方案",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            candidates.forEach { c ->
                val selected = c.key == selectedKey
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(c.key) },
                    shape = RoundedCornerShape(10.dp),
                    elevation = 1.dp,
                    border = if (selected) BorderStroke(1.5.dp, AppSecondaryBlue) else null,
                    backgroundColor = if (selected) Color(0xFFEAF3FF) else MaterialTheme.colors.surface
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(text = c.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AppInk)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "应付 ¥${"%.2f".format(c.combined.finalPrice)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) AppSecondaryBlue else AppInk
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        users.forEach { u ->
                            val s = c.shares[u.id]
                            if (s != null) {
                                Text(
                                    text = "${u.nickname} ¥${"%.2f".format(s.payable)}",
                                    fontSize = 12.sp,
                                    color = AppMuted
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (selected) "已选用" else "点击选用",
                            fontSize = 11.sp,
                            color = if (selected) AppSecondaryBlue else AppMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FillProductFloatingButton(
    checked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        shape = CircleShape,
        modifier = modifier.size(56.dp),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if (checked) AppGreen else AppMuted,
            contentColor = Color.White
        ),
        elevation = ButtonDefaults.elevation(defaultElevation = 8.dp, pressedElevation = 4.dp)
    ) {
        Text(
            text = "凑",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
    }
}

@Composable
fun ProductSelectionPanel(
    user: User?,
    selectedProductIds: Set<Long>,
    products: List<Product>,
    coupons: List<Coupon>,
    onToggle: (Product) -> Unit,
    onToggleSelectAll: ((Boolean) -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = 1.dp,
        backgroundColor = MaterialTheme.colors.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = user?.nickname ?: "选择参与计算的商品",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colors.primary
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "取消勾选即舍弃该商品，必买商品会始终保留",
                    fontSize = 12.sp,
                    color = AppMuted,
                    modifier = Modifier.weight(1f)
                )
                // 可选商品非空时显示全选勾选框：勾选=全选，取消勾选=取消全选
                val optionalProducts = products.filter { !it.isRequired }
                if (optionalProducts.isNotEmpty() && onToggleSelectAll != null) {
                    val allSelected = optionalProducts.all { it.id in selectedProductIds }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onToggleSelectAll(!allSelected) }
                    ) {
                        Checkbox(
                            checked = allSelected,
                            onCheckedChange = { onToggleSelectAll(it) },
                            colors = CheckboxDefaults.colors(checkedColor = AppBlue)
                        )
                        Text(
                            text = "全选",
                            fontSize = 12.sp,
                            color = AppBlue,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (products.isEmpty()) {
                Text(
                    text = "该用户暂无商品",
                    color = AppMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                products.chunked(2).forEach { rowProducts ->
                    Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowProducts.forEach { product ->
                        ProductSelectionItem(
                            product = product,
                            checked = product.isRequired || product.id in selectedProductIds,
                            textColor = product.displayColor(coupons),
                            onToggle = onToggle,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowProducts.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun ProductSelectionItem(
    product: Product,
    checked: Boolean,
    textColor: Color,
    onToggle: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            enabled = !product.isRequired,
            onCheckedChange = { onToggle(product) },
            modifier = Modifier.size(36.dp)
        )
        Column(modifier = Modifier.padding(start = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = product.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                if (product.isRequired) {
                    Text(
                        text = " 必买",
                        fontSize = 11.sp,
                        color = MaterialTheme.colors.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text = "¥${"%.2f".format(product.originalPrice)}",
                fontSize = 12.sp,
                color = textColor
            )
        }
    }
}

// 必买商品强制红（优先级最高），否则默认黑色
@Suppress("UNUSED_PARAMETER")
private fun Product.displayColor(_coupons: List<Coupon>): Color =
    if (isRequired) AppRed else AppInk

// 叠加券模式下，实付款距最近门槛（或其倍数）差距：≤20 红、≤40 黄
private fun priceGapColor(finalPrice: Double, coupons: List<Coupon>): Color {
    val thresholds = coupons.filter { it.type == CouponType.FULL_REDUCTION && it.threshold > 0.0 }
        .map { it.threshold }
    if (thresholds.isEmpty()) return AppInk
    val minThreshold = thresholds.minOrNull() ?: return AppInk
    // 找大于等于实付款的最小门槛倍数，算差距
    val nextThreshold = ((finalPrice / minThreshold).toInt() + 1) * minThreshold
    val gap = nextThreshold - finalPrice
    return when {
        gap <= 20.0 -> AppRed
        gap <= 40.0 -> AppAmber
        else -> AppInk
    }
}

private fun Offset.coerceInBounds(maxX: Float, maxY: Float): Offset {
    return Offset(
        x = x.coerceIn(0f, maxX),
        y = y.coerceIn(0f, maxY)
    )
}

@Composable
fun SolutionCard(
    solution: BundleSolution,
    isBest: Boolean,
    coupons: List<Coupon> = emptyList(),
    priceColor: Color = AppInk,
    ownerNames: Map<Long, String> = emptyMap()
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = if (isBest) 3.dp else 1.dp,
        backgroundColor = MaterialTheme.colors.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            BillSummary(solution = solution, priceColor = priceColor)
            val groupedCouponUsages = solution.couponUsages.filter { !it.coupon.isStackable }

            if (groupedCouponUsages.isNotEmpty()) {
                Text(text = "商品分组", fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
                
                groupedCouponUsages.forEachIndexed { groupIndex, usage ->
                    val productTotal = usage.productGroup.sumOf { it.originalPrice }
                    val fillTotal = usage.fillProducts.sumOf { it.price }
                    val groupTotal = productTotal + fillTotal
                    val discount = when (usage.coupon.type) {
                        com.example.dealoptimizer.data.model.CouponType.FULL_REDUCTION -> usage.count * usage.coupon.discountValue
                        com.example.dealoptimizer.data.model.CouponType.DISCOUNT -> groupTotal * (usage.coupon.discountValue / 100) * usage.count
                        com.example.dealoptimizer.data.model.CouponType.NO_THRESHOLD -> usage.count * usage.coupon.discountValue
                    }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(8.dp),
                        backgroundColor = AppSurface,
                        elevation = 0.dp
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // 标题行：组名 + 商品总价
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "第${groupIndex + 1}组 · 单独结账",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colors.primary
                                )
                                Text(
                                    text = "商品总价: ¥${"%.2f".format(productTotal)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // 券信息（右侧对齐）
                            Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "${usage.coupon.displayNameWithUsageMode()} x${usage.count}",
                                    fontSize = 12.sp,
                                    color = AppRed,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                Text(text = "-¥${"%.2f".format(discount)}", fontSize = 12.sp)
                            }

                            // 商品列表
                            if (usage.productGroup.isNotEmpty()) {
                                Text(
                                    text = "商品",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                                usage.productGroup.forEach { product ->
                                    val requiredBadge = if (product.isRequired) " · 必买" else ""
                                    val ownerPrefix = ownerNames[product.ownerId]?.let { "$it · " } ?: ""
                                    Text(
                                        text = "${ownerPrefix}${product.name}$requiredBadge: ¥${"%.2f".format(product.originalPrice)}",
                                        fontSize = 12.sp,
                                        color = product.displayColor(coupons),
                                        modifier = Modifier.padding(top = 1.dp)
                                    )
                                }
                            }

                            if (usage.fillProducts.isNotEmpty()) {
                                Text(
                                    text = "凑单: 凑单小物 ¥${"%.2f".format(fillTotal)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 4.dp),
                                    color = AppGreen
                                )
                            }

                            // 本组应付
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text(text = "本组应付:", fontSize = 12.sp)
                                Text(
                                    text = "¥${"%.2f".format(groupTotal - discount)}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppRed
                                )
                            }

                            // 分摊信息
                            val groupOwnerIds = usage.productGroup.map { it.ownerId }.distinct()
                            if (groupOwnerIds.isNotEmpty()) {
                                Divider(modifier = Modifier.padding(top = 8.dp), color = AppLine.copy(alpha = 0.5f))
                                if (groupOwnerIds.size > 1) {
                                    val groupOriginalTotal = usage.productGroup.sumOf { it.originalPrice }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        groupOwnerIds.forEach { ownerId ->
                                            val ownerOriginal = usage.productGroup.filter { it.ownerId == ownerId }.sumOf { it.originalPrice }
                                            val w = if (groupOriginalTotal > 0) ownerOriginal / groupOriginalTotal else 0.0
                                            val sharePayable = (groupTotal - discount) * w
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = ownerNames[ownerId] ?: "",
                                                    fontSize = 11.sp,
                                                    color = AppMuted
                                                )
                                                Text(
                                                    text = "¥${"%.2f".format(sharePayable)}",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = ownerNames[groupOwnerIds.first()] ?: "",
                                            fontSize = 11.sp,
                                            color = AppMuted
                                        )
                                        Text(
                                            text = "¥${"%.2f".format(groupTotal - discount)}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                if (solution.products.size > groupedCouponUsages.sumOf { it.productGroup.size }) {
                    val usedProductIds = groupedCouponUsages.flatMap { it.productGroup }.map { it.id }.toSet()
                    val remainingProducts = solution.products.filter { !usedProductIds.contains(it.id) }
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(8.dp),
                        backgroundColor = AppDangerSurface,
                        elevation = 0.dp
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "剩余商品 · 不使用优惠券",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppMuted
                            )
                            
                            remainingProducts.forEach { product ->
                                val requiredBadge = if (product.isRequired) " · 必买" else ""
                                val ownerPrefix = ownerNames[product.ownerId]?.let { "$it · " } ?: ""
                                Text(
                                    text = "${ownerPrefix}${product.name}$requiredBadge: ¥${"%.2f".format(product.originalPrice)}",
                                    fontSize = 12.sp,
                                    color = product.displayColor(coupons),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            
                            val remainingTotal = remainingProducts.sumOf { it.originalPrice }
                            Text(
                                text = "应付: ¥${"%.2f".format(remainingTotal)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppMuted,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            } else {
                Text(text = "商品清单", fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
                solution.products.forEach { product ->
                    val requiredBadge = if (product.isRequired) " · 必买" else ""
                    val ownerPrefix = ownerNames[product.ownerId]?.let { "$it · " } ?: ""
                    Text(
                        text = "  • ${ownerPrefix}${product.name}$requiredBadge: ¥${"%.2f".format(product.originalPrice)}",
                        fontSize = 14.sp,
                        color = product.displayColor(coupons)
                    )
                }
            }
            
        }
    }
}

@Composable
fun BillSummary(solution: BundleSolution, priceColor: Color = AppInk) {
    val itemCount = solution.products.size
    val discountRows = solutionDiscountRows(solution)

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("商品总价", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = AppInk)
                Spacer(modifier = Modifier.width(8.dp))
                Text("共${itemCount}件", fontSize = 16.sp, color = AppMuted)
            }
            Text("¥${"%.2f".format(solution.originalTotal)}", fontWeight = FontWeight.Bold, fontSize = 28.sp, color = AppInk)
        }

        Divider(modifier = Modifier.padding(vertical = 14.dp), color = AppLine)

        if (discountRows.isEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("无优惠", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppInk)
                Text("-¥0.00", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = AppWarnText)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                discountRows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = buildAnnotatedString {
                                append(row.name)
                                append(" ")
                                withStyle(SpanStyle(color = AppWarnText)) {
                                    append("x${row.count}")
                                }
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = AppInk,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp)
                        )
                        Text("-¥${"%.2f".format(row.discount)}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = AppWarnText)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("实付款", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = AppInk)
                if (solution.totalDiscount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("共减¥${"%.2f".format(solution.totalDiscount)}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppWarnText)
                }
            }
            Text("¥${"%.2f".format(solution.finalPrice)}", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = priceColor)
        }
    }
}

@Composable
fun FillSuggestionCard(suggestion: FillSuggestion) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = 1.dp,
        backgroundColor = MaterialTheme.colors.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "凑单方案", fontWeight = FontWeight.Bold)
            
            Text(text = "当前商品总价: ¥${"%.2f".format(suggestion.originalTotal)}", fontSize = 14.sp, color = AppMuted, modifier = Modifier.padding(top = 8.dp))
            
            val fillTotal = suggestion.fillProducts.sumOf { it.price }
            Text(text = "建议加购: 凑单小物 ¥${"%.2f".format(fillTotal)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colors.primary)
            
            Row(modifier = Modifier.padding(top = 12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "原总价", fontSize = 12.sp, color = AppMuted)
                    Text(text = "¥${"%.2f".format(suggestion.originalTotal)}", fontSize = 14.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "凑单后", fontSize = 12.sp, color = AppMuted)
                    Text(text = "¥${"%.2f".format(suggestion.newTotal)}", fontSize = 14.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "最终价", fontSize = 12.sp, color = AppMuted)
                    Text(
                        text = "¥${"%.2f".format(suggestion.finalPrice)}",
                        fontSize = 14.sp,
                        color = AppRed
                    )
                }
            }
            
            Text(
                text = "净节省: ¥${"%.2f".format(suggestion.saving)}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = AppGreen,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Composable
fun SummaryCard(
    combined: BundleSolution,
    incremental: Double = 0.0,
    originalColor: Color = Color.White,
    priceColor: Color = Color.White,
    users: List<User> = emptyList(),
    shares: Map<Long, MergedShare> = emptyMap()
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = 4.dp,
        backgroundColor = AppBlue
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val itemCount = combined.products.size
            val discountRows = solutionDiscountRows(combined)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("商品总价", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("共${itemCount}件", fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f))
                }
                Text("¥${"%.2f".format(combined.originalTotal)}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = originalColor)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = Color.White.copy(alpha = 0.18f))
            Spacer(modifier = Modifier.height(8.dp))

            if (discountRows.isEmpty()) {
                Text("无优惠", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
            } else {
                discountRows.forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = buildAnnotatedString {
                                append(row.name)
                                append(" ")
                                withStyle(SpanStyle(color = Color(0xFFFFE0B2))) {
                                    append("x${row.count}")
                                }
                            },
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Text(
                            text = "-¥${"%.2f".format(row.discount)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFE0B2)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = Color.White.copy(alpha = 0.18f))
            Spacer(modifier = Modifier.height(8.dp))

            // 方案C：标注合并是否产生额外优惠（仅增量>0时显示）
            if (incremental > 0.01) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "合并额外节省 ¥${"%.2f".format(incremental)}",
                        fontSize = 12.sp,
                        color = Color(0xFFA5D6A7),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("实付款", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                    if (combined.totalDiscount > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "共减¥${"%.2f".format(combined.totalDiscount)}",
                            fontSize = 14.sp,
                            color = Color(0xFFFFE0B2),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = "¥${"%.2f".format(combined.finalPrice)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = priceColor
                )
            }

            // 每人应付（叠加券多用户场景，放在实付款下方）
            if (users.isNotEmpty() && shares.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = Color.White.copy(alpha = 0.18f))
                Spacer(modifier = Modifier.height(8.dp))
                users.forEachIndexed { idx, u ->
                    val share = shares[u.id]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = u.nickname,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                        Text(
                            text = "¥${"%.2f".format(share?.ownOriginal ?: 0.0)} − ¥${"%.2f".format(share?.discount ?: 0.0)}",
                            fontSize = 13.sp,
                            color = Color.White,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "¥${"%.2f".format(share?.payable ?: 0.0)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF93C5FD)
                        )
                    }
                    if (idx < users.size - 1) Divider(
                        color = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

private fun solutionDiscountRows(solution: BundleSolution): List<DiscountSummaryRow> {
    return solution.couponUsages
        .groupBy { it.coupon.id }
        .map { (couponId, usages) ->
            val coupon = usages.first().coupon
            val count = usages.sumOf { it.count }
            val discount = usages.sumOf { usage ->
                val groupTotal = if (usage.coupon.isStackable) {
                    solution.originalTotal
                } else {
                    usage.productGroup.sumOf { it.originalPrice } + usage.fillProducts.sumOf { it.price }
                }
                when (usage.coupon.type) {
                    CouponType.FULL_REDUCTION -> usage.count * usage.coupon.discountValue
                    CouponType.DISCOUNT -> groupTotal * (usage.coupon.discountValue / 100) * usage.count
                    CouponType.NO_THRESHOLD -> usage.count * usage.coupon.discountValue
                }
            }
            DiscountSummaryRow(couponId = couponId, name = coupon.displayNameWithUsageMode(), count = count, discount = discount)
        }
}

@Composable
fun PerUserShareCard(
    users: List<User>,
    shares: Map<Long, MergedShare>,
    perUserSolutions: Map<Long, BundleSolution>,
    combined: BundleSolution?,
    incremental: Double = 0.0
) {
    val hasIncremental = incremental > 0.01
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .border(1.dp, Color(0xFFB5D4F4), RoundedCornerShape(10.dp)),
        color = Color(0xFFE8F4FC),
        shape = RoundedCornerShape(10.dp),
        elevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 结论卡：多省了 ¥XX
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().border(0.5.dp, Color(0xFFB5D4F4), RoundedCornerShape(8.dp))
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (hasIncremental) "多省了 ¥${"%.2f".format(incremental)}" else "合并无额外节省",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppSecondaryBlue
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    // 单独算推导（标签 + 合计）
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        users.forEach { u ->
                            val sol = perUserSolutions[u.id]
                            val ownOriginal = shares[u.id]?.ownOriginal ?: 0.0
                            val ownDiscount = sol?.totalDiscount ?: 0.0
                            Box(
                                modifier = Modifier
                                    .border(0.5.dp, Color(0xFF7F77DD), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (ownDiscount > 0.01)
                                        "${u.nickname} ¥${"%.0f".format(ownOriginal)}−¥${"%.0f".format(ownDiscount)}"
                                    else
                                        "${u.nickname} ¥${"%.0f".format(ownOriginal)}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF534AB7)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    val sumPerUserPayable = users.sumOf { perUserSolutions[it.id]?.finalPrice ?: (shares[it.id]?.ownOriginal ?: 0.0) }
                    val sumPerUserDiscount = users.sumOf { perUserSolutions[it.id]?.totalDiscount ?: 0.0 }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "单独合计 ", fontSize = 13.sp, color = AppInk)
                        Text(text = "¥${"%.2f".format(sumPerUserPayable)}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AppInk)
                        Text(text = "（总优惠 ¥${"%.2f".format(sumPerUserDiscount)}）", fontSize = 12.sp, color = AppRed, modifier = Modifier.padding(start = 4.dp))
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = Color(0xFFBBBBBB).copy(alpha = 0.5f), thickness = 0.5.dp)

                    // 合并凑单（标签 + 合计）
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        users.forEach { u ->
                            val ownOriginal = shares[u.id]?.ownOriginal ?: 0.0
                            Box(
                                modifier = Modifier
                                    .border(0.5.dp, Color(0xFF7F77DD), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${u.nickname} ¥${"%.0f".format(ownOriginal)}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF534AB7)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "合并凑单 ", fontSize = 13.sp, color = AppInk)
                        Text(text = "¥${"%.2f".format(combined?.finalPrice ?: 0.0)}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AppInk)
                        Text(text = "（总优惠 ¥${"%.2f".format(combined?.totalDiscount ?: 0.0)}）", fontSize = 12.sp, color = AppRed, modifier = Modifier.padding(start = 4.dp))
                    }

                    if (hasIncremental) {
                        Spacer(modifier = Modifier.height(10.dp))
                        // 占比框：这 ¥XX 按剩余金额占比
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().border(0.5.dp, Color(0xFFCCCCCC), RoundedCornerShape(8.dp))
                        ) {
                            Column(modifier = Modifier.padding(10.dp, 14.dp)) {
                                Text(
                                    text = "这 ¥${"%.2f".format(incremental)} 按剩余金额占比：",
                                    fontSize = 12.sp,
                                    color = AppMuted,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                users.forEach { u ->
                                    val share = shares[u.id]
                                    val remaining = share?.remaining ?: 0.0
                                    val totalRemaining = users.sumOf { shares[it.id]?.remaining ?: 0.0 }
                                    val pct = if (totalRemaining > 0) remaining / totalRemaining * 100 else 0.0
                                    val sol = perUserSolutions[u.id]
                                    val ownDiscount = sol?.totalDiscount ?: 0.0
                                    val incrementalShare = (share?.discount ?: 0.0) - ownDiscount
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = u.nickname, fontSize = 12.sp, color = AppInk, modifier = Modifier.width(50.dp))
                                        Text(
                                            text = "¥${"%.0f".format(remaining)} 占 ${"%.1f".format(pct)}%",
                                            fontSize = 12.sp,
                                            color = AppInk,
                                            modifier = Modifier.weight(1f),
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "-¥${"%.2f".format(incrementalShare)}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = AppRed,
                                            modifier = Modifier.width(48.dp),
                                            textAlign = TextAlign.End
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier.fillMaxWidth().height(8.dp).background(Color(0xFFEEEEEE), RoundedCornerShape(4.dp))
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxWidth((pct / 100f).toFloat()).height(8.dp).background(Color(0xFF7F77DD), RoundedCornerShape(4.dp))
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

@Composable
private fun TabLabel(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = if (selected) AppBlue else AppMuted
        )
        Spacer(modifier = Modifier.height(4.dp))
        Divider(
            color = if (selected) AppBlue else Color.Transparent,
            thickness = 2.dp
        )
    }
}

@Composable
private fun GroupCombinationCard(
    groupIndex: Int,
    usage: CouponUsage,
    coupons: List<Coupon>,
    ownerNames: Map<Long, String> = emptyMap(),
    backgroundColor: Color = Color.White
) {
    val productTotal = usage.productGroup.sumOf { it.originalPrice }
    val fillTotal = usage.fillProducts.sumOf { it.price }
    val groupTotal = productTotal + fillTotal
    val discount = when (usage.coupon.type) {
        CouponType.FULL_REDUCTION -> usage.count * usage.coupon.discountValue
        CouponType.DISCOUNT -> groupTotal * (usage.coupon.discountValue / 100) * usage.count
        CouponType.NO_THRESHOLD -> usage.count * usage.coupon.discountValue
    }
    val groupOwnerIds = usage.productGroup.map { it.ownerId }.distinct()
    val isPool = groupOwnerIds.size >= 2
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .border(1.dp, AppLine.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        backgroundColor = backgroundColor,
        elevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "第${groupIndex + 1}组",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                val typeLabel = if (isPool) "凑单组 · ${groupOwnerIds.size}人" else "单人组 · 自达标"
                val typeColor = if (isPool) AppGreen else AppBlue
                Text(
                    text = typeLabel,
                    fontSize = 12.sp,
                    color = typeColor,
                    modifier = Modifier
                        .background(typeColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))

            // 左商品清单 / 右价格摘要（并排，压缩高度）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (usage.productGroup.isNotEmpty()) {
                        usage.productGroup.forEach { product ->
                            val requiredBadge = if (product.isRequired) " · 必买" else ""
                            val ownerPrefix = ownerNames[product.ownerId]?.let { "$it · " } ?: ""
                            Text(
                                text = "${ownerPrefix}${product.name}$requiredBadge: ¥${"%.2f".format(product.originalPrice)}",
                                fontSize = 12.sp,
                                color = product.displayColor(coupons),
                                modifier = Modifier.padding(top = 1.dp)
                            )
                        }
                    }
                    if (usage.fillProducts.isNotEmpty()) {
                        Text(
                            text = "凑单: ¥${"%.2f".format(fillTotal)}",
                            fontSize = 12.sp,
                            color = AppGreen,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.widthIn(min = 110.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "商品总价: ¥${"%.2f".format(productTotal)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${usage.coupon.displayNameWithUsageMode()} x${usage.count}",
                        fontSize = 12.sp,
                        color = AppRed,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(text = "-¥${"%.2f".format(discount)}", fontSize = 12.sp)
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(text = "本组应付:", fontSize = 12.sp)
                        Text(
                            text = "¥${"%.2f".format(groupTotal - discount)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppRed
                        )
                    }
                }
            }

            // 仅当分组含 2 人及以上（凑单组）时，展示各用户分摊
            if (isPool) {
                Divider(modifier = Modifier.padding(top = 8.dp), color = AppLine.copy(alpha = 0.5f))
                val groupOriginalTotal = usage.productGroup.sumOf { it.originalPrice }
                Text(
                    text = "组内分摊（按原价占比）",
                    fontSize = 12.sp,
                    color = AppMuted,
                    modifier = Modifier.padding(top = 8.dp)
                )
                groupOwnerIds.forEach { ownerId ->
                    val ownerOriginal = usage.productGroup.filter { it.ownerId == ownerId }.sumOf { it.originalPrice }
                    val w = if (groupOriginalTotal > 0) ownerOriginal / groupOriginalTotal else 0.0
                    val ownerDiscount = discount * w
                    val ownerPayable = ownerOriginal - ownerDiscount
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = ownerNames[ownerId] ?: "",
                                fontSize = 12.sp,
                                color = AppInk
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "¥${"%.2f".format(ownerOriginal)} 占 ${"%.1f".format(w * 100)}%",
                                    fontSize = 12.sp,
                                    color = AppMuted
                                )
                                Text(
                                    text = " -¥${"%.2f".format(ownerDiscount)}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppInk,
                                    modifier = Modifier.padding(start = 6.dp)
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 3.dp)
                                .height(5.dp)
                                .background(AppLine.copy(alpha = 0.4f), RoundedCornerShape(3.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(w.toFloat())
                                    .height(5.dp)
                                    .background(AppBlue, RoundedCornerShape(3.dp))
                            )
                        }
                        Text(
                            text = "应付 ¥${"%.2f".format(ownerPayable)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppRed,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}

/** 单用券场景下，某张券用法的总优惠（与 GroupCombinationCard 内口径一致） */
private fun usageDiscount(usage: CouponUsage): Double {
    val productTotal = usage.productGroup.sumOf { it.originalPrice }
    val fillTotal = usage.fillProducts.sumOf { it.price }
    val groupTotal = productTotal + fillTotal
    return when (usage.coupon.type) {
        CouponType.FULL_REDUCTION -> usage.count * usage.coupon.discountValue
        CouponType.DISCOUNT -> groupTotal * (usage.coupon.discountValue / 100) * usage.count
        CouponType.NO_THRESHOLD -> usage.count * usage.coupon.discountValue
    }
}

/**
 * 按用户视角的分摊块（浅蓝）：
 * 块头 = 该用户实付（含凑单分摊，scheme A）；
 * 块内仅展示「自达标组」+「单独付」，凑单组统一下沉到底部凑单组合区。
 */
@Composable
private fun PerUserCouponShareCard(
    user: User,
    allUsages: List<CouponUsage>,
    allProducts: List<Product>,
    coupons: List<Coupon>
) {
    val usedProductIds = allUsages.flatMap { it.productGroup }.map { it.id }.toSet()
    val selfGroups = allUsages.filter { usage ->
        val owners = usage.productGroup.map { it.ownerId }.distinct()
        owners.size == 1 && owners[0] == user.id
    }
    val poolGroups = allUsages.filter { usage ->
        val owners = usage.productGroup.map { it.ownerId }.distinct()
        owners.size >= 2 && usage.productGroup.any { it.ownerId == user.id }
    }
    val separateProducts = allProducts.filter {
        it.ownerId == user.id && it.id !in usedProductIds
    }

    // 与该用户完全无关的块不渲染
    if (selfGroups.isEmpty() && poolGroups.isEmpty() && separateProducts.isEmpty()) return

    // 汇总（含凑单分摊）
    var totalOriginal = 0.0
    var totalDiscount = 0.0
    var poolDiscount = 0.0
    selfGroups.forEach { usage ->
        val dt = usageDiscount(usage)
        val og = usage.productGroup.sumOf { it.originalPrice } + usage.fillProducts.sumOf { it.price }
        totalOriginal += og
        totalDiscount += dt
    }
    separateProducts.forEach { p -> totalOriginal += p.originalPrice }
    poolGroups.forEach { usage ->
        val dt = usageDiscount(usage)
        val groupOriginalTotal = usage.productGroup.sumOf { it.originalPrice }
        val userOriginal = usage.productGroup.filter { it.ownerId == user.id }.sumOf { it.originalPrice }
        val w = if (groupOriginalTotal > 0) userOriginal / groupOriginalTotal else 0.0
        val userDiscount = dt * w
        totalOriginal += userOriginal
        totalDiscount += userDiscount
        poolDiscount += userDiscount
    }
    val totalPayable = totalOriginal - totalDiscount

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .border(1.dp, Color(0xFFB5D4F4), RoundedCornerShape(10.dp)),
        color = Color(0xFFE8F4FC),
        shape = RoundedCornerShape(10.dp),
        elevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // 块头：昵称 + 实付
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(Color(0xFF7F77DD), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.nickname.firstOrNull()?.toString() ?: "?",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = user.nickname, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppInk)
                Spacer(modifier = Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "实付", fontSize = 11.sp, color = AppMuted)
                    Text(
                        text = "¥${"%.2f".format(totalPayable)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppRed
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "原价 ¥${"%.2f".format(totalOriginal)}", fontSize = 12.sp, color = AppMuted)
                Text(
                    text = "  优惠 -¥${"%.2f".format(totalDiscount)}",
                    fontSize = 12.sp,
                    color = AppRed,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            if (poolDiscount > 0.01) {
                Text(
                    text = "（含凑单分摊 -¥${"%.2f".format(poolDiscount)}，见底部凑单组合）",
                    fontSize = 11.sp,
                    color = Color(0xFF1F9254),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // 块内：自达标组 + 单独付
            if (selfGroups.isNotEmpty() || separateProducts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = Color(0xFFBBBBBB).copy(alpha = 0.4f), thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(8.dp))

                selfGroups.forEach { usage ->
                    SelfGroupMiniCard(usage = usage, coupons = coupons)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (separateProducts.isNotEmpty()) {
                    SeparatePaymentCard(products = separateProducts, coupons = coupons)
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "无自达标组 · 参与凑单（见底部凑单组合）",
                    fontSize = 12.sp,
                    color = AppMuted,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

/** 用户块内：单人自达标组（白底小卡） */
@Composable
private fun SelfGroupMiniCard(usage: CouponUsage, coupons: List<Coupon>) {
    val productTotal = usage.productGroup.sumOf { it.originalPrice }
    val fillTotal = usage.fillProducts.sumOf { it.price }
    val groupTotal = productTotal + fillTotal
    val discount = usageDiscount(usage)
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, Color(0xFFB5D4F4), RoundedCornerShape(8.dp))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = usage.coupon.displayNameWithUsageMode(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF534AB7)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "单人组 · 自达标",
                    fontSize = 11.sp,
                    color = AppBlue,
                    modifier = Modifier
                        .background(AppBlue.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            usage.productGroup.forEach { product ->
                val requiredBadge = if (product.isRequired) " · 必买" else ""
                Text(
                    text = "${product.name}$requiredBadge: ¥${"%.2f".format(product.originalPrice)}",
                    fontSize = 12.sp,
                    color = product.displayColor(coupons),
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
            if (usage.fillProducts.isNotEmpty()) {
                Text(
                    text = "凑单: ¥${"%.2f".format(fillTotal)}",
                    fontSize = 12.sp,
                    color = AppGreen,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "${usage.coupon.displayNameWithUsageMode()} x${usage.count}",
                    fontSize = 11.sp,
                    color = AppMuted
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "¥${"%.2f".format(groupTotal)}",
                        fontSize = 13.sp,
                        color = AppMuted,
                        textDecoration = TextDecoration.LineThrough,
                        modifier = Modifier.padding(bottom = 1.dp)
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(text = "应付:", fontSize = 12.sp)
                        Text(
                            text = "¥${"%.2f".format(groupTotal - discount)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppRed
                        )
                    }
                }
            }
        }
    }
}

/** 用户块内：未参与任何券、单独原价结算的商品（灰底小卡） */
@Composable
private fun SeparatePaymentCard(products: List<Product>, coupons: List<Coupon>) {
    Surface(
        color = Color(0xFFF5F5F5),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, AppLine.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = "单独付 · 无券", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppMuted)
            products.forEach { product ->
                val requiredBadge = if (product.isRequired) " · 必买" else ""
                Text(
                    text = "${product.name}$requiredBadge: ¥${"%.2f".format(product.originalPrice)}",
                    fontSize = 12.sp,
                    color = product.displayColor(coupons),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            val total = products.sumOf { it.originalPrice }
            Text(
                text = "应付: ¥${"%.2f".format(total)}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = AppMuted,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
