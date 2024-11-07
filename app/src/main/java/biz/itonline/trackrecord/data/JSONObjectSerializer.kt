package biz.itonline.trackrecord.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.json.JSONObject

object JSONObjectSerializer : KSerializer<JSONObject> {
    private val jsonElementSerializer = JsonElement.Companion.serializer()

    override val descriptor = jsonElementSerializer.descriptor

    override fun serialize(encoder: Encoder, value: JSONObject) {
        val jsonElement = Json.parseToJsonElement(value.toString())
        jsonElementSerializer.serialize(encoder, jsonElement)
    }

    override fun deserialize(decoder: Decoder): JSONObject {
        val jsonElement = jsonElementSerializer.deserialize(decoder)
        return JSONObject(jsonElement.toString())
    }
}