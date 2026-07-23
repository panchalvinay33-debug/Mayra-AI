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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class RuntimeHealth { HEALTHY, ATTENTION, DEGRADED }

data class RuntimeMetric(
    val label: String,
    val value: String,
    val detail: String
)

data class RuntimePendingAction(
    val id: String,
    val title: String
)

data class RuntimeControlUiState(
    val health: RuntimeHealth,
    val headline: String,
    val metrics: List<RuntimeMetric>,
    val activePlanTitles: List<String>,
    val pendingActions: List<RuntimePendingAction>,
    val capturedAt: Long,
    val notice: String? = null,
    val error: String? = null
) {
    companion object {
        val Loading = RuntimeControlUiState(
            health = RuntimeHealth.ATTENTION,
            headline = "Reading Mayra runtime…",
            metrics = emptyList(),
            activePlanTitles = emptyList(),
            pendingActions = emptyList(),
            capturedAt = 0L
        )

        fun failure(message: String) = RuntimeControlUiState(
            health = RuntimeHealth.DEGRADED,
            headline = "Runtime status unavailable",
            metrics = emptyList(),
            activePlanTitles = emptyList(),
            pendingActions = emptyList(),
            capturedAt = System.currentTimeMillis(),
            error = message
        )
    }
}

fun RuntimeControlSnapshot.toUiState(): RuntimeControlUiState {
    val failedCount = runtime.failedRequests + plans.failedPlans
    val needsAttention = pendingActions.isNotEmpty() || plans.blockedPlans > 0 ||
        plans.waitingConfirmationSteps > 0
    val health = when {
        failedCount > 0 -> RuntimeHealth.DEGRADED
        needsAttention -> RuntimeHealth.ATTENTION
        else -> RuntimeHealth.HEALTHY
    }
    val headline = when (health) {
        RuntimeHealth.HEALTHY -> "Mayra runtime is healthy"
        RuntimeHealth.ATTENTION -> "Mayra needs your attention"
        RuntimeHealth.DEGRADED -> "Mayra runtime has failures"
    }
    return RuntimeControlUiState(
        health = health,
        headline = headline,
        metrics = listOf(
            RuntimeMetric(
                label = "Requests",
                value = runtime.processedRequests.toString(),
                detail = "${runtime.completedRequests} completed · ${runtime.failedRequests} failed"
            ),
            RuntimeMetric(
                label = "Plans",
                value = plans.activePlans.toString(),
                detail = "${plans.runningPlans} running · ${plans.blockedPlans} blocked"
            ),
            RuntimeMetric(
                label = "Confirmations",
                value = pendingActions.size.toString(),
                detail = "${plans.waitingConfirmationSteps} plan steps waiting"
            ),
            RuntimeMetric(
                label = "Average response",
                value = "${runtime.averageLatencyMillis} ms",
                detail = "${runtime.recentReceiptCount} recent receipts"
            )
        ),
        activePlanTitles = activePlans.map { it.title }.take(5),
        pendingActions = pendingActions.map { RuntimePendingAction(it.id, it.title) }.take(5),
        capturedAt = capturedAt
    )
}

@Composable
fun RuntimeControlDialog(
    state: RuntimeControlUiState,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Runtime control") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = state.headline,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                state.notice?.let {
                    Text(it, color = MaterialTheme.colorScheme.primary)
                }
                state.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                state.metrics.forEach { metric -> RuntimeMetricCard(metric) }
                if (state.pendingActions.isNotEmpty()) {
                    Text("Waiting for approval", fontWeight = FontWeight.SemiBold)
                    state.pendingActions.forEach { action ->
                        RuntimePendingActionCard(action, onApprove, onReject)
                    }
                }
                if (state.activePlanTitles.isNotEmpty()) {
                    RuntimeSummaryGroup("Active workflows", state.activePlanTitles)
                }
                if (state.metrics.isNotEmpty() &&
                    state.pendingActions.isEmpty() &&
                    state.activePlanTitles.isEmpty()
                ) {
                    Text(
                        "No active workflows or pending confirmations.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onRefresh) { Text("Refresh") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun RuntimeMetricCard(metric: RuntimeMetric) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
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
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(action.title, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onReject(action.id) }) { Text("Reject") }
                OutlinedButton(onClick = { onApprove(action.id) }) { Text("Approve") }
            }
        }
    }
}

@Composable
private fun RuntimeSummaryGroup(title: String, items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold)
        items.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
    }
}
