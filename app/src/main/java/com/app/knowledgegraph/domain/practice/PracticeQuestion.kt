package com.app.knowledgegraph.domain.practice

data class PracticeQuestion(
    val id: String,
    val chapter: String,
    val stem: String,
    val methods: List<String>,
    val correctMethod: String,
    val triggerWords: Map<String, String>,
    val conditions: List<ConditionCheck>,
    val explanation: String,
    val commonTraps: String = ""
)

data class ConditionCheck(
    val label: String,
    val value: Boolean,
    val hint: String
)
