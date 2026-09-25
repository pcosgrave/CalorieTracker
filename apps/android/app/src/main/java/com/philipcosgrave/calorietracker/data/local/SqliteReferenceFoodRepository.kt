package com.philipcosgrave.calorietracker.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.philipcosgrave.calorietracker.domain.*
import com.philipcosgrave.calorietracker.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class SqliteReferenceFoodRepository(context: Context) : ReferenceFoodRepository {
    private val app = context.applicationContext
    private val records by lazy {
        val file = java.io.File(app.filesDir, "cnf-reference-2026-v1.db")
        if (!file.exists()) {
            val temporary = java.io.File(app.filesDir, "cnf-reference-2026-v1.tmp")
            app.assets.open("cnf.db").use { input -> temporary.outputStream().use { input.copyTo(it) } }
            check(temporary.renameTo(file)) { "Unable to install reference foods" }
        }
        SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            db.rawQuery("SELECT id,name,normalized_name,food_group,nutrients,servings FROM foods", null).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        val n = JSONObject(cursor.getString(4)); val a = n.getJSONObject("additional")
                        val s = JSONArray(cursor.getString(5))
                        add(ReferenceFood("cnf:${cursor.getString(0)}", "CNF", cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3),
                            Nutrients(n.getDouble("calories"), n.getDouble("proteinGrams"), n.getDouble("carbohydrateGrams"), n.getDouble("fatGrams"), a.keys().asSequence().associateWith { a.getDouble(it) }),
                            List(s.length()) { i -> s.getJSONObject(i).let { ReferenceServing(it.getString("id"), it.getString("description"), it.getDouble("grams")) } }))
                    }
                }
            }
        }
    }
    override suspend fun search(query: String): List<ReferenceFood> = withContext(Dispatchers.IO) {
        records.map { it to FoodSearchMatching.score(query, it.normalizedName) }.filter { it.second > 0 }
            .sortedWith(compareByDescending<Pair<ReferenceFood, Int>> { it.second }.thenBy { it.first.name })
            .take(30).map { it.first }
    }
    override suspend fun getFood(id: String): ReferenceFood? = withContext(Dispatchers.IO) { records.firstOrNull { it.id == id } }
}
