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

// Colors for Light Mode (isLight = true)
val ColorJsonKeyLight = Color(0xFF0F172A)
val ColorJsonStringLight = Color(0xFF0F766E)
val ColorJsonNumberLight = Color(0xFF1D4ED8)
val ColorJsonBooleanLight = Color(0xFFB91C1C)
val ColorJsonNullLight = Color(0xFF64748B)
val ColorJsonDefaultLight = Color(0xFF1E293B)

// Colors for Dark Mode (isLight = false)
val ColorJsonKeyDark = Color(0xFFF8FAFC)
val ColorJsonStringDark = Color(0xFF34D399)
val ColorJsonNumberDark = Color(0xFF38BDF8)
val ColorJsonBooleanDark = Color(0xFFF87171)
val ColorJsonNullDark = Color(0xFF94A3B8)
val ColorJsonDefaultDark = Color(0xFFE2E8F0)

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
    onSearchResultsChanged: (Int) -> Unit = {},
    onCurrentSearchPositionChanged: (String) -> Unit = {}
) {
    var rootNodes by remember { mutableStateOf<List<JsonNode>>(emptyList()) }
    var displayNodes by remember { mutableStateOf<List<JsonNode>>(emptyList()) }
    var rawText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(jsonString, initialType) {
        if (jsonString.isNullOrBlank()) {
            rootNodes = emptyList()
            displayNodes = emptyList()
            rawText = ""
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
        rawText = dataToParse

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

    val rawSearchResults = remember(rawText, searchQuery) {
        rawText.findSearchPositions(searchQuery)
    }

    val nodeSearchResults = remember(rootNodes, searchQuery) {
        rootNodes.collectSearchMatches(searchQuery)
    }

    LaunchedEffect(rawSearchResults.size, searchQuery, rawText) {
        onSearchResultsChanged(rawSearchResults.size)
    }

    LaunchedEffect(currentSearchIndex, searchQuery, rawSearchResults, nodeSearchResults) {
        val rawPosition = rawSearchResults.getOrNull(currentSearchIndex)
        onCurrentSearchPositionChanged(rawPosition?.label.orEmpty())

        if (rawPosition != null) {
            val target = nodeSearchResults.getOrNull(currentSearchIndex)
            if (target != null) {
                rootNodes.expandAncestors(target.ancestorIds)
                displayNodes = flatten(rootNodes)
                val targetIndex = displayNodes.indexOfFirst { it.id == target.nodeId }
                if (targetIndex >= 0) {
                    scope.launch {
                        listState.animateScrollToItem(targetIndex)
                    }
                }
            } else {
                val lineIndex = (rawPosition.line - 1).coerceAtLeast(0)
                if (lineIndex < displayNodes.size) {
                    scope.launch {
                        listState.animateScrollToItem(lineIndex)
                    }
                }
            }
        } else if (searchQuery.isBlank()) {
            onCurrentSearchPositionChanged("")
        }
    }

    LaunchedEffect(displayNodes, currentSearchIndex, nodeSearchResults) {
        val target = nodeSearchResults.getOrNull(currentSearchIndex)
        if (target != null) {
            val targetIndex = displayNodes.indexOfFirst { it.id == target.nodeId }
            if (targetIndex >= 0) {
                scope.launch {
                    listState.animateScrollToItem(targetIndex)
                }
            }
        }
    }

    LazyColumn(modifier = modifier, state = listState) {
        itemsIndexed(displayNodes, key = { _, node -> node.id }) { index, node ->
            val currentMatchNodeId = nodeSearchResults.getOrNull(currentSearchIndex)?.nodeId
            val isCurrentMatch = searchQuery.isNotBlank() && currentMatchNodeId == node.id

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

private data class JsonSearchPosition(
    val line: Int,
    val column: Int
) {
    val label: String = "Ln $line, Col $column"
}

private data class JsonNodeSearchMatch(
    val nodeId: String,
    val ancestorIds: List<String>
)

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

private fun String.findSearchPositions(query: String): List<JsonSearchPosition> {
    if (query.isBlank() || isEmpty()) return emptyList()
    val positions = mutableListOf<JsonSearchPosition>()
    var searchStart = 0
    while (searchStart < length) {
        val index = indexOf(query, searchStart, ignoreCase = true)
        if (index == -1) break
        positions.add(positionForIndex(index))
        searchStart = index + query.length.coerceAtLeast(1)
    }
    return positions
}

private fun String.positionForIndex(index: Int): JsonSearchPosition {
    var line = 1
    var column = 1
    val end = index.coerceIn(0, length)
    for (i in 0 until end) {
        if (this[i] == '\n') {
            line++
            column = 1
        } else {
            column++
        }
    }
    return JsonSearchPosition(line, column)
}

private fun List<JsonNode>.collectSearchMatches(query: String): List<JsonNodeSearchMatch> {
    if (query.isBlank()) return emptyList()
    val matches = mutableListOf<JsonNodeSearchMatch>()
    collectSearchMatches(query, emptyList(), matches)
    return matches
}

private fun List<JsonNode>.collectSearchMatches(
    query: String,
    ancestors: List<String>,
    matches: MutableList<JsonNodeSearchMatch>
) {
    for (node in this) {
        val searchableText = buildString {
            if (node.key != null) append("\"${node.key}\": ")
            append(node.value.orEmpty())
        }
        repeat(searchableText.countOccurrences(query)) {
            matches.add(JsonNodeSearchMatch(node.id, ancestors))
        }
        if (node.children.isNotEmpty()) {
            val nextAncestors = if (node.isExpandable) ancestors + node.id else ancestors
            node.children.collectSearchMatches(query, nextAncestors, matches)
        }
    }
}

private fun String.countOccurrences(query: String): Int {
    if (query.isBlank()) return 0
    var count = 0
    var searchStart = 0
    while (searchStart < length) {
        val index = indexOf(query, searchStart, ignoreCase = true)
        if (index == -1) break
        count++
        searchStart = index + query.length.coerceAtLeast(1)
    }
    return count
}

private fun List<JsonNode>.expandAncestors(ancestorIds: List<String>) {
    if (ancestorIds.isEmpty()) return
    forEach { node -> node.expandAncestors(ancestorIds.toSet()) }
}

private fun JsonNode.expandAncestors(ancestorIds: Set<String>) {
    if (id in ancestorIds) isExpanded = true
    children.forEach { it.expandAncestors(ancestorIds) }
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
    val defaultColor = if (isLight) ColorJsonDefaultLight else ColorJsonDefaultDark
    val keyColor = if (isLight) ColorJsonKeyLight else ColorJsonKeyDark

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
                color = defaultColor
            )
        } else {
            Spacer(modifier = Modifier.width(16.dp))
        }

        if (node.key != null) {
            val keyText = "\"${node.key}\": "
            Text(
                text = highlightText(keyText, searchQuery, keyColor),
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
            NodeType.STRING -> if (isLight) ColorJsonStringLight else ColorJsonStringDark
            NodeType.NUMBER -> if (isLight) ColorJsonNumberLight else ColorJsonNumberDark
            NodeType.BOOLEAN -> if (isLight) ColorJsonBooleanLight else ColorJsonBooleanDark
            NodeType.NULL -> if (isLight) ColorJsonNullLight else ColorJsonNullDark
            else -> defaultColor
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
