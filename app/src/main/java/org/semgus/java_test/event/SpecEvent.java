package org.semgus.java_test.event;

/**
 * Parent interface for all SemGuS parser events.
 */
public sealed interface SpecEvent permits MetaSpecEvent, SmtSpecEvent, SemgusSpecEvent {
    // NO-OP
}
