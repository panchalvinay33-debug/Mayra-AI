package ai.mayra.app.core

import ai.mayra.app.document.DocumentSearchEngine
import ai.mayra.app.document.MayraCurrentIndexPolicy
import ai.mayra.app.document.MayraDocumentContentStore
import ai.mayra.app.document.MayraDocumentIndexMetadataStore
import ai.mayra.app.document.MayraDocumentStore
import android.content.Context

fun interface MayraAnswerProvider {
    fun answer(message: String): String
}

fun interface MayraDeviceActionExecutor {
    fun execute(message: String, decision: MayraRoutingDecision): String
}

/** Concrete adapter factory used by the app composition root. */
object MayraConcreteRuntimeAdapters {
    fun create(
        context: Context,
        answerProvider: MayraAnswerProvider,
        actionExecutor: MayraDeviceActionExecutor? = null
    ): MayraRuntimeHandlers {
        val documents = MayraDocumentStore(context)
        val content = MayraDocumentContentStore(context)
        val metadata = MayraDocumentIndexMetadataStore(context)
        val currentOnly = MayraCurrentIndexPolicy(content, metadata)

        return MayraRuntimeHandlers(
            answer = MayraRouteHandler { message, _ ->
                answerProvider.answer(message).trim().ifBlank {
                    "Mayra could not produce a reliable answer for that request."
                }
            },
            retrieve = MayraRouteHandler { message, _ ->
                val library = documents.list()
                if (library.isEmpty()) {
                    "Your Mayra Library is empty. Add a document first."
                } else {
                    val indexed = library.associateWith(currentOnly::currentText)
                    val hits = DocumentSearchEngine.search(library, indexed, message, limit = 5)
                    if (hits.isEmpty()) {
                        "No grounded match was found in a current on-device document index."
                    } else buildString {
                        append("Current local document matches:\n")
                        hits.forEachIndexed { index, hit ->
                            append("\n${index + 1}. ${hit.document.name}")
                            if (hit.snippet.isNotBlank()) append("\n${hit.snippet}")
                        }
                        append("\n\nOnly current on-device indexes were used.")
                    }
                }
            },
            act = actionExecutor?.let { executor ->
                MayraRouteHandler { message, decision -> executor.execute(message, decision) }
            }
        )
    }
}
