package com.example.dealoptimizer.presentation.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.dealoptimizer.data.model.Product
import com.example.dealoptimizer.data.model.User
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import com.example.dealoptimizer.presentation.viewmodel.ProductViewModel
import kotlin.math.roundToInt

internal val upperClothingOptions = listOf("短袖", "长袖", "吊带", "外套", "衬衫", "羽绒服", "内衣")
internal val lowerClothingOptions = listOf("长裤", "短裤", "半身裙", "内裤")
internal val wholeBodyOptions = listOf("连衣裙", "背带裤")
internal val accessoryOptions = listOf("包包", "饰品", "围巾", "帽子", "眼镜", "丝巾", "皮带")
internal val clothingOptions = upperClothingOptions + lowerClothingOptions + wholeBodyOptions + accessoryOptions
private val presetColors = listOf("白", "黑", "红", "蓝", "粉", "黄", "绿", "灰")

@Composable
fun ProductScreen() {
    val viewModel: ProductViewModel = hiltViewModel()
    val products = viewModel.allProducts.collectAsState(emptyList()).value
    val users = viewModel.allUsers.collectAsState(emptyList()).value
    val checkedUsers = viewModel.checkedUsers.collectAsState(emptyList()).value
    val savedAddButtonOffset = viewModel.addButtonOffset.collectAsState().value
    var showDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var selectedName by remember { mutableStateOf(clothingOptions.first()) }
    var priceDigits by remember { mutableStateOf(listOf(0, 0, 9, 9)) }
    var isRequired by remember { mutableStateOf(false) }
    var selectedOwnerId by remember { mutableStateOf(1L) }
    var selectedColor by remember { mutableStateOf("") }
    var openProductId by remember { mutableStateOf<Long?>(null) }

    fun startAdd() {
        editingProduct = null
        selectedName = clothingOptions.first()
        selectedColor = ""
        priceDigits = listOf(0, 0, 9, 9)
        isRequired = false
        selectedOwnerId = users.firstOrNull { it.isDefault }?.id ?: 1L
        showDialog = true
    }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalDrawer(
        drawerState = drawerState,
        gesturesEnabled = openProductId == null,
        drawerContent = {
            UserDrawerContent(
                users = users,
                onAddUser = { viewModel.addUser(it) },
                onRenameUser = { u, n -> viewModel.renameUser(u, n) },
                onDeleteUser = { viewModel.deleteUser(it) },
                onToggleChecked = { u, c -> viewModel.setUserChecked(u, c) },
                onUncheckAll = {
                    users.forEach { if (!it.isDefault) viewModel.setUserChecked(it, false) }
                },
                onClose = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("商品列表") },
                    backgroundColor = MaterialTheme.colors.surface,
                    contentColor = AppInk,
                    elevation = 1.dp,
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { if (drawerState.isOpen) drawerState.close() else drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "用户管理")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.deleteAllProducts() }) {
                            Icon(Icons.Default.Delete, contentDescription = "清空全部", tint = MaterialTheme.colors.error)
                        }
                    }
                )
            }
        ) { padding ->
            BoxWithConstraints(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .pointerInput(openProductId) {
                        if (openProductId != null) {
                            coroutineScope {
                                launch {
                                    detectTapGestures(onTap = { openProductId = null })
                                }
                                launch {
                                    detectHorizontalDragGestures(
                                        onHorizontalDrag = { change, _ -> change.consume() },
                                        onDragEnd = { openProductId = null }
                                    )
                                }
                            }
                        }
                    }
            ) {
                val density = LocalDensity.current
                val buttonSize = 64.dp
                val buttonEdgePadding = 16.dp
                val buttonSizePx = with(density) { buttonSize.toPx() }
                val maxButtonX = (with(density) { maxWidth.toPx() } - buttonSizePx).coerceAtLeast(0f)
                val maxButtonY = (with(density) { maxHeight.toPx() } - buttonSizePx).coerceAtLeast(0f)
                val startButtonOffset = with(density) {
                    Offset(
                        (maxWidth.toPx() - buttonSizePx - buttonEdgePadding.toPx()).coerceIn(0f, maxButtonX),
                        (maxHeight.toPx() * 2f / 3f - buttonSizePx / 2f).coerceIn(0f, maxButtonY)
                    )
                }
                var addButtonOffset by remember { mutableStateOf<Offset?>(null) }

                LaunchedEffect(savedAddButtonOffset, maxButtonX, maxButtonY) {
                    val hasValidButtonBounds = maxButtonX > 0f && maxButtonY > 0f
                    if (!hasValidButtonBounds) return@LaunchedEffect

                    val nextOffset = addButtonOffset
                        ?.coerceInBounds(maxButtonX, maxButtonY)
                        ?: savedAddButtonOffset
                            ?.let { Offset(it.first, it.second) }
                            ?.coerceInBounds(maxButtonX, maxButtonY)
                        ?: startButtonOffset

                    addButtonOffset = nextOffset
                    viewModel.updateAddButtonOffset(nextOffset.x, nextOffset.y)
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (checkedUsers.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "请在左上角用户管理中勾选用户", color = AppMuted)
                            }
                        }
                    } else {
                        checkedUsers.forEach { user ->
                            item {
                                val userProducts = products.filter { it.ownerId == user.id }
                                UserProductSection(
                                    user = user,
                                    products = userProducts,
                                    openProductId = openProductId,
                                    onOpenChange = { openProductId = it },
                                    onClear = { viewModel.clearByOwner(user.id) },
                                    onEdit = { p ->
                                        editingProduct = p
                                        selectedColor = p.color
                                        val baseName = p.name.removePrefix(p.color)
                                        selectedName = clothingOptions.firstOrNull { baseName.startsWith(it) }
                                            ?: clothingOptions.first()
                                        priceDigits = p.originalPrice.toInt().coerceIn(0, 9999).toDigits()
                                        isRequired = p.isRequired
                                        selectedOwnerId = p.ownerId
                                        showDialog = true
                                    },
                                    onToggleRequired = { p ->
                                        viewModel.updateProduct(p.copy(isRequired = !p.isRequired))
                                    },
                                    onDelete = { p -> viewModel.deleteProduct(p.id) }
                                )
                            }
                        }
                    }
                }
                addButtonOffset?.let { currentOffset ->
                    AddProductFloatingButton(
                        onClick = { startAdd() },
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    currentOffset.x.roundToInt(),
                                    currentOffset.y.roundToInt()
                                )
                            }
                            .pointerInput(maxButtonX, maxButtonY) {
                                detectDragGestures(
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val latestOffset = addButtonOffset ?: currentOffset
                                        val nextOffset = Offset(
                                            x = latestOffset.x + dragAmount.x,
                                            y = latestOffset.y + dragAmount.y
                                        ).coerceInBounds(maxButtonX, maxButtonY)
                                        addButtonOffset = nextOffset
                                        viewModel.updateAddButtonOffset(nextOffset.x, nextOffset.y)
                                    }
                                )
                            }
                    )
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (editingProduct == null) "添加商品" else "编辑商品") },
            text = {
                Column {
                    Text("颜色", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    ColorChipRow(
                        selectedColor = selectedColor,
                        onSelect = { selectedColor = it }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("商品名称", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    ClothingNamePicker(
                        selectedName = selectedName,
                        onSelect = { selectedName = it }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("吊牌价格", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    PriceWheel(
                        digits = priceDigits,
                        onDigitStep = { index, step ->
                            priceDigits = priceDigits.toMutableList().also { digits ->
                                digits[index] = (digits[index] + step).wrapDigit()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("归属人", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    OwnerChipRow(
                        users = users,
                        selectedOwnerId = selectedOwnerId,
                        onSelect = { selectedOwnerId = it }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isRequired, onCheckedChange = { isRequired = it })
                            Text("必须购买")
                        }
                        if (editingProduct != null) {
                            Button(
                                onClick = {
                                    viewModel.deleteProduct(editingProduct!!.id)
                                    showDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = MaterialTheme.colors.error,
                                    contentColor = Color.White
                                )
                            ) {
                                Text("删除")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val price = priceDigits.toPrice()
                            if (price > 0) {
                                val composedName = if (selectedColor.isNotEmpty()) selectedColor + selectedName else selectedName
                                val finalName = if (editingProduct == null) {
                                    nextProductName(composedName, products)
                                } else {
                                    composedName
                                }
                                val product = Product(
                                    id = editingProduct?.id ?: 0,
                                    name = finalName,
                                    originalPrice = price.toDouble(),
                                    isRequired = isRequired,
                                    ownerId = selectedOwnerId,
                                    color = selectedColor
                                )
                                if (editingProduct == null) {
                                    viewModel.insertProduct(product.name, product.originalPrice, product.isRequired, product.ownerId, color = selectedColor)
                                } else {
                                    viewModel.updateProduct(product)
                                }
                                showDialog = false
                            }
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

@Composable
fun AddProductFloatingButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        shape = CircleShape,
        modifier = modifier.size(64.dp),
        contentPadding = PaddingValues(0.dp),
        elevation = ButtonDefaults.elevation(defaultElevation = 8.dp, pressedElevation = 4.dp)
    ) {
        Icon(Icons.Default.Add, contentDescription = "添加商品", modifier = Modifier.size(32.dp))
    }
}

@Composable
fun ClothingNamePicker(selectedName: String, onSelect: (String) -> Unit) {
    Column(
        modifier = Modifier.padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ClothingOptionRow(
            options = upperClothingOptions,
            selectedName = selectedName,
            onSelect = onSelect
        )
        ClothingOptionRow(
            options = lowerClothingOptions,
            selectedName = selectedName,
            onSelect = onSelect
        )
        ClothingOptionRow(
            options = wholeBodyOptions,
            selectedName = selectedName,
            onSelect = onSelect
        )
        ClothingOptionRow(
            options = accessoryOptions,
            selectedName = selectedName,
            onSelect = onSelect
        )
    }
}

@Composable
fun ClothingOptionRow(
    options: List<String>,
    selectedName: String,
    onSelect: (String) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options) { option ->
            ClothingOptionButton(
                option = option,
                selected = option == selectedName,
                onSelect = onSelect
            )
        }
    }
}

@Composable
fun ClothingOptionButton(option: String, selected: Boolean, onSelect: (String) -> Unit) {
    OutlinedButton(
        onClick = { onSelect(option) },
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colors.primary else AppLine),
        colors = ButtonDefaults.outlinedButtonColors(
            backgroundColor = if (selected) MaterialTheme.colors.primary.copy(alpha = 0.08f) else MaterialTheme.colors.surface,
            contentColor = if (selected) MaterialTheme.colors.primary else AppInk
        )
    ) {
        Text(option, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
fun OwnerChipRow(
    users: List<User>,
    selectedOwnerId: Long,
    onSelect: (Long) -> Unit
) {
    LazyRow(
        modifier = Modifier.padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(users) { user ->
            OutlinedButton(
                onClick = { onSelect(user.id) },
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, if (user.id == selectedOwnerId) MaterialTheme.colors.primary else AppLine),
                colors = ButtonDefaults.outlinedButtonColors(
                    backgroundColor = if (user.id == selectedOwnerId) MaterialTheme.colors.primary.copy(alpha = 0.08f) else MaterialTheme.colors.surface,
                    contentColor = if (user.id == selectedOwnerId) MaterialTheme.colors.primary else AppInk
                )
            ) {
                val label = user.nickname
                Text(label, fontWeight = if (user.id == selectedOwnerId) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

@Composable
fun ColorChipRow(selectedColor: String, onSelect: (String) -> Unit) {
    LazyRow(
        modifier = Modifier.padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(presetColors) { color ->
            val selected = color == selectedColor
            OutlinedButton(
                onClick = { onSelect(if (selected) "" else color) },
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, if (selected) MaterialTheme.colors.primary else AppLine),
                colors = ButtonDefaults.outlinedButtonColors(
                    backgroundColor = if (selected) MaterialTheme.colors.primary.copy(alpha = 0.08f) else MaterialTheme.colors.surface,
                    contentColor = if (selected) MaterialTheme.colors.primary else AppInk
                )
            ) {
                Text(color, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

@Composable
fun PriceWheel(digits: List<Int>, onDigitStep: (Int, Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        digits.forEachIndexed { index, digit ->
            DigitWheel(
                digit = digit,
                onStep = { step ->
                    onDigitStep(index, step)
                },
                modifier = Modifier.width(52.dp)
            )
        }
        Text("元", fontWeight = FontWeight.Bold, color = AppMuted, modifier = Modifier.padding(start = 2.dp))
    }
}

@Composable
fun DigitWheel(
    digit: Int,
    onStep: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOnStep by rememberUpdatedState(onStep)
    val stepPx = with(LocalDensity.current) { 30.dp.toPx() }
    var dragAccumulator by remember { mutableStateOf(0f) }
    val wheelShape = RoundedCornerShape(12.dp)

    Card(
        modifier = modifier
            .height(112.dp)
            .clip(wheelShape)
            .pointerInput(stepPx) {
                detectVerticalDragGestures(
                    onDragStart = { dragAccumulator = 0f },
                    onDragEnd = { dragAccumulator = 0f },
                    onDragCancel = { dragAccumulator = 0f },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        dragAccumulator += dragAmount

                        var steps = 0
                        while (dragAccumulator <= -stepPx) {
                            steps += 1
                            dragAccumulator += stepPx
                        }
                        while (dragAccumulator >= stepPx) {
                            steps -= 1
                            dragAccumulator -= stepPx
                        }
                        if (steps != 0) {
                            currentOnStep(steps)
                        }
                    }
                )
            },
        shape = wheelShape,
        elevation = 2.dp,
        border = BorderStroke(1.dp, AppLine),
        backgroundColor = MaterialTheme.colors.surface
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
        ) {
            Text(
                text = ((digit + 9) % 10).toString(),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .wrapContentHeight(),
                textAlign = TextAlign.Center,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppMuted.copy(alpha = 0.5f)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(MaterialTheme.colors.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = digit.toString(),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppInk
                )
            }
            Text(
                text = ((digit + 1) % 10).toString(),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .wrapContentHeight(),
                textAlign = TextAlign.Center,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppMuted.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun ProductCard(product: Product, onEdit: () -> Unit, onDelete: () -> Unit, onToggleRequired: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = 1.dp,
        backgroundColor = MaterialTheme.colors.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = product.name, fontWeight = FontWeight.Bold)
                    if (product.isRequired) {
                        Badge(
                            modifier = Modifier.padding(start = 8.dp),
                            backgroundColor = MaterialTheme.colors.error
                        ) {
                            Text(text = "必买", fontSize = 10.sp)
                        }
                    }
                }
                Text(text = "¥${"%.2f".format(product.originalPrice)}", color = AppMuted)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = product.isRequired,
                    onCheckedChange = { onToggleRequired() }
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "编辑")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colors.error)
                }
            }
        }
    }
}

private fun Int.toDigits(): List<Int> {
    return toString().padStart(4, '0').takeLast(4).map { it.digitToInt() }
}

private fun List<Int>.toPrice(): Int {
    return joinToString("").toIntOrNull() ?: 0
}

private fun Int.wrapDigit(): Int {
    return ((this % 10) + 10) % 10
}

private fun nextProductName(baseName: String, products: List<Product>): String {
    val sameBaseCount = products.count { product ->
        product.name == baseName || product.name.matches(Regex("^${Regex.escape(baseName)}\\d+$"))
    }
    return if (sameBaseCount == 0) baseName else "$baseName${sameBaseCount + 1}"
}

private fun Offset.coerceInBounds(maxX: Float, maxY: Float): Offset {
    return Offset(
        x = x.coerceIn(0f, maxX),
        y = y.coerceIn(0f, maxY)
    )
}
