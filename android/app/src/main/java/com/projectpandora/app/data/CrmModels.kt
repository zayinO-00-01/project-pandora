package com.projectpandora.app.data

internal data class CrmMember(val id: Long, val name: String)
internal data class CrmCustomer(
    val id: Long, val kind: String, val name: String, val contactName: String, val phone: String, val editable: Boolean,
)
internal data class CrmCustomerInput(val kind: String, val name: String, val contactName: String, val phone: String)
internal data class CrmCustomerDetail(val customer: CrmCustomer, val services: List<CrmService>)
internal data class CrmService(
    val id: Long, val customerId: Long, val customerName: String, val owner: CrmMember, val kind: String, val status: String,
    val scheduledAt: String, val actualAt: String?, val description: String, val participants: List<CrmMember>,
    val paymentStatus: String, val editable: Boolean, val canTransfer: Boolean, val canCorrect: Boolean,
)
internal data class CrmServiceInput(
    val customerId: Long, val kind: String, val scheduledAt: String, val description: String, val participantIds: Set<Long>,
)
internal data class CrmEvent(val id: Long, val actor: String, val eventType: String, val detail: String, val recordedAt: String)
internal data class CrmTask(val id: Long, val title: String)
internal data class CrmServiceDetail(val service: CrmService, val history: List<CrmEvent>, val tasks: List<CrmTask>)
internal data class CrmCardUse(val id: Long, val serviceId: Long, val usedAt: String, val reversedAt: String?, val reverseReason: String?)
internal data class CrmOrder(
    val id: Long, val source: String, val number: String, val product: String, val status: String, val startsOn: String?,
    val expiresOn: String?, val remaining: Int?, val usable: Boolean, val mayReverse: Boolean, val uses: List<CrmCardUse>,
)
