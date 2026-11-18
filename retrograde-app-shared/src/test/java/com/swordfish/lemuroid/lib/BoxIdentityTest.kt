package com.swordfish.lemuroid.lib

import com.swordfish.lemuroid.lib.box.BoxIdentity
import com.swordfish.lemuroid.lib.box.BoxIdentity.Verdict
import org.junit.Assert.assertEquals
import org.junit.Test

class BoxIdentityTest {
    private val box = "6f1c2e0a-4b7d-4c1e-9a55-0123456789ab"
    private val other = "0b9e8d7c-6a5f-4e3d-8c2b-a1b2c3d4e5f6"

    @Test
    fun theFirstBoxIsRemembered() {
        assertEquals(Verdict.REMEMBER, BoxIdentity.check(null, box))
        assertEquals(Verdict.REMEMBER, BoxIdentity.check("", box))
    }

    @Test
    fun theSameBoxIsTheBox() {
        assertEquals(Verdict.SAME, BoxIdentity.check(box, box.uppercase()))
    }

    @Test
    fun anotherMachineAtTheAddressIsNotTheBox() {
        assertEquals(Verdict.DIFFERENT, BoxIdentity.check(box, other))
    }

    @Test
    fun aServerWithNoGuidIsTakenOnlyWhileNoBoxIsRemembered() {
        assertEquals(Verdict.SAME, BoxIdentity.check(null, null))
        assertEquals(Verdict.SAME, BoxIdentity.check(null, ""))
        assertEquals(Verdict.SAME, BoxIdentity.check(null, "00000000-0000-0000-0000-000000000000"))
    }

    @Test
    fun aServerWithNoGuidIsNotTheRememberedBox() {
        assertEquals(Verdict.DIFFERENT, BoxIdentity.check(box, null))
        assertEquals(Verdict.DIFFERENT, BoxIdentity.check(box, " "))
        assertEquals(Verdict.DIFFERENT, BoxIdentity.check(box, "00000000-0000-0000-0000-000000000000"))
    }
}
