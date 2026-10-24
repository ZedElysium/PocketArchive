package dev.elysium.pocketarchive.core.files

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import dev.elysium.pocketarchive.core.model.ArchiveItemId
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.io.FileAlreadyExistsException

class AppPrivateDocumentStore internal constructor(
    private val inputStreamOpener: DocumentInputStreamOpener,
    private val rootDirectory: File,
    private val ioDispatcher: CoroutineDispatcher,
    private val tokenGenerator: StagedDocumentTokenGenerator,
) : PrivateDocumentStore {
    constructor(context: Context) : this(
        inputStreamOpener = ContentResolverDocumentInputStreamOpener(
            context.applicationContext.contentResolver,
        ),
        rootDirectory = File(
            context.applicationContext.noBackupFilesDir,
            APP_PRIVATE_DOCUMENT_ROOT_DIRECTORY_NAME,
        ),
        ioDispatcher = Dispatchers.IO,
        tokenGenerator = StagedDocumentTokenGenerator {
            StagedDocumentToken(UUID.randomUUID().toString())
        },
    )

    override suspend fun stage(
        documentUri: String,
        archiveItemId: ArchiveItemId,
        metadata: DocumentMetadata,
    ): StagedDocument = withContext(ioDispatcher) {
        require(documentUri.isContentUri()) { "Document URI must use the content scheme" }
        rootDirectory.ensureDirectory()
        stagingDirectory.ensureDirectory()
        itemsDirectory.ensureDirectory()

        val token = tokenGenerator.generate()
        val destinationKey = archiveItemId.toPrivateDocumentKey()
        val stagingFile = stagedFile(token)
        if (stagingFile.exists()) {
            throw FileAlreadyExistsException(stagingFile)
        }

        try {
            val inputStream = inputStreamOpener.open(documentUri)
                ?: throw FileNotFoundException("Document provider returned no stream")
            val copiedBytes = inputStream.use { input ->
                FileOutputStream(stagingFile).use { output ->
                    val byteCount = input.copyToCancellable(output)
                    output.fd.sync()
                    byteCount
                }
            }
            StagedDocument(
                token = token,
                destinationKey = destinationKey,
                sizeBytes = copiedBytes,
            )
        } catch (failure: Throwable) {
            stagingFile.deleteFailure()?.let(failure::addSuppressed)
            throw failure
        }
    }

    override suspend fun commit(
        stagedDocument: StagedDocument,
    ): StoredDocument = withContext(ioDispatcher) {
        rootDirectory.ensureDirectory()
        stagingDirectory.ensureDirectory()
        itemsDirectory.ensureDirectory()

        val stagingFile = stagedFile(stagedDocument.token)
        val destinationFile = destinationFile(stagedDocument.destinationKey)
        if (!stagingFile.isFile) {
            throw FileNotFoundException("Staged document does not exist")
        }
        if (stagingFile.length() != stagedDocument.sizeBytes) {
            throw IOException("Staged document size changed before commit")
        }
        if (destinationFile.exists()) {
            throw FileAlreadyExistsException(destinationFile)
        }

        Files.move(
            stagingFile.toPath(),
            destinationFile.toPath(),
            StandardCopyOption.ATOMIC_MOVE,
        )
        StoredDocument(
            key = stagedDocument.destinationKey,
            sizeBytes = stagedDocument.sizeBytes,
        )
    }

    override suspend fun discard(stagedDocument: StagedDocument) {
        withContext(ioDispatcher) {
            val stagingFile = stagedFile(stagedDocument.token)
            stagingFile.deleteFailure()?.let { throw it }
        }
    }

    override suspend fun delete(storedDocument: StoredDocument) {
        withContext(ioDispatcher) {
            val storedFile = destinationFile(storedDocument.key)
            if (storedFile.exists() && !storedFile.isFile) {
                throw IOException("Stored document is not a regular file")
            }
            storedFile.deleteFailure()?.let { throw it }
        }
    }

    private val stagingDirectory: File
        get() = File(rootDirectory, STAGING_DIRECTORY_NAME)

    private val itemsDirectory: File
        get() = File(rootDirectory, ITEMS_DIRECTORY_NAME)

    private fun String.isContentUri(): Boolean =
        startsWith(CONTENT_URI_PREFIX, ignoreCase = true) &&
            length > CONTENT_URI_PREFIX.length

    private fun stagedFile(token: StagedDocumentToken): File {
        require(STAGED_TOKEN_PATTERN.matches(token.value)) {
            "Invalid staged document token"
        }
        return stagingDirectory.resolveOwnedChild(token.value)
    }

    private fun destinationFile(key: PrivateDocumentKey): File {
        require(PRIVATE_DOCUMENT_KEY_PATTERN.matches(key.value)) {
            "Invalid private document key"
        }
        return rootDirectory.resolveOwnedDescendant(key.value)
    }

    private fun ArchiveItemId.toPrivateDocumentKey(): PrivateDocumentKey {
        val digest = MessageDigest.getInstance(SHA_256)
            .digest(value.toByteArray(Charsets.UTF_8))
        val fileName = buildString(digest.size * 2) {
            digest.forEach { byte ->
                val unsignedByte = byte.toInt() and 0xff
                append(HEX_DIGITS[unsignedByte ushr 4])
                append(HEX_DIGITS[unsignedByte and 0x0f])
            }
        }
        return PrivateDocumentKey("$ITEMS_DIRECTORY_NAME/$fileName")
    }

    private suspend fun InputStream.copyToCancellable(
        output: FileOutputStream,
    ): Long {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var copiedBytes = 0L
        while (true) {
            currentCoroutineContext().ensureActive()
            val readCount = read(buffer)
            if (readCount < 0) break
            output.write(buffer, 0, readCount)
            copiedBytes = Math.addExact(copiedBytes, readCount.toLong())
        }
        return copiedBytes
    }

    private fun File.ensureDirectory() {
        if (isDirectory) return
        if (exists() || !mkdirs()) {
            throw IOException("Unable to create app-private document directory")
        }
    }

    private fun File.resolveOwnedChild(name: String): File {
        val parent = canonicalFile
        val child = File(parent, name).canonicalFile
        require(child.parentFile == parent) { "Path escapes its owned directory" }
        return child
    }

    private fun File.resolveOwnedDescendant(relativePath: String): File {
        val parent = canonicalFile
        val child = File(parent, relativePath).canonicalFile
        require(child.path.startsWith(parent.path + File.separator)) {
            "Path escapes its owned directory"
        }
        return child
    }

    private fun File.deleteFailure(): IOException? {
        if (!exists() || delete()) return null
        return IOException("Unable to delete staged document")
    }

    private companion object {
        const val SHA_256 = "SHA-256"
        const val HEX_DIGITS = "0123456789abcdef"
        const val CONTENT_URI_PREFIX = "content://"
        val STAGED_TOKEN_PATTERN = Regex("[A-Za-z0-9-]{1,128}")
        val PRIVATE_DOCUMENT_KEY_PATTERN = Regex("items/[0-9a-f]{64}")
    }
}

internal const val APP_PRIVATE_DOCUMENT_ROOT_DIRECTORY_NAME = "pocketarchive"
internal const val STAGING_DIRECTORY_NAME = ".staging"
internal const val ITEMS_DIRECTORY_NAME = "items"

internal fun interface DocumentInputStreamOpener {
    fun open(documentUri: String): InputStream?
}

internal fun interface StagedDocumentTokenGenerator {
    fun generate(): StagedDocumentToken
}

private class ContentResolverDocumentInputStreamOpener(
    private val contentResolver: ContentResolver,
) : DocumentInputStreamOpener {
    override fun open(documentUri: String): InputStream? =
        contentResolver.openInputStream(Uri.parse(documentUri))
}
