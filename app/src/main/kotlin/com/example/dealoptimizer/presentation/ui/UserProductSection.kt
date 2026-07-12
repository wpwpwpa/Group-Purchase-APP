package com.example.dealoptimizer.presentation.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dealoptimizer.data.model.Product
import com.example.dealoptimizer.data.model.User
import kotlin.math.roundToInt

private val CardMinWidth = 105.dp

@Composable
fun UserProductSection(
    user: User,
    products: List<Product>,
    openProductId: Long?,
    onOpenChange: (Long?) -> Unit,
    onClear: () -> Unit,
    onEdit: (Product) -> Unit,
    onToggleRequired: (Product) -> Unit,
    onDelete: (Product) -> Unit,
    onAdd: (User) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = 1.dp,
        backgroundColor = MaterialTheme.colors.surface
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = user.nickname,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colors.primary
                )
                TextButton(onClick = onClear) {
                    Text("清空", color = AppMuted, fontSize = 13.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (products.isEmpty()) {
                Text(
                    text = "该用户暂无商品",
                    color = AppMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                val gap = 8.dp
                val colCount = 1
                // 按四大类型分组，保持顺序：上半身→下半身→全身→饰品
                val categoryOrder = listOf("上半身", "下半身", "全身", "饰品")
                val categoryItems = categoryOrder.map { cat ->
                    cat to products.filter { resolveCategory(it) == cat }
                }.filter { it.second.isNotEmpty() }
                val unmatched = products.filter { resolveCategory(it) == "" }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    categoryItems.forEach { (_, items) ->
                        TypeBlock(
                            items = items,
                            colCount = colCount,
                            gap = gap,
                            openProductId = openProductId,
                            onOpenChange = onOpenChange,
                            onEdit = onEdit,
                            onToggleRequired = onToggleRequired,
                            onDelete = onDelete
                        )
                    }
                    if (unmatched.isNotEmpty()) {
                        TypeBlock(
                            items = unmatched,
                            colCount = colCount,
                            gap = gap,
                            openProductId = openProductId,
                            onOpenChange = onOpenChange,
                            onEdit = onEdit,
                            onToggleRequired = onToggleRequired,
                            onDelete = onDelete
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            AddProductBar(onClick = { onAdd(user) })
        }
    }
}

@Composable
private fun TypeBlock(
    items: List<Product>,
    colCount: Int,
    gap: androidx.compose.ui.unit.Dp,
    openProductId: Long?,
    onOpenChange: (Long?) -> Unit,
    onEdit: (Product) -> Unit,
    onToggleRequired: (Product) -> Unit,
    onDelete: (Product) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        items.chunked(colCount).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                row.forEach { product ->
                    ProductMiniCard(
                        product = product,
                        onClick = { onEdit(product) },
                        onToggleRequired = { onToggleRequired(product) },
                        onDelete = { onDelete(product) },
                        openProductId = openProductId,
                        onOpenChange = onOpenChange,
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(colCount - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/** 四大类分类：根据商品名（去掉颜色前缀）匹配所属类别 */
private fun resolveCategory(product: Product): String {
    val base = product.name.removePrefix(product.color)
    return when {
        upperClothingOptions.any { base.startsWith(it) } -> "上半身"
        lowerClothingOptions.any { base.startsWith(it) } -> "下半身"
        wholeBodyOptions.any { base.startsWith(it) } -> "全身"
        accessoryOptions.any { base.startsWith(it) } -> "饰品"
        else -> ""
    }
}

/** 类型背景色（方案A 马卡龙浅色），未匹配项走空 */
private val categoryBackground = mapOf(
    "上半身" to Color(0xFFE0F2FE),
    "下半身" to Color(0xFFDCFCE7),
    "全身" to Color(0xFFFEF9C3),
    "饰品" to Color(0xFFF3E8FF)
)

/** 类型边框色（同色系稍深一档） */
private val categoryBorder = mapOf(
    "上半身" to Color(0xFFBAE6FD),
    "下半身" to Color(0xFFBBF7D0),
    "全身" to Color(0xFFFEF08A),
    "饰品" to Color(0xFFE9D5FF)
)

@Composable
fun ProductMiniCard(
    product: Product,
    onClick: () -> Unit,
    onToggleRequired: () -> Unit,
    onDelete: () -> Unit,
    openProductId: Long?,
    onOpenChange: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    val required = product.isRequired
    val category = resolveCategory(product)
    val bgColor = categoryBackground[category] ?: MaterialTheme.colors.surface
    val borderColor = categoryBorder[category] ?: AppLine

    val density = LocalDensity.current
    val deleteWidth = 56.dp
    val deleteWidthPx = with(density) { deleteWidth.toPx() }
    var offsetX by remember { mutableStateOf(0f) }
    val isOpen = openProductId == product.id

    LaunchedEffect(isOpen) {
        offsetX = if (isOpen) -deleteWidthPx else 0f
    }

    Box(modifier = modifier) {
        // 底层：左滑后露出的删除区（右侧红底 + 删除图标）
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color(0xFFE24B4A), RoundedCornerShape(8.dp))
                .clickable { onDelete() },
            contentAlignment = Alignment.CenterEnd
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "删除",
                tint = Color.White,
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(22.dp)
            )
        }
        // 前景：卡片内容（随 offset 平移，手势挂在卡片本身以跟随平移）
        Card(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        offsetX = (offsetX + delta).coerceIn(-deleteWidthPx, 0f)
                    },
                    onDragStopped = {
                        val shouldOpen = offsetX <= -deleteWidthPx / 2f
                        offsetX = if (shouldOpen) -deleteWidthPx else 0f
                        onOpenChange(if (shouldOpen) product.id else null)
                    }
                )
                .clickable {
                    if (isOpen) {
                        onOpenChange(null)
                    } else {
                        onOpenChange(null)
                        onClick()
                    }
                },
            shape = RoundedCornerShape(8.dp),
            elevation = 1.dp,
            border = if (required) BorderStroke(1.5.dp, Color(0xFFE24B4A)) else BorderStroke(1.dp, borderColor),
            backgroundColor = if (required) Color(0xFFFEE2E2) else bgColor
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = product.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "¥${"%.2f".format(product.originalPrice)}",
                        color = AppMuted,
                        fontSize = 12.sp
                    )
                }
                Checkbox(
                    checked = required,
                    onCheckedChange = { onToggleRequired() },
                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFFE24B4A)),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun AddProductBar(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colors.surface,
        border = BorderStroke(1.dp, AppLine)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "添加商品",
                tint = MaterialTheme.colors.primary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "添加商品",
                color = MaterialTheme.colors.primary,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
        }
    }
}
