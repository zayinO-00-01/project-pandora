package com.projectpandora.app.data

import com.google.gson.Gson
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
    fun error(date: String, content: String, status: String): String? {
        val parsed=try {LocalDate.parse(date)} catch(_:Exception) {return "请选择有效的日志日期"}
        if(parsed>today())return "日志日期不能晚于今天"
        if(content.isBlank())return "工作记录不能为空"
        if(content.length>5000)return "工作记录不能超过5000字"
        if(status !in listOf("draft","submitted"))return "无效的日志状态"
        return null
    }
}
object TaskValidation {
    fun error(progress: String, note: String): String? {
        if(!progress.matches(Regex("[0-9]+")) || progress.toIntOrNull() !in 0..100)return "进度请输入 0–100 的整数"
        if(note.isBlank())return "请填写进度说明"
        if(note.length>2000)return "进度说明不能超过2000字"
        return null
    }
}
/** Synchronous transport; callers use Dispatchers.IO. Redirects never forward a token. */
class ApiClient(address: String) {
    val server=ServerAddress.normalize(address)
    private val json=Gson()
    fun login(username: String, password: String): Session {
        val result: LoginResponse=request("/auth/login","POST",mapOf("username" to username,"password" to password),null,LoginResponse::class.java)
        if(result.token.isNullOrBlank() || result.userId<=0 || result.role !in listOf("ADMIN","LEADER","STAFF"))throw ApiException(502,"登录响应不完整，请检查服务地址")
        return Session(server,result.token,result.userId,result.role,result.displayName,username)
    }
    fun users(session: Session): List<UserSummary> = request("/users","GET",null,session,object:TypeToken<List<UserSummary>>(){}.type)
    fun logs(session: Session): List<WorkLog> = request("/logs","GET",null,session,object:TypeToken<List<WorkLog>>(){}.type)
    fun tasks(session: Session): List<DispatchTask> = request("/tasks","GET",null,session,object:TypeToken<List<DispatchTask>>(){}.type)
    fun saveProgress(session: Session, id: Long, input: TaskProgressInput): DispatchTask {
        require(id>0) {"无效的任务编号"}
        TaskValidation.error(input.progress.toString(),input.progressNote)?.let {throw IllegalArgumentException(it)}
        val result: DispatchTask = request("/tasks/$id/progress","PUT",input,session,DispatchTask::class.java)
        if(result.id!=id)throw ApiException(502,"服务器返回的任务编号不一致，请刷新后重试")
        return result
    }
    fun panels(session: Session): PanelMap = request("/panels/map","GET",null,session,PanelMap::class.java)
    fun save(session: Session, id: Long?, input: LogInput): WorkLog {
        LogValidation.error(input.logDate,input.content,input.status)?.let {throw IllegalArgumentException(it)}
        if(id!=null)require(id>0) {"无效的日志编号"}
        return request(if(id==null)"/logs" else "/logs/$id",if(id==null)"POST" else "PUT",input,session,WorkLog::class.java)
    }
    fun submit(session: Session, id: Long): WorkLog {
        require(id>0) {"无效的日志编号"}
        return request("/logs/$id/submit","POST",null,session,WorkLog::class.java)
    }
    private fun <T> request(path: String, method: String, body: Any?, session: Session?, type: Type): T {
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
                    404->"未找到接口或记录，请检查服务地址"
                    in 300..399->"服务发生跳转，请直接填写正确服务器地址"
                    else->"服务器请求失败（$status），请稍后重试"
                })
            }
            try {return json.fromJson<T>(response,type) ?: throw JsonParseException("empty")}
            catch(_:JsonParseException) {throw ApiException(502,"服务器响应格式不正确，请确认连接的是潘多拉服务")}
        } catch(e:SocketTimeoutException) {throw ApiException(0,"连接超时，请确认服务器已启动，手机与电脑在同一网络")}
          catch(e:IOException) {throw ApiException(0,"连接失败，请检查服务器地址、同一 Wi-Fi 和电脑防火墙")}
          finally {connection.disconnect()}
    }
}
