package com.netlogger.lib.presentation.ui.filter

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetloggerFilterBottomSheet(
    initialMethods: Set<String>,
    initialStatus: Set<String>,
    onApply: (methods: Set<String>, status: Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMethods by remember { mutableStateOf(initialMethods) }
    var selectedStatus by remember { mutableStateOf(initialStatus) }

    ModalBottomSheet(
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        NetloggerFilterContent(
            selectedMethods = selectedMethods,
            selectedStatus = selectedStatus,
            onMethodsChange = { selectedMethods = it },
            onStatusChange = { selectedStatus = it },
            onApply = { onApply(selectedMethods, selectedStatus) }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NetloggerFilterContent(
    selectedMethods: Set<String>,
    selectedStatus: Set<String>,
    onMethodsChange: (Set<String>) -> Unit,
    onStatusChange: (Set<String>) -> Unit,
    onApply: () -> Unit
) {
    val methods = listOf("GET", "POST", "PUT", "DELETE", "PATCH")
    val statusGroups =
        listOf("2xx Success", "3xx Redirection", "4xx Client Error", "5xx Server Error")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text("Filters", fontSize = 18.sp, fontWeight = FontWeight.Bold)

        // Filter by Method
        Column {
            Text(
                "Filter by Method",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 200.dp)
            ) {
                items(methods) { method ->
                    val isSelected = selectedMethods.contains(method)
                    MethodCheckboxItem(
                        label = method,
                        checked = isSelected,
                        onCheckedChange = { checked ->
                            onMethodsChange(if (checked) selectedMethods + method else selectedMethods - method)
                        }
                    )
                }
            }
        }

        // Filter by Status
        Column {
            Text(
                "Filter by Status",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            Spacer(modifier = Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                statusGroups.forEach { group ->
                    val isSelected = selectedStatus.contains(group)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            onStatusChange(if (!isSelected) selectedStatus + group else selectedStatus - group)
                        },
                        label = { Text(group) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFE0F2F1),
                            selectedLabelColor = Color(0xFF00796B)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = Color(0xFFB8C6C3),
                            selectedBorderColor = Color(0xFF00796B)
                        )
                    )
                }
            }
        }

        Button(
            onClick = onApply,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Apply Filters", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MethodCheckboxItem(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(4.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF00796B))
        )
        Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun NetloggerFilterPreview() {
    MaterialTheme {
        var selectedMethods by remember { mutableStateOf(setOf("GET", "POST")) }
        var selectedStatus by remember { mutableStateOf(setOf("2xx Success", "5xx Server Error")) }
        NetloggerFilterContent(
            selectedMethods = selectedMethods,
            selectedStatus = selectedStatus,
            onMethodsChange = {
                selectedMethods = it
            },
            onStatusChange = {
                selectedStatus = it
            },
            onApply = {}
        )
    }
}
