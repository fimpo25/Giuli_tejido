package com.giuli.tejido.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Project::class, TimeSession::class, Counter::class, Note::class, CostItem::class,
        InventoryItem::class, PriceHistory::class, CostChange::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): TejidoDao

    companion object {
        /** v1 → v2: color de proyecto, inventario, histórico de precios y de costos. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `projects` ADD COLUMN `colorIndex` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `cost_items` ADD COLUMN `inventoryItemId` INTEGER")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_cost_items_inventoryItemId` ON `cost_items` (`inventoryItemId`)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `inventory_items` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, `unit` TEXT NOT NULL, `currentPrice` REAL NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `price_history` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`itemId` INTEGER NOT NULL, `price` REAL NOT NULL, `changedAt` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_price_history_itemId` ON `price_history` (`itemId`)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `cost_changes` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`projectId` INTEGER NOT NULL, `costItemId` INTEGER NOT NULL, `article` TEXT NOT NULL, " +
                        "`oldUnitPrice` REAL NOT NULL, `newUnitPrice` REAL NOT NULL, `quantity` REAL NOT NULL, " +
                        "`totalBefore` REAL NOT NULL, `totalAfter` REAL NOT NULL, `changedAt` INTEGER NOT NULL, " +
                        "`source` TEXT NOT NULL)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_cost_changes_projectId` ON `cost_changes` (`projectId`)",
                )
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "giuli_tejido.db",
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
    }
}
