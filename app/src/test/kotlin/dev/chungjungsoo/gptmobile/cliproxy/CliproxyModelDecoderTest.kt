package dev.chungjungsoo.gptmobile.cliproxy

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

class CliproxyModelDecoderTest {
    @Test fun keepsOnlyModelIdsFromOpenAiCompatibleCatalog() {
        val root = Json.parseToJsonElement("""{"data":[{"id":"gpt-5.6-terra"},{"id":"gpt-6-astra"}]}""").jsonObject
        val ids = root["data"]!!.jsonArray.map { it.jsonObject["id"]!!.jsonPrimitive.content }
        assertEquals(listOf("gpt-5.6-terra", "gpt-6-astra"), ids)
    }
}
