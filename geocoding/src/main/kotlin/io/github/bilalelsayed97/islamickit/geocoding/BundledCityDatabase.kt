package io.github.bilalelsayed97.islamickit.geocoding

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.annotation.WorkerThread
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Access to the bundled city database asset.
 *
 * SQLite opens files by path, so the 37 MB asset is copied out of the APK once
 * ([materialize]) and opened read-only from there on every later launch. All
 * of this is blocking file I/O: call it from a background thread, open once,
 * and keep the result for the process lifetime.
 *
 * ```kotlin
 * val handle = BundledCityDatabase.open(context)          // off the main thread
 * val service = PrayerTimesService(geocoder = handle.geocoder, directory = handle.directory)
 * ```
 */
object BundledCityDatabase {
    /** File name of the database under the library's `assets/`. */
    const val ASSET_NAME: String = "prayer_times.db"

    /**
     * Marker bumped whenever the bundled database's contents change.
     *
     * It is part of the materialized file's name, so a shipped update lands
     * under a new path instead of silently reusing the previous release's copy.
     */
    const val VERSION: String = "2"

    /** Name of the materialized copy (carries [VERSION]). */
    const val FILE_NAME: String = "islamic_kit_prayer_times_v$VERSION.db"

    /** The default directory for the copy: excluded from Auto Backup, so the 37 MB never leave the device. */
    @JvmStatic
    fun defaultDirectory(context: Context): File = File(context.noBackupFilesDir, "islamic_kit")

    /**
     * Copies the asset to `<directory>/[FILE_NAME]` and returns that file,
     * reusing an existing non-empty copy without reading the asset again.
     *
     * The copy is streamed to a `.tmp` sibling and renamed into place, so a
     * crash mid-copy never leaves a truncated file that would pass the reuse
     * check on the next launch.
     *
     * @throws IOException when the asset cannot be read or the copy cannot be written.
     */
    @WorkerThread
    @JvmStatic
    @JvmOverloads
    @Throws(IOException::class)
    fun materialize(context: Context, directory: File = defaultDirectory(context)): File {
        val file = File(directory, FILE_NAME)
        if (file.exists() && file.length() > 0) return file

        if (!directory.isDirectory && !directory.mkdirs() && !directory.isDirectory) {
            throw IOException("Cannot create $directory")
        }
        val tmp = File(directory, "$FILE_NAME.tmp")
        context.assets.open(ASSET_NAME).use { input ->
            FileOutputStream(tmp).use { output ->
                input.copyTo(output)
                output.fd.sync()
            }
        }
        if (!tmp.renameTo(file)) {
            tmp.delete()
            throw IOException("Cannot move $tmp to $file")
        }
        return file
    }

    /**
     * Opens the database at [path] read-only.
     *
     * `NO_LOCALIZED_COLLATORS` keeps Android from registering its ICU
     * collations, so `COLLATE NOCASE` orders exactly as it does for the Dart
     * package (and the conformance fixtures).
     */
    @JvmStatic
    fun openDatabase(path: File): SQLiteDatabase = SQLiteDatabase.openDatabase(
        path.path,
        null,
        SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS,
    )

    /** [materialize] + [openDatabase] wrapped in a [SqliteCityGeocoder]. */
    @WorkerThread
    @JvmStatic
    @JvmOverloads
    @Throws(IOException::class)
    fun openGeocoder(context: Context, directory: File = defaultDirectory(context)): SqliteCityGeocoder =
        SqliteCityGeocoder(openDatabase(materialize(context, directory)))

    /** [materialize] + [openDatabase] wrapped in a [SqliteCityDirectory]. */
    @WorkerThread
    @JvmStatic
    @JvmOverloads
    @Throws(IOException::class)
    fun openDirectory(context: Context, directory: File = defaultDirectory(context)): SqliteCityDirectory =
        SqliteCityDirectory(openDatabase(materialize(context, directory)))

    /**
     * [materialize] + [openDatabase], with a geocoder and a directory sharing
     * the single read-only connection. Close the [Handle] to close both.
     */
    @WorkerThread
    @JvmStatic
    @JvmOverloads
    @Throws(IOException::class)
    fun open(context: Context, directory: File = defaultDirectory(context)): Handle {
        val path = materialize(context, directory)
        val database = openDatabase(path)
        return Handle(path, database, SqliteCityGeocoder(database), SqliteCityDirectory(database))
    }

    /** An open bundled database with its two query facades. */
    class Handle(
        /** The materialized database file. */
        val path: File,
        /** The shared read-only connection. */
        val database: SQLiteDatabase,
        val geocoder: SqliteCityGeocoder,
        val directory: SqliteCityDirectory,
    ) : Closeable {
        /** Closes the shared connection (and with it [geocoder] and [directory]). */
        override fun close() = database.close()
    }
}
