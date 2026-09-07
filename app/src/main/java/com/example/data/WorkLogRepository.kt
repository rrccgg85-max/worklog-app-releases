package com.example.data

import kotlinx.coroutines.flow.Flow

class WorkLogRepository(private val database: WorkLogDatabase) {
    private val dao = database.workLogDao()
    private val scannedTextDao = database.scannedTextDao()

    val allLogs: Flow<List<WorkLog>> = dao.getAllWorkLogs()
    val allScannedTexts: Flow<List<ScannedText>> = scannedTextDao.getAllScannedTexts()

    suspend fun insertScannedText(scannedText: ScannedText) {
        scannedTextDao.insertScannedText(scannedText)
    }

    suspend fun deleteScannedText(scannedText: ScannedText) {
        scannedTextDao.deleteScannedText(scannedText)
    }

    suspend fun deleteScannedTextById(id: String) {
        scannedTextDao.deleteScannedTextById(id)
    }

    suspend fun deleteAllScannedTexts() {
        scannedTextDao.deleteAllScannedTexts()
    }

    fun getLogsByStatus(status: String): Flow<List<WorkLog>> = dao.getWorkLogsByStatus(status)

    fun getLogById(id: String): Flow<WorkLog?> = dao.getWorkLogById(id)
    suspend fun getLogByIdDirect(id: String): WorkLog? = dao.getWorkLogByIdDirect(id)
    suspend fun getLogByIdOrCaseNumberDirect(id: String): WorkLog? = dao.getWorkLogByIdOrCaseNumberDirect(id)

    fun getLogsBySyncStatus(syncStatus: String): Flow<List<WorkLog>> = dao.getWorkLogsBySyncStatus(syncStatus)

    suspend fun getPendingSyncWorkLogsList(): List<WorkLog> = dao.getPendingSyncWorkLogsList()

    suspend fun updateSyncStatus(id: String, syncStatus: String) {
        dao.updateSyncStatus(id, syncStatus)
    }

    fun getTodayTaskCount(startOfDay: Long): Flow<Int> = dao.getTodayTaskCount(startOfDay)

    suspend fun getTodayTaskCountDirect(startOfDay: Long): Int = dao.getTodayTaskCountDirect(startOfDay)

    suspend fun getTaskCountForDayRangeDirect(startOfDay: Long, endOfDay: Long): Int =
        dao.getTaskCountForDayRangeDirect(startOfDay, endOfDay)

    suspend fun insert(workLog: WorkLog) {
        dao.insertWorkLog(workLog)
    }

    suspend fun update(workLog: WorkLog) {
        dao.updateWorkLog(workLog)
    }

    suspend fun delete(workLog: WorkLog) {
        dao.deleteWorkLog(workLog)
    }

    suspend fun deleteById(id: String) {
        dao.deleteWorkLogById(id)
    }

    suspend fun pruneOldSyncedClosedLogs(daysToKeep: Int = 30): Int {
        val cutoff = System.currentTimeMillis() - (daysToKeep.toLong() * 24 * 60 * 60 * 1000)
        val deletedCount = dao.deleteOldSyncedClosedLogs(cutoff)
        database.vacuum()
        return deletedCount
    }

    suspend fun deleteAll() {
        dao.deleteAllWorkLogs()
    }

    suspend fun optimizeDatabase() {
        database.vacuum()
    }
}
