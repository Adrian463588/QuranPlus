package com.quranplus.app.core.security

/**
 * Categorized security violations detected by the on-device security engine.
 */
sealed interface SecurityViolation {
    data class SignatureMismatch(
        val expectedSha256: String,
        val actualSha256: String
    ) : SecurityViolation

    data object DebuggerAttached : SecurityViolation

    data class TracerAttached(
        val tracerPid: Int
    ) : SecurityViolation

    data class HookFrameworkDetected(
        val artifact: String
    ) : SecurityViolation

    data class RootDetected(
        val indicator: String
    ) : SecurityViolation
}

/**
 * Aggregated runtime security posture assessment.
 */
data class SecurityStatus(
    val isSecure: Boolean,
    val violations: List<SecurityViolation> = emptyList()
) {
    val hasCriticalTampering: Boolean
        get() = violations.any {
            it is SecurityViolation.SignatureMismatch ||
                it is SecurityViolation.DebuggerAttached ||
                it is SecurityViolation.TracerAttached
        }
}
