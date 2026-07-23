package ai.mayra.app.runtime

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay

enum class RuntimeHealth { HEALTHY, ATTENTION, DEGRADED }

data class RuntimeMetric(val label: String, val value: String, val detail: String)
data class RuntimePendingAction(val id: String, val title: String)
data class RuntimeActivePlan(
    val id: String,
    val title: String,
    val state: String,
    val progressPercent: Int,
    val progressDetail: String
)

data class RuntimeControlUiState(
    val health: RuntimeHealth,
    val headline: String,
    val metrics: List<RuntimeMetric>,
    val activePlans: List<RuntimeActivePlan>,
    val pendingActions: List<RuntimePendingAction>,
    val capturedAt: Long,
    val notice: String? = null,
    val error: String? = null,
    val isBusy: Boolean = false,
    val busyLabel: String? = null
) {
    companion object {
        val Loading = RuntimeControlUiState(
            RuntimeHealth.ATTENTION,
            "Reading Mayra runtime…",
            emptyList(),
            emptyList(),
            emptyList(),
            0L
        )

        fun failure(message: String) = RuntimeControlUiState(
            RuntimeHealth.DEGRADED,
            "Runtime status unavailable",
            emptyList(),
            emptyList(),
            emptyList(),
            System.currentTimeMillis(),
            error = message
        )
    }
}

internal fun classifyRuntimeHealth(
    failedCount: Long,
    pendingActionCount: Int,
    blockedPlanCount: Int,
    waitingConfirmationSteps: Int
): RuntimeHealth = when {
    failedCount > 0 -> RuntimeHealth.DEGRADED
    pendingActionCount > 0 || blockedPlanCount > 0 || waitingConfirmationSteps > 0 -> RuntimeHealth.ATTENTION
    else -> RuntimeHealth.HEALTHY
}

internal fun runtimeSnapshotFreshness(capturedAt: Long, now: Long): String {
    if (capturedAt <= 0L) return "Waiting for first snapshot"
    val ageSeconds = ((now - capturedAt).coerceAtLeast(0L) / 1_000L)
    return when {
        ageSeconds < 2L -> "Updated just now"
        ageSeconds < 60L -> "Updated ${ageSeconds}s ago"
        else -> "Snapshot may be stale · updated ${ageSeconds / 60L}m ago"
    }
}

internal fun workflowProgress(
    totalSteps: Int,
    completedSteps: Int,
    failedSteps: Int,
    waitingSteps: Int
): Pair<Int, String> {
    val percent = if (totalSteps <= 0) 0 else (completedSteps * 100 / totalSteps).coerceIn(0, 100)
    val detail = "$completedSteps/$totalSteps completed · $failedSteps failed · $waitingSteps waiting"
    return percent to detail
}

fun RuntimeControlSnapshot.toUiState(): RuntimeControlUiState {
    val health = classifyRuntimeHealth(
        runtime.failedRequests + plans.failedPlans,
        pendingActions.size,
        plans.blockedPlans,
        plans.waitingConfirmationSteps
    )
    return RuntimeControlUiState(
        health = health,
        headline = when (health) {
            RuntimeHealth.HEALTHY -> "Mayra runtime is healthy"
            RuntimeHealth.ATTENTION -> "Mayra needs your attention"
            RuntimeHealth.DEGRADED -> "Mayra runtime has failures"
        },
        metrics = listOf(
            RuntimeMetric("Requests", runtime.processedRequests.toString(), "${runtime.completedRequests} completed · ${runtime.failedRequests} failed"),
            RuntimeMetric("Plans", plans.activePlans.toString(), "${plans.runningPlans} running · ${plans.blockedPlans} blocked"),
            RuntimeMetric("Confirmations", pendingActions.size.toString(), "${plans.waitingConfirmationSteps} plan steps waiting"),
            RuntimeMetric("Average response", "${runtime.averageLatencyMillis} ms", "${runtime.recentReceiptCount} recent receipts"),
            RuntimeMetric("Audit activity", recentAuditCount.toString(), "recent trusted runtime events")
        ),
        activePlans = activePlans.take(5).map { plan ->
            val completed = plan.steps.count { it.state.name == "COMPLETED" }
            val failed = plan.steps.count { it.state.name == "FAILED" }
            val waiting = plan.steps.count { it.state.name in setOf("WAITING", "READY", "RUNNING") }
            val (percent, detail) = workflowProgress(plan.steps.size, completed, failed, waiting)
            RuntimeActivePlan(plan.id, plan.title, plan.state.name, percent, detail)
        },
        pendingActions = pendingActions.take(5).map { RuntimePendingAction(it.id, it.title) },
        capturedAt = capturedAt
    )
}

@Composable
fun RuntimeControlDialog(
    state: RuntimeControlUiState,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit,
    controller: RuntimeControlViewModel = viewModel(),
    onApprove: (String) -> Unit = controller::approve,
    onReject: (String) -> Unit = controller::reject,
    onRunNext: (String) -> Unit = controller::runNext,
    onCancelPlan: (String) -> Unit = controller::cancelPlan,
    onClearHistory: () -> Unit = controller::clearCompletedHistory
) {
    LaunchedEffect(Unit) {
        while (true) {
            delay(AUTO_REFRESH_MILLIS)
            if (!state.isBusy) onRefresh()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Runtime control") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(state.headline, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    runtimeSnapshotFreshness(state.capturedAt, System.currentTimeMillis()),
                    style = MaterialTheme.typography.bodySmall
                )
                state.busyLabel?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                state.notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                state.metrics.forEach { RuntimeMetricCard(it) }
                if (state.pendingActions.isNotEmpty()) {
                    Text("Waiting for approval", fontWeight = FontWeight.SemiBold)
                    state.pendingActions.forEach { RuntimePendingActionCard(it, state.isBusy, onApprove, onReject) }
                }
                if (state.activePlans.isNotEmpty()) {
                    Text("Active workflows", fontWeight = FontWeight.SemiBold)
                    state.activePlans.forEach { RuntimeActivePlanCard(it, state.isBusy, onRunNext, onCancelPlan) }
                }
                if (state.metrics.isNotEmpty() && state.pendingActions.isEmpty() && state.activePlans.isEmpty()) {
                    Text("No active workflows or pending confirmations.", style = MaterialTheme.typography.bodySmall)
                }
                OutlinedButton(
                    onClick = onClearHistory,
                    enabled = !state.isBusy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear completed workflow history")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onRefresh, enabled = !state.isBusy) { Text("Refresh") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun RuntimeMetricCard(metric: RuntimeMetric) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(metric.label, fontWeight = FontWeight.SemiBold)
                Text(metric.detail, style = MaterialTheme.typography.bodySmall)
            }
            Text(metric.value, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RuntimePendingActionCard(
    action: RuntimePendingAction,
    isBusy: Boolean,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(action.title, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onReject(action.id) }, enabled = !isBusy) { Text("Reject") }
                OutlinedButton(onClick = { onApprove(action.id) }, enabled = !isBusy) { Text("Approve") }
            }
        }
    }
}

@Composable
private fun RuntimeActivePlanCard(
    plan: RuntimeActivePlan,
    isBusy: Boolean,
    onRunNext: (String) -> Unit,
    onCancelPlan: (String) -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(plan.title, fontWeight = FontWeight.SemiBold)
            Text("${plan.state} · ${plan.progressPercent}%", style = MaterialTheme.typography.bodySmall)
            Text(plan.progressDetail, style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onCancelPlan(plan.id) }, enabled = !isBusy) { Text("Cancel") }
                OutlinedButton(onClick = { onRunNext(plan.id) }, enabled = !isBusy) { Text("Run next") }
            }
        }
    }
}

private const val AUTO_REFRESH_MILLIS = 5_000L
