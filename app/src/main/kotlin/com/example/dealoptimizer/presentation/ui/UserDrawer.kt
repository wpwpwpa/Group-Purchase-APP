package com.example.dealoptimizer.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dealoptimizer.data.model.User

@Composable
fun UserDrawerContent(
    users: List<User>,
    onAddUser: (String) -> Unit,
    onRenameUser: (User, String) -> Unit,
    onDeleteUser: (User) -> Unit,
    onToggleChecked: (User, Boolean) -> Unit,
    onUncheckAll: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingUser by remember { mutableStateOf<User?>(null) }
    var editName by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "用户管理", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                TextButton(onClick = onUncheckAll) {
                    Text("一键取消勾选", fontSize = 13.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(users) { user ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                elevation = 0.dp,
                backgroundColor = AppSurface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = user.isSelected,
                        onCheckedChange = { onToggleChecked(user, it) }
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = user.nickname,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    IconButton(onClick = {
                        editingUser = user
                        editName = user.nickname
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = "编辑昵称")
                    }
                    if (!user.isDefault) {
                        IconButton(onClick = { onDeleteUser(user) }) {
                            Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colors.error)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("添加用户")
            }
        }
    }

    if (showAddDialog) {
        var name by remember(showAddDialog) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("添加用户") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("昵称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isNotEmpty()) {
                        onAddUser(trimmed)
                        showAddDialog = false
                    }
                }) { Text("确认") }
            },
            dismissButton = {
                Button(onClick = { showAddDialog = false }) { Text("取消") }
            }
        )
    }

    if (editingUser != null) {
        AlertDialog(
            onDismissRequest = { editingUser = null },
            title = { Text("编辑昵称") },
            text = {
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("昵称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    val trimmed = editName.trim()
                    if (trimmed.isNotEmpty() && editingUser != null) {
                        onRenameUser(editingUser!!, trimmed)
                        editingUser = null
                    }
                }) { Text("确认") }
            },
            dismissButton = {
                Button(onClick = { editingUser = null }) { Text("取消") }
            }
        )
    }
}
