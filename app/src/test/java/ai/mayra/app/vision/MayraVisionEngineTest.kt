package ai.mayra.app.vision

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MayraVisionEngineTest {
    @Test
    fun `on-device provider is preferred`() = runBlocking {
        val local = fakeProvider("local", VisionProviderKind.ON_DEVICE, remote = false, priority = 1)
        val remote = fakeProvider("remote", VisionProviderKind.REMOTE, remote = true, priority = 50)
        val runtime = MayraVisionRuntime(listOf(remote, local))

        val result = runtime.analyze(request())

        assertTrue(result is VisionRuntimeResult.Completed)
        val analysis = (result as VisionRuntimeResult.Completed).analysis
        assertTrue(analysis.evidence.any { it.providerId == "local" })
        assertFalse(analysis.usedRemoteProcessing)
    }

    @Test
    fun `sensitive request blocks remote provider by default`() = runBlocking {
        val remote = fakeProvider("remote", VisionProviderKind.REMOTE, remote = true)
        val runtime = MayraVisionRuntime(listOf(remote))

        val result = runtime.analyze(request(sensitivity = VisionSensitivity.SENSITIVE, mode = VisionProcessingMode.ALLOW_REMOTE))

        assertTrue(result is VisionRuntimeResult.Blocked)
    }

    @Test
    fun `duplicate request is suppressed inside window`() = runBlocking {
        var clock = 1_000L
        val runtime = MayraVisionRuntime(listOf(fakeProvider("local", VisionProviderKind.ON_DEVICE, false)), now = { clock })
        val request = request(id = "one")

        assertTrue(runtime.analyze(request) is VisionRuntimeResult.Completed)
        clock += 500
        val duplicate = runtime.analyze(request.copy(id = "two", createdAt = clock, expiresAt = clock + 10_000))

        assertTrue(duplicate is VisionRuntimeResult.DuplicateSuppressed)
        assertEquals("one", (duplicate as VisionRuntimeResult.DuplicateSuppressed).previousRequestId)
    }

    @Test
    fun `expired and oversized requests are blocked`() = runBlocking {
        val runtime = MayraVisionRuntime(listOf(fakeProvider("local", VisionProviderKind.ON_DEVICE, false)), now = { 10_000L })
        val expired = request(createdAt = 1L, expiresAt = 2L)
        val huge = request(asset = asset(sizeBytes = 30L * 1024 * 1024), createdAt = 9_000L, expiresAt = 20_000L)

        assertTrue(runtime.analyze(expired) is VisionRuntimeResult.Blocked)
        assertTrue(runtime.analyze(huge) is VisionRuntimeResult.Blocked)
    }

    @Test
    fun `retryable failure falls back to next provider`() = runBlocking {
        val failing = object : MayraVisionProvider {
            override val descriptor = descriptor("first", VisionProviderKind.ON_DEVICE, false, 20)
            override suspend fun analyze(request: VisionRequest) = VisionProviderResult.Failure("model busy", retryable = true)
        }
        val success = fakeProvider("second", VisionProviderKind.ON_DEVICE, false, priority = 1)
        val runtime = MayraVisionRuntime(listOf(failing, success))

        val result = runtime.analyze(request())

        assertTrue(result is VisionRuntimeResult.Completed)
        assertEquals(1L, runtime.diagnostics().providerFailures["first"])
    }

    @Test
    fun `planner detects receipt document medicine and question`() {
        val planner = VisionIntentPlanner()
        val receipt = planner.plan("Is bill ka total kitna hai?", asset())
        val medicine = planner.plan("Ye medicine ka naam kya hai?", asset(sensitive = true))
        val document = planner.plan("Is document ko read aur summarize karo", asset())

        assertTrue(VisionTask.READ_RECEIPT in receipt.tasks)
        assertTrue(VisionTask.ANSWER_QUESTION in receipt.tasks)
        assertTrue(VisionTask.IDENTIFY_MEDICINE in medicine.tasks)
        assertEquals(VisionSensitivity.SENSITIVE, medicine.sensitivity)
        assertTrue(VisionTask.EXTRACT_TEXT in document.tasks)
        assertTrue(VisionTask.SUMMARIZE_DOCUMENT in document.tasks)
    }

    @Test
    fun `planner requests image when asset is absent`() {
        val plan = VisionIntentPlanner().plan("Photo me kya hai?", null)

        assertNotNull(plan.clarification)
        assertFalse(plan.allowMemory)
    }

    @Test
    fun `memory stores searchable non-sensitive metadata`() {
        var clock = 1_000L
        val memory = MayraVisionMemory(now = { clock })
        val request = request(allowMemory = true, createdAt = clock, expiresAt = clock + 10_000)
        val analysis = analysis(request, "A red medicine box", labels = listOf(VisionLabel("medicine", 0.9)))

        val record = memory.remember(request, analysis)
        val hits = memory.search("red medicine")

        assertNotNull(record)
        assertEquals(1, hits.size)
        assertEquals(record?.id, hits.first().record.id)
    }

    @Test
    fun `highly sensitive memory is denied by default`() {
        val memory = MayraVisionMemory()
        val request = request(allowMemory = true, sensitivity = VisionSensitivity.HIGHLY_SENSITIVE)

        assertNull(memory.remember(request, analysis(request, "ID card")))
        assertEquals(1L, memory.diagnostics().deniedWrites)
    }

    @Test
    fun `memory duplicate replaces older record`() {
        var clock = 1_000L
        val memory = MayraVisionMemory(now = { clock })
        val first = request(allowMemory = true, createdAt = clock, expiresAt = clock + 10_000)
        memory.remember(first, analysis(first, "First description"))
        clock += 100
        val second = first.copy(id = "second", createdAt = clock, expiresAt = clock + 10_000)
        memory.remember(second, analysis(second, "Updated description"))

        assertEquals(1, memory.diagnostics().records)
        assertEquals(1L, memory.diagnostics().duplicateMerges)
        assertEquals("Updated description", memory.recent().single().summary)
    }

    @Test
    fun `memory expires and prunes records`() {
        var clock = 1_000L
        val memory = MayraVisionMemory(VisionMemoryPolicy(personalRetentionMs = 100L), now = { clock })
        val request = request(allowMemory = true, createdAt = clock, expiresAt = clock + 10_000)
        memory.remember(request, analysis(request, "Temporary"))
        clock += 200

        assertTrue(memory.recent().isEmpty())
        assertEquals(1L, memory.diagnostics().expiredPruned)
    }

    @Test
    fun `response composer includes receipt total and warning`() {
        val request = request(tasks = setOf(VisionTask.READ_RECEIPT))
        val analysis = analysis(request, "Receipt detected").copy(
            receiptFields = listOf(ReceiptField("total", "₹450", 0.95)),
            warnings = listOf("Image thodi blurry hai")
        )

        val response = VisionResponseComposer().compose(analysis)

        assertTrue(response.contains("₹450"))
        assertTrue(response.contains("Dhyan rahe"))
    }

    @Test
    fun `coordinator remembers completed analysis and returns voice response`() = runBlocking {
        val runtime = MayraVisionRuntime(listOf(fakeProvider("local", VisionProviderKind.ON_DEVICE, false)))
        val coordinator = MayraVisionCoordinator(runtime)

        val (result, response) = coordinator.handle("Photo me kya hai?", asset())

        assertTrue(result is VisionRuntimeResult.Completed)
        assertTrue(response.contains("Test image"))
        assertNotNull(coordinator.snapshot().lastAnalysis)
    }

    private fun request(
        id: String = "request-1",
        asset: VisionAsset = asset(),
        tasks: Set<VisionTask> = setOf(VisionTask.DESCRIBE),
        sensitivity: VisionSensitivity = VisionSensitivity.PERSONAL,
        mode: VisionProcessingMode = VisionProcessingMode.PREFER_ON_DEVICE,
        allowMemory: Boolean = false,
        createdAt: Long = 1_000L,
        expiresAt: Long = 10_000L
    ) = VisionRequest(
        id = id,
        asset = asset,
        tasks = tasks,
        sensitivity = sensitivity,
        mode = mode,
        allowMemory = allowMemory,
        createdAt = createdAt,
        expiresAt = expiresAt
    )

    private fun asset(sizeBytes: Long = 1_024, sensitive: Boolean = false) = VisionAsset(
        id = "asset-1",
        uri = "content://test/image.jpg",
        mimeType = "image/jpeg",
        displayName = "image.jpg",
        width = 100,
        height = 100,
        sizeBytes = sizeBytes,
        source = VisionAssetSource.GALLERY,
        fingerprint = "same-fingerprint",
        sensitive = sensitive
    )

    private fun fakeProvider(
        id: String,
        kind: VisionProviderKind,
        remote: Boolean,
        priority: Int = 0
    ) = object : MayraVisionProvider {
        override val descriptor = descriptor(id, kind, remote, priority)
        override suspend fun analyze(request: VisionRequest): VisionProviderResult = VisionProviderResult.Success(
            analysis(request, "Test image").copy(
                evidence = listOf(VisionEvidence(id, request.tasks.first(), 0.9)),
                usedRemoteProcessing = remote
            )
        )
    }

    private fun descriptor(id: String, kind: VisionProviderKind, remote: Boolean, priority: Int = 0) = VisionProviderDescriptor(
        id = id,
        displayName = id,
        kind = kind,
        supportedTasks = VisionTask.entries.toSet(),
        requiresNetwork = remote,
        sendsImageOffDevice = remote,
        maxImageBytes = 20L * 1024 * 1024,
        priority = priority
    )

    private fun analysis(
        request: VisionRequest,
        summary: String,
        labels: List<VisionLabel> = emptyList()
    ) = VisionAnalysis(
        requestId = request.id,
        assetId = request.asset.id,
        summary = summary,
        labels = labels,
        processingMillis = 50,
        usedRemoteProcessing = false,
        confidence = 0.9
    )
}
