package com.example.a0725.repository

import com.example.a0725.model.ScheduleEvent   // ⬅️ 這行一定要有！
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth
import kotlinx.coroutines.tasks.await

object ScheduleRepo {
    private val db get() = Firebase.firestore
    private val auth get() = Firebase.auth

    private fun eventsCol(uid: String) =
        db.collection("users").document(uid).collection("events")

    fun listenEvents(
        onChange: (List<ScheduleEvent>) -> Unit,
        onError: (Throwable) -> Unit
    ): ListenerRegistration {
        val uid = auth.currentUser?.uid
            ?: return object : ListenerRegistration { override fun remove() {} } // 未登入時給個 no-op

        return eventsCol(uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) { onError(e); return@addSnapshotListener }
                val list: List<ScheduleEvent> = snapshot?.documents?.mapNotNull { d ->
                    d.toObject(ScheduleEvent::class.java)?.copy(id = d.id)
                }.orEmpty()
                onChange(list)
            }
    }

    suspend fun addEvent(e: ScheduleEvent): String {
        val uid = auth.currentUser?.uid ?: error("Not signed in")
        val ref = eventsCol(uid).document()
        val data = e.copy(id = ref.id)
        ref.set(data).await()
        return ref.id   // ✅ 回傳 Firestore 最終 id
    }


    suspend fun updateEvent(e: ScheduleEvent) {
        val uid = auth.currentUser?.uid ?: error("Not signed in")
        require(e.id.isNotBlank()) { "event.id is empty" }
        eventsCol(uid).document(e.id).set(e).await()
    }

    suspend fun deleteEvent(id: String) {
        val uid = auth.currentUser?.uid ?: error("Not signed in")
        eventsCol(uid).document(id).delete().await()
    }
    fun getEventsOnce(
        onSuccess: (List<ScheduleEvent>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val uid = Firebase.auth.currentUser?.uid
        if (uid == null) {
            onSuccess(emptyList())
            return
        }
        Firebase.firestore
            .collection("users").document(uid)
            .collection("events")
            .get()
            .addOnSuccessListener { snap ->
                val list = snap.documents.mapNotNull { doc ->
                    doc.toObject(ScheduleEvent::class.java)?.copy(id = doc.id)
                }
                onSuccess(list)
            }
            .addOnFailureListener { e -> onError(e) }
    }

}




