package com.courseflow.app.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateManagerTest {
    @Test
    fun newerSemanticVersionIsDetected() {
        assertTrue(isVersionNewer("1.1.0", "1.0.9"))
        assertTrue(isVersionNewer("v2.0", "1.99.99"))
    }

    @Test
    fun sameOrOlderVersionIsNotAnUpdate() {
        assertFalse(isVersionNewer("1.0.0", "1.0.0"))
        assertFalse(isVersionNewer("0.9.9", "1.0.0"))
    }
}
