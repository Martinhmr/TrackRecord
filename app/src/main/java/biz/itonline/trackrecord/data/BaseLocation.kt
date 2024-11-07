package biz.itonline.trackrecord.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class BaseLocation(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val accuracy: Float,
)

fun parseJsonToBaseLocation(jsonString: String?): List<BaseLocation>? {
    return try {

        Json.decodeFromString(jsonString!!)
    } catch (e: Exception) {
        null
    }
}

fun parseBaseLocationToJson(baseLocation: List<BaseLocation>): String {
       return Json.encodeToString(baseLocation)
}