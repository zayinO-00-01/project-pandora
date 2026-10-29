package com.projectpandora.app.data

import com.google.gson.Gson
import com.google.gson.JsonParser
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test

class WorkspaceSyncTest {
    @Test fun companyOriginalContentAndEmptyNoteUseExistingContract() {
        MockWebServer().use {server ->
            server.enqueue(MockResponse().setBody("""[{"id":4,"title":"重点","content":"完整原文","sortOrder":0,"enabled":true,"updatedAt":"2026-10-22T02:30:11Z","myNote":"备注"}]"""))
            server.enqueue(MockResponse().setBody("""{"id":4,"title":"重点","content":"完整原文","sortOrder":0,"enabled":true,"updatedAt":"2026-10-22T02:30:11Z","myNote":""}"""))
            server.start()
            val api=ApiClient(server.url("/").toString());val me=Session(api.server,"token",2,"STAFF",null,"staff")
            val result=api.companyNotices(me)
            assertEquals("完整原文",JsonParser.parseString(Gson().toJson(result)).asJsonArray[0].asJsonObject["content"].asString)
            api.saveCompanyNote(me,4L,"")
            assertEquals("/api/v1/company-notices",server.takeRequest().path)
            val request=server.takeRequest();assertEquals("/api/v1/company-notices/4/note",request.path);assertEquals("PUT",request.method)
            assertEquals("",JsonParser.parseString(request.body.readUtf8()).asJsonObject["content"].asString)
        }
    }
    @Test fun profileReadsFullNullableSelfRecord() {
        MockWebServer().use {server ->
            server.enqueue(MockResponse().setBody("""{"id":2,"username":"staff","role":"STAFF","displayName":null,"phone":"13800000000","idCard":null,"birthDate":null,"gender":null,"hireDate":null,"emergencyContact":null,"emergencyPhone":null,"address":"杭州","managerId":null}"""));server.start()
            val api=ApiClient(server.url("/").toString());val me=Session(api.server,"t",2,"STAFF",null,"staff")
            val result=JsonParser.parseString(Gson().toJson(api.profile(me))).asJsonObject
            assertEquals("13800000000",result["phone"].asString);assertEquals("杭州",result["address"].asString)
            assertEquals("/api/v1/users/me",server.takeRequest().path)
        }
    }
    @Test fun pendingDraftSurvivesRefreshAndConflictBlocksImplicitSave() {
        val draft=PersonalSync(4,emptyList(),listOf(PanelItem(-1,"待保存",0,null,"内容")),5,emptyList())
        assertFalse(draft.canSave())
        val refreshed=draft.receive(PersonalPrioritySnapshot(8,listOf(PanelItem(7,"另一台设备",0,null))))
        assertEquals(4L,refreshed.revision);assertEquals("待保存",refreshed.items.single().title);assertEquals(8L,refreshed.conflictRevision)
        try {refreshed.input();fail("must require explicit overwrite")} catch(_:IllegalArgumentException) {}
        val restored=Gson().fromJson(Gson().toJson(WorkspaceDraft(refreshed,mapOf(4L to ""))),WorkspaceDraft::class.java)
        assertEquals(refreshed,restored.personal);assertTrue(restored.companyNotes.containsKey(4L));assertEquals("",restored.companyNotes[4L])
    }
    @Test fun overwriteUsesFetchedRevisionAndRecreatesDeletedIdsWithoutCollisions() {
        val state=PersonalSync(3,emptyList(),listOf(PanelItem(9,"已删除",0,null),PanelItem(-1,"新事项",1,null),PanelItem(8,"保留",2,null)),5,listOf(PanelItem(8,"保留",0,null)))
        val input=state.input(true)
        assertEquals(5L,input.revision);assertEquals(listOf(-1L,-2L,8L),input.items.map {it.id})
        assertEquals(3L,state.revision);assertEquals(9L,state.items.first().id)
    }
    @Test fun personalSaveSendsBaseRevisionAndRetainsConflictStatus() {
        MockWebServer().use {server ->
            server.enqueue(MockResponse().setBody("""{"revision":4,"items":[]}"""))
            server.enqueue(MockResponse().setResponseCode(409).setBody("""{"message":"版本已更新"}"""));server.start()
            val api=ApiClient(server.url("/").toString());val me=Session(api.server,"t",2,"STAFF",null,"staff")
            assertEquals(4L,api.personalPriorities(me).revision)
            try {api.savePersonalPriorities(me,PersonalPrioritiesInput(4,listOf(PersonalPriorityWrite(-1,"目标","安排",0))));fail("stale revision accepted")}
            catch(e:ApiException) {assertEquals(409,e.status)}
            assertEquals("/api/v1/priorities/personal",server.takeRequest().path)
            val request=server.takeRequest();assertEquals("PUT",request.method)
            val body=JsonParser.parseString(request.body.readUtf8()).asJsonObject
            assertEquals(4L,body["revision"].asLong);assertFalse(body["items"].asJsonArray[0].asJsonObject.has("myRemark"))
        }
    }
    @Test fun staleSuccessResponseMustNotAcknowledgePendingPersonalEdits() {
        MockWebServer().use {server->
            server.enqueue(MockResponse().setBody("""{"revision":4,"items":[]}"""));server.start()
            val api=ApiClient(server.url("/").toString());val me=Session(api.server,"t",2,"STAFF",null,"staff")
            try {api.savePersonalPriorities(me,PersonalPrioritiesInput(4,listOf(PersonalPriorityWrite(-1,"目标","安排",0))));fail("stale snapshot cannot acknowledge edits")}
            catch(e:ApiException) {assertEquals(502,e.status)}
        }
    }
    @Test fun openingEditorPinsBaseRevisionAgainstInFlightRefresh() {
        val clean=PersonalSync(4,listOf(PanelItem(9,"原目标",0,null,"旧内容")))
        val pinned=clean.beginEdit()
        val refreshed=pinned.receive(PersonalPrioritySnapshot(5,emptyList()))
        assertEquals(4L,refreshed.revision);assertEquals(9L,refreshed.items.single().id);assertEquals(5L,refreshed.conflictRevision)
        assertFalse(refreshed.canSave())
    }
    @Test fun logTimesUseShanghaiAndKeepSeconds() {
        assertEquals("2026-10-22 10:30:11",com.projectpandora.app.ui.timestamp("2026-10-22T02:30:11Z"))
    }
}
