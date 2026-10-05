package com.netlogger.lib.presentation.model

import kotlin.uuid.Uuid
import kotlin.uuid.ExperimentalUuidApi

enum class NodeType {
    STRING, NUMBER, BOOLEAN, NULL, OBJECT_START, ARRAY_START, OBJECT_END, ARRAY_END, EMPTY_OBJECT, EMPTY_ARRAY
}

@OptIn(ExperimentalUuidApi::class)
data class JsonNode(
    val id: String = Uuid.random().toString(),
    val key: String?,
    val value: String?,
    val isExpandable: Boolean,
    var isExpanded: Boolean,
    val depth: Int,
    var children: List<JsonNode> = emptyList(),
    val type: NodeType,
    val isLastItemInParent: Boolean = false
)
