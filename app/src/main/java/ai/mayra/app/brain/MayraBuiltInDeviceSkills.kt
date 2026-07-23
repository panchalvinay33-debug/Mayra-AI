package ai.mayra.app.brain

import ai.mayra.app.core.ActionExecutionResult
import ai.mayra.app.core.ActionExecutor

abstract class DeviceActionSkill(
    final override val descriptor: SkillDescriptor,
    private val executor: ActionExecutor,
    private val successMessage: String
) : KeywordSkill() {
    protected abstract suspend fun perform(request: SkillRequest): ActionExecutionResult

    final override suspend fun execute(request: SkillRequest): SkillResult = perform(request).toSkillResult(successMessage)

    private fun ActionExecutionResult.toSkillResult(success: String): SkillResult = when (this) {
        ActionExecutionResult.Success -> SkillResult.Success(success)
        is ActionExecutionResult.ConfirmationRequired -> SkillResult.NeedsConfirmation(
            prompt = message,
            actionType = descriptor.id,
            payload = success
        )
        is ActionExecutionResult.PermissionRequired -> SkillResult.MissingPermission(
            permissions = permissions.map { it.name }.toSet(),
            explanation = message
        )
        is ActionExecutionResult.NotSupported -> SkillResult.Failure(reason, retryable = false)
        is ActionExecutionResult.Failure -> SkillResult.Failure(error, retryable = true)
    }
}

class OpenAppSkill(private val actionExecutor: ActionExecutor) : DeviceActionSkill(
    descriptor = SkillDescriptor(
        id = "open_app",
        displayName = "Open app",
        supportedIntents = setOf("open_app", "launch_app"),
        sensitive = false
    ),
    executor = actionExecutor,
    successMessage = "App opened."
) {
    override val keywords = setOf("open", "launch", "start", "खोल", "चालू")

    override suspend fun perform(request: SkillRequest): ActionExecutionResult {
        val app = request.parameters["app"]
            ?: request.parameters["appName"]
            ?: request.utterance.substringAfterLast(" ").trim()
        if (app.isBlank()) return ActionExecutionResult.Failure("App name is missing")
        return actionExecutor.openApp(app)
    }
}

class CallContactSkill(private val actionExecutor: ActionExecutor) : DeviceActionSkill(
    descriptor = SkillDescriptor(
        id = "call",
        displayName = "Call contact",
        supportedIntents = setOf("call", "phone"),
        sensitive = true
    ),
    executor = actionExecutor,
    successMessage = "Call started."
) {
    override val keywords = setOf("call", "phone", "dial", "फोन", "कॉल")

    override suspend fun perform(request: SkillRequest): ActionExecutionResult {
        val contact = request.parameters["contact"]
            ?: request.parameters["name"]
            ?: request.utterance
                .replace(Regex("(?i)\\b(call|phone|dial)\\b"), "")
                .replace("कॉल", "")
                .replace("फोन", "")
                .trim()
        if (contact.isBlank()) return ActionExecutionResult.Failure("Contact name is missing")
        return actionExecutor.callContact(contact)
    }
}

class SendMessageSkill(private val actionExecutor: ActionExecutor) : DeviceActionSkill(
    descriptor = SkillDescriptor(
        id = "message",
        displayName = "Send message",
        supportedIntents = setOf("message", "sms", "send_message"),
        sensitive = true
    ),
    executor = actionExecutor,
    successMessage = "Message prepared."
) {
    override val keywords = setOf("message", "sms", "send", "मैसेज", "संदेश", "भेज")

    override suspend fun perform(request: SkillRequest): ActionExecutionResult {
        val recipient = request.parameters["recipient"]
            ?: request.parameters["contact"]
            ?: request.parameters["name"]
            ?: return ActionExecutionResult.Failure("Message recipient is missing")
        val message = request.parameters["message"] ?: request.parameters["text"]
        return actionExecutor.sendMessage(recipient, message)
    }
}

class ReminderSkill(private val actionExecutor: ActionExecutor) : DeviceActionSkill(
    descriptor = SkillDescriptor(
        id = "reminder",
        displayName = "Create reminder",
        supportedIntents = setOf("reminder", "create_reminder"),
        sensitive = false
    ),
    executor = actionExecutor,
    successMessage = "Reminder opened."
) {
    override val keywords = setOf("remind", "reminder", "याद", "रिमाइंडर")

    override suspend fun perform(request: SkillRequest): ActionExecutionResult {
        val reminder = request.parameters["request"]
            ?: request.parameters["text"]
            ?: request.utterance
        if (reminder.isBlank()) return ActionExecutionResult.Failure("Reminder text is missing")
        return actionExecutor.createReminder(reminder)
    }
}

fun MayraSkillRegistry.registerBuiltInDeviceSkills(executor: ActionExecutor) {
    register(OpenAppSkill(executor))
    register(CallContactSkill(executor))
    register(SendMessageSkill(executor))
    register(ReminderSkill(executor))
    register(HelpSkill())
}
