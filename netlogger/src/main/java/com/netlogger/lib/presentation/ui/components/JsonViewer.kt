package com.netlogger.lib.presentation.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.netlogger.lib.presentation.model.JsonNode
import com.netlogger.lib.presentation.model.NodeType

val ColorJsonString = Color(0xFF008000)
val ColorJsonNumber = Color(0xFF0000FF)
val ColorJsonBoolean = Color(0xFFB22222)
val ColorJsonNull = Color(0xFF808080)

@Composable
fun JsonViewer(jsonString: String?, modifier: Modifier = Modifier, initialType: Int = 0, isLight: Boolean = false) {
    var rootNodes by remember { mutableStateOf<List<JsonNode>>(emptyList()) }
    var displayNodes by remember { mutableStateOf<List<JsonNode>>(emptyList()) }

    LaunchedEffect(jsonString, initialType) {
        if (jsonString.isNullOrBlank()) {
            rootNodes = emptyList()
            displayNodes = emptyList()
            return@LaunchedEffect
        }
        val dataToParse = try {
            val element = JsonParser.parseString(jsonString).asJsonObject
            when (initialType) {
                1 -> element.get("requestBody")?.asString ?: "{}"
                2 -> element.get("responseBody")?.asString ?: "{}"
                else -> jsonString
            }
        } catch (e: Exception) {
            jsonString
        }

        try {
            val element = JsonParser.parseString(dataToParse)
            val rootNode = buildJsonTree(element, null, 0, true)
            rootNodes = listOf(rootNode)
        } catch (e: Exception) {
            rootNodes = listOf(
                JsonNode(
                    key = null,
                    value = dataToParse,
                    isExpandable = false,
                    isExpanded = false,
                    depth = 0,
                    type = NodeType.STRING
                )
            )
        }
        displayNodes = flatten(rootNodes)
    }

    LazyColumn(modifier = modifier) {
        items(displayNodes, key = { it.id }) { node ->
            JsonNodeRow(
                node = node, onToggle = {
                    node.isExpanded = !node.isExpanded
                    displayNodes = flatten(rootNodes)
                },
                isLight = isLight
            )
        }
    }
}

private fun buildJsonTree(
    element: JsonElement,
    key: String?,
    depth: Int,
    isLast: Boolean
): JsonNode {
    val isExpanded = depth < 4
    if (element.isJsonObject) {
        val obj = element.asJsonObject
        if (obj.size() == 0) return JsonNode(
            key = key,
            value = "{}",
            isExpandable = false,
            isExpanded = false,
            depth = depth,
            type = NodeType.EMPTY_OBJECT,
            isLastItemInParent = isLast
        )
        val children = mutableListOf<JsonNode>()
        val entries = obj.entrySet().toList()
        for (i in entries.indices) {
            children.add(
                buildJsonTree(
                    entries[i].value,
                    entries[i].key,
                    depth + 1,
                    i == entries.size - 1
                )
            )
        }
        return JsonNode(
            key = key,
            value = "{",
            isExpandable = true,
            isExpanded = isExpanded,
            depth = depth,
            children = children,
            type = NodeType.OBJECT_START,
            isLastItemInParent = isLast
        )
    } else if (element.isJsonArray) {
        val arr = element.asJsonArray
        if (arr.size() == 0) return JsonNode(
            key = key,
            value = "[]",
            isExpandable = false,
            isExpanded = false,
            depth = depth,
            type = NodeType.EMPTY_ARRAY,
            isLastItemInParent = isLast
        )
        val children = mutableListOf<JsonNode>()
        for (i in 0 until arr.size()) {
            children.add(buildJsonTree(arr[i], null, depth + 1, i == arr.size() - 1))
        }
        return JsonNode(
            key = key,
            value = "[",
            isExpandable = true,
            isExpanded = isExpanded,
            depth = depth,
            children = children,
            type = NodeType.ARRAY_START,
            isLastItemInParent = isLast
        )
    } else if (element.isJsonNull) {
        return JsonNode(
            key = key,
            value = "null",
            isExpandable = false,
            isExpanded = false,
            depth = depth,
            type = NodeType.NULL,
            isLastItemInParent = isLast
        )
    } else {
        val prim = element.asJsonPrimitive
        val type =
            if (prim.isBoolean) NodeType.BOOLEAN else if (prim.isNumber) NodeType.NUMBER else NodeType.STRING
        val value = if (prim.isString) "\"${prim.asString}\"" else prim.asString
        return JsonNode(
            key = key,
            value = value,
            isExpandable = false,
            isExpanded = false,
            depth = depth,
            type = type,
            isLastItemInParent = isLast
        )
    }
}

private fun flatten(nodes: List<JsonNode>): List<JsonNode> {
    val result = mutableListOf<JsonNode>()
    for (node in nodes) {
        result.add(node)
        if (node.isExpandable && node.isExpanded) {
            result.addAll(flatten(node.children))
            val endValue = if (node.type == NodeType.OBJECT_START) "}" else "]"
            val endNode = JsonNode(
                key = null,
                value = endValue + if (!node.isLastItemInParent) "," else "",
                isExpandable = false,
                isExpanded = false,
                depth = node.depth,
                type = if (node.type == NodeType.OBJECT_START) NodeType.OBJECT_END else NodeType.ARRAY_END
            )
            result.add(endNode)
        }
    }
    return result
}

@Composable
private fun JsonNodeRow(isLight: Boolean = false, node: JsonNode, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = node.isExpandable, onClick = onToggle)
            .padding(start = (node.depth * 16).dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (node.isExpandable) {
            Text(
                text = if (node.isExpanded) "▼" else "▶",
                fontSize = 10.sp,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .width(12.dp),
                color = MaterialTheme.colorScheme.onBackground
            )
        } else {
            Spacer(modifier = Modifier.width(16.dp))
        }

        if (node.key != null) {
            Text(
                text = "\"${node.key}\": ",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = if (isLight) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.background
            )
        }

        val displayValue = if (node.isExpandable && !node.isExpanded) {
            val collapsedValue = if (node.type == NodeType.OBJECT_START) "{ ... }" else "[ ... ]"
            collapsedValue + if (!node.isLastItemInParent) "," else ""
        } else {
            node.value + if (!node.isExpandable && !node.isLastItemInParent && node.type != NodeType.OBJECT_END && node.type != NodeType.ARRAY_END) "," else ""
        }

        val color = when (node.type) {
            NodeType.STRING -> ColorJsonString
            NodeType.NUMBER -> ColorJsonNumber
            NodeType.BOOLEAN -> ColorJsonBoolean
            NodeType.NULL -> ColorJsonNull
            else -> MaterialTheme.colorScheme.onBackground
        }

        Text(
            text = displayValue,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = color
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun JsonViewerPreview() {
    MaterialTheme {
        val sampleJson = """
            {
                "status": 200,
                "message": "Success",
                "data": {
                    "id": 12345,
                    "isActive": true,
                    "items": ["apple", "banana", null]
                }
            }
        """.trimIndent()
        Box(modifier = Modifier.padding(16.dp)) {
            JsonViewer(jsonString = sampleJson)
        }
    }
}
