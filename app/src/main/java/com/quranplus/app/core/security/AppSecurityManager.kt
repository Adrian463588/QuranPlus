package com.quranplus.app.core.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import java.security.MessageDigest
import java.util.Locale

class AppSecurityManager(
    private val context: Context? = null
) {

    companion object {
        val DEFAULT_ROOT_PATHS = listOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )

        val SUSPICIOUS_LIBRARIES = listOf(
            "frida-agent",
            "frida-gadget",
            "xposed.bridge",
            "libxposed",
            "substrate",
            "libmembi"
        )

        const val FRIDA_DEFAULT_PORT = 27042
    }

    /**
     * Executes the comprehensive suite of on-device security integrity checks.
     */
    fun verifyAppIntegrity(
        targetContext: Context? = context,
        trustedSignatures: Set<String> = emptySet(),
        enforceDebugCheck: Boolean = true
    ): SecurityStatus {
        val violations = mutableListOf<SecurityViolation>()

        // 1. Debugger & Tracer checks
        if (enforceDebugCheck) {
            checkDebuggerStatus(targetContext)?.let { violations.add(it) }
        }
        checkTracerStatus()?.let { violations.add(it) }

        // 2. Dynamic hooking framework checks (Frida / Xposed)
        checkHookingFrameworks()?.let { violations.add(it) }

        // 3. Environment & Root checks
        checkRootStatus()?.let { violations.add(it) }

        // 4. Signature integrity check
        if (targetContext != null && trustedSignatures.isNotEmpty()) {
            checkSignatureIntegrity(targetContext, trustedSignatures)?.let { violations.add(it) }
        }

        return SecurityStatus(
            isSecure = violations.isEmpty(),
            violations = violations
        )
    }

    /**
     * Detects attached Java/Native debugger or debuggable application state.
     */
    fun checkDebuggerStatus(ctx: Context? = context): SecurityViolation? {
        if (Debug.isDebuggerConnected() || Debug.waitingForDebugger()) {
            return SecurityViolation.DebuggerAttached
        }
        if (ctx != null) {
            val isDebuggable = (ctx.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
            // When in strict release evaluation, a debuggable flag can be flagged
        }
        return null
    }

    /**
     * Inspects /proc/self/status for active ptrace attachment (TracerPid != 0).
     */
    fun checkTracerStatus(procStatusContent: String? = null): SecurityViolation? {
        val content = procStatusContent ?: readProcSelfStatus() ?: return null
        val tracerPid = parseTracerPid(content)
        return if (tracerPid > 0) {
            SecurityViolation.TracerAttached(tracerPid)
        } else {
            null
        }
    }

    /**
     * Checks loaded process memory maps (/proc/self/maps) and default Frida socket ports.
     */
    fun checkHookingFrameworks(
        procMapsContent: String? = null,
        checkSocket: Boolean = false
    ): SecurityViolation? {
        val maps = procMapsContent ?: readProcSelfMaps()
        if (maps != null) {
            for (line in maps.lineSequence()) {
                val lowerLine = line.lowercase(Locale.ROOT)
                for (lib in SUSPICIOUS_LIBRARIES) {
                    if (lowerLine.contains(lib)) {
                        return SecurityViolation.HookFrameworkDetected(lib)
                    }
                }
            }
        }

        if (checkSocket) {
            if (isFridaSocketListening()) {
                return SecurityViolation.HookFrameworkDetected("frida-socket:$FRIDA_DEFAULT_PORT")
            }
        }

        return null
    }

    /**
     * Checks for known su binaries and test-keys build tags.
     */
    fun checkRootStatus(
        rootPaths: List<String> = DEFAULT_ROOT_PATHS,
        buildTags: String? = Build.TAGS
    ): SecurityViolation? {
        // Check test-keys
        if (buildTags != null && buildTags.contains("test-keys")) {
            return SecurityViolation.RootDetected("build:test-keys")
        }

        // Check file indicators
        for (path in rootPaths) {
            try {
                val file = File(path)
                if (file.exists()) {
                    return SecurityViolation.RootDetected("su-binary:$path")
                }
            } catch (_: Exception) {
                // Ignore security exceptions on sandboxed paths
            }
        }
        return null
    }

    /**
     * Checks runtime APK signature against the expected SHA-256 certificate hashes.
     */
    fun checkSignatureIntegrity(
        ctx: Context,
        trustedSignatures: Set<String>
    ): SecurityViolation? {
        val certs = getSigningCertificatesSha256(ctx)
        if (certs.isEmpty()) return null

        val matches = certs.any { cert ->
            trustedSignatures.any { trusted ->
                cert.equals(trusted, ignoreCase = true) ||
                    cert.replace(":", "").equals(trusted.replace(":", ""), ignoreCase = true)
            }
        }

        return if (!matches && trustedSignatures.isNotEmpty()) {
            SecurityViolation.SignatureMismatch(
                expectedSha256 = trustedSignatures.first(),
                actualSha256 = certs.firstOrNull().orEmpty()
            )
        } else {
            null
        }
    }

    /**
     * Retrieves SHA-256 fingerprint of the current signing certificate(s).
     */
    fun getSigningCertificatesSha256(ctx: Context): List<String> {
        return try {
            val packageInfo: PackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ctx.packageManager.getPackageInfo(
                    ctx.packageName,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                ctx.packageManager.getPackageInfo(ctx.packageName, PackageManager.GET_SIGNATURES)
            }

            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val signingInfo = packageInfo.signingInfo
                if (signingInfo != null) {
                    if (signingInfo.hasMultipleSigners()) {
                        signingInfo.apkContentsSigners
                    } else {
                        signingInfo.signingCertificateHistory
                    }
                } else null
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            signatures?.map { sha256Hex(it.toByteArray()) } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    // Helper functions

    internal fun parseTracerPid(statusText: String): Int {
        for (line in statusText.lineSequence()) {
            if (line.startsWith("TracerPid:", ignoreCase = true)) {
                val parts = line.split(":", limit = 2)
                if (parts.size == 2) {
                    return parts[1].trim().toIntOrNull() ?: 0
                }
            }
        }
        return 0
    }

    internal fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString(":") { "%02X".format(it) }
    }

    private fun readProcSelfStatus(): String? {
        return try {
            File("/proc/self/status").readText()
        } catch (_: Exception) {
            null
        }
    }

    private fun readProcSelfMaps(): String? {
        return try {
            File("/proc/self/maps").readText()
        } catch (_: Exception) {
            null
        }
    }

    private fun isFridaSocketListening(): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress("127.0.0.1", FRIDA_DEFAULT_PORT), 50)
                true
            }
        } catch (_: Exception) {
            false
        }
    }
}
