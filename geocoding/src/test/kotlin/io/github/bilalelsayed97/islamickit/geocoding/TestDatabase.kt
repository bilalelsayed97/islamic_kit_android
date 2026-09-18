package io.github.bilalelsayed97.islamickit.geocoding

import android.database.sqlite.SQLiteDatabase
import java.io.File

/**
 * Opens the committed asset in place (`src/main/assets/prayer_times.db`, the
 * module directory is the unit tests' working directory) so the query tests
 * skip the 37 MB materialisation that `BundledCityDatabaseTest` covers.
 */
internal object TestDatabase {
    val file: File = File("src/main/assets/prayer_times.db")

    fun open(): SQLiteDatabase {
        check(file.isFile) { "Database asset missing at ${file.absolutePath}" }
        return BundledCityDatabase.openDatabase(file)
    }
}
