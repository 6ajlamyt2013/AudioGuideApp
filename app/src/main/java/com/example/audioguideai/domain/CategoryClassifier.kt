package com.example.audioguideai.domain

import com.example.audioguideai.data.model.Category

object CategoryClassifier {
    private val rules: List<Pair<Category, Regex>> = listOf(
        Category.HISTORICAL to Regex("\\b(battle|fortress|monument|memorial|ancient|крепост|битв|памятник|историч)\\b", RegexOption.IGNORE_CASE),
        Category.RELIGIOUS_BUILDINGS to Regex("\\b(church|cathedral|chapel|mosque|temple|synagogue|церковь|собор|мечеть|храм|синагог)\\b", RegexOption.IGNORE_CASE),
        Category.RELIGION to Regex("\\b(christian|muslim|buddhist|христиан|мусульман|буддист)\\b", RegexOption.IGNORE_CASE),
        Category.DENOMINATION to Regex("\\b(orthodox|catholic|православн|католич)\\b", RegexOption.IGNORE_CASE),
        Category.TOURISM to Regex("\\b(attraction|museum|artwork|viewpoint|information|hotel|guest_house|достопримечательн|музей|арт|обзорная|информационн|отель|гостевой)\\b", RegexOption.IGNORE_CASE),
    )

    fun classify(title: String, description: String): Category {
        val text = "$title $description"
        for ((cat, rx) in rules) if (rx.containsMatchIn(text)) return cat
        return Category.TOURISM
    }
}
