package pro.sihao.jarvis.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import android.content.Context
import pro.sihao.jarvis.data.database.dao.MessageDao
import pro.sihao.jarvis.data.database.dao.LLMProviderDao
import pro.sihao.jarvis.data.database.dao.ModelConfigDao
import pro.sihao.jarvis.data.database.entity.MessageEntity
import pro.sihao.jarvis.data.database.entity.LLMProviderEntity
import pro.sihao.jarvis.data.database.entity.ModelConfigEntity
import pro.sihao.jarvis.data.database.converters.DatabaseConverters
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        MessageEntity::class,
        LLMProviderEntity::class,
        ModelConfigEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(DatabaseConverters::class)
abstract class JarvisDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun llmProviderDao(): LLMProviderDao
    abstract fun modelConfigDao(): ModelConfigDao

    companion object {
        @Volatile
        private var INSTANCE: JarvisDatabase? = null

        // Migration from version 1 to 2 - Adding LLMProvider and ModelConfig tables
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create llm_providers table
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `llm_providers` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `displayName` TEXT NOT NULL,
                        `baseUrl` TEXT NOT NULL,
                        `authenticationType` TEXT NOT NULL,
                        `defaultModel` TEXT,
                        `isActive` INTEGER NOT NULL DEFAULT 1,
                        `supportsModelDiscovery` INTEGER NOT NULL DEFAULT 1,
                        `maxTokens` INTEGER,
                        `description` TEXT,
                        `createdTimestamp` INTEGER NOT NULL,
                        `updatedTimestamp` INTEGER NOT NULL,
                        UNIQUE(`name`)
                    )
                    """.trimIndent()
                )

                // Create indexes for llm_providers
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_llm_providers_name` ON `llm_providers` (`name`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_llm_providers_isActive` ON `llm_providers` (`isActive`)")

                // Create model_configs table
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `model_configs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `providerId` INTEGER NOT NULL,
                        `modelName` TEXT NOT NULL,
                        `displayName` TEXT NOT NULL,
                        `maxTokens` INTEGER,
                        `contextWindow` INTEGER,
                        `inputCostPer1K` REAL,
                        `outputCostPer1K` REAL,
                        `temperature` REAL NOT NULL DEFAULT 0.7,
                        `topP` REAL NOT NULL DEFAULT 1.0,
                        `isActive` INTEGER NOT NULL DEFAULT 1,
                        `isDefault` INTEGER NOT NULL DEFAULT 0,
                        `description` TEXT,
                        `capabilities` TEXT NOT NULL DEFAULT '[]',
                        `createdTimestamp` INTEGER NOT NULL,
                        `updatedTimestamp` INTEGER NOT NULL,
                        FOREIGN KEY(`providerId`) REFERENCES `llm_providers`(`id`) ON DELETE CASCADE,
                        UNIQUE(`providerId`, `modelName`)
                    )
                    """.trimIndent()
                )

                // Create indexes for model_configs
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_model_configs_providerId_modelName` ON `model_configs` (`providerId`, `modelName`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_model_configs_isActive` ON `model_configs` (`isActive`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_model_configs_isDefault` ON `model_configs` (`isDefault`)")
            }
        }

        // Migration from version 2 to 3 - Adding encryptedApiKey to providers
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add encryptedApiKey column to llm_providers table
                database.execSQL("ALTER TABLE `llm_providers` ADD COLUMN `encryptedApiKey` TEXT")
            }
        }

        fun getDatabase(context: Context): JarvisDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    JarvisDatabase::class.java,
                    "jarvis_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}