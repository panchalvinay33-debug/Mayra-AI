package ai.mayra.app.vision

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class MayraVisionEngineTest {
    private var clock = 1_000L

    @Test fun `on-device provider is preferred`() = runBlocking {
        val runtime = MayraVisionRuntime(
            listOf(provider("remote", true, 50), provider("local", false, 1)),
            now = { clock }
        )
        val result = runtime.analyze(request()) as VisionRuntimeResult.Completed
        assertEquals("local", result.analysis.evidence.single().providerId)
        assertFalse(result.analysis.usedRemoteProcessing)
    }

    @Test fun `sensitive request blocks remote provider`() = runBlocking {
        val runtime = MayraVisionRuntime(listOf(provider("remote", true)), now = { clock })
        val result = runtime.analyze(request(sensitivity = VisionSensitivity.SENSITIVE, mode = VisionProcessingMode.ALLOW_REMOTE))
        assertTrue(result is VisionRuntimeResult.Blocked)
    }

    @Test fun `duplicate request is suppressed`() = runBlocking {
        val runtime = MayraVisionRuntime(listOf(provider("local", false)), now = { clock })
        assertTrue(runtime.analyze(request(id = "one")) is VisionRuntimeResult.Completed)
        clock += 500
        val duplicate = runtime.analyze(request(id = "two")) as VisionRuntimeResult.DuplicateSuppressed
        assertEquals("one", duplicate.previousRequestId)
    }

    @Test fun `expired oversized and unsupported images are blocked`() = runBlocking {
        val runtime = MayraVisionRuntime(listOf(provider("local", false)), now = { 10_000L })
        assertTrue(runtime.analyze(request(createdAt = 1, expiresAt = 2)) is VisionRuntimeResult.Blocked)
        assertTrue(runtime.analyze(request(asset = asset(size = 30L * 1024 * 1024), createdAt = 9_000, expiresAt = 20_000)) is VisionRuntimeResult.Blocked)
        assertTrue(runtime.analyze(request(asset = asset(mime = "image/gif"), createdAt = 9_000, expiresAt = 20_000)) is VisionRuntimeResult.Blocked)
    }

    @Test fun `retryable provider failure falls back`() = runBlocking {
        val failing = object : MayraVisionProvider {
            override val descriptor = descriptor("first", false, 20)
            override suspend fun analyze(request: VisionRequest) = VisionProviderResult.Failure("busy", true)
        }
        val runtime = MayraVisionRuntime(listOf(failing, provider("second", false)), now = { clock })
        assertTrue(runtime.analyze(request()) is VisionRuntimeResult.Completed)
        assertEquals(1L, runtime.diagnostics().providerFailures["first"])
    }

    @Test fun `planner recognizes multimodal tasks and sensitivity`() {
        val planner = VisionIntentPlanner()
        val receipt = planner.plan("Is bill ka total kitna hai?", asset())
        val medicine = planner.plan("Ye medical prescription aur medicine kya hai?", asset(sensitive = true))
        val document = planner.plan("Document read aur summarize karo", asset())
        assertTrue(receipt.tasks.containsAll(setOf(VisionTask.READ_RECEIPT, VisionTask.ANSWER_QUESTION)))
        assertTrue(VisionTask.IDENTIFY_MEDICINE in medicine.tasks)
        assertEquals(VisionSensitivity.SENSITIVE, medicine.sensitivity)
        assertTrue(document.tasks.containsAll(setOf(VisionTask.EXTRACT_TEXT, VisionTask.SUMMARIZE_DOCUMENT)))
        assertNotNull(planner.plan("Photo me kya hai?", null).clarification)
    }

    @Test fun `memory is searchable merges duplicates and expires`() {
        val memory = MayraVisionMemory(VisionMemoryPolicy(personalRetentionMs = 100), now = { clock })
        val first = request(id = "first", allowMemory = true)
        assertNotNull(memory.remember(first, analysis(first, "Red medicine box", listOf(VisionLabel("medicine", .9)))))
        assertEquals(1, memory.search("red medicine").size)
        clock += 10
        val second = request(id = "second", allowMemory = true)
        memory.remember(second, analysis(second, "Updated medicine box"))
        assertEquals(1, memory.diagnostics().records)
        assertEquals(1L, memory.diagnostics().duplicateMerges)
        clock += 101
        assertTrue(memory.recent().isEmpty())
        assertEquals(1L, memory.diagnostics().expiredPruned)
    }

    @Test fun `highly sensitive memory is denied`() {
        val memory = MayraVisionMemory(now = { clock })
        val request = request(allowMemory = true, sensitivity = VisionSensitivity.HIGHLY_SENSITIVE)
        assertNull(memory.remember(request, analysis(request, "ID card")))
        assertEquals(1L, memory.diagnostics().deniedWrites)
    }

    @Test fun `response includes total text and warning`() {
        val request = request(tasks = setOf(VisionTask.READ_RECEIPT))
        val result = analysis(request, "Receipt detected").copy(
            textBlocks = listOf(VisionTextBlock("Shop receipt", .9)),
            receiptFields = listOf(ReceiptField("total", "₹450", .95)),
            warnings = listOf("Image blurry hai")
        )
        val response = VisionResponseComposer().compose(result)
        assertTrue(response.contains("₹450"))
        assertTrue(response.contains("Shop receipt"))
        assertTrue(response.contains("Dhyan rahe"))
    }

    @Test fun `coordinator returns response and snapshot`() = runBlocking {
        val coordinator = MayraVisionCoordinator(
            MayraVisionRuntime(listOf(provider("local", false)), now = { clock }),
            memory = MayraVisionMemory(now = { clock }),
            now = { clock }
        )
        val (result, response) = coordinator.handle("Photo me kya hai?", asset())
        assertTrue(result is VisionRuntimeResult.Completed)
        assertTrue(response.contains("Test image"))
        assertNotNull(coordinator.snapshot().lastAnalysis)
    }

    private fun request(
        id: String = "request",
        asset: VisionAsset = asset(),
        tasks: Set<VisionTask> = setOf(VisionTask.DESCRIBE),
        sensitivity: VisionSensitivity = VisionSensitivity.PERSONAL,
        mode: VisionProcessingMode = VisionProcessingMode.PREFER_ON_DEVICE,
        allowMemory: Boolean = false,
        createdAt: Long = clock,
        expiresAt: Long = clock + 10_000
    ) = VisionRequest(id = id, asset = asset, tasks = tasks, sensitivity = sensitivity, mode = mode,
        allowMemory = allowMemory, createdAt = createdAt, expiresAt = expiresAt)

    private fun asset(size: Long = 1_024, mime: String = "image/jpeg", sensitive: Boolean = false) = VisionAsset(
        id = "asset", uri = "content://test/image", mimeType = mime, sizeBytes = size,
        source = VisionAssetSource.GALLERY, fingerprint = "same", sensitive = sensitive
    )

    private fun provider(id: String, remote: Boolean, priority: Int = 0) = object : MayraVisionProvider {
        override val descriptor = descriptor(id, remote, priority)
        override suspend fun analyze(request: VisionRequest) = VisionProviderResult.Success(
            analysis(request, "Test image").copy(
                evidence = listOf(VisionEvidence(id, request.tasks.first(), .9)),
                usedRemoteProcessing = remote
            )
        )
    }

    private fun descriptor(id: String, remote: Boolean, priority: Int = 0) = VisionProviderDescriptor(
        id, id, if (remote) VisionProviderKind.REMOTE else VisionProviderKind.ON_DEVICE,
        VisionTask.entries.toSet(), remote, remote, 20L * 1024 * 1024, priority
    )

    private fun analysis(request: VisionRequest, summary: String, labels: List<VisionLabel> = emptyList()) = VisionAnalysis(
        request.id, request.asset.id, summary, labels = labels, processingMillis = 50,
        usedRemoteProcessing = false, confidence = .9
    )
}
