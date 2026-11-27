package com.projectpandora.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.projectpandora.app.AppState
import com.projectpandora.app.PandoraViewModel

@Composable
internal fun CompanyScreen(
    state: AppState,
    model: PandoraViewModel,
    showBusiness: Boolean,
    onSectionChange: (Boolean) -> Unit,
) {
    val feedback = LocalControlFeedback.current
    val sections = rememberSaveableStateHolder()
    Column(Modifier.fillMaxSize()) {
        SingleChoiceSegmentedButtonRow(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp)
        ) {
            listOf("日历", "客户与业务").forEachIndexed { index, label ->
                val isBusiness = index == 1
                SegmentedButton(
                    selected = showBusiness == isBusiness,
                    onClick = {
                        if (showBusiness != isBusiness) {
                            feedback()
                            onSectionChange(isBusiness)
                        }
                    },
                    shape = SegmentedButtonDefaults.itemShape(index, 2),
                    enabled = !state.saving,
                ) { Text(label) }
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (showBusiness) {
                sections.SaveableStateProvider("business") { CompanyBusinessScreen(state, model) }
            } else {
                sections.SaveableStateProvider("calendar") { CalendarScreen(state, model) }
            }
        }
    }
}
