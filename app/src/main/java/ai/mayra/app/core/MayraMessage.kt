package ai.mayra.app.core

data class MayraMessage(
    val text: String,
    val sender: Sender,
    val timestamp: Long = System.currentTimeMillis()
) {
    enum class Sender { USER, MAYRA }
}
