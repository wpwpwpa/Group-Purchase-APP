package com.example.dealoptimizer.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.dealoptimizer.data.model.Coupon
import com.example.dealoptimizer.data.model.CouponType
import com.example.dealoptimizer.presentation.viewmodel.CouponViewModel

@Composable
fun CouponScreen() {
    val viewModel: CouponViewModel = hiltViewModel()
    val coupons = viewModel.allCoupons.collectAsState(emptyList()).value
    val users = viewModel.allUsers.collectAsState(emptyList()).value
    val isStackableMode by viewModel.isStackableMode.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingCoupon by remember { mutableStateOf<Coupon?>(null) }
    var type by remember { mutableStateOf(CouponType.FULL_REDUCTION) }
    var threshold by remember { mutableStateOf("") }
    var purchasePrice by remember { mutableStateOf("") }
    var discountValue by remember { mutableStateOf("") }
    var maxUsages by remember { mutableStateOf("") }
    var isStackable by remember { mutableStateOf(false) }
    var isSingleUse by remember { mutableStateOf(false) }
    var buyerId by remember { mutableStateOf(1L) }

    val filteredCoupons = coupons.filter { it.isStackable == isStackableMode }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("优惠券管理") },
                backgroundColor = MaterialTheme.colors.surface,
                contentColor = AppInk,
                elevation = 1.dp,
                actions = {
                    IconButton(onClick = {
                        editingCoupon = null
                        type = CouponType.FULL_REDUCTION
                        threshold = ""
                        purchasePrice = ""
                        discountValue = ""
                        maxUsages = ""
                        isStackable = isStackableMode
                        isSingleUse = !isStackableMode
                        buyerId = 1L
                        showDialog = true
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "添加优惠券")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // 顶部两段式切换器：叠加券（默认在前）/ 单用券
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .background(AppSurface, RoundedCornerShape(8.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SegmentedTab(
                    text = "叠加券",
                    selected = isStackableMode,
                    onClick = { viewModel.setCouponMode(true) },
                    modifier = Modifier.weight(1f)
                )
                SegmentedTab(
                    text = "单用券",
                    selected = !isStackableMode,
                    onClick = { viewModel.setCouponMode(false) },
                    modifier = Modifier.weight(1f)
                )
            }

            when {
                coupons.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "暂无优惠券，点击右上角添加", color = AppMuted)
                    }
                }
                filteredCoupons.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isStackableMode) "暂无叠加券，点击右上角添加" else "暂无单用券，点击右上角添加",
                            color = AppMuted
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredCoupons) { coupon ->
                            CouponCard(
                                coupon = coupon,
                                onEdit = {
                                    editingCoupon = coupon
                                    type = coupon.type
                                    threshold = if (coupon.threshold == 0.0) "" else coupon.threshold.toString()
                                    purchasePrice = if (coupon.purchasePrice == 0.0) "" else coupon.purchasePrice.toString()
                                    discountValue = coupon.discountValue.toString()
                                    maxUsages = if (coupon.maxUsages == Int.MAX_VALUE) "" else coupon.maxUsages.toString()
                                    isStackable = coupon.isStackable
                                    isSingleUse = !coupon.isStackable
                                    buyerId = coupon.ownerId
                                    showDialog = true
                                },
                                onEnabledChange = { enabled ->
                                    viewModel.updateCoupon(coupon.copy(isEnabled = enabled))
                                },
                                onDelete = { viewModel.deleteCoupon(coupon.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (editingCoupon == null) "添加优惠券" else "编辑优惠券") },
            text = {
                Column {
                    // 券类型选择
                    Text(
                        text = "券类型",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppInk,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val typeOptions = listOf(
                            CouponType.FULL_REDUCTION to "满减",
                            CouponType.DISCOUNT to "折扣",
                            CouponType.NO_THRESHOLD to "无门槛",
                            CouponType.VOUCHER to "代金券"
                        )
                        typeOptions.forEach { (t, label) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (type == t) MaterialTheme.colors.primary else AppSurface)
                                    .clickable { type = t }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    color = if (type == t) Color.White else AppMuted
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    when (type) {
                        CouponType.FULL_REDUCTION -> {
                            OutlinedTextField(
                                label = { Text("满减门槛（满多少可用）") },
                                value = threshold,
                                onValueChange = { threshold = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                label = { Text("优惠券购买价格（实付买价）") },
                                value = purchasePrice,
                                onValueChange = { purchasePrice = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth()
                            )
                            val thresholdValue = threshold.toDoubleOrNull() ?: 0.0
                            val purchaseValue = purchasePrice.toDoubleOrNull() ?: 0.0
                            val calculatedDiscount = maxOf(thresholdValue - purchaseValue, 0.0)
                            Text(
                                text = "优惠金额: ¥${"%.2f".format(calculatedDiscount)}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colors.primary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        CouponType.DISCOUNT -> {
                            OutlinedTextField(
                                label = { Text("满减门槛（满多少可用）") },
                                value = threshold,
                                onValueChange = { threshold = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                label = { Text("折扣比例（如 90=打9折）") },
                                value = discountValue,
                                onValueChange = { discountValue = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        CouponType.NO_THRESHOLD -> {
                            OutlinedTextField(
                                label = { Text("立减金额") },
                                value = discountValue,
                                onValueChange = { discountValue = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        CouponType.VOUCHER -> {
                            // 代金券（买面值）：面值 F = 门槛、抵扣额 = F，买价 P 单独记，谁买谁承担
                            OutlinedTextField(
                                label = { Text("面值（满面值才可用，等同门槛）") },
                                value = threshold,
                                onValueChange = { threshold = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                label = { Text("买价（实付买价）") },
                                value = purchasePrice,
                                onValueChange = { purchasePrice = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth()
                            )
                            val faceValue = threshold.toDoubleOrNull() ?: 0.0
                            val purchaseValue = purchasePrice.toDoubleOrNull() ?: 0.0
                            Text(
                                text = "抵扣面值: ¥${"%.2f".format(faceValue)}　净省: ¥${"%.2f".format(maxOf(faceValue - purchaseValue, 0.0))}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colors.primary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            // 买券人（谁买谁承担）
                            Text(
                                text = "买券人",
                                fontSize = 12.sp,
                                color = AppMuted,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                users.forEach { u ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (buyerId == u.id) MaterialTheme.colors.primary else AppSurface)
                                            .clickable { buyerId = u.id }
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = u.nickname,
                                            fontSize = 12.sp,
                                            color = if (buyerId == u.id) Color.White else AppMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        label = { Text("最大使用次数（留空不限制）") },
                        value = maxUsages,
                        onValueChange = { maxUsages = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "使用规则",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppInk,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = isStackable,
                            onClick = {
                                isStackable = true
                                isSingleUse = false
                            }
                        )
                        Text("可叠加使用")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = isSingleUse,
                            onClick = {
                                isStackable = false
                                isSingleUse = true
                            }
                        )
                        Text("单次分组使用")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val thresholdValue = threshold.toDoubleOrNull() ?: 0.0
                        val purchaseValue = purchasePrice.toDoubleOrNull() ?: 0.0
                        // 代金券：面值 = 抵扣额（不折买价）；满减：优惠 = 门槛 − 买价（沿用老逻辑）
                        val finalDiscountValue = when (type) {
                            CouponType.VOUCHER -> thresholdValue
                            CouponType.FULL_REDUCTION -> maxOf(thresholdValue - purchaseValue, 0.0)
                            else -> discountValue.toDoubleOrNull() ?: 0.0
                        }
                        val coupon = Coupon(
                            id = editingCoupon?.id ?: 0,
                            name = couponAutoName(type, isStackable, thresholdValue, finalDiscountValue),
                            type = type,
                            threshold = thresholdValue,
                            purchasePrice = purchaseValue,
                            discountValue = finalDiscountValue,
                            maxUsages = maxUsages.toIntOrNull() ?: Int.MAX_VALUE,
                            isStackable = isStackable,
                            isSingleUse = isSingleUse,
                            isEnabled = editingCoupon?.isEnabled ?: true,
                            ownerId = buyerId
                        )
                        if (editingCoupon == null) {
                            viewModel.insertCoupon(
                                coupon.name,
                                coupon.type,
                                coupon.threshold,
                                coupon.purchasePrice,
                                coupon.discountValue,
                                coupon.maxUsages,
                                coupon.isStackable,
                                coupon.isSingleUse,
                                coupon.ownerId
                            )
                        } else {
                            viewModel.updateCoupon(coupon)
                        }
                        showDialog = false
                    }
                ) {
                    Text("确认")
                }
            },
            dismissButton = {
                Button(onClick = { showDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

fun CouponType.displayName(): String {
    return when (this) {
        CouponType.FULL_REDUCTION -> "满减"
        CouponType.DISCOUNT -> "折扣"
        CouponType.NO_THRESHOLD -> "无门槛"
        CouponType.VOUCHER -> "代金券"
    }
}

private fun couponAutoName(type: CouponType, isStackable: Boolean, threshold: Double, discountValue: Double): String {
    val prefix = if (isStackable) "叠·" else "单·"
    val name = when (type) {
        CouponType.FULL_REDUCTION -> "满${threshold.cleanAmount()}减${discountValue.cleanAmount()}"
        CouponType.DISCOUNT -> "满${threshold.cleanAmount()}打${discountValue.cleanAmount()}折"
        CouponType.NO_THRESHOLD -> "无门槛减${discountValue.cleanAmount()}"
        CouponType.VOUCHER -> "面值${threshold.cleanAmount()}"
    }
    return "$prefix$name"
}

fun Coupon.displayNameWithUsageMode(): String {
    val prefix = if (isStackable) "叠·" else "单·"
    return when {
        name.startsWith("叠·") || name.startsWith("单·") -> name
        name.startsWith("叠") || name.startsWith("单") -> "${name.first()}·${name.drop(1)}"
        else -> "$prefix$name"
    }
}

private fun Double.cleanAmount(): String {
    return if (this % 1.0 == 0.0) {
        toLong().toString()
    } else {
        "%.2f".format(this).trimEnd('0').trimEnd('.')
    }
}

fun Coupon.getDescription(): String {
    return when (type) {
        CouponType.FULL_REDUCTION -> {
            if (purchasePrice > 0.0) {
                "满¥${"%.2f".format(threshold)}，¥${"%.2f".format(purchasePrice)}购买，优惠¥${"%.2f".format(discountValue)}"
            } else {
                "满¥${"%.2f".format(threshold)}减¥${"%.2f".format(discountValue)}"
            }
        }
        CouponType.DISCOUNT -> "${discountValue}折"
        CouponType.NO_THRESHOLD -> "立减¥${"%.2f".format(discountValue)}"
        CouponType.VOUCHER -> "面值¥${"%.2f".format(threshold)}，¥${"%.2f".format(purchasePrice)}购买"
    }
}

@Composable
fun CouponCard(
    coupon: Coupon,
    onEdit: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = 1.dp,
        backgroundColor = MaterialTheme.colors.surface
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = coupon.displayNameWithUsageMode(), fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (coupon.isEnabled) "启用" else "停用",
                        color = if (coupon.isEnabled) MaterialTheme.colors.primary else AppMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Switch(
                        checked = coupon.isEnabled,
                        onCheckedChange = onEnabledChange
                    )
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "编辑")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colors.error)
                    }
                }
            }
            Text(text = coupon.getDescription(), fontSize = 14.sp, color = AppMuted)
            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (coupon.isStackable) {
                    Text(text = "可叠加", color = MaterialTheme.colors.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                } else {
                    Text(text = "单次分组", color = MaterialTheme.colors.error, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                if (coupon.maxUsages != Int.MAX_VALUE) {
                    Text(
                        text = " · ${coupon.maxUsages}张",
                        color = AppMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun SegmentedTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) MaterialTheme.colors.primary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) Color.White else AppMuted
        )
    }
}
