package com.theplace.receiptscanner.data

import android.content.Context
import com.theplace.receiptscanner.platform.PdfStorage
import com.theplace.receiptscanner.platform.PlatformScanResult
import com.theplace.receiptscanner.util.defaultReceiptName
import com.theplace.receiptscanner.util.nowMs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class AndroidReceiptRepository(
    private val dao: ReceiptDao,
    private val storage: PdfStorage,
) : ReceiptRepository {

    override fun observeAll(): Flow<List<Receipt>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun findById(id: Long): Receipt? = dao.findById(id)?.toDomain()

    override suspend fun addFromScan(scan: PlatformScanResult): Receipt {
        val fileName = storage.newFileName()
        val sizeBytes = storage.importFrom(scan.uri, fileName)
        val now = nowMs()
        val receipt = Receipt(
            name = defaultReceiptName(now),
            fileName = fileName,
            pageCount = scan.pageCount,
            sizeBytes = sizeBytes,
            createdAt = now,
        )
        val id = dao.insert(receipt.toEntity())
        return receipt.copy(id = id)
    }

    override suspend fun rename(receipt: Receipt, newName: String) {
        dao.update(receipt.copy(name = newName).toEntity())
    }

    override suspend fun delete(receipt: Receipt) {
        storage.delete(receipt.fileName)
        dao.delete(receipt.toEntity())
    }

    companion object {
        fun from(context: Context): AndroidReceiptRepository {
            val db = ReceiptDatabase.get(context)
            return AndroidReceiptRepository(db.receiptDao(), PdfStorage(context))
        }
    }
}
