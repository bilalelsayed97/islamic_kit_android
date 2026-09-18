package io.github.bilalelsayed97.islamickit.geocoding

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.Rule
import org.junit.runner.RunWith
import java.io.File

/** Asset → materialised copy → open, and the reuse rule. */
@RunWith(AndroidJUnit4::class)
class BundledCityDatabaseTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun materializesTheAssetOnceAndOpensIt() {
        val dir = temp.newFolder("db")
        val file = BundledCityDatabase.materialize(context, dir)

        assertEquals(BundledCityDatabase.FILE_NAME, file.name)
        assertEquals("islamic_kit_prayer_times_v2.db", file.name)
        assertEquals(dir, file.parentFile)
        assertEquals(37_093_376L, file.length())
        assertFalse(File(dir, "${BundledCityDatabase.FILE_NAME}.tmp").exists())

        val modified = file.lastModified()
        val again = BundledCityDatabase.materialize(context, dir)
        assertEquals(file, again)
        assertEquals("second call must not rewrite the file", modified, again.lastModified())

        BundledCityDatabase.openDatabase(file).use { db ->
            assertTrue(db.isReadOnly)
            assertEquals(2, db.version) // PRAGMA user_version
        }
    }

    @Test
    fun reusesAnExistingNonEmptyCopyWithoutReadingTheAsset() {
        val dir = temp.newFolder("existing")
        val existing = File(dir, BundledCityDatabase.FILE_NAME)
        existing.writeText("x")

        val file = BundledCityDatabase.materialize(context, dir)
        assertEquals(existing, file)
        assertEquals(1L, file.length())
    }

    @Test
    fun replacesAnEmptyCopy() {
        val dir = temp.newFolder("empty")
        File(dir, BundledCityDatabase.FILE_NAME).writeBytes(ByteArray(0))

        assertEquals(37_093_376L, BundledCityDatabase.materialize(context, dir).length())
    }

    @Test
    fun openHandsOutAGeocoderAndADirectoryOnOneConnection() {
        val dir = temp.newFolder("handle")
        BundledCityDatabase.open(context, dir).use { handle ->
            assertEquals(File(dir, BundledCityDatabase.FILE_NAME), handle.path)
            assertNotNull(handle.geocoder.resolve("Cairo", country = "EG"))
            assertEquals("Cairo", handle.directory.nearestCity(30.06263, 31.24967)?.nameEn)
        }
        BundledCityDatabase.openGeocoder(context, dir).use { geocoder ->
            assertEquals("EG", geocoder.resolve("Cairo", country = "Egypt")?.country)
        }
        BundledCityDatabase.openDirectory(context, dir).use { directory ->
            assertEquals(251, directory.countries().size)
        }
    }
}
