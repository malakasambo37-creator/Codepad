package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.CodeDocument
import com.example.data.model.CodeLanguage
import com.example.data.model.toCodeDocumentOrNull
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class DocumentRepository(
    private val db: FirebaseFirestore
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth
        get() = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    /**
     * Real-time stream of a user's documents.
     * Aligns strictly with firestore.rules (`request.auth.uid == userId && existing().userId == request.auth.uid`)
     * by scoping to `/users/{userId}/documents` AND applying `.whereEqualTo("userId", userId)`.
     */
    fun observeDocuments(userId: String): Flow<List<CodeDocument>> {
        val path = "users/$userId/documents"
        return db.collection("users")
            .document(userId)
            .collection("documents")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toCodeDocumentOrNull() }
                    .sortedWith(
                        compareByDescending<CodeDocument> { it.isPinned }
                            .thenByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                    )
            }
            .catch { throwable ->
                if (throwable is Exception && throwable !is CancellationException) {
                    handleFirestoreError(throwable, OperationType.LIST, path)
                }
                throw throwable
            }
    }

    suspend fun getUserDocuments(userId: String = requireUserId()): Result<List<CodeDocument>> {
        val path = "users/$userId/documents"
        return try {
            val snapshot = db.collection("users")
                .document(userId)
                .collection("documents")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            val docs = snapshot.documents.mapNotNull { it.toCodeDocumentOrNull() }
            Result.success(docs)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun getDocumentById(ownerUserId: String, docId: String): Result<CodeDocument?> {
        val docRef = db.collection("users")
            .document(ownerUserId)
            .collection("documents")
            .document(docId)
        return try {
            val snapshot = docRef.get().await()
            Result.success(snapshot.toCodeDocumentOrNull())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun createDocument(
        title: String,
        content: String,
        language: CodeLanguage,
        isPinned: Boolean = false,
        isOpenTab: Boolean = true,
        lineEnding: String = "LF"
    ): Result<String> {
        val uid = requireUserId()
        val safeTitle = title.trim().ifEmpty { "untitled.${language.defaultExtension}" }.take(120)
        val safeContent = content.take(500_000)
        val safeLineEnding = if (lineEnding == "CRLF") "CRLF" else "LF"

        val collectionRef = db.collection("users").document(uid).collection("documents")
        val docRef = collectionRef.document()
        val payload = mapOf(
            "userId" to uid,
            "title" to safeTitle,
            "content" to safeContent,
            "language" to language.id,
            "isPinned" to isPinned,
            "isOpenTab" to isOpenTab,
            "cursorPosition" to 0L,
            "lineEnding" to safeLineEnding,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateDocument(doc: CodeDocument): Result<Unit> {
        val uid = requireUserId()
        require(doc.id.isNotBlank()) { "Document ID must not be blank for update" }

        val safeTitle = doc.title.trim().ifEmpty { "untitled.txt" }.take(120)
        val safeContent = doc.content.take(500_000)
        val safeLang = CodeLanguage.fromId(doc.language).id
        val safeCursor = doc.cursorPosition.coerceIn(0L, safeContent.length.toLong())
        val safeLineEnding = if (doc.lineEnding == "CRLF") "CRLF" else "LF"

        val docRef = db.collection("users")
            .document(uid)
            .collection("documents")
            .document(doc.id)

        val updates = mapOf(
            "title" to safeTitle,
            "content" to safeContent,
            "language" to safeLang,
            "isPinned" to doc.isPinned,
            "isOpenTab" to doc.isOpenTab,
            "cursorPosition" to safeCursor,
            "lineEnding" to safeLineEnding,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteDocument(docId: String): Result<Unit> {
        val uid = requireUserId()
        require(docId.isNotBlank()) { "Document ID must not be blank for delete" }
        val docRef = db.collection("users")
            .document(uid)
            .collection("documents")
            .document(docId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }
}
