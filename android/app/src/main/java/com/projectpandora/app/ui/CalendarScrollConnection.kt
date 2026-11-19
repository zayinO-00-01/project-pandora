package com.projectpandora.app.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity

internal class CalendarScrollConnection(
    private val isAtTop: ()->Boolean,
    private val startResize: (Float)->Boolean,
    private val resize: (Float)->Unit,
    private val finishResize: (Float)->Unit
): NestedScrollConnection {
    private var resizing=false

    override fun onPreScroll(available: Offset,source: NestedScrollSource): Offset {
        if(source!=NestedScrollSource.UserInput)return Offset.Zero
        // Collapse at the start of the list; otherwise the list keeps the gesture.
        if(resizing || (available.y<0f && isAtTop()))return resizeBy(available.y)
        return Offset.Zero
    }

    override fun onPostScroll(consumed: Offset,available: Offset,source: NestedScrollSource): Offset {
        // Only expand with the downward distance the list could not consume at its start.
        if(source==NestedScrollSource.UserInput && available.y>0f && isAtTop())return resizeBy(available.y)
        return Offset.Zero
    }

    private fun resizeBy(delta: Float): Offset {
        if(delta==0f)return Offset.Zero
        if(!resizing) {
            if(!startResize(delta))return Offset.Zero
            resizing=true
        }
        resize(delta)
        return Offset(0f,delta)
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        if(!resizing)return Velocity.Zero
        resizing=false
        finishResize(available.y)
        return Velocity(0f,available.y)
    }
}
