package com.example.repository

import com.example.data.*
import kotlinx.coroutines.flow.Flow

class CodeEditorRepository(private val dao: CodeEditorDao) {

    // Files
    val allFiles: Flow<List<EditorFile>> = dao.getAllFilesFlow()
    val openFiles: Flow<List<EditorFile>> = dao.getOpenFilesFlow()

    suspend fun getFileById(id: Int): EditorFile? = dao.getFileById(id)

    suspend fun insertFile(file: EditorFile): Long = dao.insertFile(file)

    suspend fun updateFile(file: EditorFile) = dao.updateFile(file)

    suspend fun deleteFile(file: EditorFile) = dao.deleteFile(file)

    // DB Connections
    val allConnections: Flow<List<DbConnection>> = dao.getAllConnectionsFlow()

    suspend fun insertConnection(connection: DbConnection): Long = dao.insertConnection(connection)

    suspend fun deleteConnection(connection: DbConnection) = dao.deleteConnection(connection)

    // Extensions
    val allExtensions: Flow<List<AppExtension>> = dao.getAllExtensionsFlow()

    suspend fun insertExtension(extension: AppExtension) = dao.insertExtension(extension)

    suspend fun updateExtension(extension: AppExtension) = dao.updateExtension(extension)

    // Debug Logs
    val allLogs: Flow<List<DebugLog>> = dao.getAllLogsFlow()

    suspend fun insertLog(log: DebugLog) = dao.insertLog(log)

    suspend fun clearAllLogs() = dao.clearAllLogs()

    // GitHub Profiles
    val githubProfile: Flow<GithubProfile?> = dao.getGithubProfileFlow()

    suspend fun insertGithubProfile(profile: GithubProfile) = dao.insertGithubProfile(profile)

    suspend fun deleteGithubProfile() = dao.deleteGithubProfile()

    // Git Commits
    val allCommits: Flow<List<GitCommit>> = dao.getAllCommitsFlow()

    suspend fun insertCommit(commit: GitCommit) = dao.insertCommit(commit)

    suspend fun clearAllCommits() = dao.clearAllCommits()
}

