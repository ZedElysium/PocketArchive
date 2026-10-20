package dev.elysium.pocketarchive.core.files

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File

class TestDocumentProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        val sourceFile = File(requireNotNull(context).cacheDir, SOURCE_FILE_NAME)
        sourceFile.writeBytes(CONTENT)
        return ParcelFileDescriptor.open(
            sourceFile,
            ParcelFileDescriptor.MODE_READ_ONLY,
        )
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String = "application/pdf"

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    companion object {
        const val DOCUMENT_URI =
            "content://dev.elysium.pocketarchive.core.files.test.documents/document"
        val CONTENT = "content resolver document".toByteArray()
        private const val SOURCE_FILE_NAME = "content-resolver-source.pdf"
    }
}
