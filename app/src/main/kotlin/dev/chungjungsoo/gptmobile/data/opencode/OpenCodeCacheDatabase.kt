package dev.chungjungsoo.gptmobile.data.opencode

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

/** Cache only. Profile configuration remains exclusively in the Feature 01 DataStore. */
@Entity(tableName = "opencode_session_cache", primaryKeys = ["serverId", "directory", "sessionId"])
data class CachedOpenCodeSession(
    val serverId: String,
    val directory: String,
    val sessionId: String,
    val profileRevision: Long,
    val title: String,
    val updatedAt: Long,
    val status: String = "unknown",
    val fetchedAt: Long
)

@Entity(tableName = "opencode_projects", primaryKeys = ["serverId", "directory"])
data class CachedOpenCodeProject(val serverId: String, val directory: String, val remoteId: String, val profileRevision: Long)

@Entity(tableName = "opencode_messages", primaryKeys = ["serverId", "directory", "sessionId", "messageId"])
data class CachedOpenCodeMessage(val serverId: String, val directory: String, val sessionId: String, val messageId: String, val profileRevision: Long, val role: String, val position: Int)

@Entity(
    tableName = "opencode_parts",
    primaryKeys = ["serverId", "directory", "sessionId", "messageId", "partId"],
    foreignKeys = [
        ForeignKey(
            entity = CachedOpenCodeMessage::class,
            parentColumns = ["serverId", "directory", "sessionId", "messageId"],
            childColumns = ["serverId", "directory", "sessionId", "messageId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["serverId", "directory", "sessionId", "messageId"])]
)
data class CachedOpenCodePart(val serverId: String, val directory: String, val sessionId: String, val messageId: String, val partId: String, val type: String, val text: String?, val position: Int)

@Entity(tableName = "opencode_access", primaryKeys = ["serverId"])
data class OpenCodeCacheAccess(val serverId: String, val revision: Long, val denied: Boolean)

@Entity(tableName = "opencode_pending_prompts", primaryKeys = ["serverId", "directory", "sessionId", "clientMessageId"])
data class CachedOpenCodePendingPrompt(
    val serverId: String,
    val directory: String,
    val sessionId: String,
    val clientMessageId: String,
    val profileRevision: Long,
    val content: String,
    val state: String,
    val uncertainty: String?,
    val createdAt: Long
)

@Entity(tableName = "opencode_interactions", primaryKeys = ["serverId", "directory", "sessionId", "requestId"])
data class CachedOpenCodeInteraction(
    val serverId: String,
    val directory: String,
    val sessionId: String,
    val requestId: String,
    val profileRevision: Long,
    val kind: String,
    val title: String,
    val details: String,
    val allowsAlways: Boolean,
    val state: String
)

@Dao
abstract class OpenCodeCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun setAccess(access: OpenCodeCacheAccess)

    @Query("SELECT * FROM opencode_access WHERE serverId=:serverId")
    abstract suspend fun access(serverId: String): OpenCodeCacheAccess?

    @Query("SELECT serverId FROM opencode_session_cache UNION SELECT serverId FROM opencode_projects UNION SELECT serverId FROM opencode_messages UNION SELECT serverId FROM opencode_access UNION SELECT serverId FROM opencode_pending_prompts UNION SELECT serverId FROM opencode_interactions")
    abstract suspend fun serverIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertPendingPrompt(prompt: CachedOpenCodePendingPrompt)

    @Query("UPDATE opencode_pending_prompts SET state=:state, uncertainty=:uncertainty WHERE serverId=:serverId AND directory=:directory AND sessionId=:sessionId AND clientMessageId=:clientMessageId")
    abstract suspend fun updatePendingPrompt(serverId: String, directory: String, sessionId: String, clientMessageId: String, state: String, uncertainty: String? = null)

    @Query("UPDATE opencode_pending_prompts SET state='UNKNOWN', uncertainty='App restarted before request outcome was known' WHERE serverId=:serverId AND directory=:directory AND sessionId=:sessionId AND profileRevision=:revision AND state IN ('PENDING', 'SENDING')")
    abstract suspend fun recoverPendingPrompts(serverId: String, directory: String, sessionId: String, revision: Long)

    @Query("SELECT * FROM opencode_pending_prompts WHERE serverId=:serverId AND directory=:directory AND sessionId=:sessionId AND profileRevision=:revision ORDER BY createdAt")
    abstract suspend fun pendingPrompts(serverId: String, directory: String, sessionId: String, revision: Long): List<CachedOpenCodePendingPrompt>

    @Query("SELECT COUNT(*) FROM opencode_pending_prompts WHERE serverId=:serverId AND directory=:directory AND sessionId=:sessionId AND profileRevision=:revision AND state IN ('PENDING', 'SENDING', 'ACCEPTED')")
    abstract suspend fun activePromptCount(serverId: String, directory: String, sessionId: String, revision: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertInteractions(rows: List<CachedOpenCodeInteraction>)

    @Query("SELECT * FROM opencode_interactions WHERE serverId=:serverId AND directory=:directory AND sessionId=:sessionId AND profileRevision=:revision AND state='PENDING' ORDER BY requestId")
    abstract suspend fun interactions(serverId: String, directory: String, sessionId: String, revision: Long): List<CachedOpenCodeInteraction>

    @Query("UPDATE opencode_interactions SET state=:state WHERE serverId=:serverId AND directory=:directory AND sessionId=:sessionId AND requestId=:requestId")
    abstract suspend fun updateInteraction(serverId: String, directory: String, sessionId: String, requestId: String, state: String)

    @Query("DELETE FROM opencode_projects WHERE serverId=:serverId AND (:revision IS NULL OR profileRevision != :revision)")
    protected abstract suspend fun purgeProjects(serverId: String, revision: Long?)

    @Query("DELETE FROM opencode_messages WHERE serverId=:serverId AND (:revision IS NULL OR profileRevision != :revision)")
    protected abstract suspend fun purgeMessages(serverId: String, revision: Long?)

    @Query("DELETE FROM opencode_session_cache WHERE serverId=:serverId AND (:revision IS NULL OR profileRevision != :revision)")
    protected abstract suspend fun purgeSessions(serverId: String, revision: Long?)

    @Query("DELETE FROM opencode_access WHERE serverId=:serverId AND (:revision IS NULL OR revision != :revision)")
    protected abstract suspend fun purgeAccess(serverId: String, revision: Long?)

    @Query("DELETE FROM opencode_pending_prompts WHERE serverId=:serverId AND (:revision IS NULL OR profileRevision != :revision)")
    protected abstract suspend fun purgePendingPrompts(serverId: String, revision: Long?)

    @Query("DELETE FROM opencode_interactions WHERE serverId=:serverId AND (:revision IS NULL OR profileRevision != :revision)")
    protected abstract suspend fun purgeInteractions(serverId: String, revision: Long?)

    @Transaction
    open suspend fun purgeObsolete(serverId: String, revision: Long?) {
        purgeProjects(serverId, revision)
        purgeMessages(serverId, revision)
        purgeSessions(serverId, revision)
        purgeAccess(serverId, revision)
        purgePendingPrompts(serverId, revision)
        purgeInteractions(serverId, revision)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertProjects(rows: List<CachedOpenCodeProject>)

    @Query("SELECT * FROM opencode_projects WHERE serverId=:serverId AND profileRevision=:revision ORDER BY directory")
    abstract suspend fun projects(serverId: String, revision: Long): List<CachedOpenCodeProject>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun upsertMessages(rows: List<CachedOpenCodeMessage>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun upsertParts(rows: List<CachedOpenCodePart>)

    @Query("SELECT * FROM opencode_messages WHERE serverId=:serverId AND directory=:directory AND sessionId=:sessionId AND profileRevision=:revision ORDER BY messageId")
    abstract suspend fun messages(serverId: String, directory: String, sessionId: String, revision: Long): List<CachedOpenCodeMessage>

    @Query("SELECT * FROM opencode_parts WHERE serverId=:serverId AND directory=:directory AND sessionId=:sessionId ORDER BY messageId, position")
    abstract suspend fun parts(serverId: String, directory: String, sessionId: String): List<CachedOpenCodePart>

    @Transaction
    open suspend fun storeHistory(serverId: String, directory: String, sessionId: String, revision: Long, page: OpenCodeHistoryPage) {
        val messages = page.messages.mapIndexed { i, m -> CachedOpenCodeMessage(serverId, directory, sessionId, m.id, revision, m.role, i) }
        upsertMessages(messages)
        upsertParts(page.messages.flatMap { m -> m.parts.mapIndexed { i, p -> CachedOpenCodePart(serverId, directory, sessionId, m.id, p.id, p.type, p.text, i) } })
    }

    @Query("DELETE FROM opencode_messages WHERE serverId=:serverId AND directory=:directory AND sessionId=:sessionId")
    protected abstract suspend fun deleteMessages(serverId: String, directory: String, sessionId: String)

    @Query("DELETE FROM opencode_messages WHERE serverId=:serverId AND directory=:directory AND sessionId=:sessionId AND messageId=:messageId")
    abstract suspend fun deleteMessage(serverId: String, directory: String, sessionId: String, messageId: String)

    @Query("DELETE FROM opencode_session_cache WHERE serverId=:serverId AND directory=:directory AND sessionId=:sessionId")
    protected abstract suspend fun deleteSessionRow(serverId: String, directory: String, sessionId: String)

    @Transaction
    open suspend fun deleteSession(serverId: String, directory: String, sessionId: String) {
        deleteMessages(serverId, directory, sessionId)
        deletePendingPrompts(serverId, directory, sessionId)
        deleteInteractions(serverId, directory, sessionId)
        deleteSessionRow(serverId, directory, sessionId)
    }

    @Query("DELETE FROM opencode_pending_prompts WHERE serverId=:serverId AND directory=:directory AND sessionId=:sessionId")
    protected abstract suspend fun deletePendingPrompts(serverId: String, directory: String, sessionId: String)

    @Query("DELETE FROM opencode_interactions WHERE serverId=:serverId AND directory=:directory AND sessionId=:sessionId")
    protected abstract suspend fun deleteInteractions(serverId: String, directory: String, sessionId: String)

    @Query("SELECT * FROM opencode_session_cache WHERE serverId = :serverId AND directory = :directory AND profileRevision = :revision ORDER BY updatedAt DESC, sessionId ASC")
    abstract fun sessions(serverId: String, directory: String, revision: Long): Flow<List<CachedOpenCodeSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun upsert(rows: List<CachedOpenCodeSession>)

    /** Partial pages never prune records absent from the response. */
    @Transaction
    open suspend fun upsertPage(serverId: String, directory: String, revision: Long, rows: List<CachedOpenCodeSession>) {
        require(rows.all { it.serverId == serverId && it.directory == directory && it.profileRevision == revision })
        require(rows.map { it.sessionId }.distinct().size == rows.size)
        upsert(rows)
    }

    @Query("DELETE FROM opencode_session_cache WHERE serverId = :serverId")
    abstract suspend fun deleteServerCache(serverId: String)

    @Query("DELETE FROM opencode_session_cache WHERE serverId = :serverId AND profileRevision != :revision")
    abstract suspend fun removeOldRevisions(serverId: String, revision: Long)
}

@Database(entities = [CachedOpenCodeSession::class, CachedOpenCodeProject::class, CachedOpenCodeMessage::class, CachedOpenCodePart::class, OpenCodeCacheAccess::class, CachedOpenCodePendingPrompt::class, CachedOpenCodeInteraction::class], version = 3, exportSchema = true)
abstract class OpenCodeCacheDatabase : RoomDatabase() {
    abstract fun cacheDao(): OpenCodeCacheDao

    companion object {
        fun open(context: Context): OpenCodeCacheDatabase = Room.databaseBuilder(
            context.applicationContext,
            OpenCodeCacheDatabase::class.java,
            context.noBackupFilesDir.resolve("opencode-cache.db").absolutePath
        ).addMigrations(
            object : Migration(1, 2) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("CREATE TABLE IF NOT EXISTS `opencode_pending_prompts` (`serverId` TEXT NOT NULL, `directory` TEXT NOT NULL, `sessionId` TEXT NOT NULL, `clientMessageId` TEXT NOT NULL, `profileRevision` INTEGER NOT NULL, `content` TEXT NOT NULL, `state` TEXT NOT NULL, `uncertainty` TEXT, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`serverId`, `directory`, `sessionId`, `clientMessageId`))")
                }
            },
            object : Migration(2, 3) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("CREATE TABLE IF NOT EXISTS `opencode_interactions` (`serverId` TEXT NOT NULL, `directory` TEXT NOT NULL, `sessionId` TEXT NOT NULL, `requestId` TEXT NOT NULL, `profileRevision` INTEGER NOT NULL, `kind` TEXT NOT NULL, `title` TEXT NOT NULL, `details` TEXT NOT NULL, `allowsAlways` INTEGER NOT NULL, `state` TEXT NOT NULL, PRIMARY KEY(`serverId`, `directory`, `sessionId`, `requestId`))")
                }
            }
        ).build()
    }
}
