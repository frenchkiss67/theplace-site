package com.theplace.receiptscanner.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [ReceiptEntity::class], version = 4, exportSchema = false)
internal abstract class ReceiptDatabase : RoomDatabase() {
    abstract fun receiptDao(): ReceiptDao

    companion object {
        @Volatile private var instance: ReceiptDatabase? = null

        /**
         * Migration v1→v2 : ajoute les colonnes optionnelles `category`
         * (nom de l'enum ReceiptCategory) et `totalCents` (montant en
         * centimes). Toutes deux nullables → pas de valeur par défaut
         * à backfill, les anciens tickets restent non classés.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE receipts ADD COLUMN category TEXT")
                db.execSQL("ALTER TABLE receipts ADD COLUMN totalCents INTEGER")
            }
        }

        /**
         * Migration v2→v3 : ajoute la colonne `extractedText` peuplée
         * par OCR. Nullable, pas de backfill — les anciens tickets ne
         * seront pas re-scannés automatiquement (option future).
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE receipts ADD COLUMN extractedText TEXT")
            }
        }

        /**
         * Migration v3→v4 : ajoute `purchasedAt` (epoch ms, date d'achat
         * choisie par l'utilisateur) et `warrantyMonths` (durée garantie
         * en mois). Tous deux nullables — pas de garantie suivie par défaut.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE receipts ADD COLUMN purchasedAt INTEGER")
                db.execSQL("ALTER TABLE receipts ADD COLUMN warrantyMonths INTEGER")
            }
        }

        fun get(context: Context): ReceiptDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ReceiptDatabase::class.java,
                    "receipts.db",
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { instance = it }
            }
    }
}
