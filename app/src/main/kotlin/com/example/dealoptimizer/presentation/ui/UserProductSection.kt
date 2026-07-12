package com.example.dealoptimizer.presentation.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dealoptimizer.data.model.Product
import com.example.dealoptimizer.data.model.User

private val CardMinWidth = 105.dp

@Composable
fun UserProductSection(
    user: User,
    products: List<Product>,
    onClear: () -> Unit,
    onEdit: (Product) -> Unit
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
                BoxWithConstraints {
                    val gap = 8.dp
                    val colCount = maxOf(
                        1,
                        ((maxWidth - gap) / (CardMinWidth + gap)).toInt()
                    )
                    val typeItems = clothingOptions.map { type ->
                        type to products.filter { resolveType(it) == type }
                    }.filter { it.second.isNotEmpty() }
                    val unmatched = products.filter { resolveType(it) == "" }

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        typeItems.forEach { (_, items) ->
                            TypeBlock(items = items, colCount = colCount, gap = gap, onEdit = onEdit)
                        }
                        if (unmatched.isNotEmpty()) {
                            TypeBlock(items = unmatched, colCount = colCount, gap = gap, onEdit = onEdit)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TypeBlock(
    items: List<Product>,
    colCount: Int,
    gap: androidx.compose.ui.unit.Dp,
    onEdit: (Product) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        items.chunked(colCount).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                row.forEach { product ->
                    ProductMiniCard(
                        product = product,
                        onClick = { onEdit(product) },
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

private fun resolveType(product: Product): String {
    val base = product.name.removePrefix(product.color)
    return clothingOptions.firstOrNull { base.startsWith(it) } ?: ""
}

@Composable
fun ProductMiniCard(product: Product, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val required = product.isRequired
    Card(
        modifier = modifier
            .height(84.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        elevation = 1.dp,
        border = if (required) BorderStroke(1.5.dp, Color(0xFFE24B4A)) else null,
        backgroundColor = if (required) Color(0xFFFEE2E2) else MaterialTheme.colors.surface
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = product.name,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "¥${"%.2f".format(product.originalPrice)}",
                color = AppMuted,
                fontSize = 13.sp
            )
        }
    }
}
