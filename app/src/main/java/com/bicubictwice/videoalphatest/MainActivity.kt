package com.bicubictwice.videoalphatest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.tritus.transparentvideo.video.transparent.TransparentVideo
import kotlinx.coroutines.isActive

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var demoScreen by remember { mutableIntStateOf(1) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                when (demoScreen) {
                    1 -> Demo1()
                    2 -> Demo2()
                    else -> Demo3()
                }

                Buttons(
                    modifier = Modifier.align(Alignment.BottomStart),
                    currentDemo = demoScreen,
                    onDemoSelect = { demoScreen = it }
                )
                ShowFPS(modifier = Modifier.align(Alignment.TopEnd))
            }
        }
    }

    @Composable
    private fun Demo1() {
        repeat(1) {
            VideoAnimationWidget(resourceId = R.raw.sample)
        }
    }

    @Composable
    private fun Demo2() {
        repeat(1) {
            TransparentVideo(source = R.raw.sample)
        }
    }

    @Composable
    private fun Demo3() {
        val boxColors = remember { listOf(Color.Red, Color.Yellow) }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopStart
        ) {
            for (index in 0 until 3) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .offset((index * 50).dp, (index * 110).dp)
                        .background(boxColors[index % boxColors.size])
                        .zIndex(if (index % 2 == 0) 0f else 1f),
                    contentAlignment = Alignment.Center
                ) {
                    var checked by remember { mutableStateOf(false) }
                    Switch(checked = checked, onCheckedChange = { checked = !checked })
                }
            }

            for (index in 0 until 3) {
                VideoAnimationWidget(
                    modifier = Modifier
                        .size(100.dp)
                        .offset(30.dp + (index * 50).dp, (index * 100).dp),
                    resourceId = R.raw.sample
                )
            }
        }
    }

    @Composable
    private fun Buttons(modifier: Modifier = Modifier, currentDemo: Int, onDemoSelect: (Int) -> Unit) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (demo in 1..3) {
                val isSelected = (demo == currentDemo)
                OutlinedButton(
                    onClick = { onDemoSelect(demo) },
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) Color.Gray else Color.LightGray
                    )
                ) { Text(text = "$demo") }
            }
        }
    }

    @Composable
    private fun ShowFPS(modifier: Modifier = Modifier) {
        var text by remember { mutableStateOf("") }

        Text(modifier = modifier, text = text)

        LaunchedEffect(Unit) {
            var frames = 0
            var startTime = withFrameMillis { it }
            while (isActive) {
                val time = withFrameMillis { it }
                frames++
                if (time - startTime >= 1000) {
                    text = "FPS: $frames"
                    frames = 0
                    startTime = time
                }
            }
        }
    }
}
