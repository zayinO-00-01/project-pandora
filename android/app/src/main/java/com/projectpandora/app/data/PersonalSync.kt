package com.projectpandora.app.data

/** revision is the base of the durable draft, never the result of an unrelated refresh. */
data class PersonalSync(
    val revision: Long? = null,
    val serverItems: List<PanelItem> = emptyList(),
    val draft: List<PanelItem>? = null,
    val conflictRevision: Long? = null,
    val conflictItems: List<PanelItem>? = null
) {
    val items: List<PanelItem> get()=draft ?: serverItems
    val dirty: Boolean get()=draft!=null
    fun canSave()=dirty && revision!=null && conflictRevision==null
    fun receive(snapshot: PersonalPrioritySnapshot): PersonalSync = if(dirty) {
        if(snapshot.revision==revision) this else copy(conflictRevision=snapshot.revision,conflictItems=snapshot.items)
    } else PersonalSync(snapshot.revision,snapshot.items.sortedBy {it.sortOrder})
    fun beginEdit()=if(dirty)this else edit(serverItems)
    fun edit(items: List<PanelItem>)=copy(draft=items.mapIndexed {index,item->item.copy(sortOrder=index)})
    fun saved(snapshot: PersonalPrioritySnapshot)=PersonalSync(snapshot.revision,snapshot.items.sortedBy {it.sortOrder})
    fun input(overwrite: Boolean=false): PersonalPrioritiesInput {
        require(dirty && revision!=null)
        require(conflictRevision==null || overwrite) {"请先处理另一台设备的修改"}
        val existing=conflictItems?.map {it.id}?.toSet().orEmpty()
        return PersonalPrioritiesInput(if(overwrite)conflictRevision ?: revision else revision,
            items.mapIndexed {index,item->PersonalPriorityWrite(if(overwrite && (item.id<=0 || item.id !in existing))-(index+1L) else item.id,item.title,item.detail.orEmpty(),index)})
    }
}
data class WorkspaceDraft(val personal: PersonalSync = PersonalSync(), val companyNotes: Map<Long,String> = emptyMap())
