package ai.mayra.app.brain

/** Source-compatible factories for tests written before policy-list parameters were added. */
internal fun MayraBrainCoordinator(
    eventBus: BrainEventBus,
    contextProvider: () -> BrainContextSnapshot
): MayraBrainCoordinator = MayraBrainCoordinator(
    eventBus = eventBus,
    contextProvider = contextProvider
)

internal fun MayraPlanRuntime(
    planner: MayraTaskPlanner,
    store: MayraPlanStore,
    skills: MayraSkillRegistry,
    contextProvider: () -> BrainContextSnapshot
): MayraPlanRuntime = MayraPlanRuntime(
    planner = planner,
    store = store,
    skills = skills,
    contextProvider = contextProvider
)
