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
        state.localPriorities.personal ?: state.panels?.personalTop.orEmpty().sortedBy {it.sortOrder}
    var selectedId by rememberSaveable {mutableStateOf(initialSelectedId)}
    var editing by rememberSaveable {mutableStateOf(false)}
    var title by rememberSaveable {mutableStateOf("")}
    var detail by rememberSaveable {mutableStateOf("")}
    var note by rememberSaveable {mutableStateOf(initialSelectedId?.let {id->entries.firstOrNull {it.id==id}?.myRemark}.orEmpty())}
    var deleting by remember {mutableStateOf(false)}
    val selected=entries.firstOrNull {it.id==selectedId}
    val listState=rememberLazyListState()
    val latestEntries by rememberUpdatedState(entries)
    val latestPersonal by rememberUpdatedState(state.localPriorities.personal)
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
    val dragModifier=if(company || editing || selected!=null)Modifier else Modifier.pointerInput(company,editing,selectedId) {
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
        item {Text(if(examples)"仅用于查看卡面效果，不会提交到服务器。" else if(company)"公司重点由 Web 管理端维护，安卓只读；我的备注保存在服务端，仅本人可见。" else "本机工作清单 · 尚未同步到服务器",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.secondary)}
        if(editing) {
            item {OutlinedTextField(title,{title=it.take(120)},label={Text("事项名称")},singleLine=true,modifier=Modifier.fillMaxWidth(),supportingText={Text("${title.length}/120")})}
            item {OutlinedTextField(detail,{detail=it.take(2000)},label={Text("目标与工作安排")},minLines=6,modifier=Modifier.fillMaxWidth(),supportingText={Text("${detail.length}/2000")})}
            item {ControlButton(onClick={model.savePersonalPriority(selectedId,title,detail);editing=false;selectedId=null},enabled=title.isNotBlank() && (selected!=null || entries.size<10),modifier=Modifier.fillMaxWidth()) {Text("保存到本机")}}
        } else if(selected!=null) {
            item {Text(selected.title,style=MaterialTheme.typography.titleLarge)}
            item {Text(selected.detail?.takeIf {it.isNotBlank()} ?: "暂无详细安排",style=MaterialTheme.typography.bodyLarge)}
            if(company && !examples) {
                item {HorizontalDivider();OutlinedTextField(note,{note=it.take(2000)},label={Text("我的备注")},minLines=4,modifier=Modifier.fillMaxWidth())}
                item {ControlButton(onClick={model.saveCompanyRemark(selected.id,note)},enabled=note.isNotBlank() && !state.remarkSaving,modifier=Modifier.fillMaxWidth()) {Text(if(state.remarkSaving)"正在保存…" else "保存我的备注（仅本人可见）")}}
            } else if(!company) {
                item {Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    ControlButton(onClick={title=selected.title;detail=selected.detail.orEmpty();editing=true}) {Text("编辑事项")}
                    ControlOutlinedButton(onClick={deleting=true}) {Text("删除事项")}
                }}
            }
        } else {
            item {Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                Text("${entries.size} / 10 项",style=MaterialTheme.typography.labelMedium,modifier=Modifier.padding(vertical=12.dp))
                if(!company)ControlTextButton(onClick={selectedId=null;title="";detail="";editing=true},enabled=entries.size<10) {Text("新增事项")}
            }}
            if(!company && entries.size>1)item {Text("长按事项或右侧拖动柄，上下拖动调整顺序",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
            state.panelError?.let {item {Hint("公司事项读取失败：$it",true,onRetry={model.refresh()})}}
            if(entries.isEmpty())item {EmptyCard(if(company)"暂未发布公司事项" else "安排第一件重要的事",if(company)"公司事项数据接口已保留，发布后可在这里阅读并记录个人备注。" else "点击新增事项，写下目标与工作安排。")}
            if(company)itemsIndexed(entries.chunked(2),key={_,row->"company-${row.first().id}"}) {rowIndex,row->
                Row(Modifier.fillMaxWidth().padding(bottom=4.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    row.forEachIndexed {columnIndex,entry->
                        CompanyDepthCard(entry,rowIndex*2+columnIndex,Modifier.weight(1f)) {
                            selectedId=entry.id;note=entry.myRemark.orEmpty()
                        }
                    }
                    if(row.size==1)Spacer(Modifier.weight(1f))
                }
            } else itemsIndexed(entries,key={_,item->item.id}) {_,entry->
                val position=entries.indexOfFirst {it.id==entry.id}
                OutlinedCard(onClick={selectedId=entry.id;note=entry.myRemark.orEmpty()},modifier=Modifier.fillMaxWidth()
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
    if(deleting && selected!=null)AlertDialog(modifier=Modifier.panelOutline(MaterialTheme.shapes.extraLarge),onDismissRequest={deleting=false},title={Text("删除这件事项？")},text={Text(selected.title)},confirmButton={ControlTextButton(onClick={model.deletePersonalPriority(selected.id);selectedId=null;deleting=false}) {Text("删除")}},dismissButton={ControlTextButton(onClick={deleting=false}) {Text("取消")}})
}
