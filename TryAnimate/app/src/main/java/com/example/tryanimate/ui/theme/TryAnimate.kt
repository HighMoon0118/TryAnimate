package com.example.tryanimate.ui.theme

import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material.Card
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlin.math.absoluteValue
import kotlin.math.roundToInt


@Composable
fun DrawCalendar (

) {
    Swiper () {
        repeat(3) {
            ItemMonth {
                repeat(5) {
                    ItemWeek {
                        repeat(7) {
                            ItemDay()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Swiper (
    content: @Composable () -> Unit
) {

    val vAnchor = remember { mutableStateOf (listOf(0f, 0f, 0f) ) }
    val vState = remember { mutableStateOf (1080f) }
    val vAnimate = animateFloatAsState(
        targetValue = vState.value,
        animationSpec = tween(durationMillis = if (vState.value in vAnchor.value) 500 else 0, easing = FastOutSlowInEasing),
    )

    val hAnchor = remember { mutableStateOf ((-499..499).map { i -> i * 1080 } ) }
    val hState = remember { mutableStateOf (0f) }
    val hAnimate = animateFloatAsState(
        targetValue = hState.value,
        animationSpec = tween(100, easing = FastOutSlowInEasing),
    )

    val x_List = remember { mutableStateOf ( arrayOf(-1080, 0, 1080)) }

    Layout(
        modifier = Modifier
            .MySwiper(
                vState = vState,
                hState = hState,
                vAnchor = vAnchor,
                hAnchor = hAnchor,
            ),
        content = content
    ) { measurables, constraints ->

        val w = constraints.maxWidth * 3
        val itemW = constraints.maxWidth
        val itemH = constraints.maxHeight

        if (vAnchor.value[1].toInt() != itemW) {
            vAnchor.value = listOf((itemW / 2).toFloat(), itemW.toFloat(), itemH.toFloat())
        }

        val h = vAnimate.value.toInt()

        val items = measurables.map { measurable ->
            measurable.measure(Constraints(itemW, itemW, h, h))
        }

        layout(w, h){
            items.forEachIndexed { i, item ->
                x_List.value[i] += hAnimate.value.toInt()
                item.placeRelative(x = x_List.value[i], y = 0)
            }
        }
    }
}

fun Modifier.MySwiper(
    vState: MutableState<Float>,
    hState: MutableState<Float>,
    vAnchor: MutableState<List<Float>>,
    hAnchor: MutableState<List<Int>>
) = composed {

    pointerInput(Unit) {

        val vAnchor = vAnchor.value
        val hAnchor = hAnchor.value
        var currentHState = 499
        var currentVState = 1

        coroutineScope {

            while (true) {
                awaitPointerEventScope {

                    val down = awaitFirstDown()

                    horizontalDrag(down.id) { change ->

                        val a = change.previousPosition.x
                        val b = change.position.x

                        launch {
                            hState.value = hAnchor[currentHState] + b - a
                        }
                    }

                }

                awaitPointerEventScope {
                    val down = awaitFirstDown()

                    verticalDrag(down.id) { change ->
                        val a = change.previousPosition.y
                        val b = change.position.y

                        var target = vState.value + (b - a)

                        if (target <= 300) target = 300f
                        else if (target >= vAnchor[2]) target = vAnchor[2]

                        launch {
                            vState.value = target
                        }
                    }

                    launch {
                        val currentY = vState.value

                        if (currentVState == 0) {
                            if (currentY > vAnchor[1]) currentVState = 2
                            else if (currentY > vAnchor[0]) currentVState = 1
                        } else if (currentVState == 1) {
                            if (currentY < vAnchor[1]) currentVState = 0
                            else if (currentY > vAnchor[1]) currentVState = 2
                        } else if (currentVState == 2) {
                            if (currentY < vAnchor[1]) currentVState = 0
                            else if (currentY < vAnchor[2]) currentVState = 1
                        }

                        vState.value = vAnchor[currentVState]
                    }
                }
            }
        }
    }
}


@Composable
fun ItemMonth (
    content: @Composable () -> Unit
) {
    Layout(content = content) { measurables, constraints ->

        val w = constraints.maxWidth
        val h = constraints.maxHeight
        val itemH = h / 5

        val items = measurables.map { measurable -> measurable.measure(Constraints(w, w, h, h)) }

        layout(w, h){
            var y = 0
            items.forEach { item ->
                item.placeRelative(x = 0, y = y)
                y += itemH
            }
        }
    }
}

@Composable
fun ItemWeek (
    content: @Composable () -> Unit
) {
    Layout(content = content) { measurables, constraints ->

        val w = constraints.maxWidth
        val h = constraints.maxHeight
        val itemW = w / 7

        val items = measurables.map { measurable -> measurable.measure(Constraints(itemW, itemW, h, h)) }


        layout(w, h){
            var x = 0
            items.forEach { item ->
                item.placeRelative(x = x, y = 0)
                x += itemW
            }
        }
    }
}

@Composable
fun ItemDay(

) {
    Card() {
        Text(text = "day")
    }
}