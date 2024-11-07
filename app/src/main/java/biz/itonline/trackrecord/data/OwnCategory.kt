package biz.itonline.trackrecord.data

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.json.JSONObject

@Serializable
data class OwnCategory(
    @SerialName("ID_category")
    val categoryId: String,
    @SerialName("name_category")
    val categoryName: String,
    @SerialName("note_category")
    val categoryNote: String,
    val trackPoly: String,
    @Contextual
    @Serializable(with = JSONObjectSerializer::class)
    val param: JSONObject?
)