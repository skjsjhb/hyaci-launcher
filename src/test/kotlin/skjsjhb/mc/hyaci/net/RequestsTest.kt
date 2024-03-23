package skjsjhb.mc.hyaci.net

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import skjsjhb.mc.hyaci.util.getString
import skjsjhb.mc.hyaci.util.gets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RequestsTest {
    @Test
    fun `Test POST JSON`() {
        Requests.postJson("https://jsonplaceholder.typicode.com/posts", mapOf(), buildJsonObject {
            put("title", "foo")
            put("body", "bar")
            put("userId", 1)
        }).let {
            assertEquals("foo", it.getString("title"))
            assertEquals("bar", it.getString("body"))
            assertNotNull(it.gets("id"))
        }
    }
}