package com.landawn.abacus.util;

/**
 * Defines strategies for representing enum values during type conversion.
 * The choice between NAME, ORDINAL, and CODE affects data portability, storage size,
 * and resilience to code changes.
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * public enum Status { PENDING, ACTIVE, SUSPENDED, CLOSED }
 *
 * // Using NAME - stored as "ACTIVE", "SUSPENDED", etc.
 * @Type(enumerated = EnumType.NAME)
 * private Status accountStatus;
 *
 * // Using ORDINAL - stored as 0, 1, 2, 3
 * @Type(enumerated = EnumType.ORDINAL)
 * private Status orderStatus;
 * }</pre>
 * 
 * @see UnifiedStatus
 */
public enum EnumType {

    /**
     * Persist enumerated type property or field as a string using its constant name.
     *
     * <p>This is the recommended approach as it's readable, maintainable, and resilient
     * to enum reordering. The only concern is renaming enum constants, which would
     * require data migration.</p>
     *
     * <p><b>Example:</b> For enum {@code Status { PENDING, ACTIVE, CLOSED }},
     * values are stored as "PENDING", "ACTIVE", "CLOSED". You can safely reorder
     * or add new values without breaking existing data.</p>
     *
     * <p><b>Benefits:</b></p>
     * <ul>
     *   <li>Human-readable data in storage</li>
     *   <li>Resilient to enum reordering</li>
     *   <li>Easier debugging and data inspection</li>
     *   <li>Safe to add new enum values</li>
     * </ul>
     */
    NAME,

    /**
     * Persist enumerated type property or field as an integer using its ordinal position.
     *
     * <p><b>Warning:</b> This representation is fragile as it depends on the declaration order
     * of enum constants. Adding, removing, or reordering enum values will break
     * compatibility with existing persisted data.</p>
     *
     * <p><b>Example:</b> For enum {@code Status { PENDING, ACTIVE, CLOSED }},
     * values are stored as 0, 1, 2 respectively. If you later change it to
     * {@code Status { ACTIVE, PENDING, CLOSED }}, all existing data will be incorrect.</p>
     *
     * <p><b>Use when:</b></p>
     * <ul>
     *   <li>Storage space is critical</li>
     *   <li>The enum is guaranteed to never change order</li>
     *   <li>Performance is a priority and enum order is stable</li>
     * </ul>
     */
    ORDINAL,

    /**
     * Persist enumerated type property or field as an int using a predefined code,
     * typically supplied by a public {@code int code()} method.
     * @see UnifiedStatus#code()
     */
    CODE
}
