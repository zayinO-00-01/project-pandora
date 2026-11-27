package com.projectpandora.app.ui

import com.projectpandora.app.data.DirectoryContact

private val mobileNumber=Regex("1[3-9][0-9]{9}")
private val nonNameCharacters=Regex("[^\\p{sc=Han}A-Za-z ]")
private val repeatedSpaces=Regex(" +")
private val legacyContact=Regex("演示联系人[0-9]+")

internal fun accountPhoneInput(value: String)=value.filter {it in '0'..'9'}.take(11)
internal fun validAccountPhone(value: String)=mobileNumber.matches(value)
internal fun emergencyContactInput(value: String)=value.replace(nonNameCharacters,"")
    .replace(repeatedSpaces," ").trimStart().take(64).trimEnd {it.isHighSurrogate()}

internal fun profileContactName(value: String?)=value?.takeUnless {legacyContact.matches(it.trim())}
internal fun profileAddress(value: String?)=value?.removePrefix("演示资料（虚构） · ")

internal fun profileGenderLabel(value: String?)=when(value?.trim()?.uppercase()) {
    "MALE","男"->"男"
    "FEMALE","女"->"女"
    "UNSPECIFIED"->"不透露"
    else->null
}

internal fun directoryManagerLabel(person: DirectoryContact, viewerId: Long): String {
    if(person.managerId==viewerId)return "本人"
    person.managerName?.takeIf {it.isNotBlank()}?.let {return it}
    // Older servers hid both manager fields. Only staff and team leads must reach a department through a manager.
    val previouslyHidden=person.hasManager==null && person.departmentId!=null && person.role in setOf("STAFF","TEAM_LEAD")
    return when {
        person.hasManager==true || person.managerId!=null || previouslyHidden->"已设置，当前不可查看"
        person.role=="FOUNDER"->"无直属上级"
        else->"未设置"
    }
}
