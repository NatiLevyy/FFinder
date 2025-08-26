package com.locationsharing.app

import org.junit.Assert.assertTrue
import org.junit.Test

class SmokeTest {
    @Test
    fun buildsAndLoadsBuildConfig() {
        assertTrue(BuildConfig.APPLICATION_ID.isNotBlank())
    }
}