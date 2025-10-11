package com.example.audioguideai.domain

import com.example.audioguideai.data.model.Category

object CategoryClassifier {
    private val rules: List<Pair<Category, Regex>> = listOf(
        Category.HISTORICAL to Regex("\b(battle|fortress|monument|memorial|ancient|крепост|битв|памятник|историч)\b", RegexOption.IGNORE_CASE),
        Category.STRUCTURES to Regex("\b(bridge|tower|fountain|виадук|мост|башн|фонтан)\b", RegexOption.IGNORE_CASE),
        Category.ART to Regex("\b(mural|graffiti|installation|арт|инсталляц|мурал|стеноп)\b", RegexOption.IGNORE_CASE),
        Category.NATURE to Regex("\b(park|waterfall|cliff|скал|водопад|парк|заповедн)\b", RegexOption.IGNORE_CASE),
        Category.ARCHITECTURE to Regex("\b(gothic|modern|constructivism|готик|модерн|конструктивизм|барокко)\b", RegexOption.IGNORE_CASE),
        Category.CULTURE to Regex("\b(museum|theatre|library|музей|театр|библиотек|выставоч)\b", RegexOption.IGNORE_CASE),
        Category.LEGENDS to Regex("\b(legend|myth|haunted|легенд|миф|привид)\b", RegexOption.IGNORE_CASE),
        Category.ROUTES to Regex("\b(trail|route|маршрут|тропа|начало маршрута)\b", RegexOption.IGNORE_CASE),
        Category.SCIENCE to Regex("\b(observatory|meteorological|technopark|обсерватор|метеостанц|технопарк)\b", RegexOption.IGNORE_CASE)
    )

    fun classify(title: String, description: String): Category {
        val text = "$title$description"
        for ((cat, rx) in rules) if (rx.containsMatchIn(text)) return cat
        return Category.HISTORICAL
    }
}
