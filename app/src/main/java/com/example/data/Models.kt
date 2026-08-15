package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "editor_files")
data class EditorFile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val content: String,
    val language: String,
    val isCurrentlyOpen: Boolean = false,
    val lastModified: Long = System.currentTimeMillis()
)

@Entity(tableName = "db_connections")
data class DbConnection(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String, // e.g., "SQLite", "PostgreSQL", "MySQL", "MongoDB"
    val host: String,
    val databaseName: String,
    val username: String,
    val tables: String // Semi-colon separated list of tables
)

@Entity(tableName = "app_extensions")
data class AppExtension(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val author: String,
    val version: String,
    val isInstalled: Boolean = false,
    val iconName: String,
    val category: String = "Linter" // Linter, Theme, Syntax, Debugger, AI
)

@Entity(tableName = "debug_logs")
data class DebugLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val level: String, // "INFO", "DEBUG", "ERROR", "WARNING"
    val tag: String,
    val message: String,
    val fileName: String = ""
)

@Entity(tableName = "github_profiles")
data class GithubProfile(
    @PrimaryKey val id: Int = 1, // Only 1 profile
    val username: String,
    val token: String,
    val repoName: String,
    val branchName: String = "main",
    val isSynced: Boolean = false,
    val lastSyncTime: Long = 0L
)

/**
 * Git Commit Model for local Git integration and commit log history
 */
@Entity(tableName = "git_commits")
data class GitCommit(
    @PrimaryKey val hash: String, // e.g., "7f9a12c"
    val message: String,
    val author: String = "Developer <dev@local>",
    val branch: String = "main",
    val timestamp: Long = System.currentTimeMillis(),
    val filesChanged: Int = 1,
    val additions: Int = 12,
    val deletions: Int = 2
)

/**
 * SQLite Table Column Schema representation
 */
data class TableColumnSchema(
    val name: String,
    val type: String,
    val isPrimaryKey: Boolean = false,
    val isNullable: Boolean = true,
    val defaultValue: String? = null
)

/**
 * Debugger Breakpoint item
 */
data class BreakpointItem(
    val id: String,
    val fileName: String,
    val lineNumber: Int,
    val isEnabled: Boolean = true
)

/**
 * Debugger Call Stack frame
 */
data class StackFrame(
    val id: Int,
    val functionName: String,
    val fileName: String,
    val lineNumber: Int,
    val module: String = "app"
)

/**
 * Debugger Local Variable
 */
data class DebugVariable(
    val name: String,
    val type: String,
    val value: String
)

