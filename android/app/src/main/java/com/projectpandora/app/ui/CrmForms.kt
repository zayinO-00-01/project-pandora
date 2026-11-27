package com.projectpandora.app.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.view.ContextThemeWrapper
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.projectpandora.app.data.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val businessZone = ZoneId.of("Asia/Shanghai")
private val clockFormat = DateTimeFormatter.ofPattern("HH:mm")
private fun currentTime() = LocalDateTime.now(businessZone).withSecond(0).withNano(0)

@Composable
internal fun <T> CrmPicker(label: String, selected: String, options: List<Pair<T, String>>,
                           enabled: Boolean = true, change: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Box(Modifier.fillMaxWidth()) {
            ControlOutlinedButton(onClick = { open = true }, enabled = enabled && options.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                Text("$selected  ▾", modifier = Modifier.weight(1f))
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.heightIn(max = 320.dp)) {
                options.forEach { (key, text) ->
                    DropdownMenuItem(text = { Text(text) }, onClick = { open = false; change(key) })
                }
            }
        }
    }
}

@Composable
private fun CrmDialog(title: String, busy: Boolean, error: String?, valid: Boolean, dismiss: () -> Unit,
                      submit: () -> Unit, fields: @Composable ColumnScope.() -> Unit) {
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(title) },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            fields()
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        } },
        confirmButton = { ControlTextButton(onClick = submit, enabled = !busy && valid) { Text(if (busy) "正在保存…" else "确认") } },
        dismissButton = { ControlTextButton(onClick = dismiss, enabled = !busy) { Text("返回") } },
    )
}

@Composable
private fun CrmField(value: String, label: String, max: Int, enabled: Boolean, long: Boolean = false, change: (String) -> Unit) {
    OutlinedTextField(value, { change(it.take(max)) }, label = { Text(label) }, enabled = enabled,
        modifier = Modifier.fillMaxWidth(), singleLine = !long, minLines = if (long) 3 else 1)
}

@Composable
private fun CrmDateTime(label: String, value: String, enabled: Boolean, change: (String) -> Unit) {
    val context = LocalContext.current
    val dark = LocalAppearance.current.mode == Appearance.DARK
    val date = LocalDateTime.parse(value)
    val theme = ContextThemeWrapper(context, if (dark) android.R.style.Theme_Material_Dialog_Alert else android.R.style.Theme_Material_Light_Dialog_Alert)
    Text("$label（北京时间）", style = MaterialTheme.typography.labelMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        ControlTextButton(enabled = enabled, onClick = {
            DatePickerDialog(theme, { _, y, m, d ->
                change(LocalDate.of(y, m + 1, d).atTime(date.toLocalTime()).toString())
            }, date.year, date.monthValue - 1, date.dayOfMonth).show()
        }) { Text(date.toLocalDate().toString() + " ▾") }
        ControlTextButton(enabled = enabled, onClick = {
            TimePickerDialog(theme, { _, h, m ->
                change(date.toLocalDate().atTime(LocalTime.of(h, m)).toString())
            }, date.hour, date.minute, true).show()
        }) { Text(date.format(clockFormat) + " ▾") }
    }
}

@Composable
internal fun CrmCustomerForm(initial: CrmCustomer?, busy: Boolean, error: String?, dismiss: () -> Unit,
                             submit: (CrmCustomerInput) -> Unit) {
    var kind by rememberSaveable(initial?.id) { mutableStateOf(initial?.kind ?: "PERSON") }
    var name by rememberSaveable(initial?.id) { mutableStateOf(initial?.name.orEmpty()) }
    var contact by rememberSaveable(initial?.id) { mutableStateOf(initial?.contactName.orEmpty()) }
    var phone by rememberSaveable(initial?.id) { mutableStateOf(initial?.phone.orEmpty()) }
    val company = kind == "COMPANY"
    CrmDialog(if (initial == null) "新建客户" else "编辑客户档案", busy, error, name.isNotBlank() && (!company || contact.isNotBlank()),
        dismiss, { submit(CrmCustomerInput(kind, name, if (company) contact else "", phone)) }) {
        CrmPicker("客户类型", if (company) "企业客户" else "个人客户", listOf("PERSON" to "个人客户", "COMPANY" to "企业客户"),
            enabled = !busy) { kind = it }
        CrmField(name, if (company) "企业名称" else "客户姓名", 120, !busy) { name = it }
        if (company) CrmField(contact, "企业对接人", 120, !busy) { contact = it }
        CrmField(phone, "联系电话（选填）", 30, !busy) { phone = it }
    }
}

@Composable
internal fun CrmServiceForm(initial: CrmService?, customer: CrmCustomer?, customers: List<CrmCustomer>,
                            members: List<CrmMember>, busy: Boolean, error: String?, dismiss: () -> Unit,
                            submit: (CrmServiceInput) -> Unit) {
    val available = (listOfNotNull(customer) + customers).distinctBy { it.id }
    var customerId by rememberSaveable { mutableStateOf(initial?.customerId ?: customer?.id) }
    var kind by rememberSaveable { mutableStateOf(initial?.kind ?: "VISIT") }
    var date by rememberSaveable { mutableStateOf(initial?.scheduledAt ?: currentTime().toString()) }
    var description by rememberSaveable { mutableStateOf(initial?.description.orEmpty()) }
    var people by rememberSaveable { mutableStateOf(ArrayList(initial?.participants.orEmpty().map { it.id })) }
    val selected = customerId
    CrmDialog(if (initial == null) "登记服务" else "修改服务安排", busy, error, selected != null && description.isNotBlank(), dismiss,
        { selected?.let { submit(CrmServiceInput(it, kind, date, description, people.toSet())) } }) {
        CrmPicker("客户", available.firstOrNull { it.id == selected }?.name ?: "请选择", available.map { it.id to it.name }, !busy) {
            customerId = it
        }
        CrmPicker("业务类型", crmServiceKinds.getValue(kind), crmServiceKinds.entries.map { it.key to it.value }, !busy) { kind = it }
        CrmDateTime("计划时间", date, !busy) { date = it }
        CrmField(description, "服务说明", 2000, !busy, long = true) { description = it }
        CrmPicker("参与人员（选填）", "添加参与人", members.filter { it.id !in people }.map { it.id to it.name },
            !busy && people.size < 30) { people = ArrayList(people + it) }
        people.forEach { id ->
            val name = members.firstOrNull { it.id == id }?.name ?: initial?.participants?.firstOrNull { it.id == id }?.name ?: "原成员"
            ControlTextButton(onClick = { people = ArrayList(people - id) }, enabled = !busy) { Text("移除 $name") }
        }
    }
}

@Composable
internal fun CrmCompleteForm(busy: Boolean, error: String?, dismiss: () -> Unit, submit: (String) -> Unit) {
    var date by rememberSaveable { mutableStateOf(currentTime().toString()) }
    CrmDialog("完成服务", busy, error, true, dismiss, { submit(date) }) {
        CrmDateTime("实际发生时间", date, !busy) { date = it }
        Text("完成后仍可添加跟进备注和关联任务。")
    }
}

@Composable
internal fun CrmCorrectionForm(row: CrmService, customers: List<CrmCustomer>, members: List<CrmMember>,
                               busy: Boolean, error: String?, dismiss: () -> Unit, submit: (Long, Long, String, String, String) -> Unit) {
    var customerId by rememberSaveable { mutableLongStateOf(row.customerId) }
    var ownerId by rememberSaveable { mutableLongStateOf(row.owner.id) }
    var date by rememberSaveable { mutableStateOf(row.actualAt ?: currentTime().toString()) }
    var desc by rememberSaveable { mutableStateOf(row.description) }
    var reason by rememberSaveable { mutableStateOf("") }
    CrmDialog("更正完成记录", busy, error, desc.isNotBlank() && reason.isNotBlank(), dismiss, { submit(customerId, ownerId, date, desc, reason) }) {
        val businessCustomers = (listOf(row.customerId to row.customerName) + customers.map { it.id to it.name }).distinctBy { it.first }
        val businessMembers = (listOf(row.owner.id to row.owner.name) + members.map { it.id to it.name }).distinctBy { it.first }
        CrmPicker("客户", businessCustomers.firstOrNull { it.first == customerId }?.second ?: "请选择", businessCustomers, !busy) { customerId = it }
        CrmPicker("负责人", businessMembers.firstOrNull { it.first == ownerId }?.second ?: "请选择", businessMembers, !busy) { ownerId = it }
        CrmDateTime("实际发生时间", date, !busy) { date = it }
        CrmField(desc, "服务说明", 2000, !busy, long = true) { desc = it }
        CrmField(reason, "更正原因", 400, !busy) { reason = it }
        Text("原客户、负责人、时间和说明都会保留。")
    }
}

@Composable
internal fun CrmChoiceForm(title: String, label: String, options: List<Pair<Long, String>>, reasonLabel: String?,
                           busy: Boolean, error: String?, dismiss: () -> Unit, submit: (Long, String) -> Unit) {
    var selected by rememberSaveable { mutableStateOf<Long?>(null) }
    var reason by rememberSaveable { mutableStateOf("") }
    val id = selected
    val display = options.firstOrNull { it.first == id }?.second
    CrmDialog(title, busy, error, display != null && (reasonLabel == null || reason.isNotBlank()), dismiss,
        { id?.let { submit(it, reason) } }) {
        CrmPicker(label, display ?: (if (options.isEmpty()) "暂无可选记录" else "请选择"), options, !busy) { selected = it }
        if (reasonLabel != null) CrmField(reason, reasonLabel, 500, !busy) { reason = it }
    }
}

@Composable
internal fun CrmTextForm(title: String, label: String, max: Int, busy: Boolean, error: String?, dismiss: () -> Unit, submit: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    CrmDialog(title, busy, error, text.isNotBlank(), dismiss, { submit(text) }) {
        CrmField(text, label, max, !busy, long = true) { text = it }
    }
}

@Composable
internal fun CrmConfirm(title: String, message: String, busy: Boolean, error: String?, dismiss: () -> Unit, submit: () -> Unit) {
    CrmDialog(title, busy, error, true, dismiss, submit) { Text(message) }
}
