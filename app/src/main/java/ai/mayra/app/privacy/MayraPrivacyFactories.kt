package ai.mayra.app.privacy

/** Test-friendly factory allowing trailing-lambda clock injection. */
@Suppress("FunctionName")
fun MayraPrivacyCenter(clock: () -> Long): MayraPrivacyCenter =
    MayraPrivacyCenter(
        clock = clock,
        redactor = SensitiveTextRedactor(),
        maxAuditEvents = 1_000
    )
