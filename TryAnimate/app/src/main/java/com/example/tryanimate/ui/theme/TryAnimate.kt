package com.example.tryanimate.ui.theme

import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material.Card
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.ConsumedData
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlin.math.abs
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
        animationSpec = tween(
            durationMillis = if (vState.value in vAnchor.value) 600 else 0,
            easing = LinearOutSlowInEasing
        ),
    )

    val hState = remember { mutableStateOf (0f) }
    val hAnimate = animateFloatAsState(
        targetValue = hState.value,
        animationSpec = tween(
            durationMillis = 600,
            easing = LinearOutSlowInEasing
        ),
    )

    val x_List = remember { mutableStateOf ( arrayOf(0, 1080, 2160)) }

    val lmr by remember { mutableStateOf( arrayOf(0, 1, 2)) }

    Layout(
        modifier = Modifier
            .MySwiper(
                vState = vState,
                hState = hState,
                vAnchor = vAnchor,
                thresholds = 0.1f
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

        var tmp = x_List.value.map { num -> num + hAnimate.value.toInt() }

        if (tmp[lmr[2]] >= 3240) {
            x_List.value[lmr[2]] -= 3240
            for (i in 0 until 3) {
                lmr[i] -= 1
                if (lmr[i] < 0) lmr[i] = 2
            }
        } else if (tmp[lmr[0]] <= -1080) {
            x_List.value[lmr[0]] += 3240
            for (i in 0 until 3) {
                lmr[i] += 1
                if (lmr[i] > 2) lmr[i] = 0
            }
        }

        layout(w, constraints.maxHeight){
            items.forEachIndexed { i, item ->
                item.placeRelative(x = tmp[i], y = 0)
            }
        }
    }
}

fun Modifier.MySwiper(
    vState: MutableState<Float>,
    hState: MutableState<Float>,
    vAnchor: MutableState<List<Float>>,
    thresholds: Float
) = composed {

    pointerInput(Unit) {

        val vAnchor = vAnchor.value
        var currentHState = 0
        var currentVState = 1

        coroutineScope {

            while (true) {

                var isVertical = false
                val velocityTracker = VelocityTracker()
                var hDist = 0f

                awaitPointerEventScope {



                    val down = awaitFirstDown()

                    var change = awaitDragOrCancellation(down.id)?.apply {

                        if (abs(down.position.x - position.x) < abs(down.position.y - position.y) ) {
                            isVertical = true
                        }
                    }

                    if (change != null && change.pressed) {

                        if (isVertical) {

                            verticalDrag(change.id) { change ->
                                velocityTracker.addPosition(
                                    change.uptimeMillis,
                                    change.position
                                )
                                val a = change.previousPosition.y
                                val b = change.position.y

                                var target = vState.value + (b - a)

                                if (target <= 300) target = 300f
                                else if (target >= vAnchor[2]) target = vAnchor[2]

                                launch {
                                    vState.value = target
                                }
                            }
                        } else {

                            if (hState.value - currentHState >= 1080) currentHState += 1080
                            else if (hState.value - currentHState <= -1080) currentHState -= 1080

                            velocityTracker.addPosition(
                                change.uptimeMillis,
                                change.position
                            )
                            horizontalDrag(change.id) { change ->
                                velocityTracker.addPosition(
                                    change.uptimeMillis,
                                    change.position
                                )

                                val a = change.position.x

                                hDist = a - down.position.x

                                launch {
                                    hState.value = currentHState + hDist
                                }
                            }
                        }
                    }
                }


                launch {
                    if (isVertical) {
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
                    } else {
                        val width = 1080

                        val line = width * thresholds

                        if (hDist < -line) {
                            hState.value = currentHState - 1080f
                        } else if (hDist > line) {
                            hState.value = currentHState + 1080f
                        } else {
                            hState.value = currentHState.toFloat()
                        }
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

        val items = measurables.map { measurable -> measurable.measure(Constraints(0, w, itemH, itemH)) }

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

        val items = measurables.map { measurable -> measurable.measure(Constraints(itemW, itemW, 0, h)) }


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
    Card(modifier = Modifier.fillMaxSize()) {
        Text(text = "day")
    }
}