package com.theplace.receiptscanner.data

import android.content.Context
import com.theplace.receiptscanner.util.PdfStorage
import kotlinx.coroutines.flow.Flow

class ReceiptRepository(
    private val dao: ReceiptDao,
    private val storage: PdfStorage,
) {
    fun observeAll(): Flow<List<Receipt>> = dao.observeAll()

    suspend fun findById(id: Long): Receipt? = dao.findById(id)

    suspend fun add(receipt: Receipt): Long = dao.insert(receipt)

    suspend fun rename(receipt: Receipt, newName: String) {
        dao.update(receipt.copy(name = newName))
    }

    suspend fun delete(receipt: Receipt) {
        storage.delete(receipt.fileName)
        dao.delete(receipt)
    }

    companion object {
        fun from(context: Context): ReceiptRepository {
            val db = ReceiptDatabase.get(context)
            return ReceiptRepository(db.receiptDao(), PdfStorage(context))
        }
    }
}
