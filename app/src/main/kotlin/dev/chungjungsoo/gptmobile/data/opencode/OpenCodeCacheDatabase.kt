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

@Dao
abstract class OpenCodeCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun setAccess(access: OpenCodeCacheAccess)

    @Query("SELECT * FROM opencode_access WHERE serverId=:serverId")
    abstract suspend fun access(serverId: String): OpenCodeCacheAccess?

    @Query("SELECT serverId FROM opencode_session_cache UNION SELECT serverId FROM opencode_projects UNION SELECT serverId FROM opencode_messages UNION SELECT serverId FROM opencode_access")
    abstract suspend fun serverIds(): List<String>

    @Query("DELETE FROM opencode_projects WHERE serverId=:serverId AND (:revision IS NULL OR profileRevision != :revision)")
    protected abstract suspend fun purgeProjects(serverId: String, revision: Long?)

    @Query("DELETE FROM opencode_messages WHERE serverId=:serverId AND (:revision IS NULL OR profileRevision != :revision)")
    protected abstract suspend fun purgeMessages(serverId: String, revision: Long?)

    @Query("DELETE FROM opencode_session_cache WHERE serverId=:serverId AND (:revision IS NULL OR profileRevision != :revision)")
    protected abstract suspend fun purgeSessions(serverId: String, revision: Long?)

    @Query("DELETE FROM opencode_access WHERE serverId=:serverId AND (:revision IS NULL OR revision != :revision)")
    protected abstract suspend fun purgeAccess(serverId: String, revision: Long?)

    @Transaction
    open suspend fun purgeObsolete(serverId: String, revision: Long?) {
        purgeProjects(serverId, revision)
        purgeMessages(serverId, revision)
        purgeSessions(serverId, revision)
        purgeAccess(serverId, revision)
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
        deleteSessionRow(serverId, directory, sessionId)
    }

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

@Database(entities = [CachedOpenCodeSession::class, CachedOpenCodeProject::class, CachedOpenCodeMessage::class, CachedOpenCodePart::class, OpenCodeCacheAccess::class], version = 1, exportSchema = true)
abstract class OpenCodeCacheDatabase : RoomDatabase() {
    abstract fun cacheDao(): OpenCodeCacheDao

    companion object {
        fun open(context: Context): OpenCodeCacheDatabase = Room.databaseBuilder(
            context.applicationContext,
            OpenCodeCacheDatabase::class.java,
            context.noBackupFilesDir.resolve("opencode-cache.db").absolutePath
        ).build()
    }
}
