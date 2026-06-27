package com.qualitara.itinera.workflow.exception

/**
 * Thrown when a workflow transition is requested that is not valid for the current session state.
 *
 * Examples: attempting go-live before validation, or transitioning from COMPLETE.
 */
class InvalidWorkflowTransitionException(message: String) : RuntimeException(message)
