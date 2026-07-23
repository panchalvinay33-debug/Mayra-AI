package ai.mayra.app.brain

/** Source-compatible factory for tests written before policy-list parameters were added. */
internal fun MayraBrainCoordinator(
    eventBus: BrainEventBus,
    contextProvider: () -> BrainContextSnapshot
): MayraBrainCoordinator = MayraBrainCoordinator(
    eventBus = eventBus,
    contextProvider = contextProvider
)
