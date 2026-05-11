package com.netlogger.lib.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.netlogger.lib.presentation.model.JsonNode
import com.netlogger.lib.presentation.model.NodeType
import kotlinx.coroutines.launch

val ColorJsonString = Color(0xFF008000)
val ColorJsonNumber = Color(0xFF0000FF)
val ColorJsonBoolean = Color(0xFFB22222)
val ColorJsonNull = Color(0xFF808080)
val ColorSearchHighlight = Color(0xFFFFEB3B)
val ColorSearchCurrentHighlight = Color(0xFFFF9800)

@Composable
fun JsonViewer(
    jsonString: String?,
    modifier: Modifier = Modifier,
    initialType: Int = 0,
    isLight: Boolean = false,
    searchQuery: String = "",
    currentSearchIndex: Int = -1,
    onSearchResultsChanged: (Int) -> Unit = {}
) {
    var rootNodes by remember { mutableStateOf<List<JsonNode>>(emptyList()) }
    var displayNodes by remember { mutableStateOf<List<JsonNode>>(emptyList()) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

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

    // Search logic
    val searchResults = remember(displayNodes, searchQuery) {
        if (searchQuery.isBlank()) emptyList<Int>()
        else {
            displayNodes.mapIndexedNotNull { index, node ->
                val textToSearch = "${node.key ?: ""}${node.value}"
                if (textToSearch.contains(searchQuery, ignoreCase = true)) index else null
            }
        }
    }

    LaunchedEffect(searchResults.size) {
        onSearchResultsChanged(searchResults.size)
    }

    LaunchedEffect(currentSearchIndex, searchResults) {
        if (currentSearchIndex in searchResults.indices) {
            scope.launch {
                listState.animateScrollToItem(searchResults[currentSearchIndex])
            }
        }
    }

    LazyColumn(modifier = modifier, state = listState) {
        itemsIndexed(displayNodes, key = { _, node -> node.id }) { index, node ->
            val isCurrentMatch = searchQuery.isNotBlank() && 
                    currentSearchIndex in searchResults.indices && 
                    searchResults[currentSearchIndex] == index

            JsonNodeRow(
                node = node,
                onToggle = {
                    node.isExpanded = !node.isExpanded
                    displayNodes = flatten(rootNodes)
                },
                isLight = isLight,
                searchQuery = searchQuery,
                isCurrentMatch = isCurrentMatch
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
private fun JsonNodeRow(
    isLight: Boolean = false,
    node: JsonNode,
    onToggle: () -> Unit,
    searchQuery: String = "",
    isCurrentMatch: Boolean = false
) {
    val backgroundColor = if (isCurrentMatch) ColorSearchCurrentHighlight else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
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
            val keyText = "\"${node.key}\": "
            Text(
                text = highlightText(keyText, searchQuery, if (isLight) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.background),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
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
            text = highlightText(displayValue, searchQuery, color),
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
    }
}

fun highlightText(text: String, query: String, baseColor: Color): AnnotatedString {
    if (query.isBlank() || !text.contains(query, ignoreCase = true)) {
        return AnnotatedString(text, SpanStyle(color = baseColor))
    }

    return buildAnnotatedString {
        var start = 0
        while (start < text.length) {
            val index = text.indexOf(query, start, ignoreCase = true)
            if (index == -1) {
                withStyle(SpanStyle(color = baseColor)) {
                    append(text.substring(start))
                }
                break
            } else {
                withStyle(SpanStyle(color = baseColor)) {
                    append(text.substring(start, index))
                }
                withStyle(SpanStyle(color = Color.Black, background = ColorSearchHighlight, fontWeight = FontWeight.Bold)) {
                    append(text.substring(index, index + query.length))
                }
                start = index + query.length
            }
        }
    }
}
