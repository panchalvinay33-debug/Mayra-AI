package ai.mayra.app.core

data class MayraMessage(
    val text: String,
    val sender: Sender,
    val timestamp: Long = System.currentTimeMillis(),
    val usedPersonalMemoryKeys: List<String> = emptyList()
) {
    init {
        require(usedPersonalMemoryKeys.none(String::isBlank))
    }

    enum class Sender { USER, MAYRA }
}
