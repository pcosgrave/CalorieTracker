package com.philipcosgrave.calorietracker.domain

import com.philipcosgrave.calorietracker.model.*
import kotlinx.coroutines.CancellationException

interface ReferenceFoodRepository {
    suspend fun search(query: String): List<ReferenceFood>
    suspend fun getFood(id: String): ReferenceFood?
}
data class NormalizedFoodName(val canonicalName: String, val attributes: List<String>, val confidence: Double)
fun interface FoodNameNormalizer { suspend fun normalize(query: String): NormalizedFoodName? }

object FoodSearchMatching {
    private val aliases = mapOf("bell pepper" to "sweet pepper", "scallion" to "green onion", "cilantro" to "coriander", "minced beef" to "ground beef")
    private val sizes = setOf("large", "small", "medium", "a", "an", "the", "of")
    fun words(text: String): List<String> = text.lowercase(java.util.Locale.ROOT)
        .replace(Regex("[^a-z0-9.%]+"), " ").trim().split(Regex("\\s+"))
        .filter { it.isNotBlank() }.map { if (it.length > 3 && it.endsWith("s") && !it.endsWith("ss")) it.dropLast(1) else it }
    fun tokens(text: String): List<String> {
        var name = words(text).joinToString(" ")
        aliases.forEach { (from, to) -> name = name.replace(from, to) }
        return words(name).filterNot { it in sizes }
    }
    fun score(query: String, name: String): Int {
        val q = tokens(query); val n = tokens(name)
        if (q.isEmpty()) return 0
        val exact = q.count { it in n }
        val prefix = q.count { a -> a !in n && n.any { it.startsWith(a) } }
        val fuzzy = q.count { a -> a !in n && n.none { it.startsWith(a) } && a.length >= 4 && n.any { editDistance(a, it) <= 1 } }
        if (exact + prefix + fuzzy < q.size) return 0
        return (if (q == n) 1200 else 1000) + exact * 20 + prefix * 10 - fuzzy * 120 - n.size
    }
    fun strong(query: String, name: String) = tokens(query).all { it in tokens(name) } && tokens(query).isNotEmpty()
    private fun editDistance(a: String, b: String): Int {
        if (kotlin.math.abs(a.length - b.length) > 1) return 2
        var prev = IntArray(b.length + 1) { it }
        for (i in a.indices) {
            val next = IntArray(b.length + 1); next[0] = i + 1
            for (j in b.indices) next[j + 1] = minOf(next[j] + 1, prev[j + 1] + 1, prev[j] + if (a[i] == b[j]) 0 else 1)
            prev = next
        }
        return prev.last()
    }
}

class IngredientSearch(private val repository: ReferenceFoodRepository, private val normalizer: FoodNameNormalizer) {
    private val cache = mutableMapOf<String, NormalizedFoodName?>()
    suspend fun search(query: String, local: List<FoodItem>): List<ReferenceFood> {
        val direct = repository.search(query)
        val strong = local.any { FoodSearchMatching.strong(query, it.name) } || direct.any { FoodSearchMatching.strong(query, it.name) }
        val results = if (strong) direct else {
            val key = FoodSearchMatching.words(query).joinToString(" ")
            val normalized = if (cache.containsKey(key)) cache[key] else try {
                normalizer.normalize(query).also { cache[key] = it; if (cache.size > 100) cache.remove(cache.keys.first()) }
            } catch (e: CancellationException) { throw e } catch (_: Exception) { cache[key] = null; null }
            val inferred = normalized?.takeIf { it.confidence >= .75 && it.canonicalName.length in 2..120 }
            if (inferred == null) direct else (direct + repository.search(inferred.canonicalName + " " + inferred.attributes.joinToString(" "))).distinctBy { it.id }
        }
        return results.filterNot { ref -> local.any { it.source == ref.source && it.sourceId == ref.sourceFoodId } }
    }
}
