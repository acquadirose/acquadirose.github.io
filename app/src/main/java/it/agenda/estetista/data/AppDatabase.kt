package it.agenda.estetista.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Operator::class, Service::class, Appointment::class, Client::class, PostIt::class, Shift::class, Absence::class],
    version = 5,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AgendaDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        /** v1 → v2: anagrafica clienti (popolata dagli appuntamenti esistenti) e post-it. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `clients` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, `phone` TEXT NOT NULL, `email` TEXT NOT NULL, " +
                        "`birthday` TEXT NOT NULL, `notes` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `postit` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`date` INTEGER, `text` TEXT NOT NULL, `color` INTEGER NOT NULL, " +
                        "`done` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)"
                )
                db.execSQL("ALTER TABLE `appointments` ADD COLUMN `clientId` INTEGER")
                val now = System.currentTimeMillis()
                db.execSQL(
                    "INSERT INTO clients (name, phone, email, birthday, notes, createdAt) " +
                        "SELECT TRIM(clientName), MAX(phone), '', '', '', $now FROM appointments " +
                        "WHERE TRIM(clientName) != '' GROUP BY TRIM(clientName) COLLATE NOCASE"
                )
                db.execSQL(
                    "UPDATE appointments SET clientId = (SELECT c.id FROM clients c " +
                        "WHERE c.name = TRIM(appointments.clientName) COLLATE NOCASE)"
                )
            }
        }

        /** v2 → v3: anno dell'ultimo auguri di compleanno. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `clients` ADD COLUMN `birthdayMsgYear` INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** v3 → v4: turni settimanali e assenze. */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `shifts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`operatorId` INTEGER NOT NULL, `dayOfWeek` INTEGER NOT NULL, " +
                        "`startMin` INTEGER NOT NULL, `endMin` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `absences` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`operatorId` INTEGER NOT NULL, `fromDate` INTEGER NOT NULL, `toDate` INTEGER NOT NULL, " +
                        "`startMin` INTEGER NOT NULL, `endMin` INTEGER NOT NULL, `reason` TEXT NOT NULL)"
                )
            }
        }

        /** v4 → v5: anno di nascita e comune in anagrafica. */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `clients` ADD COLUMN `birthYear` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `clients` ADD COLUMN `city` TEXT NOT NULL DEFAULT ''")
            }
        }

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "agenda.db",
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5).build().also { instance = it }
            }
    }
}
