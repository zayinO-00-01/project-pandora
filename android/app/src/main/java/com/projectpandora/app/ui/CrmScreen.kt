package com.projectpandora.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.projectpandora.app.AppState
import com.projectpandora.app.PandoraViewModel
import com.projectpandora.app.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal val crmServiceKinds = linkedMapOf("VISIT" to "企业参访", "BANQUET" to "私宴", "SALON" to "沙龙", "SHOW" to "演出观演", "OTHER" to "其他服务")
private val serviceStatuses = linkedMapOf("" to "全部", "PENDING" to "待服务", "COMPLETED" to "已完成", "CANCELLED" to "已取消")
private val paymentStatuses = mapOf("NOT_APPLICABLE" to "不涉及收款", "UNPAID" to "未支付", "PARTIAL" to "部分支付",
    "PAID" to "已支付", "PARTIAL_REFUND" to "部分退款", "REFUNDED" to "已退款")
private val products = mapOf("MEMBERSHIP" to "商城会员", "ANNUAL_CARD" to "演出年卡", "GENERAL" to "商城订单")
internal fun crmTime(value: String?) = value?.replace('T', ' ')?.take(16).orEmpty()

private enum class CrmAction { CUSTOMER, SERVICE, COMPLETE, CANCEL, TRANSFER, NOTE, LINK, CORRECT, USE, REVERSE }

@Composable
internal fun CompanyBusinessScreen(
    state: AppState, model: PandoraViewModel,
    initialServiceId: Long? = null, onExit: (() -> Unit)? = null,
) {
    val session = state.session ?: return
    if (state.developmentPreview) {
        Box(Modifier.padding(horizontal = 16.dp)) {
            EmptyCard("公司客户与业务", "连接服务器并使用公司账号登录后，即可管理客户和服务。")
        }
        return
    }
    val api = remember(session.server) { ApiClient(session.server) }
    val scope = rememberCoroutineScope()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var routes by rememberSaveable(initialServiceId) {
        mutableStateOf(arrayListOf(initialServiceId?.let { "service/$it" } ?: "list"))
    }
    var search by rememberSaveable { mutableStateOf("") }
    var status by rememberSaveable { mutableStateOf("") }
    var employeeId by rememberSaveable { mutableStateOf<Long?>(null) }
    val route = routes.last()
    var revision by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var actionError by remember { mutableStateOf<String?>(null) }
    var customers by remember { mutableStateOf<List<CrmCustomer>>(emptyList()) }
    var services by remember { mutableStateOf<List<CrmService>>(emptyList()) }
    var members by remember { mutableStateOf<List<CrmMember>>(emptyList()) }
    var customer by remember(route) { mutableStateOf<CrmCustomerDetail?>(null) }
    var service by remember(route) { mutableStateOf<CrmServiceDetail?>(null) }
    var orders by remember(route) { mutableStateOf<List<CrmOrder>>(emptyList()) }
    var action by remember(route) { mutableStateOf<CrmAction?>(null) }
    var orderTarget by remember(route) { mutableStateOf<CrmOrder?>(null) }
    var useTarget by remember(route) { mutableStateOf<CrmCardUse?>(null) }

    fun open(kind: String, id: Long) {
        val next = "$kind/$id"
        val index = routes.indexOf(next)
        routes = if (index >= 0) ArrayList(routes.take(index + 1)) else ArrayList(routes + next)
    }
    fun back() {
        if (busy) return
        if (routes.size > 1) routes = ArrayList(routes.dropLast(1)) else onExit?.invoke()
    }
    fun form(next: CrmAction) { actionError = null; action = next }
    fun <T> save(job: () -> T, after: (T) -> Unit = {}) {
        if (busy) return
        busy = true
        actionError = null
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) { job() }
                action = null
                after(result)
                revision++
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                actionError = e.message ?: "操作失败，请重试"
            } finally { busy = false }
        }
    }

    BackHandler(enabled = routes.size > 1 || onExit != null) { back() }
    LaunchedEffect(session, route, revision) {
        loading = true
        error = null
        try {
            when {
                route.startsWith("customer/") -> {
                    val id = route.substringAfter('/').toLong()
                    val result = withContext(Dispatchers.IO) {
                        Triple(api.crmCustomer(session, id), api.crmOrders(session, id), api.crmMembers(session))
                    }
                    customer = result.first
                    orders = result.second
                    members = result.third
                }
                route.startsWith("service/") -> {
                    val id = route.substringAfter('/').toLong()
                    val result = withContext(Dispatchers.IO) {
                        Triple(api.crmService(session, id), api.crmCustomers(session), api.crmMembers(session))
                    }
                    service = result.first
                    customers = result.second
                    members = result.third
                }
                else -> {
                    val result = withContext(Dispatchers.IO) {
                        Triple(api.crmCustomers(session), api.crmServices(session), api.crmMembers(session))
                    }
                    customers = result.first
                    services = result.second
                    members = result.third
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (e is ApiException && e.status in listOf(403, 404)) {
                customer = null
                service = null
                error = if (route.startsWith("service/")) "服务不存在或你已无权查看。" else e.message
            } else {
                error = e.message ?: "暂时无法读取客户与业务，请重试"
            }
        } finally { loading = false }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        if (route == "list") {
            TabRow(selectedTabIndex = tab, containerColor = Color.Transparent) {
                listOf("客户", "业务记录").forEachIndexed { index, label ->
                    Tab(selected = tab == index, onClick = { tab = index }, text = { Text(label) })
                }
            }
        } else BackdropTextButton(onClick = ::back, enabled = !busy) { Text("‹ 返回") }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let { Hint(it, true, onRetry = { revision++ }) }
        if (error != null && customer == null && service == null) return@Column

        when {
            route == "list" && tab == 0 -> {
                val term = search.trim()
                val visible = customers.filter {
                    term.isEmpty() || listOf(it.name, it.contactName, it.phone).any { text -> text.contains(term, ignoreCase = true) }
                }
                LazyColumn(contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        OutlinedTextField(search, { search = it.take(120) }, label = { Text("搜索客户") },
                            placeholder = { Text("名称、联系人或可见电话") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ControlButton(onClick = { form(CrmAction.CUSTOMER) }, enabled = !loading) { Text("新建客户") }
                            ControlTextButton(onClick = { revision++ }, enabled = !loading) { Text("刷新") }
                        }
                    }
                    if (visible.isEmpty() && !loading) item {
                        EmptyCard(if (term.isEmpty()) "暂无客户" else "未找到符合条件的客户", "可新建个人或企业客户。")
                    }
                    items(visible, key = { it.id }) { row ->
                        CrmListItem(row.name, listOf(customerKind(row.kind), row.contactName, row.phone).filter(String::isNotBlank).joinToString(" · ")) {
                            open("customer", row.id)
                        }
                    }
                }
            }
            route == "list" -> {
                val visible = services.filter { row ->
                    (status.isEmpty() || row.status == status) &&
                        (employeeId == null || row.owner.id == employeeId || row.participants.any { it.id == employeeId })
                }
                LazyColumn(contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ControlButton(onClick = { form(CrmAction.SERVICE) }, enabled = !loading && customers.isNotEmpty()) { Text("登记服务") }
                            ControlTextButton(onClick = { revision++ }, enabled = !loading) { Text("刷新") }
                        }
                    }
                    item {
                        CrmPicker("服务状态", serviceStatuses.getValue(status), serviceStatuses.entries.map { it.key to it.value }) { status = it }
                    }
                    item {
                        CrmPicker("涉及成员", members.firstOrNull { it.id == employeeId }?.name ?: "所有成员",
                            listOf(null to "所有成员") + members.map { it.id to it.name }) { employeeId = it }
                    }
                    if (visible.isEmpty() && !loading) item {
                        EmptyCard("暂无服务记录", if (customers.isEmpty()) "先到「客户」新建客户，再登记服务。" else "可登记参访、私宴、沙龙等服务。")
                    }
                    items(visible, key = { it.id }) { row -> ServiceListItem(row) { open("service", row.id) } }
                }
            }
            customer != null -> customer?.let { detail ->
                val row = detail.customer
                LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        CrmHeading(row.name, customerKind(row.kind))
                        if (row.contactName.isNotBlank()) Text("联系人：${row.contactName}")
                        if (row.phone.isNotBlank()) Text("电话：${row.phone}")
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ControlButton(onClick = { form(CrmAction.SERVICE) }, enabled = !loading) { Text("登记服务") }
                            if (row.editable) ControlTextButton(onClick = { form(CrmAction.CUSTOMER) }, enabled = !loading) { Text("编辑档案") }
                        }
                    }
                    item { CrmHeading("订单与权益") }
                    if (orders.isEmpty()) item { Text("暂无可查看的订单") }
                    items(orders, key = { "order/${it.id}" }) { order ->
                        OrderCard(order, detail.services, session.userId, onUse = {
                            orderTarget = order; form(CrmAction.USE)
                        }, onReverse = { use ->
                            orderTarget = order; useTarget = use; form(CrmAction.REVERSE)
                        }, onService = { open("service", it) })
                    }
                    item { CrmHeading("服务记录") }
                    if (detail.services.isEmpty()) item { Text("暂无可查看的服务") }
                    items(detail.services, key = { "customerService/${it.id}" }) { item -> ServiceListItem(item) { open("service", item.id) } }
                }
            }
            service != null -> service?.let { detail ->
                val row = detail.service
                LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        CrmHeading(crmServiceKinds[row.kind] ?: row.kind, "${serviceStatuses[row.status]} · ${paymentStatuses[row.paymentStatus]}")
                        BackdropTextButton(onClick = { open("customer", row.customerId) }) { Text(row.customerName + " ›") }
                        Text("负责人：${row.owner.name}")
                        Text("参与人：" + row.participants.joinToString("、") { it.name }.ifEmpty { "无" })
                        Text("计划：${crmTime(row.scheduledAt)}")
                        row.actualAt?.let { Text("实际：${crmTime(it)}") }
                        Text(row.description, modifier = Modifier.padding(top = 8.dp))
                    }
                    if (row.editable && row.status == "PENDING") item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ControlButton(onClick = { form(CrmAction.COMPLETE) }) { Text("完成服务") }
                            ControlTextButton(onClick = { form(CrmAction.CANCEL) }) { Text("取消服务") }
                        }
                        ControlTextButton(onClick = { form(CrmAction.SERVICE) }) { Text("修改安排") }
                    }
                    if (row.canTransfer) item { ControlTextButton(onClick = { form(CrmAction.TRANSFER) }) { Text("转交负责人") } }
                    if (row.canCorrect) item { ControlTextButton(onClick = { form(CrmAction.CORRECT) }) { Text("更正完成记录") } }
                    item {
                        CrmHeading("跟进任务")
                        if (row.editable) ControlTextButton(onClick = { form(CrmAction.LINK) }) { Text("关联已有任务") }
                        if (detail.tasks.isEmpty()) Text("暂无关联任务")
                    }
                    items(detail.tasks, key = { "task/${it.id}" }) { task ->
                        CrmListItem(task.title, "打开任务") { model.openTask(task.id) }
                    }
                    item {
                        CrmHeading("记录与跟进")
                        if (row.editable) ControlTextButton(onClick = { form(CrmAction.NOTE) }) { Text("写跟进备注") }
                    }
                    items(detail.history, key = { "event/${it.id}" }) { event ->
                        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
                            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                Text("${event.actor} · ${crmTime(event.recordedAt)}", style = MaterialTheme.typography.labelMedium)
                                Text(event.detail)
                            }
                        }
                    }
                }
            }
        }
    }
    val dismiss = { if (!busy) action = null }
    val row = service?.service
    when (action) {
        CrmAction.CUSTOMER -> CrmCustomerForm(customer?.customer, busy, actionError, dismiss) { input ->
            save({ api.saveCrmCustomer(session, customer?.customer?.id, input) }) { open("customer", it.id) }
        }
        CrmAction.SERVICE -> CrmServiceForm(row, customer?.customer, customers, members, busy, actionError, dismiss) { input ->
            save({ api.saveCrmService(session, row?.id, input) }) { open("service", it.service.id) }
        }
        CrmAction.COMPLETE -> if (row != null) CrmCompleteForm(busy, actionError, dismiss) { date ->
            save({ api.crmServiceStatus(session, row.id, "COMPLETED", date) })
        }
        CrmAction.CANCEL -> if (row != null) CrmConfirm("取消服务", "取消后会保留记录，已收款项由财务另行处理。", busy, actionError, dismiss) {
            save({ api.crmServiceStatus(session, row.id, "CANCELLED", null) })
        }
        CrmAction.TRANSFER -> if (row != null) CrmChoiceForm("转交负责人", "新负责人", members.filter { it.id != row.owner.id }.map { it.id to it.name },
            "转交原因", busy, actionError, dismiss) { owner, reason ->
            save({ api.crmTransfer(session, row.id, owner, reason) }) { routes = ArrayList(routes.dropLast(1)) }
        }
        CrmAction.NOTE -> if (row != null) CrmTextForm("跟进备注", "内容", 2000, busy, actionError, dismiss) {
            save({ api.crmFollowUp(session, row.id, it) })
        }
        CrmAction.LINK -> if (row != null) CrmChoiceForm("关联已有任务", "任务",
            state.tasks.filter { taskIsApproved(it) && !it.deleted && service?.tasks?.none { link -> link.id == it.id } == true }.map { it.id to it.title },
            null, busy, actionError, dismiss) { id, _ ->
            save({ api.crmLinkTask(session, row.id, id) })
        }
        CrmAction.CORRECT -> if (row != null) CrmCorrectionForm(row, customers, members, busy, actionError, dismiss) { cid, owner, date, description, reason ->
            save({ api.crmCorrect(session, row.id, cid, owner, date, description, reason) })
        }
        CrmAction.USE -> orderTarget?.let { order ->
            val choices = customer?.services.orEmpty().filter {
                it.kind == "SHOW" && it.status == "COMPLETED" && it.owner.id == session.userId &&
                    orders.none { entry -> entry.uses.any { use -> use.serviceId == it.id } }
            }
            CrmChoiceForm("确认到场并核销", "已完成的观演", choices.map { it.id to "${crmTime(it.actualAt)} · ${it.description.take(28)}" },
                null, busy, actionError, dismiss) { id, _ -> save({ api.crmUseCard(session, order.id, id) }) }
        }
        CrmAction.REVERSE -> orderTarget?.let { order -> useTarget?.let { use ->
            CrmTextForm("撤销该次核销", "撤销原因", 500, busy, actionError, dismiss) {
                save({ api.crmReverseCard(session, order.id, use.id, it) })
            }
        } }
        null -> Unit
    }
}

private fun customerKind(kind: String) = if (kind == "COMPANY") "企业客户" else "个人客户"

@Composable
private fun CrmHeading(title: String, detail: String? = null) {
    Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
    detail?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
}

@Composable
private fun CrmListItem(title: String, detail: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ServiceListItem(service: CrmService, open: () -> Unit) {
    CrmListItem("${crmServiceKinds[service.kind]} · ${service.customerName}",
        "${serviceStatuses[service.status]} · ${crmTime(service.actualAt ?: service.scheduledAt)} · ${service.owner.name}", open)
}

@Composable
private fun OrderCard(order: CrmOrder, services: List<CrmService>, userId: Long,
                      onUse: () -> Unit, onReverse: (CrmCardUse) -> Unit, onService: (Long) -> Unit) {
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text("${products[order.product]} · ${paymentStatuses[order.status]}", style = MaterialTheme.typography.titleSmall)
            Text("${order.source} · ${order.number}")
            order.expiresOn?.let { Text("有效期：${order.startsOn} 至 $it（不含当日）") }
            order.remaining?.let { Text("剩余 $it / 6 次") }
            val visit = services.any {
                it.owner.id == userId && it.kind == "SHOW" && it.status == "COMPLETED" && order.uses.none { use -> use.serviceId == it.id }
            }
            if (order.usable && visit) ControlTextButton(onClick = onUse) { Text("登记到场") }
            order.uses.forEach { use ->
                HorizontalDivider()
                Text(if (use.reversedAt == null) "已核销 · ${crmTime(use.usedAt)}" else "已撤销 · ${use.reverseReason}")
                ControlTextButton(onClick = { onService(use.serviceId) }) { Text("查看观演记录") }
                if (order.mayReverse && use.reversedAt == null) ControlTextButton(onClick = { onReverse(use) }) { Text("撤销核销") }
            }
        }
    }
}
