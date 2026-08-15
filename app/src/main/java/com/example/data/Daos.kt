package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CodeEditorDao {
    // Files
    @Query("SELECT * FROM editor_files ORDER BY name ASC")
    fun getAllFilesFlow(): Flow<List<EditorFile>>

    @Query("SELECT * FROM editor_files WHERE isCurrentlyOpen = 1")
    fun getOpenFilesFlow(): Flow<List<EditorFile>>

    @Query("SELECT * FROM editor_files WHERE id = :id")
    suspend fun getFileById(id: Int): EditorFile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: EditorFile): Long

    @Update
    suspend fun updateFile(file: EditorFile)

    @Delete
    suspend fun deleteFile(file: EditorFile)

    // DB Connections
    @Query("SELECT * FROM db_connections ORDER BY name ASC")
    fun getAllConnectionsFlow(): Flow<List<DbConnection>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConnection(connection: DbConnection): Long

    @Delete
    suspend fun deleteConnection(connection: DbConnection)

    // Extensions
    @Query("SELECT * FROM app_extensions ORDER BY name ASC")
    fun getAllExtensionsFlow(): Flow<List<AppExtension>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExtension(extension: AppExtension)

    @Update
    suspend fun updateExtension(extension: AppExtension)

    // Debug Logs
    @Query("SELECT * FROM debug_logs ORDER BY timestamp DESC LIMIT 200")
    fun getAllLogsFlow(): Flow<List<DebugLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: DebugLog)

    @Query("DELETE FROM debug_logs")
    suspend fun clearAllLogs()

    // GitHub Profiles
    @Query("SELECT * FROM github_profiles WHERE id = 1")
    fun getGithubProfileFlow(): Flow<GithubProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGithubProfile(profile: GithubProfile)

    @Query("DELETE FROM github_profiles")
    suspend fun deleteGithubProfile()

    // Git Commits (Local VCS)
    @Query("SELECT * FROM git_commits ORDER BY timestamp DESC")
    fun getAllCommitsFlow(): Flow<List<GitCommit>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommit(commit: GitCommit)

    @Query("DELETE FROM git_commits")
    suspend fun clearAllCommits()
}

