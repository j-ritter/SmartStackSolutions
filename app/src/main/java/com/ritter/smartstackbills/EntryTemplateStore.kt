package com.ritter.smartstackbills

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object EntryTemplateStore {
    enum class Type(val key: String) {
        OPEN_PAYMENT("open_payment"),
        CLOSED_PAYMENT("closed_payment"),
        INCOME("income")
    }

    data class Template(
        val id: String,
        val type: Type,
        val templateName: String,
        val title: String,
        val amount: Double,
        val category: String,
        val subcategory: String,
        val vendorOrSource: String,
        val repeat: String,
        val comment: String
    )

    private const val PREFS = "entry_templates"
    private const val KEY = "templates"

    fun templates(context: Context, type: Type): List<Template> =
        readAll(context).filter { it.type == type }.sortedBy { it.templateName.lowercase() }

    fun save(context: Context, template: Template) {
        val existing = readAll(context).filterNot { it.id == template.id }
        writeAll(context, (existing + template).takeLast(30))
    }

    private fun readAll(context: Context): List<Template> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]").orEmpty()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val json = array.optJSONObject(index) ?: return@mapNotNull null
                val type = Type.values().firstOrNull { it.key == json.optString("type") } ?: return@mapNotNull null
                Template(
                    id = json.optString("id"),
                    type = type,
                    templateName = json.optString("templateName"),
                    title = json.optString("title"),
                    amount = json.optDouble("amount", 0.0),
                    category = json.optString("category"),
                    subcategory = json.optString("subcategory"),
                    vendorOrSource = json.optString("vendorOrSource"),
                    repeat = json.optString("repeat", "No"),
                    comment = json.optString("comment")
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun writeAll(context: Context, templates: List<Template>) {
        val array = JSONArray()
        templates.forEach { template ->
            array.put(
                JSONObject()
                    .put("id", template.id)
                    .put("type", template.type.key)
                    .put("templateName", template.templateName)
                    .put("title", template.title)
                    .put("amount", template.amount)
                    .put("category", template.category)
                    .put("subcategory", template.subcategory)
                    .put("vendorOrSource", template.vendorOrSource)
                    .put("repeat", template.repeat)
                    .put("comment", template.comment)
            )
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, array.toString())
            .apply()
    }
}
