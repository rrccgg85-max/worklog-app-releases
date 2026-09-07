package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkLogDao {
    @Query("SELECT * FROM work_logs ORDER BY timestamp DESC")
    fun getAllWorkLogs(): Flow<List<WorkLog>>

    @Query("SELECT * FROM work_logs WHERE status = :status ORDER BY timestamp DESC")
    fun getWorkLogsByStatus(status: String): Flow<List<WorkLog>>

    @Query("SELECT * FROM work_logs WHERE id = :id LIMIT 1")
    fun getWorkLogById(id: String): Flow<WorkLog?>

    @Query("SELECT * FROM work_logs WHERE id = :id LIMIT 1")
    suspend fun getWorkLogByIdDirect(id: String): WorkLog?

    @Query("SELECT * FROM work_logs WHERE id = :id OR caseNumber = :id LIMIT 1")
    suspend fun getWorkLogByIdOrCaseNumberDirect(id: String): WorkLog?

    @Query("SELECT * FROM work_logs WHERE syncStatus = :syncStatus ORDER BY timestamp ASC")
    fun getWorkLogsBySyncStatus(syncStatus: String): Flow<List<WorkLog>>

    @Query("SELECT * FROM work_logs WHERE syncStatus = 'PENDING' ORDER BY timestamp ASC")
    suspend fun getPendingSyncWorkLogsList(): List<WorkLog>

    @Query("UPDATE work_logs SET syncStatus = :syncStatus WHERE id = :id")
    suspend fun updateSyncStatus(id: String, syncStatus: String)

    @Query("SELECT COUNT(*) FROM work_logs WHERE timestamp >= :startOfDay")
    fun getTodayTaskCount(startOfDay: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM work_logs WHERE timestamp >= :startOfDay")
    suspend fun getTodayTaskCountDirect(startOfDay: Long): Int

    @Query("SELECT COUNT(*) FROM work_logs WHERE timestamp >= :startOfDay AND timestamp <= :endOfDay")
    suspend fun getTaskCountForDayRangeDirect(startOfDay: Long, endOfDay: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkLog(workLog: WorkLog)

    @Update
    suspend fun updateWorkLog(workLog: WorkLog)

    @Delete
    suspend fun deleteWorkLog(workLog: WorkLog)

    @Query("DELETE FROM work_logs WHERE id = :id")
    suspend fun deleteWorkLogById(id: String)

    @Query("DELETE FROM work_logs WHERE status = 'Closed' AND syncStatus = 'SYNCED' AND timestamp < :cutoffTimestamp")
    suspend fun deleteOldSyncedClosedLogs(cutoffTimestamp: Long): Int

    @Query("DELETE FROM work_logs")
    suspend fun deleteAllWorkLogs()
}
