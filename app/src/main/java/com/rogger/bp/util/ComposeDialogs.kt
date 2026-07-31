package com.rogger.bp.util

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rogger.bp.R
import com.rogger.bp.data.model.PostCategory

@Composable
fun CategorySelectionDialog(
    categories: List<PostCategory>,
    onDismiss: () -> Unit,
    onCategorySelected: (String, String) -> Unit,
    onAddCategory: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.dialog_title_select),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (categories.isEmpty()) {
                    Text(
                        text = "Nenhuma categoria encontrada.",
                        modifier = Modifier.padding(vertical = 16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                    ) {
                        items(categories) { category ->
                            Text(
                                text = category.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onCategorySelected(category.firestoreId, category.name)
                                        onDismiss()
                                    }
                                    .padding(vertical = 12.dp, horizontal = 12.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                TextButton(
                    onClick = {
                        onAddCategory()
                        onDismiss()
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Criar Nova Categoria")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.dialog_button_cancel))
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
fun DeleteConfirmationDialog(
    title: String = stringResource(id = R.string.dialog_title),
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(id = R.string.excluir))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.dialog_button_cancel))
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}
