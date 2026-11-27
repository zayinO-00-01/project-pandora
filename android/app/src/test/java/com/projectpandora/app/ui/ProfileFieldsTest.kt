package com.projectpandora.app.ui

import com.projectpandora.app.data.DirectoryContact
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileFieldsTest {
    @Test fun phonesAcceptMobileNumbersAndRejectLandlines() {
        assertEquals("13990002001", accountPhoneInput("13990002001ewt22"))
        assertTrue(validAccountPhone("13990002001"))
        assertFalse(validAccountPhone("010-12345678"))
        assertFalse(validAccountPhone("01012345678"))
    }

    @Test fun contactFieldKeepsNamesAndHidesOldDemoPlaceholders() {
        assertEquals("李明 Alice", emergencyContactInput("李明8  Alice#"))
        assertEquals("", profileContactName("演示联系人8").orEmpty())
        assertEquals("李明", profileContactName("李明"))
        assertEquals("示例市协作路8号", profileAddress("演示资料（虚构） · 示例市协作路8号"))
        assertEquals("协作路8号", profileAddress("协作路8号"))
    }

    @Test fun directoryDistinguishesHiddenManagersFromMissingManagers() {
        val colleague=DirectoryContact(8,"staff5","同事","STAFF",null,null,null,null,null,4,"综合管理部")
        assertEquals("已设置，当前不可查看", directoryManagerLabel(colleague, 7))
        assertEquals("已设置，当前不可查看", directoryManagerLabel(colleague.copy(hasManager=true), 7))
        assertEquals("未设置", directoryManagerLabel(colleague.copy(hasManager=false), 7))
        assertEquals("张予安", directoryManagerLabel(colleague.copy(managerId=5,managerName="张予安"), 7))
        assertEquals("本人", directoryManagerLabel(colleague.copy(managerId=7,managerName="李思"), 7))
    }
}
