package com.example.audioguideai.data.model

enum class Category(
    val titleRu: String,
    val icon: String,
    val osmKey: String,
    val osmValues: List<String>? = null,
    val enabledByDefault: Boolean = true,
    val nodeOnly: Boolean = false,
) {
    HISTORICAL("Исторические объекты", "🏛️", "historic", null, true, false),
    RELIGIOUS_BUILDINGS(
        "Религиозные здания",
        "⛪",
        "building",
        listOf("church", "cathedral", "chapel", "mosque", "temple", "synagogue"),
        true,
        false
    ),
    RELIGION(
        "Религиозная принадлежность",
        "📿",
        "religion",
        listOf("christian", "muslim", "buddhist"),
        true,
        true // только node
    ),
    DENOMINATION(
        "Конфессии",
        "✝️",
        "denomination",
        listOf("orthodox", "catholic"),
        true,
        true // только node
    ),
    TOURISM(
        "Туристические",
        "🏨",
        "tourism",
        listOf("attraction", "museum", "artwork", "viewpoint", "information", "hotel", "guest_house"),
        true,
        false
    );
}
