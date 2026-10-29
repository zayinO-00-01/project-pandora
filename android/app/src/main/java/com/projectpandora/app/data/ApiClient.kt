package com.projectpandora.app.data

import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import java.io.IOException
import java.lang.reflect.Type
import java.net.HttpURLConnection
import java.net.URI
import java.net.SocketTimeoutException
import java.time.LocalDate

class ApiException(val status: Int, message: String): Exception(message)
object ServerAddress {
    fun normalize(raw: String): String {
        val uri=try {URI(raw.trim())} catch(_:Exception) {throw IllegalArgumentException("请输入完整的 http:// 或 https:// 服务器地址")}
        require(uri.scheme in listOf("http","https") && !uri.host.isNullOrBlank()) {"请输入完整的 http:// 或 https:// 服务器地址"}
        require(uri.rawUserInfo==null && uri.rawQuery==null && uri.rawFragment==null) {"服务器地址不要包含用户名、密码、查询参数或 #"}
        require(uri.port==-1 || uri.port in 1..65535) {"端口必须为 1–65535"}
        require(uri.path.trimEnd('/') in listOf("","/api/v1")) {"请填写服务器根地址，或以 /api/v1 结尾"}
        return "${uri.scheme.lowercase()}://${uri.rawAuthority.lowercase()}/api/v1"
    }
}
object LogValidation {
    fun error(date: String, content: String, status: String, endDate: String? = null,creating: Boolean = true): String? {
        val parsed=try {LocalDate.parse(date)} catch(_:Exception) {return "请选择有效的日志日期"}
        if(creating && parsed>today())return "日志日期不能晚于今天"
        if(creating && !canWriteLog(date))return "可创建今天及往前 3 天的日报"
        if(creating && !canWriteLogRange(date,endDate))return "起止日期须连续且在今天及往前 3 天内"
        if(content.isBlank())return "工作记录不能为空"
        if(content.length>5000)return "工作记录不能超过5000字"
        if(status !in listOf("draft","submitted"))return "无效的日志状态"
        return null
    }
}
object TaskValidation {
    fun error(progress: String, note: String, status: String? = null): String? {
        if(status!=null && status !in setOf("todo","doing","done"))return "请选择工作状态"
        if(progress.isBlank()) {
            if(status==null)return "请选择工作状态"
        } else {
            if(!progress.matches(Regex("[0-9]+")) || progress.toIntOrNull() !in 0..100)return "百分比请填 0–100 的整数，或留空"
            if(status!=null && !percentageFits(status,progress.toInt()))return "百分比与工作状态不一致，可以清空百分比后提交"
        }
        if(note.isBlank())return "请填写进度说明"
        if(note.length>2000)return "进度说明不能超过2000字"
        return null
    }
    fun percentageFits(status: String, value: Int)=when(status) {"todo"->value==0;"doing"->value in 0..99;"done"->value==100;else->false}
}
/** Synchronous transport; callers use Dispatchers.IO. Redirects never forward a token. */
class ApiClient(address: String) {
    val server=ServerAddress.normalize(address)
    private val json=GsonBuilder().serializeNulls().create()
    fun login(username: String, password: String): Session {
        val result: LoginResponse=request("/auth/login","POST",mapOf("username" to username,"password" to password),null,LoginResponse::class.java)
        if(result.role=="ADMIN")throw ApiException(403,"系统管理员请使用 Web 管理端，App 仅供业务账号使用")
        if(result.token.isNullOrBlank() || result.userId<=0 || result.role !in AppRoles.business)throw ApiException(502,"登录响应不完整，请检查服务地址")
        return Session(server,result.token,result.userId,result.role,result.displayName,username)
    }
    fun labels(session: Session): List<WorkLabel> = request("/labels","GET",null,session,object:TypeToken<List<WorkLabel>>(){}.type)
    fun reviews(session: Session): List<TaskReview> = request("/task-reviews","GET",null,session,object:TypeToken<List<TaskReview>>(){}.type)
    fun decideTask(session: Session,id: Long,status: String): TaskReview = request("/task-reviews/$id","PUT",mapOf("status" to status),session,TaskReview::class.java)
    fun editTask(session: Session,id: Long,input: TaskCreateInput): DispatchTask = request("/tasks/$id","PUT",input,session,DispatchTask::class.java)
    fun saveKeyword(session: Session,taskId: Long,id: Long?,text: String): ActionResult = request(if(id==null)"/tasks/$taskId/keywords" else "/tasks/$taskId/keywords/$id",if(id==null)"POST" else "PUT",mapOf("text" to text),session,ActionResult::class.java)
    fun deleteKeyword(session: Session,taskId: Long,id: Long): ActionResult = request("/tasks/$taskId/keywords/$id","DELETE",null,session,ActionResult::class.java)
    fun saveLabel(session: Session,id: Long?,name: String,color: String): WorkLabel = request(if(id==null)"/labels" else "/labels/$id",if(id==null)"POST" else "PUT",mapOf("name" to name,"color" to color),session,WorkLabel::class.java)
    fun archiveLabel(session: Session,id: Long): ActionResult = request("/labels/$id","DELETE",null,session,ActionResult::class.java)
    fun accountProfile(session: Session): MyProfile = request("/profile","GET",null,session,MyProfile::class.java)
    fun avatar(session: Session,data: String): MyProfile = request("/profile/avatar","PUT",mapOf("avatarData" to data),session,MyProfile::class.java)
    fun password(session: Session,current: String,new: String): ActionResult = request("/profile/password","PUT",mapOf("currentPassword" to current,"newPassword" to new),session,ActionResult::class.java)
    fun users(session: Session): List<UserSummary> = request("/users","GET",null,session,object:TypeToken<List<UserSummary>>(){}.type)
    fun profile(session: Session): Profile = request("/users/me","GET",null,session,Profile::class.java)
    fun changePassword(session: Session, currentPassword: String, newPassword: String) {
        require(newPassword.length in 6..72) {"新密码需为 6-72 位"}
        val ignored: Map<String,Any?> = request("/users/me/password","PUT",mapOf("currentPassword" to currentPassword,"newPassword" to newPassword),session,object:TypeToken<Map<String,Any?>>(){}.type)
        ignored.size
    }
    fun updateAvatar(session: Session, avatar: String): Profile = request("/users/me/avatar","PUT",mapOf("avatar" to avatar),session,Profile::class.java)
    fun organization(session: Session): Organization = request("/users/organization","GET",null,session,Organization::class.java)
    fun viewRequests(session: Session): List<ViewRequest> = request("/view-requests","GET",null,session,object:TypeToken<List<ViewRequest>>(){}.type)
    fun requestView(session: Session,targetId: Long,reason: String): ViewRequest = request("/view-requests","POST",mapOf("targetId" to targetId,"reason" to reason),session,ViewRequest::class.java)
    fun decideView(session: Session,id: Long,status: String): ViewRequest = request("/view-requests/$id","PUT",mapOf("status" to status),session,ViewRequest::class.java)
    fun memberLogs(session: Session,userId: Long): List<WorkLog> = request("/logs?userId=$userId","GET",null,session,object:TypeToken<List<WorkLog>>(){}.type)
    fun memberTasks(session: Session,userId: Long): List<DispatchTask> = request("/tasks?userId=$userId","GET",null,session,object:TypeToken<List<DispatchTask>>(){}.type)
    fun logs(session: Session): List<WorkLog> = request("/logs","GET",null,session,object:TypeToken<List<WorkLog>>(){}.type)
    fun integrity(session: Session): IntegrityResponse = request("/logs/integrity","GET",null,session,IntegrityResponse::class.java)
    fun task(session: Session,id: Long): DispatchTask = request("/tasks/$id","GET",null,session,DispatchTask::class.java)
    fun taskNote(session: Session,id: Long): TaskNote = request("/tasks/$id/note","GET",null,session,TaskNote::class.java)
    fun saveTaskNote(session: Session,id: Long,content: String): TaskNote {
        require(content.length<=2000) {"备注不能超过2000字"}
        return request("/tasks/$id/note","PUT",mapOf("content" to content),session,TaskNote::class.java)
    }
    fun tasks(session: Session): List<DispatchTask> = request("/tasks","GET",null,session,object:TypeToken<List<DispatchTask>>(){}.type)
    fun createTask(session: Session,input: TaskCreateInput): DispatchTask {
        if(!AppRoles.canCreateTask(session.role))throw ApiException(403,"仅业务成员可创建任务")
        TaskDispatchValidation.inputError(input,session.userId)?.let {throw IllegalArgumentException(it)}
        val result: DispatchTask=request("/tasks","POST",input,session,DispatchTask::class.java)
        if(result.id<=0 || result.assigneeId!=input.assigneeId || result.createdBy!=session.userId)throw ApiException(502,"服务器返回的任务信息不一致，请刷新确认后重试")
        return result
    }
    fun withdrawTask(session: Session,id: Long): DispatchTask = request("/tasks/$id/withdraw","POST",null,session,DispatchTask::class.java)
    fun pendingTaskApprovals(session: Session): List<DispatchTask> = request("/tasks/pending-approvals","GET",null,session,object:TypeToken<List<DispatchTask>>(){}.type)
    fun decideTaskApproval(session: Session,id: Long,decision: String,note: String): DispatchTask {
        if(!AppRoles.canViewDown(session.role))throw ApiException(403,"仅直属上级可以审核任务")
        require(id>0 && decision in setOf("approved","rejected") && note.length<=500) {"请填写有效的审核决定和意见"}
        val result: DispatchTask=request("/tasks/$id/approval","PUT",mapOf("decision" to decision,"note" to note.trim()),session,DispatchTask::class.java)
        if(result.id!=id || result.approvalStatus!=decision)throw ApiException(502,"审核响应不一致，请刷新确认")
        return result
    }
    fun saveProgress(session: Session, id: Long, input: TaskProgressInput): DispatchTask {
        require(id>0) {"无效的任务编号"}
        TaskValidation.error(input.progress?.toString().orEmpty(),input.progressNote,input.status)?.let {throw IllegalArgumentException(it)}
        val feedback=input.copy(status=input.status ?: legacyTaskStatus(input.progress))
        val result: DispatchTask = request("/tasks/$id/progress","PUT",feedback,session,DispatchTask::class.java)
        if(result.id!=id)throw ApiException(502,"服务器返回的任务编号不一致，请刷新后重试")
        return result
    }
    fun companyNotices(session: Session): List<CompanyNotice> = request("/company-notices","GET",null,session,object:TypeToken<List<CompanyNotice>>(){}.type)
    fun saveCompanyNote(session: Session,id: Long,content: String): CompanyNotice {
        require(id>0 && content.length<=2000)
        return request("/company-notices/$id/note","PUT",mapOf("content" to content),session,CompanyNotice::class.java)
    }
    fun personalPriorities(session: Session): PersonalPrioritySnapshot = request("/priorities/personal","GET",null,session,PersonalPrioritySnapshot::class.java)
    fun savePersonalPriorities(session: Session,input: PersonalPrioritiesInput): PersonalPrioritySnapshot {
        require(input.items.size<=10 && input.items.all {it.title.isNotBlank() && it.title.length<=120 && it.detail.length<=2000})
        val result: PersonalPrioritySnapshot=request("/priorities/personal","PUT",input,session,PersonalPrioritySnapshot::class.java)
        val valid=runCatching {
            result.revision>input.revision && result.items.size==input.items.size && result.items.map {it.id}.distinct().size==result.items.size &&
                result.items.sortedBy {it.sortOrder}.zip(input.items).all {(saved,sent)->saved.id>0 &&
                    (sent.id==null || sent.id<0 || saved.id==sent.id) && saved.title==sent.title.trim() && saved.detail.orEmpty()==sent.detail}
        }.getOrDefault(false)
        if(!valid)throw ApiException(502,"保存结果暂未确认，修改已保留，请刷新后检查")
        return result
    }
    fun panels(session: Session): PanelMap = request("/panels/map","GET",null,session,PanelMap::class.java)
    fun taskReviews(session: Session): List<DispatchTask> = request("/tasks/reviews","GET",null,session,object:TypeToken<List<DispatchTask>>(){}.type)
    fun approveTask(session: Session, id: Long, note: String): DispatchTask {
        require(id>0) {"无效的任务编号"}
        return request("/tasks/$id/approve","POST",mapOf("note" to note),session,DispatchTask::class.java)
    }
    fun rejectTask(session: Session, id: Long, note: String): DispatchTask {
        require(id>0) {"无效的任务编号"}
        if(note.isBlank())throw IllegalArgumentException("请填写驳回理由")
        return request("/tasks/$id/reject","POST",mapOf("note" to note),session,DispatchTask::class.java)
    }
    fun changeTask(session: Session, id: Long, input: TaskCreateInput): DispatchTask {
        require(id>0) {"无效的任务编号"}
        TaskDispatchValidation.inputError(input,session.userId)?.let {throw IllegalArgumentException(it)}
        return request("/tasks/$id","PUT",input,session,DispatchTask::class.java)
    }
    fun deleteTask(session: Session, id: Long) {
        require(id>0) {"无效的任务编号"}
        requestVoid("/tasks/$id","DELETE",session)
    }
    fun notifications(session: Session): NotificationList = request("/notifications","GET",null,session,NotificationList::class.java)
    fun readNotification(session: Session, id: Long) {
        require(id>0) {"无效的消息编号"}
        requestVoid("/notifications/$id/read","POST",session)
    }
    fun readAllNotifications(session: Session) { requestVoid("/notifications/read-all","POST",session) }
    fun save(session: Session, id: Long?, input: LogInput): WorkLog {
        LogValidation.error(input.logDate,input.content,input.status,input.endDate,creating=id==null)?.let {throw IllegalArgumentException(it)}
        if(id!=null) {
            require(id>0) {"无效的日志编号"}
        }
        if(input.endDate!=null && input.endDate!=input.logDate && !integrity(session).supportsDateRanges)throw ApiException(409,"当前服务器尚未支持多日日志，请先更新后端；内容已保留")
        return request(if(id==null)"/logs" else "/logs/$id",if(id==null)"POST" else "PUT",input,session,WorkLog::class.java)
    }
    fun submit(session: Session, id: Long): WorkLog {
        require(id>0) {"无效的日志编号"}
        return request("/logs/$id/submit","POST",null,session,WorkLog::class.java)
    }
    private fun requestVoid(path: String, method: String, session: Session) {
        if(session.role !in AppRoles.business)throw ApiException(403,"此账号不能使用 App 业务接口")
        require(session.server==server) {"服务器已改变，请重新登录"}
        val connection=URI(server+path).toURL().openConnection() as HttpURLConnection
        try {
            connection.connectTimeout=8000;connection.readTimeout=8000
            connection.instanceFollowRedirects=false
            connection.requestMethod=method
            connection.setRequestProperty("Accept","application/json")
            connection.setRequestProperty("Authorization","Bearer ${session.token}")
            val status=connection.responseCode
            if(status !in 200..299) {
                val response=connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use {it.readText()} ?: ""
                val serverMessage=runCatching {JsonParser.parseString(response).asJsonObject.get("message")?.asString}.getOrNull()
                throw ApiException(status,serverMessage ?: when(status) {
                    403->"无权操作这条记录"
                    404->"暂时找不到这项内容，请刷新"
                    else->"暂时无法完成操作，请稍后重试"
                })
            }
        } catch(e:SocketTimeoutException) {throw ApiException(0,"连接超时，请确认服务器已启动，手机与电脑在同一网络")}
          catch(e:IOException) {throw ApiException(0,"连接失败，请检查服务器地址、同一 Wi-Fi 和电脑防火墙")}
          finally {connection.disconnect()}
    }
    private fun <T> request(path: String, method: String, body: Any?, session: Session?, type: Type): T {
        if(session!=null && session.role !in AppRoles.business)throw ApiException(403,"此账号不能使用 App 业务接口")
        require(session==null || session.server==server) {"服务器已改变，请重新登录"}
        val connection=URI(server+path).toURL().openConnection() as HttpURLConnection
        try {
            connection.connectTimeout=8000;connection.readTimeout=8000
            connection.instanceFollowRedirects=false
            connection.requestMethod=method
            connection.setRequestProperty("Accept","application/json")
            session?.let {connection.setRequestProperty("Authorization","Bearer ${it.token}")}
            if(body!=null) {
                connection.doOutput=true
                connection.setRequestProperty("Content-Type","application/json; charset=utf-8")
                connection.outputStream.use {it.write(json.toJson(body).toByteArray(Charsets.UTF_8))}
            }
            val status=connection.responseCode
            val stream=if(status in 200..299)connection.inputStream else connection.errorStream
            val response=stream?.bufferedReader(Charsets.UTF_8)?.use {it.readText()} ?: ""
            if(status !in 200..299) {
                val serverMessage=runCatching {JsonParser.parseString(response).asJsonObject.get("message")?.asString}.getOrNull()
                throw ApiException(status,serverMessage ?: when(status) {
                    401->"账号密码错误或登录已过期，请重新登录"
                    403->"无权操作这条记录"
                    404->"暂时找不到这项内容，请刷新；仍有问题请联系管理员"
                    in 300..399->"服务发生跳转，请直接填写正确服务器地址"
                    else->"暂时无法完成操作，请稍后重试"
                })
            }
            try {return json.fromJson<T>(response,type) ?: throw JsonParseException("empty")}
            catch(_:JsonParseException) {throw ApiException(502,"暂时无法读取工作内容，请检查连接设置或联系管理员")}
        } catch(e:SocketTimeoutException) {throw ApiException(0,"连接超时，请确认服务器已启动，手机与电脑在同一网络")}
          catch(e:IOException) {throw ApiException(0,"连接失败，请检查服务器地址、同一 Wi-Fi 和电脑防火墙")}
          finally {connection.disconnect()}
    }
}
