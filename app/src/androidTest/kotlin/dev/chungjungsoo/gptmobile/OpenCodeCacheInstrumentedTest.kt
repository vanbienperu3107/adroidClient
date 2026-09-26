package dev.chungjungsoo.gptmobile

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.chungjungsoo.gptmobile.data.opencode.CachedOpenCodeSession
import dev.chungjungsoo.gptmobile.data.opencode.OpenCodeCacheAccess
import dev.chungjungsoo.gptmobile.data.opencode.OpenCodeCacheDatabase
import dev.chungjungsoo.gptmobile.data.opencode.OpenCodeHistoryMessage
import dev.chungjungsoo.gptmobile.data.opencode.OpenCodeHistoryPage
import dev.chungjungsoo.gptmobile.data.opencode.OpenCodeHistoryPart
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OpenCodeCacheInstrumentedTest {
    @Test fun partialPageAndServerPurgeAreIsolated() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, OpenCodeCacheDatabase::class.java).build()
        try {
            val dao = db.cacheDao()
            val a = CachedOpenCodeSession("a", "/A", "ses_same", 1, "A", 1, "idle", 1)
            val b = a.copy(serverId = "b", title = "B")
            dao.upsertPage("a", "/A", 1, listOf(a))
            dao.upsertPage("b", "/A", 1, listOf(b))
            dao.upsertPage("a", "/A", 1, emptyList())
            assertEquals(1, dao.sessions("a", "/A", 1).first().size)
            val page = OpenCodeHistoryPage(listOf(OpenCodeHistoryMessage("msg_a", "user", listOf(OpenCodeHistoryPart("prt_a", "text", "synthetic")))))
            dao.storeHistory("a", "/A", "ses_same", 1, page)
            dao.setAccess(OpenCodeCacheAccess("a", 1, true))
            assertTrue(dao.access("a")!!.denied)
            dao.purgeObsolete("a", null)
            assertTrue(dao.parts("a", "/A", "ses_same").isEmpty())
            assertTrue(dao.sessions("a", "/A", 1).first().isEmpty())
            assertEquals(1, dao.sessions("b", "/A", 1).first().size)
        } finally {
            db.close()
        }
    }
}
