package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        EditorFile::class,
        DbConnection::class,
        AppExtension::class,
        DebugLog::class,
        GithubProfile::class,
        GitCommit::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun codeEditorDao(): CodeEditorDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `git_commits` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `hash` TEXT NOT NULL,
                        `message` TEXT NOT NULL,
                        `author` TEXT NOT NULL,
                        `branch` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `filesChanged` INTEGER NOT NULL,
                        `additions` INTEGER NOT NULL,
                        `deletions` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "code_editor_database"
                )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .addCallback(AppDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database.codeEditorDao())
                }
            }
        }

        suspend fun populateDatabase(dao: CodeEditorDao) {
            // Populate sample files
            dao.insertFile(
                EditorFile(
                    name = "Main.kt",
                    content = """// تطبيق Kotlin بسيط
package com.example.editor

fun main() {
    val editorName = "محرر الأكواد VS Code"
    println("مرحباً بك في ${'$'}editorName المتقدم لنظام الأندرويد!")
    
    val featureList = listOf("التلوين التلقائي", "إدارة الملفات", "قواعد البيانات", "جيت هاب")
    for (feature in featureList) {
        println(" - ميزة مدعومة: ${'$'}feature")
    }
}""",
                    language = "kotlin",
                    isCurrentlyOpen = true
                )
            )

            dao.insertFile(
                EditorFile(
                    name = "index.js",
                    content = """// JavaScript code for local server
const express = require('express');
const app = express();
const PORT = process.env.PORT || 3000;

app.use(express.json());

// API Endpoints
app.get('/api/status', (req, res) => {
    res.json({
        app: "VS Code Mobile",
        version: "v1.0.0",
        status: "Running smoothly",
        uptime: process.uptime()
    });
});

app.listen(PORT, () => {
    console.log(`Server running on port ${'$'}{PORT}`);
});""",
                    language = "javascript",
                    isCurrentlyOpen = false
                )
            )

            dao.insertFile(
                EditorFile(
                    name = "styles.css",
                    content = """/* VS Code UI Custom Theme */
:root {
    --editor-bg: #1e1e1e;
    --editor-fg: #d4d4d4;
    --accent-color: #007acc;
    --sidebar-bg: #252526;
    --activity-bar-bg: #333333;
}

body {
    margin: 0;
    padding: 0;
    font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
    background-color: var(--editor-bg);
    color: var(--editor-fg);
}

.code-container {
    display: flex;
    height: 100vh;
    overflow: hidden;
}""",
                    language = "css",
                    isCurrentlyOpen = false
                )
            )

            dao.insertFile(
                EditorFile(
                    name = "query.sql",
                    content = """-- استعراض جداول قاعدة البيانات
SELECT id, username, email 
FROM users 
WHERE status = 'active' 
ORDER BY registered_at DESC;

-- دمج جداول الطلبات والمنتجات
SELECT o.order_id, u.username, o.total_amount, p.product_name
FROM orders o
JOIN users u ON o.user_id = u.id
JOIN order_items oi ON o.order_id = oi.order_id
JOIN products p ON oi.product_id = p.id
LIMIT 50;""",
                    language = "sql",
                    isCurrentlyOpen = false
                )
            )

            // Populate sample DB Connections
            dao.insertConnection(
                DbConnection(
                    name = "Local SQLite Prod",
                    type = "SQLite",
                    host = "local_device",
                    databaseName = "prod_users.db",
                    username = "admin",
                    tables = "users;products;orders;order_items;analytics_events"
                )
            )

            dao.insertConnection(
                DbConnection(
                    name = "PostgreSQL Dev",
                    type = "PostgreSQL",
                    host = "192.168.1.120:5432",
                    databaseName = "development_db",
                    username = "pg_developer",
                    tables = "accounts;profiles;projects;logs;tasks;messages"
                )
            )

            // Populate extensions
            val initialExtensions = listOf(
                AppExtension(
                    name = "Kotlin Auto-Complete & Syntax",
                    description = "الدعم الكامل للغة كوتلن مع الإكمال التلقائي والتلوين الذكي للأكواد.",
                    author = "JetBrains",
                    version = "1.9.22",
                    isInstalled = true,
                    iconName = "kotlin",
                    category = "Syntax"
                ),
                AppExtension(
                    name = "SQL Viewer & Explorer",
                    description = "تصفح الجداول، واستعراض قواعد البيانات، وتشغيل الاستعلامات SQL مباشرة.",
                    author = "Database Tools",
                    version = "2.4.1",
                    isInstalled = true,
                    iconName = "database",
                    category = "Database"
                ),
                AppExtension(
                    name = "Arabic Language Pack",
                    description = "تعريب كامل لواجهة محرر الأكواد VS Code مع دعم القوائم والاتجاه من اليمين لليسار.",
                    author = "Microsoft Community",
                    version = "1.86.0",
                    isInstalled = true,
                    iconName = "language",
                    category = "Language"
                ),
                AppExtension(
                    name = "GitHub Repos Connector",
                    description = "مزامنة مشاريعك تلقائياً وسحب الأكواد ورفع التعديلات (Commit/Push) لمستودعات جيت هاب.",
                    author = "GitHub",
                    version = "3.1.2",
                    isInstalled = false,
                    iconName = "git",
                    category = "VCS"
                ),
                AppExtension(
                    name = "Error Lens & Linter",
                    description = "إبراز الأخطاء والتنبيهات في الأسطر مباشرة أثناء الكتابة لتصحيح فوري للمشاكل البرمجية.",
                    author = "ErrorLabs",
                    version = "1.0.5",
                    isInstalled = false,
                    iconName = "bug",
                    category = "Debugger"
                ),
                AppExtension(
                    name = "Prettier Code Formatter",
                    description = "تنسيق تلقائي وجميل للأكواد (JS, TS, HTML, CSS, JSON) عند الحفظ لمطابقة معايير النظافة.",
                    author = "Esben Petersen",
                    version = "10.2.0",
                    isInstalled = false,
                    iconName = "style",
                    category = "Theme"
                ),
                AppExtension(
                    name = "Copilot AI Assistant",
                    description = "مساعد ذكاء اصطناعي يقوم بكتابة الأكواد بالنيابة عنك وإكمال السطور بناءً على الوصف.",
                    author = "GitHub AI",
                    version = "1.150.0",
                    isInstalled = false,
                    iconName = "ai",
                    category = "AI"
                )
            )

            for (ext in initialExtensions) {
                dao.insertExtension(ext)
            }

            // Populate sample debug logs
            dao.insertLog(
                DebugLog(
                    level = "INFO",
                    tag = "System",
                    message = "تم تشغيل بيئة تطوير محرر الأكواد بنجاح. تهيئة الملفات الحالية.",
                    fileName = "Main.kt"
                )
            )
            dao.insertLog(
                DebugLog(
                    level = "DEBUG",
                    tag = "Tokeniser",
                    message = "تم تحليل 12 كلمة مفتاحية و 4 نصوص في ملف Main.kt لتفعيل التلوين.",
                    fileName = "Main.kt"
                )
            )
            dao.insertLog(
                DebugLog(
                    level = "WARNING",
                    tag = "Linter",
                    message = "المتغير 'editorName' لم يتم تعديله، يفضل استخدامه كـ 'val'.",
                    fileName = "Main.kt"
                )
            )

            // Populate sample Git commits history
            dao.insertCommit(
                GitCommit(
                    hash = "a1b2c3d",
                    message = "Initial commit: Setup project structure and core editor files",
                    author = "Ayman <ayman@android.dev>",
                    branch = "main",
                    timestamp = System.currentTimeMillis() - 86400000L * 2,
                    filesChanged = 4,
                    additions = 150,
                    deletions = 0
                )
            )
            dao.insertCommit(
                GitCommit(
                    hash = "4e5f6a7",
                    message = "feat(syntax): Add dynamic regex-based syntax highlighter for Kotlin/JS",
                    author = "Ayman <ayman@android.dev>",
                    branch = "main",
                    timestamp = System.currentTimeMillis() - 86400000L,
                    filesChanged = 2,
                    additions = 68,
                    deletions = 12
                )
            )
            dao.insertCommit(
                GitCommit(
                    hash = "8b9c0d1",
                    message = "feat(workspace): Implement draggable sidebar & mini-map navigator",
                    author = "Ayman <ayman@android.dev>",
                    branch = "main",
                    timestamp = System.currentTimeMillis() - 3600000L * 3,
                    filesChanged = 3,
                    additions = 95,
                    deletions = 18
                )
            )
        }
    }
}
