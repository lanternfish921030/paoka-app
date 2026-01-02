package com.example.a0725.repository


import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import com.google.firebase.firestore.ListenerRegistration




/* ---------- 資料型別 ---------- */


data class UploadResult(
    val storagePath: String,
    val downloadUrl: String
)


data class AnalysisResult(
    val docId: String
)


/** 一筆角度資料（單一時間點 + 步態事件） */
data class AngleFrame(
    val frameIndex: Int?,
    val phase: String?,
    val imagePath: String?,
    val shoulderL: Int?,
    val shoulderR: Int?,
    val elbowL: Int?,
    val elbowR: Int?,
    val hipL: Int?,
    val hipR: Int?,
    val kneeL: Int?,
    val kneeR: Int?,
)


data class AnglesData(
    val stepMs: Long,
    val frames: List<AngleFrame>
)


enum class ProcessMode { OVERLAY, SKELETON_ONLY }


/** SkeletonScreen / GaitScreen 用的影片清單 */
data class UserVideoEntry(
    val id: String,
    val title: String,
    val originalDownloadUrl: String,
    val originalStoragePath: String,


    // 疊加影片 (新增 downloadUrl)
    val overlayStoragePath: String?,
    val overlayDownloadUrl: String?,


    // 純骨架影片 (新增 downloadUrl)
    val skeletonStoragePath: String?,
    val skeletonDownloadUrl: String?,


    val analysisDocId: String?
)


/* ---------- Repository ---------- */


class VideoRepository(
    private val storage: FirebaseStorage = Firebase.storage,
    private val legacyStorage: FirebaseStorage = Firebase.storage,
    private val firestore: FirebaseFirestore = Firebase.firestore
) {


    private fun currentUid(): String =
        FirebaseAuth.getInstance().currentUser?.uid
            ?: throw IllegalStateException("User not signed in")


    /** 上傳「原始影片」到 /videos/{uid}/original/{timestamp}.mp4 */
    suspend fun uploadOriginalVideo(
        context: Context,
        uri: Uri,
        pathPrefix: String = "videos"
    ): UploadResult = withContext(Dispatchers.IO) {
        val uid = currentUid()
        val fileName = "${System.currentTimeMillis()}.mp4"
        val storagePath = "$pathPrefix/$uid/original/$fileName"
        val ref = storage.reference.child(storagePath)


        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "openInputStream null" }
            ref.putStream(input).await()
        }
        val url = ref.downloadUrl.await().toString()
        UploadResult(storagePath, url)
    }


    /** Skeleton 用：上傳 Storage + 建 videos 文件 */
    suspend fun uploadOriginalAndRegister(
        context: Context,
        uri: Uri,
        title: String = "未命名影片",
        pathPrefix: String = "videos"
    ): String = withContext(Dispatchers.IO) {
        val uid = currentUid()
        val up = uploadOriginalVideo(context, uri, pathPrefix)


        val doc = firestore.collection("users").document(uid)
            .collection("videos").document()


        val payload = mapOf(
            "createdAt" to Timestamp.now(),
            "title" to title,
            "original" to mapOf(
                "storagePath" to up.storagePath,
                "downloadUrl" to up.downloadUrl
            )
        )
        doc.set(payload).await()


        enqueueProcessJob(
            uid = uid,
            videoId = doc.id,
            original = up,
            stepMs = 100L,
            pipeline = "skeleton"
        )


        return@withContext doc.id
    }


    /** Gait 用：上傳到 videos/gait/... */
    suspend fun uploadOriginalAndRegisterForGait(
        context: Context,
        uri: Uri,
        title: String = "跑步影片"
    ): String = withContext(Dispatchers.IO) {
        val uid = currentUid()
        val up = uploadOriginalVideo(context, uri, pathPrefix = "videos/gait")


        val doc = firestore.collection("users").document(uid)
            .collection("videos").document()


        val payload = mapOf(
            "createdAt" to Timestamp.now(),
            "title" to title,
            "original" to mapOf(
                "storagePath" to up.storagePath,
                "downloadUrl" to up.downloadUrl
            )
        )
        doc.set(payload).await()


        enqueueProcessJob(
            uid = uid,
            videoId = doc.id,
            original = up,
            stepMs = 100L,
            pipeline = "gait"
        )


        return@withContext doc.id
    }


    // ---------- 修改重點 1: observeUserVideos 讀取 downloadUrl ----------
    fun observeUserVideos(userId: String, onChange: (List<UserVideoEntry>) -> Unit): ListenerRegistration {
        return firestore.collection("users").document(userId).collection("videos")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    onChange(emptyList())
                    return@addSnapshotListener
                }


                val list = mutableListOf<UserVideoEntry>()
                value?.documents?.forEach { doc ->
                    try {
                        val original = doc.get("original") as? Map<*, *>
                        val downloadUrl = original?.get("downloadUrl") as? String ?: ""
                        val storagePath = original?.get("storagePath") as? String ?: ""
                        val title = doc.getString("title") ?: "未命名影片"


                        val overlay = doc.get("overlay") as? Map<*, *>
                        val skeleton = doc.get("skeleton") as? Map<*, *>


                        // ⭐️ 這裡要讀取 downloadUrl
                        val overlayPath = overlay?.get("storagePath") as? String
                        val overlayUrl = overlay?.get("downloadUrl") as? String


                        val skeletonPath = skeleton?.get("storagePath") as? String
                        val skeletonUrl = skeleton?.get("downloadUrl") as? String


                        val analysisDocId = doc.getString("analysisDocId")


                        list.add(
                            UserVideoEntry(
                                id = doc.id,
                                title = title,
                                originalDownloadUrl = downloadUrl,
                                originalStoragePath = storagePath,
                                overlayStoragePath = overlayPath,
                                overlayDownloadUrl = overlayUrl,     // 傳入
                                skeletonStoragePath = skeletonPath,
                                skeletonDownloadUrl = skeletonUrl,   // 傳入
                                analysisDocId = analysisDocId
                            )
                        )
                    } catch (e: Exception) {
                        Log.e("VideoRepo", "Error parsing doc ${doc.id}", e)
                    }
                }
                onChange(list)
            }
    }


    private fun isGaitVideoPath(path: String?): Boolean {
        return path?.startsWith("videos/gait/") == true
    }


    // ---------- 修改重點 2: listUserVideos 也要讀取 downloadUrl ----------
    suspend fun listUserVideos(): List<UserVideoEntry> = withContext(Dispatchers.IO) {
        val uid = currentUid()
        val snap = firestore.collection("users").document(uid)
            .collection("videos")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()


        snap.documents.mapNotNull { d ->
            val original = d.get("original") as? Map<*, *> ?: return@mapNotNull null
            val downloadUrl = original["downloadUrl"] as? String ?: return@mapNotNull null
            val storagePath = original["storagePath"] as? String ?: ""
            val title = d.getString("title") ?: "未命名影片"


            val overlay = d.get("overlay") as? Map<*, *>
            val skeleton = d.get("skeleton") as? Map<*, *>


            val overlayPath = overlay?.get("storagePath") as? String
            val overlayUrl = overlay?.get("downloadUrl") as? String // ⭐️ 新增


            val skeletonPath = skeleton?.get("storagePath") as? String
            val skeletonUrl = skeleton?.get("downloadUrl") as? String // ⭐️ 新增


            val analysisDocId = d.getString("analysisDocId") ?: d.getString("analysisId")


            UserVideoEntry(
                id = d.id,
                title = title,
                originalDownloadUrl = downloadUrl,
                originalStoragePath = storagePath,
                overlayStoragePath = overlayPath,
                overlayDownloadUrl = overlayUrl,     // 補上
                skeletonStoragePath = skeletonPath,
                skeletonDownloadUrl = skeletonUrl,   // 補上
                analysisDocId = analysisDocId
            )
        }
    }


    // ---------- 修改重點 3: listSkeletonVideos 也要補上 ----------
    suspend fun listSkeletonVideos(): List<UserVideoEntry> = withContext(Dispatchers.IO) {
        val uid = currentUid()
        val snap = firestore.collection("users").document(uid)
            .collection("videos")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()


        snap.documents.mapNotNull { d ->
            val original = d.get("original") as? Map<*, *> ?: return@mapNotNull null
            val downloadUrl = original["downloadUrl"] as? String ?: return@mapNotNull null
            val storagePath = original["storagePath"] as? String ?: ""


            if (isGaitVideoPath(storagePath)) return@mapNotNull null


            val title = d.getString("title") ?: "未命名影片"
            val overlay = d.get("overlay") as? Map<*, *>
            val skeleton = d.get("skeleton") as? Map<*, *>


            val overlayPath = overlay?.get("storagePath") as? String
            val overlayUrl = overlay?.get("downloadUrl") as? String // ⭐️


            val skeletonPath = skeleton?.get("storagePath") as? String
            val skeletonUrl = skeleton?.get("downloadUrl") as? String // ⭐️


            val analysisDocId = d.getString("analysisDocId") ?: d.getString("analysisId")


            UserVideoEntry(
                id = d.id,
                title = title,
                originalDownloadUrl = downloadUrl,
                originalStoragePath = storagePath,
                overlayStoragePath = overlayPath,
                overlayDownloadUrl = overlayUrl,     // 補上
                skeletonStoragePath = skeletonPath,
                skeletonDownloadUrl = skeletonUrl,   // 補上
                analysisDocId = analysisDocId
            )
        }
    }


    // ---------- 修改重點 4: listGaitVideos 也要補上 ----------
    suspend fun listGaitVideos(): List<UserVideoEntry> = withContext(Dispatchers.IO) {
        val uid = currentUid()
        val snap = firestore.collection("users").document(uid)
            .collection("videos")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()


        snap.documents.mapNotNull { d ->
            val original = d.get("original") as? Map<*, *> ?: return@mapNotNull null
            val downloadUrl = original["downloadUrl"] as? String ?: return@mapNotNull null
            val storagePath = original["storagePath"] as? String ?: ""


            if (!isGaitVideoPath(storagePath)) return@mapNotNull null


            val title = d.getString("title") ?: "跑步影片"
            val overlay = d.get("overlay") as? Map<*, *>
            val skeleton = d.get("skeleton") as? Map<*, *>


            val overlayPath = overlay?.get("storagePath") as? String
            val overlayUrl = overlay?.get("downloadUrl") as? String // ⭐️


            val skeletonPath = skeleton?.get("storagePath") as? String
            val skeletonUrl = skeleton?.get("downloadUrl") as? String // ⭐️


            val analysisDocId = d.getString("analysisDocId") ?: d.getString("analysisId")


            UserVideoEntry(
                id = d.id,
                title = title,
                originalDownloadUrl = downloadUrl,
                originalStoragePath = storagePath,
                overlayStoragePath = overlayPath,
                overlayDownloadUrl = overlayUrl,     // 補上
                skeletonStoragePath = skeletonPath,
                skeletonDownloadUrl = skeletonUrl,   // 補上
                analysisDocId = analysisDocId
            )
        }
    }


    suspend fun resolveDownloadUrl(storagePath: String): String =
        withContext(Dispatchers.IO) {
            storage.reference.child(storagePath).downloadUrl.await().toString()
        }


    suspend fun loadAngles(analysisDocId: String): AnglesData =
        withContext(Dispatchers.IO) {
            val doc = firestore.collection("video_analyses")
                .document(analysisDocId)
                .get()
                .await()


            if (!doc.exists()) {
                throw IllegalStateException("analysis doc not found")
            }


            val stepMs =
                (doc.getLong("stepMs") ?: doc.getLong("step_ms") ?: 100L).toLong()


            val list = (doc.get("angles") as? List<*>) ?: emptyList<Any>()


            val frames = list.mapNotNull { anyRow ->
                val row = anyRow as? Map<*, *> ?: return@mapNotNull null


                fun v(key: String): Int? =
                    (row[key] as? Number)?.toInt()


                val frameIndex = (row["frameIndex"] as? Number)?.toInt()
                val phase = row["phase"] as? String
                val rawImagePath = row["imagePath"] as? String


                val imageUrl: String? = if (!rawImagePath.isNullOrBlank()) {
                    try {
                        storage.reference
                            .child(rawImagePath)
                            .downloadUrl
                            .await()
                            .toString()
                    } catch (e: Exception) {
                        Log.e("VideoRepository", "resolve image url failed: $rawImagePath", e)
                        null
                    }
                } else {
                    null
                }


                AngleFrame(
                    frameIndex = frameIndex,
                    phase = phase,
                    imagePath = imageUrl,
                    shoulderL = v("L_SHOULDER"),
                    shoulderR = v("R_SHOULDER"),
                    elbowL = v("L_ELBOW"),
                    elbowR = v("R_ELBOW"),
                    hipL = v("L_HIP"),
                    hipR = v("R_HIP"),
                    kneeL = v("L_KNEE"),
                    kneeR = v("R_KNEE"),
                )
            }


            AnglesData(stepMs = stepMs, frames = frames)
        }


    suspend fun deleteVideos(videoIds: List<String>) = withContext(Dispatchers.IO) {
        if (videoIds.isEmpty()) return@withContext
        val uid = currentUid()
        for (id in videoIds) {
            deleteVideoCompletely(uid, id)
        }
    }


    suspend fun enqueueProcessJob(
        uid: String,
        videoId: String,
        original: UploadResult,
        stepMs: Long = 100L,
        pipeline: String = "skeleton"
    ): String = withContext(Dispatchers.IO) {
        val jobRef = firestore.collection("jobs").document()
        val payload = mapOf(
            "userId" to uid,
            "videoId" to videoId,
            "original" to mapOf(
                "storagePath" to original.storagePath,
                "downloadUrl" to original.downloadUrl
            ),
            "stepMs" to stepMs,
            "status" to "QUEUED",
            "pipeline" to pipeline,
            "createdAt" to Timestamp.now()
        )
        jobRef.set(payload).await()
        return@withContext jobRef.id
    }


    suspend fun deleteVideoCompletely(
        uid: String,
        videoId: String
    ) = withContext(Dispatchers.IO) {
        val videoRef = firestore.collection("users").document(uid)
            .collection("videos").document(videoId)
        val snap = videoRef.get().await()
        if (!snap.exists()) return@withContext


        val paths = mutableListOf<String>()


        val originalMap = snap.get("original") as? Map<*, *>
        val originalPath = originalMap?.get("storagePath") as? String ?: ""


        if (originalPath.isNotEmpty()) {
            paths.add(originalPath)
        }
        (snap.get("overlay") as? Map<*, *>)?.get("storagePath")?.let {
            if (it is String) paths.add(it)
        }
        (snap.get("skeleton") as? Map<*, *>)?.get("storagePath")?.let {
            if (it is String) paths.add(it)
        }


        for (p in paths) {
            var deleted = false
            try {
                storage.reference.child(p).delete().await()
                deleted = true
            } catch (e: Exception) {
                Log.e("VideoRepository", "main bucket 刪除失敗 path=$p", e)
            }


            if (!deleted) {
                try {
                    legacyStorage.reference.child(p).delete().await()
                } catch (e2: Exception) {
                    Log.e("VideoRepository", "legacy bucket 刪除失敗 path=$p", e2)
                }
            }


            if (isGaitVideoPath(originalPath)) {
                try {
                    val processedPrefix = "videos/gait/$uid/processed/$videoId"
                    val processedRef = storage.reference.child(processedPrefix)
                    val listResult = processedRef.listAll().await()
                    for (item in listResult.items) {
                        try {
                            item.delete().await()
                        } catch (e: Exception) { }
                    }
                } catch (e: Exception) { }
            }
        }


        val analysisId =
            snap.getString("analysisDocId") ?: snap.getString("analysisId")
        if (!analysisId.isNullOrEmpty()) {
            try {
                firestore.collection("video_analyses")
                    .document(analysisId)
                    .delete()
                    .await()
            } catch (e: Exception) { }
        }


        videoRef.delete().await()
    }


    // ---------- 修改重點 5: listenVideo 也要補上 ----------
    fun listenVideo(
        uid: String,
        videoId: String,
        onChange: (UserVideoEntry?) -> Unit
    ): ListenerRegistration {
        return firestore.collection("users").document(uid)
            .collection("videos").document(videoId)
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    onChange(null)
                    return@addSnapshotListener
                }
                if (snap == null || !snap.exists()) {
                    onChange(null)
                    return@addSnapshotListener
                }


                val original = snap.get("original") as? Map<*, *> ?: run {
                    onChange(null); return@addSnapshotListener
                }
                val downloadUrl = original["downloadUrl"] as? String ?: run {
                    onChange(null); return@addSnapshotListener
                }
                val storagePath = original["storagePath"] as? String ?: ""
                val title = snap.getString("title") ?: "未命名影片"


                val overlay = snap.get("overlay") as? Map<*, *>
                val skeleton = snap.get("skeleton") as? Map<*, *>


                val overlayPath = overlay?.get("storagePath") as? String
                val overlayUrl = overlay?.get("downloadUrl") as? String // ⭐️


                val skeletonPath = skeleton?.get("storagePath") as? String
                val skeletonUrl = skeleton?.get("downloadUrl") as? String // ⭐️


                val analysisDocId =
                    snap.getString("analysisDocId") ?: snap.getString("analysisId")


                onChange(
                    UserVideoEntry(
                        id = snap.id,
                        title = title,
                        originalDownloadUrl = downloadUrl,
                        originalStoragePath = storagePath,
                        overlayStoragePath = overlayPath,
                        overlayDownloadUrl = overlayUrl,     // 補上
                        skeletonStoragePath = skeletonPath,
                        skeletonDownloadUrl = skeletonUrl,   // 補上
                        analysisDocId = analysisDocId
                    )
                )
            }
    }
}



