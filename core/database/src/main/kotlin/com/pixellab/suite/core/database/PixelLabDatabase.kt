package com.pixellab.suite.core.database

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "automation_routines")
data class AutomationRoutineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val triggerType: String,       // TIME, BATTERY_BELOW, BATTERY_ABOVE, CHARGING_STATE, BOOT, MANUAL, LIGHT_SENSOR
    val triggerParameter: String,  // e.g. "08:00", "20", "CHARGING", "50"
    val conditionLogic: String,    // "AND" or "OR"
    val conditionsJson: String,    // serialized list of ConditionSpec
    val actionType: String,        // NOTIFICATION, REMINDER, START_TIMER, LAUNCH_APP, OPEN_SETTINGS, SAVE_LOG
    val actionPayload: String,
    val isEnabled: Boolean = true,
    val lastExecutedAt: Long? = null,
    val lastResultStatus: String = "NEVER_RUN",
    val executionCount: Int = 0
)

@Entity(tableName = "automation_logs")
data class AutomationLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Long,
    val routineName: String,
    val timestamp: Long,
    val triggerSource: String,
    val outcome: String,
    val diagnosticMessage: String
)

@Entity(tableName = "sensor_sessions")
data class SensorSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sensorName: String,
    val sensorType: Int,
    val startedAt: Long,
    val endedAt: Long,
    val sampleCount: Int,
    val minMagnitude: Float,
    val maxMagnitude: Float,
    val avgMagnitude: Float,
    val exportedCsvPath: String? = null
)

@Entity(tableName = "battery_samples")
data class BatterySampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val levelPercent: Int,
    val isCharging: Boolean,
    val plugType: String,
    val temperatureCelsius: Float,
    val voltageMillivolts: Int,
    val currentMicroamps: Long?,
    val healthLabel: String
)

@Entity(tableName = "productivity_notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val category: String = "General",
    val isPinned: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "clipboard_snippets")
data class SnippetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val textContent: String,
    val tag: String = "Snippet",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "local_ai_models")
data class AiModelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val displayName: String,
    val uriString: String,
    val format: String,            // GGUF, TFLITE
    val quantization: String,      // Q4_K_M, Q8_0, INT8, FP16
    val fileSizeBytes: Long,
    val estimatedRamBytes: Long,
    val contextLength: Int,
    val compatibilityVerdict: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "activity_history")
data class ActivityLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val moduleId: String,
    val actionTitle: String,
    val detail: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface PixelLabDao {
    // Automation Routines
    @Query("SELECT * FROM automation_routines ORDER BY id DESC")
    fun observeRoutines(): Flow<List<AutomationRoutineEntity>>

    @Query("SELECT * FROM automation_routines WHERE isEnabled = 1")
    suspend fun getEnabledRoutines(): List<AutomationRoutineEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRoutine(routine: AutomationRoutineEntity): Long

    @Delete
    suspend fun deleteRoutine(routine: AutomationRoutineEntity)

    @Query("SELECT * FROM automation_logs ORDER BY timestamp DESC LIMIT 100")
    fun observeAutomationLogs(): Flow<List<AutomationLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAutomationLog(log: AutomationLogEntity)

    // Sensor Sessions
    @Query("SELECT * FROM sensor_sessions ORDER BY startedAt DESC LIMIT 50")
    fun observeSensorSessions(): Flow<List<SensorSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSensorSession(session: SensorSessionEntity)

    // Battery History
    @Query("SELECT * FROM battery_samples ORDER BY timestamp DESC LIMIT 120")
    fun observeBatterySamples(): Flow<List<BatterySampleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatterySample(sample: BatterySampleEntity)

    // Notes
    @Query("SELECT * FROM productivity_notes ORDER BY isPinned DESC, updatedAt DESC")
    fun observeNotes(): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNote(note: NoteEntity): Long

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    // Snippets
    @Query("SELECT * FROM clipboard_snippets ORDER BY createdAt DESC")
    fun observeSnippets(): Flow<List<SnippetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSnippet(snippet: SnippetEntity): Long

    @Delete
    suspend fun deleteSnippet(snippet: SnippetEntity)

    // Local AI Models
    @Query("SELECT * FROM local_ai_models ORDER BY addedAt DESC")
    fun observeAiModels(): Flow<List<AiModelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAiModel(model: AiModelEntity): Long

    @Delete
    suspend fun deleteAiModel(model: AiModelEntity)

    // Activity History
    @Query("SELECT * FROM activity_history ORDER BY timestamp DESC LIMIT 60")
    fun observeActivityLogs(): Flow<List<ActivityLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityLog(log: ActivityLogEntity)

    // Privacy Center Full Wipe Controls
    @Query("DELETE FROM automation_routines")
    suspend fun clearRoutines()

    @Query("DELETE FROM automation_logs")
    suspend fun clearAutomationLogs()

    @Query("DELETE FROM sensor_sessions")
    suspend fun clearSensorSessions()

    @Query("DELETE FROM battery_samples")
    suspend fun clearBatterySamples()

    @Query("DELETE FROM productivity_notes")
    suspend fun clearNotes()

    @Query("DELETE FROM clipboard_snippets")
    suspend fun clearSnippets()

    @Query("DELETE FROM local_ai_models")
    suspend fun clearAiModels()

    @Query("DELETE FROM activity_history")
    suspend fun clearActivityLogs()
}

@Database(
    entities = [
        AutomationRoutineEntity::class,
        AutomationLogEntity::class,
        SensorSessionEntity::class,
        BatterySampleEntity::class,
        NoteEntity::class,
        SnippetEntity::class,
        AiModelEntity::class,
        ActivityLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PixelLabDatabase : RoomDatabase() {
    abstract fun dao(): PixelLabDao

    companion object {
        @Volatile
        private var INSTANCE: PixelLabDatabase? = null

        fun getInstance(context: Context): PixelLabDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PixelLabDatabase::class.java,
                    "pixel_lab_offline.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
