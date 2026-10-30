package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.CodeLanguage
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class DocumentRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createDocument_validPayload_createsAndReturnsId() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = DocumentRepository(firestore)

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createDocument(
                title = "main_${UUID.randomUUID().toString().take(6)}.kt",
                content = "fun main() = println(\"Hi\")",
                language = CodeLanguage.KOTLIN
            )
        }
        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().isNotEmpty())
    }

    @Test
    fun observeDocuments_authenticatedOwner_emitsCreatedDocument() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = DocumentRepository(firestore)
        val docId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createDocument(
                title = "script_${UUID.randomUUID().toString().take(6)}.py",
                content = "print('ok')",
                language = CodeLanguage.PYTHON
            ).getOrThrow()
        }

        val emitted = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeDocuments(aliceUid).first { docs -> docs.any { it.id == docId } }
        }
        assertTrue(emitted.any { it.id == docId })
    }

    @Test
    fun updateDocument_authenticatedOwner_updatesContentAndPreservesMetadata() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = DocumentRepository(firestore)
        val docId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createDocument(
                title = "notes.txt",
                content = "initial",
                language = CodeLanguage.PLAINTEXT
            ).getOrThrow()
        }

        val created = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getDocumentById(aliceUid, docId).getOrThrow()
        }
        assertNotNull(created)

        val updateRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateDocument(
                created!!.copy(
                    title = "notes_updated.md",
                    content = "# Updated",
                    language = CodeLanguage.MARKDOWN.id,
                    isPinned = true
                )
            )
        }
        assertTrue(updateRes.isSuccess)

        val updated = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getDocumentById(aliceUid, docId).getOrThrow()
        }
        assertEquals("notes_updated.md", updated?.title)
        assertEquals("# Updated", updated?.content)
        assertEquals(true, updated?.isPinned)
    }

    @Test
    fun getDocumentById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = DocumentRepository(firestore)
        val docId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createDocument(
                title = "secret.kt",
                content = "// private",
                language = CodeLanguage.KOTLIN
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = DocumentRepository(firestore)
        val crossResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getDocumentById(aliceUid, docId)
        }
        assertTrue(crossResult.isFailure)
        val fsEx = crossResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    @Test
    fun observeDocuments_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repository = DocumentRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeDocuments(aliceUid).first()
            }
            fail("Expected observeDocuments to fail when signed out")
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException()
            assertNotNull("Expected nested FirebaseFirestoreException, got $e", fsEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice_codepad@test.com"
        const val BOB_EMAIL = "bob_codepad@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
