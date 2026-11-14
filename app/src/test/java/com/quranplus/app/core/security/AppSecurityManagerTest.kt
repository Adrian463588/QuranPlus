package com.quranplus.app.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AppSecurityManagerTest {

    private val securityManager = AppSecurityManager()

    @Test
    fun tracerPid_whenZero_returnsNull() {
        val procStatus = """
            Name:   com.quranplus.app
            State:  S (sleeping)
            Tgid:   28410
            Pid:    28410
            PPid:   1982
            TracerPid:  0
            Uid:    10234   10234   10234   10234
        """.trimIndent()

        val violation = securityManager.checkTracerStatus(procStatus)
        assertNull(violation)
    }

    @Test
    fun tracerPid_whenNonZero_returnsTracerAttached() {
        val procStatus = """
            Name:   com.quranplus.app
            State:  t (tracing stop)
            Tgid:   28410
            Pid:    28410
            PPid:   1982
            TracerPid:  4321
            Uid:    10234   10234   10234   10234
        """.trimIndent()

        val violation = securityManager.checkTracerStatus(procStatus)
        assertNotNull(violation)
        assertTrue(violation is SecurityViolation.TracerAttached)
        assertEquals(4321, (violation as SecurityViolation.TracerAttached).tracerPid)
    }

    @Test
    fun hookingDetection_whenCleanMaps_returnsNull() {
        val procMaps = """
            7f91c00000-7f91c20000 r--p 00000000 fd:01 12345 /system/lib64/libc.so
            7f91c20000-7f91c40000 r-xp 00020000 fd:01 12345 /system/lib64/libc.so
            7f91c40000-7f91c50000 rw-p 00040000 fd:01 12345 /system/lib64/libc.so
        """.trimIndent()

        val violation = securityManager.checkHookingFrameworks(procMapsContent = procMaps)
        assertNull(violation)
    }

    @Test
    fun hookingDetection_whenFridaPresent_returnsViolation() {
        val procMaps = """
            7f91c00000-7f91c20000 r--p 00000000 fd:01 12345 /system/lib64/libc.so
            7f92000000-7f92100000 r-xp 00000000 00:00 0     /data/local/tmp/frida-agent-64.so
        """.trimIndent()

        val violation = securityManager.checkHookingFrameworks(procMapsContent = procMaps)
        assertNotNull(violation)
        assertTrue(violation is SecurityViolation.HookFrameworkDetected)
        assertEquals("frida-agent", (violation as SecurityViolation.HookFrameworkDetected).artifact)
    }

    @Test
    fun hookingDetection_whenXposedPresent_returnsViolation() {
        val procMaps = """
            7f91c00000-7f91c20000 r--p 00000000 fd:01 12345 /data/app/de.robv.android.xposed.installer/libxposed_art.so
        """.trimIndent()

        val violation = securityManager.checkHookingFrameworks(procMapsContent = procMaps)
        assertNotNull(violation)
        assertTrue(violation is SecurityViolation.HookFrameworkDetected)
        assertEquals("libxposed", (violation as SecurityViolation.HookFrameworkDetected).artifact)
    }

    @Test
    fun rootDetection_whenTestKeysPresent_returnsViolation() {
        val violation = securityManager.checkRootStatus(
            rootPaths = emptyList(),
            buildTags = "test-keys"
        )
        assertNotNull(violation)
        assertTrue(violation is SecurityViolation.RootDetected)
        assertEquals("build:test-keys", (violation as SecurityViolation.RootDetected).indicator)
    }

    @Test
    fun rootDetection_whenReleaseKeysAndNoBinary_returnsNull() {
        val violation = securityManager.checkRootStatus(
            rootPaths = listOf("/non_existent/path/su"),
            buildTags = "release-keys"
        )
        assertNull(violation)
    }

    @Test
    fun rootDetection_whenSuFileExists_returnsViolation() {
        val tempSuFile = File.createTempFile("mock_su_", ".bin")
        try {
            val violation = securityManager.checkRootStatus(
                rootPaths = listOf(tempSuFile.absolutePath),
                buildTags = "release-keys"
            )
            assertNotNull(violation)
            assertTrue(violation is SecurityViolation.RootDetected)
        } finally {
            tempSuFile.delete()
        }
    }

    @Test
    fun sha256Formatting_producesColonDelimitedHex() {
        val input = "QuranPlusSecurity".toByteArray(Charsets.UTF_8)
        val hash = securityManager.sha256Hex(input)
        assertTrue(hash.contains(":"))
        assertEquals(32, hash.split(":").size)
    }

    @Test
    fun securityStatus_aggregationEvaluatesCorrectly() {
        val cleanStatus = SecurityStatus(isSecure = true, violations = emptyList())
        assertFalse(cleanStatus.hasCriticalTampering)

        val tamperedStatus = SecurityStatus(
            isSecure = false,
            violations = listOf(
                SecurityViolation.SignatureMismatch("EXPECTED", "ACTUAL")
            )
        )
        assertTrue(tamperedStatus.hasCriticalTampering)

        val nonCriticalStatus = SecurityStatus(
            isSecure = false,
            violations = listOf(
                SecurityViolation.RootDetected("build:test-keys")
            )
        )
        assertFalse(nonCriticalStatus.hasCriticalTampering)
    }
}
