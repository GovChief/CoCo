package com.diplomski.mucnjak.coco.ui.components.splitscreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import com.diplomski.mucnjak.coco.ui.ComposeMock
import com.diplomski.mucnjak.coco.ui.theme.CoCoTheme
import com.diplomski.mucnjak.coco.ui.theme.StudentCoCoTheme

@Composable
fun GridContainer(
    screens: List<@Composable (RowScope.(rotation: Float) -> Unit)>
) {
    Column(
        Modifier.fillMaxSize()
    ) {
        repeat(if (screens.size <= 1) 1 else 2) { columnIndex ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (columnIndex == 0) {
                    repeat(screens.size / 2) { rowIndex ->
                        screens[rowIndex](180f)
                    }
                } else {
                    repeat(screens.size - screens.size / 2) { rowIndex ->
                        screens[screens.size / 2 + rowIndex](0f)
                    }
                }
            }
        }
    }
}
