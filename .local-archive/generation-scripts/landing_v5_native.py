from pathlib import Path
p=Path('android/app/src/main/java/com/projectpandora/app/ui/LandingJourney.kt');s=p.read_text(encoding='utf-8')
start=s.index('@Composable private fun LandingProof(')
s=s[:start]+'''@Composable private fun LandingProof(stage: Int,compact: Boolean) {
    val scale=LocalDensity.current.fontScale.coerceAtLeast(1f)
    val wide=LocalConfiguration.current.screenWidthDp>=600
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.Bottom) {
        Text(if(stage==1)"10 月 11 日" else "2026 / 10",modifier=Modifier.weight(1f),fontFamily=JourneySerif,fontSize=if(stage==1)32.sp else 26.sp,lineHeight=38.sp)
        Text(if(stage==1)"工作日志 · 示例" else "工作轨迹 · 示例",fontSize=11.sp,lineHeight=16.sp,color=ConsoleTokens.Muted)
    }
    HorizontalDivider(Modifier.padding(top=14.dp),color=ConsoleTokens.Line)
    if(stage==1) {
        Spacer(Modifier.height(if(compact)18.dp else 24.dp))
        Text("已完成",fontSize=11.sp,lineHeight=16.sp,color=ConsoleTokens.Muted)
        Text("完成登录与权限联调。",modifier=Modifier.padding(top=10.dp),fontSize=if(compact)17.sp else if(wide)24.sp else 20.sp,lineHeight=if(compact)26.sp else 32.sp,fontWeight=FontWeight.Medium)
        Text("已确认员工仅能查看\\n本人工作记录。",modifier=Modifier.padding(top=10.dp),fontSize=if(wide)15.sp else 13.sp,lineHeight=23.sp,color=ConsoleTokens.Muted)
        Spacer(Modifier.height(if(compact)18.dp else 32.dp))
        Row {
            Box(Modifier.width(1.dp).height(80.dp*scale).background(ConsoleTokens.Copper))
            Column(Modifier.padding(start=18.dp)) {Text("下一步",fontSize=11.sp,lineHeight=16.sp,color=ConsoleTokens.Muted);Text("整理异常网络场景，\\n补充验收记录。",modifier=Modifier.padding(top=8.dp),fontSize=if(wide)15.sp else 14.sp,lineHeight=26.sp)}
        }
    } else {
        Spacer(Modifier.height(8.dp))
        Row {for(day in listOf("一","二","三","四","五","六","日"))Box(Modifier.weight(1f).height(22.dp*scale),contentAlignment=Alignment.Center) {Text(day,fontSize=10.sp,lineHeight=14.sp,color=ConsoleTokens.Muted)}}
        repeat(5) {week->Row {repeat(7) {column->val day=week*7+column-2
            Surface(color=Color.Transparent,border=if(day==11)BorderStroke(1.dp,ConsoleTokens.Copper) else null,modifier=Modifier.weight(1f).height((if(compact)19.dp else if(wide)28.dp else 22.dp)*scale)) {
                Box(contentAlignment=Alignment.Center) {
                    Text(if(day in 1..31)day.toString() else "",fontSize=if(wide)15.sp else 13.sp,lineHeight=16.sp,color=if(day==11)ConsoleTokens.Copper else ConsoleTokens.Muted)
                    if(day==9 || day==10)Box(Modifier.size(3.dp).align(Alignment.BottomCenter).background(ConsoleTokens.Muted,androidx.compose.foundation.shape.CircleShape))
                }
            }
        }}}
        HorizontalDivider(Modifier.padding(top=16.dp),color=ConsoleTokens.Line)
        val entries=listOf(Triple("09","对齐交付范围","日志 · 核对权限与反馈验收顺序"),Triple("10","恢复输入和草稿","日志 · 完成离线恢复测试"),Triple("11","完成日志模块联调","任务 · 17:00 截止 · 进展 60%"))
        Box(Modifier.fillMaxWidth().padding(top=16.dp)) {
            Box(Modifier.padding(start=3.dp,top=8.dp).width(1.dp).height((if(compact)124.dp else if(wide)164.dp else 146.dp)*scale).background(ConsoleTokens.Copper))
            Column {for((date,title,detail) in entries)Row(Modifier.heightIn(min=(if(compact)48.dp else if(wide)64.dp else 58.dp)*scale),verticalAlignment=Alignment.Top) {
                Box(Modifier.padding(top=8.dp).size(7.dp).background(ConsoleTokens.Copper,androidx.compose.foundation.shape.CircleShape))
                Text(date,modifier=Modifier.padding(start=11.dp,end=18.dp),fontFamily=JourneySerif,fontSize=20.sp,lineHeight=26.sp)
                Column {Text(title,fontSize=if(wide)16.sp else 14.sp,lineHeight=20.sp,fontWeight=FontWeight.Medium);Text(detail,modifier=Modifier.padding(top=5.dp),fontSize=11.sp,lineHeight=18.sp,color=ConsoleTokens.Muted)}
            }}
        }
    }
}
'''
p.write_text(s,encoding='utf-8')
p=Path('.tools/subset_landing_font.py');s=p.read_text(encoding='utf-8');s=s.replace("subsetter.populate(text=", "subsetter.populate(text=Path('android/app/src/main/java/com/projectpandora/app/ui/LandingJourney.kt').read_text(encoding='utf-8')+");p.write_text(s,encoding='utf-8')
