package com.example.dealoptimizer.presentation.ui

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
    var showDialog by remember { mutableStateOf(false) }
    var editingCoupon by remember { mutableStateOf<Coupon?>(null) }
    var type by remember { mutableStateOf(CouponType.FULL_REDUCTION) }
    var threshold by remember { mutableStateOf("") }
    var purchasePrice by remember { mutableStateOf("") }
    var discountValue by remember { mutableStateOf("") }
    var maxUsages by remember { mutableStateOf("") }
    var isStackable by remember { mutableStateOf(false) }
    var isSingleUse by remember { mutableStateOf(false) }

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
                        isStackable = false
                        isSingleUse = true
                        showDialog = true
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "添加优惠券")
                    }
                }
            )
        }
    ) { padding ->
        if (coupons.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "暂无优惠券，点击右上角添加", color = AppMuted)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(coupons) { coupon ->
                    CouponCard(
                        coupon = coupon,
                        onEdit = {
                            editingCoupon = coupon
                            type = CouponType.FULL_REDUCTION
                            threshold = if (coupon.threshold == 0.0) "" else coupon.threshold.toString()
                            purchasePrice = if (coupon.purchasePrice == 0.0) "" else coupon.purchasePrice.toString()
                            discountValue = coupon.discountValue.toString()
                            maxUsages = if (coupon.maxUsages == Int.MAX_VALUE) "" else coupon.maxUsages.toString()
                            isStackable = coupon.isStackable
                            isSingleUse = !coupon.isStackable
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

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (editingCoupon == null) "添加优惠券" else "编辑优惠券") },
            text = {
                Column {
                    OutlinedTextField(
                        label = { Text("满减门槛（满多少可用）") },
                        value = threshold,
                        onValueChange = { threshold = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (type == CouponType.FULL_REDUCTION) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            label = { Text("优惠券购买价格") },
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
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            label = { Text("优惠金额/折扣比例") },
                            value = discountValue,
                            onValueChange = { discountValue = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
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
                        val finalDiscountValue = if (type == CouponType.FULL_REDUCTION) {
                            maxOf(thresholdValue - purchaseValue, 0.0)
                        } else {
                            discountValue.toDoubleOrNull() ?: 0.0
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
                            isEnabled = editingCoupon?.isEnabled ?: true
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
                                coupon.isSingleUse
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
    }
}

private fun couponAutoName(type: CouponType, isStackable: Boolean, threshold: Double, discountValue: Double): String {
    val prefix = if (isStackable) "叠·" else "单·"
    val name = when (type) {
        CouponType.FULL_REDUCTION -> "满${threshold.cleanAmount()}减${discountValue.cleanAmount()}"
        CouponType.DISCOUNT -> "满${threshold.cleanAmount()}打${discountValue.cleanAmount()}折"
        CouponType.NO_THRESHOLD -> "无门槛减${discountValue.cleanAmount()}"
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
