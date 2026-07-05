package com.example.dealoptimizer.presentation.ui

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dealoptimizer.data.model.BundleSolution
import com.example.dealoptimizer.data.model.Coupon
import com.example.dealoptimizer.data.model.CouponType
import com.example.dealoptimizer.data.model.FillSuggestion
import com.example.dealoptimizer.data.model.Product
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

@Composable
fun CalculatorScreen() {
    val viewModel: CalculatorViewModel = hiltViewModel()
    val solution = viewModel.solution.collectAsState().value
    val comparisonResult = viewModel.comparisonResult.collectAsState().value
    val allProducts = viewModel.allProducts.collectAsState().value
    val allCoupons = viewModel.allCoupons.collectAsState().value
    var selectedProductIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var knownProductIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var lastCalculationInput by remember { mutableStateOf<CalculationInputSignature?>(null) }
    var useFillProducts by remember { mutableStateOf(false) }
    val selectedProducts = allProducts.filter { it.id in selectedProductIds || it.isRequired }
    val droppedProducts = allProducts.filter { it.id !in selectedProducts.map { product -> product.id }.toSet() }

    LaunchedEffect(allProducts) {
        val currentProductIds = allProducts.map { it.id }.toSet()
        selectedProductIds = if (knownProductIds.isEmpty() && selectedProductIds.isEmpty()) {
            currentProductIds
        } else {
            (selectedProductIds intersect currentProductIds) + (currentProductIds - knownProductIds)
        }
        knownProductIds = currentProductIds
    }

    LaunchedEffect(selectedProductIds, allProducts, allCoupons, useFillProducts) {
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("凑单计算") },
                backgroundColor = MaterialTheme.colors.surface,
                contentColor = AppInk,
                elevation = 1.dp,
                actions = {
                    IconButton(onClick = { viewModel.calculateForProducts(selectedProducts, useFillProducts) }) {
                        Icon(Icons.Default.Settings, contentDescription = "计算")
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
            var fillButtonOffset by remember { mutableStateOf(startButtonOffset) }

            LaunchedEffect(maxButtonX, maxButtonY) {
                fillButtonOffset = fillButtonOffset.coerceInBounds(maxButtonX, maxButtonY)
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (allProducts.isNotEmpty()) {
                    item {
                        ProductSelectionPanel(
                            products = allProducts,
                            coupons = allCoupons,
                            selectedProductIds = selectedProductIds,
                            onToggle = { product ->
                                if (product.isRequired) return@ProductSelectionPanel
                                val nextSelectedProductIds = if (product.id in selectedProductIds) {
                                    selectedProductIds - product.id
                                } else {
                                    selectedProductIds + product.id
                                }
                                selectedProductIds = nextSelectedProductIds
                            }
                        )
                    }
                }

                if (solution != null) {
                    item {
                        SolutionCard(
                            solution = solution,
                            isBest = true,
                            droppedProducts = droppedProducts,
                            coupons = allCoupons
                        )
                    }
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

            FillProductFloatingButton(
                checked = useFillProducts,
                onClick = {
                    useFillProducts = !useFillProducts
                },
                modifier = Modifier
                    .offset {
                        IntOffset(
                            fillButtonOffset.x.roundToInt(),
                            fillButtonOffset.y.roundToInt()
                        )
                    }
                    .pointerInput(maxButtonX, maxButtonY) {
                        detectDragGesturesAfterLongPress(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                fillButtonOffset = Offset(
                                    x = fillButtonOffset.x + dragAmount.x,
                                    y = fillButtonOffset.y + dragAmount.y
                                ).coerceInBounds(maxButtonX, maxButtonY)
                            }
                        )
                    }
            )
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
            text = if (checked) "凑" else "衣",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
    }
}

@Composable
fun ProductSelectionPanel(
    selectedProductIds: Set<Long>,
    products: List<Product>,
    coupons: List<Coupon>,
    onToggle: (Product) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = 1.dp,
        backgroundColor = MaterialTheme.colors.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "选择参与计算的商品",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Text(
                text = "取消勾选即舍弃该商品，必买商品会始终保留",
                fontSize = 12.sp,
                color = AppMuted,
                modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)
            )

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
                            textColor = product.thresholdGapColor(coupons),
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

private fun Product.thresholdGapColor(coupons: List<Coupon>): Color {
    val nearestGap = coupons
        .asSequence()
        .filter { it.isEnabled && it.type == CouponType.FULL_REDUCTION && it.threshold > originalPrice }
        .map { it.threshold - originalPrice }
        .minOrNull()

    return when {
        nearestGap == null -> AppInk
        nearestGap <= 20.0 -> AppRed
        nearestGap <= 40.0 -> AppAmber
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
    droppedProducts: List<Product> = emptyList(),
    coupons: List<Coupon> = emptyList()
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = if (isBest) 3.dp else 1.dp,
        backgroundColor = MaterialTheme.colors.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            BillSummary(solution = solution)
            val groupedCouponUsages = solution.couponUsages.filter { !it.coupon.isStackable }

            if (droppedProducts.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    shape = RoundedCornerShape(8.dp),
                    backgroundColor = AppWarnSurface,
                    elevation = 0.dp
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "已舍弃的非必买商品",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppWarnText
                        )
                        droppedProducts.forEach { product ->
                            val requiredBadge = if (product.isRequired) " · 必买" else ""
                            Text(
                                text = "${product.name}$requiredBadge: ¥${"%.2f".format(product.originalPrice)}",
                                fontSize = 12.sp,
                                color = AppWarnText,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
            
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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 10.dp)
                            ) {
                                Text(
                                    text = "第${groupIndex + 1}组 · 单独结账",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colors.primary
                                )

                                if (usage.productGroup.isNotEmpty()) {
                                    Text(
                                        text = "商品",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                    usage.productGroup.forEach { product ->
                                        val requiredBadge = if (product.isRequired) " · 必买" else ""
                                        Text(
                                            text = "${product.name}$requiredBadge: ¥${"%.2f".format(product.originalPrice)}",
                                            fontSize = 12.sp,
                                            color = product.thresholdGapColor(coupons),
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
                            }

                            Column(
                                modifier = Modifier.widthIn(min = 118.dp),
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
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                                Text(text = "-¥${"%.2f".format(discount)}", fontSize = 12.sp)
                                Text(text = "本组应付:", fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
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
                                Text(
                                    text = "${product.name}$requiredBadge: ¥${"%.2f".format(product.originalPrice)}",
                                    fontSize = 12.sp,
                                    color = product.thresholdGapColor(coupons),
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
                    Text(
                        text = "  • ${product.name}$requiredBadge: ¥${"%.2f".format(product.originalPrice)}",
                        fontSize = 14.sp,
                        color = product.thresholdGapColor(coupons)
                    )
                }
            }
            
        }
    }
}

@Composable
fun BillSummary(solution: BundleSolution) {
    val itemCount = solution.products.size
    val discountRows = solution.couponUsages
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
                    com.example.dealoptimizer.data.model.CouponType.FULL_REDUCTION -> usage.count * usage.coupon.discountValue
                    com.example.dealoptimizer.data.model.CouponType.DISCOUNT -> groupTotal * (usage.coupon.discountValue / 100) * usage.count
                    com.example.dealoptimizer.data.model.CouponType.NO_THRESHOLD -> usage.count * usage.coupon.discountValue
                }
            }
            DiscountSummaryRow(couponId = couponId, name = coupon.displayNameWithUsageMode(), count = count, discount = discount)
        }

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
            Text("¥${"%.2f".format(solution.finalPrice)}", fontWeight = FontWeight.Bold, fontSize = 32.sp, color = AppInk)
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
