/**
 * Persistence model types representing database rows.
 *
 * All types in this package are plain data carriers. They hold no business logic,
 * no behavior, and no awareness of the workflow state machine.
 *
 * Record classes in this package map 1:1 to database tables and must not be
 * extended with domain behavior. Cross-cutting concerns such as validation,
 * state transitions, and payload interpretation belong in the service layer.
 */
@file:Suppress("PACKAGE")
