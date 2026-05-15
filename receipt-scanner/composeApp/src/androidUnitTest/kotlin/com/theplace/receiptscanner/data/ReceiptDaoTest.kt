package com.theplace.receiptscanner.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests Room en mémoire via Robolectric. Couvre insert / observeAll /
 * findById / update (category + totalCents) / delete.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ReceiptDaoTest {

    private lateinit var db: ReceiptDatabase
    private lateinit var dao: ReceiptDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ReceiptDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.receiptDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insert_then_observeAll_returns_inserted() = runTest {
        val id = dao.insert(entity(name = "Carrefour"))
        val list = dao.observeAll().first()
        assertEquals(1, list.size)
        assertEquals("Carrefour", list[0].name)
        assertEquals(id, list[0].id)
    }

    @Test
    fun findById_returns_inserted_or_null() = runTest {
        val id = dao.insert(entity())
        assertNotNull(dao.findById(id))
        assertNull(dao.findById(99999L))
    }

    @Test
    fun update_persists_category_and_totalCents() = runTest {
        val id = dao.insert(entity())
        val existing = dao.findById(id)!!
        dao.update(existing.copy(category = "Groceries", totalCents = 1230))

        val refetched = dao.findById(id)!!
        assertEquals("Groceries", refetched.category)
        assertEquals(1230L, refetched.totalCents)
    }

    @Test
    fun update_rename_changes_name_only() = runTest {
        val id = dao.insert(entity(name = "Old"))
        val existing = dao.findById(id)!!
        dao.update(existing.copy(name = "New"))

        val refetched = dao.findById(id)!!
        assertEquals("New", refetched.name)
        assertNull(refetched.category)
        assertNull(refetched.totalCents)
    }

    @Test
    fun delete_removes_entity() = runTest {
        val id = dao.insert(entity())
        dao.delete(dao.findById(id)!!)
        assertNull(dao.findById(id))
    }

    @Test
    fun observeAll_orders_by_createdAt_desc() = runTest {
        dao.insert(entity(name = "A", createdAt = 100))
        dao.insert(entity(name = "B", createdAt = 300))
        dao.insert(entity(name = "C", createdAt = 200))

        val names = dao.observeAll().first().map { it.name }
        assertEquals(listOf("B", "C", "A"), names)
    }

    private fun entity(
        name: String = "Ticket",
        createdAt: Long = 0L,
    ) = ReceiptEntity(
        name = name,
        fileName = "$name.pdf",
        pageCount = 1,
        sizeBytes = 1024,
        createdAt = createdAt,
    )
}
