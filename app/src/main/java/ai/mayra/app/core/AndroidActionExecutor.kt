package ai.mayra.app.core

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Android-backed [ActionExecutor].
 *
 * App launches execute immediately. Calls and messages intentionally open safe system composers
 * instead of directly placing a call or sending a message. Contact-name lookup and structured
 * reminder scheduling will be connected in later phases.
 */
class AndroidActionExecutor(
    context: Context,
    private val appResolver: AndroidAppResolver = AndroidAppResolver(context)
) : ActionExecutor {

    private val appContext = context.applicationContext

    override suspend fun openApp(packageOrName: String): ActionExecutionResult {
        val resolved = appResolver.resolve(packageOrName)
            ?: return ActionExecutionResult.NotSupported(
                "I couldn't find an installed app named $packageOrName."
            )

        return launch(
            resolved.launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            failureMessage = "${resolved.label} could not be opened."
        )
    }

    override suspend fun callContact(name: String): ActionExecutionResult {
        val target = name.trim()
        if (target.isEmpty()) {
            return ActionExecutionResult.Failure("A contact name or phone number is required.")
        }

        if (!target.looksLikePhoneNumber()) {
            return ActionExecutionResult.ConfirmationRequired(
                "I found the call request for $target. Contact lookup must be connected before I can open the dialer."
            )
        }

        val dialIntent = Intent(
            Intent.ACTION_DIAL,
            Uri.parse("tel:${Uri.encode(target)}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        return launch(dialIntent, "The phone dialer is not available.")
    }

    override suspend fun sendMessage(
        recipient: String,
        message: String?
    ): ActionExecutionResult {
        val target = recipient.trim()
        if (target.isEmpty()) {
            return ActionExecutionResult.Failure("A recipient is required.")
        }

        if (!target.looksLikePhoneNumber()) {
            return ActionExecutionResult.ConfirmationRequired(
                "I prepared the message for $target. Contact lookup must be connected before I can open the SMS composer."
            )
        }

        val smsIntent = Intent(
            Intent.ACTION_SENDTO,
            Uri.parse("smsto:${Uri.encode(target)}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            message?.takeIf { it.isNotBlank() }?.let {
                putExtra("sms_body", it)
            }
        }

        return launch(smsIntent, "No SMS application is available.")
    }

    override suspend fun createReminder(request: String): ActionExecutionResult {
        if (request.isBlank()) {
            return ActionExecutionResult.Failure("Reminder details are required.")
        }

        return ActionExecutionResult.NotSupported(
            "I understood the reminder, but date and time parsing must be connected before it can be scheduled safely."
        )
    }

    private fun launch(intent: Intent, failureMessage: String): ActionExecutionResult = try {
        appContext.startActivity(intent)
        ActionExecutionResult.Success
    } catch (_: ActivityNotFoundException) {
        ActionExecutionResult.NotSupported(failureMessage)
    } catch (securityException: SecurityException) {
        ActionExecutionResult.Failure(
            securityException.message ?: "Android blocked this action."
        )
    } catch (throwable: Throwable) {
        ActionExecutionResult.Failure(
            throwable.message ?: failureMessage
        )
    }

    private fun String.looksLikePhoneNumber(): Boolean {
        val compact = filterNot(Char::isWhitespace)
        if (compact.length < 3) return false
        return compact.all { it.isDigit() || it == '+' || it == '-' || it == '(' || it == ')' }
    }
}
