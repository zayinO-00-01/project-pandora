package com.projectpandora.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.isActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.projectpandora.app.AppState
import com.projectpandora.app.PandoraViewModel
import com.projectpandora.app.data.*

@Composable fun PriorityScreen(state: AppState,model: PandoraViewModel,company: Boolean,initialSelectedId: Long?=null,companyExamples: Boolean=false,onBack: ()->Unit) {
    val examples=company && companyExamples
    val entries=if(examples)CompanyPriorityExamples.items else if(company)state.panels?.companyImportant.orEmpty().sortedBy {it.sortOrder} else
        if(state.developmentPreview)state.localPriorities.personal.orEmpty() else state.personal.items
    var selectedId by rememberSaveable {mutableStateOf(initialSelectedId)}
    var editing by rememberSaveable {mutableStateOf(false)}
    var title by rememberSaveable {mutableStateOf("")}
    var detail by rememberSaveable {mutableStateOf("")}
    var deleting by remember {mutableStateOf(false)}
    val selected=entries.firstOrNull {it.id==selectedId}
    val note=selectedId?.let {state.companyNoteDrafts[it] ?: selected?.myRemark}.orEmpty()
    val editable=!state.saving && !state.personalLoading && (state.developmentPreview || state.personal.revision!=null)
    var resolution by remember {mutableStateOf<String?>(null)}
    if(resolution!=null)AlertDialog(modifier=Modifier.panelOutline(MaterialTheme.shapes.extraLarge),onDismissRequest={resolution=null},
        title={Text(when(resolution) {"overwrite"->"确认覆盖服务器清单？";"import"->"导入旧版本机清单？";else->"放弃待同步修改？"})},
        text={Text(when(resolution) {"overwrite"->"将以当前待同步清单替换服务器清单；其他设备的修改可能被替换。";"import"->"旧版清单会替换当前显示内容，确认后仍需同步保存。旧版本机备份会保留。";else->"重新读取服务器清单后，当前待同步修改将被清除。"})},
        confirmButton={ControlTextButton(onClick={val action=resolution;resolution=null;when(action) {"overwrite"->model.syncPersonalPriorities(true);"import"->model.importLegacyPersonal();else->model.discardPersonalDraft()}},enabled=!state.saving) {Text("确认")}},
        dismissButton={ControlTextButton(onClick={resolution=null}) {Text("取消")}})
    val listState=rememberLazyListState()
    val latestEntries by rememberUpdatedState(entries)
    val latestPersonal by rememberUpdatedState(if(state.developmentPreview)state.localPriorities.personal else state.personal.draft)
    val feedback=LocalControlFeedback.current
    val density=LocalDensity.current
    val edge=with(density) {56.dp.toPx()}
    val maxScroll=with(density) {10.dp.toPx()}
    var draggedId by remember {mutableStateOf<Long?>(null)}
    var dragTop by remember {mutableFloatStateOf(0f)}
    var dragDistance by remember {mutableFloatStateOf(0f)}
    var dragHeight by remember {mutableIntStateOf(0)}
    var originalIds by remember {mutableStateOf(emptyList<Long>())}
    var originalPersonal by remember {mutableStateOf<List<PanelItem>?>(null)}
    fun reorderAtPointer() {
        val id=draggedId ?: return
        val center=dragTop+dragDistance+dragHeight/2f
        val target=listState.layoutInfo.visibleItemsInfo.firstOrNull {it.key is Long && it.key!=id && center>=it.offset && center<it.offset+it.size}
        if(target!=null) {
            // Keep the viewport anchored by index while keys move, so the dragged card is not disposed at the first visible row.
            listState.requestScrollToItem(listState.firstVisibleItemIndex,listState.firstVisibleItemScrollOffset)
            model.previewPersonalPriorityOrder(id,target.key as Long)
        }
    }
    fun finishDrag(cancelled: Boolean) {
        if(draggedId==null)return
        draggedId=null
        model.finishPersonalPriorityOrder(originalIds,originalPersonal,cancelled)
    }
    LaunchedEffect(draggedId) {
        if(draggedId==null)return@LaunchedEffect
        while(isActive) {
            withFrameNanos { }
            val layout=listState.layoutInfo
            val top=dragTop+dragDistance
            val bottom=top+dragHeight
            val scroll=when {
                top<layout.viewportStartOffset+edge -> -((layout.viewportStartOffset+edge-top)/5).coerceIn(0f,maxScroll)
                bottom>layout.viewportEndOffset-edge -> ((bottom-layout.viewportEndOffset+edge)/5).coerceIn(0f,maxScroll)
                else -> 0f
            }
            if(scroll!=0f) {listState.scrollBy(scroll);reorderAtPointer()}
        }
    }
    fun back() {if(editing)editing=false else if(selectedId!=null)selectedId=null else onBack()}
    BackHandler {back()}
    val dragModifier=if(company || editing || selected!=null || !editable)Modifier else Modifier.pointerInput(company,editing,selectedId) {
        detectDragGesturesAfterLongPress(
            onDragStart={offset->
                val info=listState.layoutInfo.visibleItemsInfo.firstOrNull {it.key is Long && offset.y>=it.offset && offset.y<it.offset+it.size}
                if(info!=null) {
                    dragTop=info.offset.toFloat();dragHeight=info.size;dragDistance=0f
                    originalIds=latestEntries.map {it.id};originalPersonal=latestPersonal
                    draggedId=info.key as Long;feedback()
                }
            },
            onDragEnd={finishDrag(false)},onDragCancel={finishDrag(true)},
            onDrag={change,amount->if(draggedId!=null) {change.consume();dragDistance+=amount.y;reorderAtPointer()}}
        )
    }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal=24.dp).then(dragModifier),state=listState,userScrollEnabled=draggedId==null,verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(bottom=32.dp)) {
        item {ControlTextButton(onClick={back()},contentPadding=PaddingValues(0.dp)) {Text(if(editing || selected!=null)"返回事项" else "返回总览")}}
        item {ScreenHeading(if(editing)if(selected==null)"新增个人事项" else "编辑个人事项" else if(selected!=null)"事项详情" else if(company)"公司十大事" else "个人十大事",
            if(examples)"示例预览 · 非公司发布内容" else if(company)"公司重点由 Web 管理端维护，安卓只读。" else "按优先级安排十件重要的事。")}
        item {Text(if(examples)"仅用于查看卡面效果，不会提交到服务器。" else if(company)"公司原文只读；我的备注保存后可跨设备查看。" else "修改保留在本机，点击同步保存后可跨设备查看。",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.secondary)}
        if(!examples && !company) {
            if(state.personalLoading)item {LinearProgressIndicator(Modifier.fillMaxWidth())}
            state.personalError?.let {item {Hint(it,true,onRetry=model::refresh,retryEnabled=!state.saving)}}
            if(state.personal.dirty && !editing)item {
                Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Text(if(state.personal.conflictRevision!=null)"其他设备已有更新，你的修改仍保留。" else "有待同步修改",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.secondary)
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        if(state.personal.conflictRevision==null)ControlButton(onClick={model.syncPersonalPriorities()},enabled=editable) {Text(if(state.saving)"同步中…" else "同步保存")}
                        else ControlButton(onClick={resolution="overwrite"},enabled=editable) {Text("确认覆盖")}
                        ControlTextButton(onClick={resolution="discard"},enabled=editable) {Text("载入服务器清单")}
                    }
                }
            }
            if(!state.developmentPreview && state.localPriorities.personal!=null && !state.personal.dirty)item {
                ControlTextButton(onClick={resolution="import"},enabled=editable) {Text("导入旧版本机清单")}
            }
        }
        if(!examples && company) {
            state.companyNoteError?.let {item {Hint(it,true)}}
            if(state.localPriorities.remarks.isNotEmpty())item {ControlTextButton(onClick=model::importLegacyCompanyNotes,enabled=!state.saving) {Text("导入旧版本机备注")}}
        }
        if(editing) {
            item {OutlinedTextField(title,{title=it.take(120)},label={Text("事项名称")},singleLine=true,modifier=Modifier.fillMaxWidth(),supportingText={Text("${title.length}/120")})}
            item {OutlinedTextField(detail,{detail=it.take(2000)},label={Text("目标与工作安排")},minLines=6,modifier=Modifier.fillMaxWidth(),supportingText={Text("${detail.length}/2000")})}
            item {ControlButton(onClick={if(model.savePersonalPriority(selectedId,title,detail)) {model.syncPersonalPriorities();editing=false;selectedId=null}},enabled=editable && title.isNotBlank() && (selected!=null || entries.size<10),modifier=Modifier.fillMaxWidth()) {Text(if(state.developmentPreview)"保存预览内容" else "保存并同步")}}
        } else if(selected!=null) {
            item {Text(selected.title,style=MaterialTheme.typography.titleLarge)}
            item {Text(selected.detail?.takeIf {it.isNotBlank()} ?: "暂无详细安排",style=MaterialTheme.typography.bodyLarge)}
            if(company && !examples) {
                item {HorizontalDivider();OutlinedTextField(note,{model.changeCompanyNote(selected.id,it.take(2000))},enabled=!state.saving,label={Text("我的备注")},minLines=4,modifier=Modifier.fillMaxWidth())}
                item {ControlButton(onClick={model.saveCompanyRemark(selected.id,note)},enabled=!state.saving,modifier=Modifier.fillMaxWidth()) {Text(if(state.saving)"保存中…" else "保存备注")}}
                if(state.companyNoteDrafts.containsKey(selected.id))item {Text("备注有待保存的输入",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.secondary)}
            } else if(!company) {
                item {Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    ControlButton(onClick={if(model.beginPersonalPriorityEdit(state.personal.revision)) {title=selected.title;detail=selected.detail.orEmpty();editing=true}},enabled=editable) {Text("编辑事项")}
                    ControlOutlinedButton(onClick={deleting=true},enabled=editable) {Text("删除事项")}
                }}
            }
        } else {
            item {Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                Text("${entries.size} / 10 项",style=MaterialTheme.typography.labelMedium,modifier=Modifier.padding(vertical=12.dp))
                if(!company)ControlTextButton(onClick={if(model.beginPersonalPriorityEdit(state.personal.revision)) {selectedId=null;title="";detail="";editing=true}},enabled=editable && entries.size<10) {Text("新增事项")}
            }}
            if(!company && entries.size>1)item {Text("长按事项或右侧拖动柄，上下拖动调整顺序",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
            if(company && !examples)state.panelError?.let {item {Hint("公司事项读取失败：$it",true,onRetry={model.refresh()})}}
            if(entries.isEmpty() && !state.personalLoading && (company || state.personalError==null))item {EmptyCard(if(company)"暂未发布公司事项" else "安排第一件重要的事",if(company)"发布后可在这里阅读公司原文并记录个人备注。" else "点击新增事项，写下目标与工作安排。")}
            if(company)itemsIndexed(entries.chunked(2),key={_,row->"company-${row.first().id}"}) {rowIndex,row->
                Row(Modifier.fillMaxWidth().padding(bottom=4.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    row.forEachIndexed {columnIndex,entry->
                        CompanyDepthCard(entry,rowIndex*2+columnIndex,Modifier.weight(1f)) {
                            selectedId=entry.id
                        }
                    }
                    if(row.size==1)Spacer(Modifier.weight(1f))
                }
            } else itemsIndexed(entries,key={_,item->item.id}) {_,entry->
                val position=entries.indexOfFirst {it.id==entry.id}
                OutlinedCard(onClick={selectedId=entry.id},modifier=Modifier.fillMaxWidth()
                    .zIndex(if(draggedId==entry.id)1f else 0f)
                    .graphicsLayer {
                        translationY=if(draggedId==entry.id)dragTop+dragDistance-(listState.layoutInfo.visibleItemsInfo.firstOrNull {it.key==entry.id}?.offset ?: 0) else 0f
                        shadowElevation=if(draggedId==entry.id)8.dp.toPx() else 0f
                    }) {
                    Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                            Text("${(position+1).toString().padStart(2,'0')}  ${entry.title}",style=MaterialTheme.typography.titleMedium,maxLines=2,overflow=TextOverflow.Ellipsis,modifier=Modifier.weight(1f))
                            if(!company)Box(Modifier.size(32.dp),contentAlignment=androidx.compose.ui.Alignment.Center) {Icon(Icons.Default.Menu,"拖动${entry.title}",tint=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.size(20.dp))}
                        }
                        Text(entry.detail?.takeIf {it.isNotBlank()} ?: "查看事项详情",maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        if(company && !entry.myRemark.isNullOrBlank())Text("有我的备注",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.secondary)

                    }
                }
            }
        }
    }
    if(deleting && selected!=null)AlertDialog(modifier=Modifier.panelOutline(MaterialTheme.shapes.extraLarge),onDismissRequest={deleting=false},title={Text("删除这件事项？")},text={Text(selected.title)},confirmButton={ControlTextButton(onClick={model.deletePersonalPriority(selected.id);model.syncPersonalPriorities();selectedId=null;deleting=false}) {Text("删除")}},dismissButton={ControlTextButton(onClick={deleting=false}) {Text("取消")}})
}
