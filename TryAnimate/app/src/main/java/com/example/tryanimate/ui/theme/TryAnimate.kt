package com.example.tryanimate.ui.theme

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.gestures.*
import androidx.compose.material.Card
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
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
    val x_List = mutableListOf(-1080, 0, 1080)


    var minH by remember { mutableStateOf(1079) }
    var midH by remember { mutableStateOf(1080) }
    var maxH by remember { mutableStateOf(1081) }

    var state = remember { Animatable(1080f) }

    var anchors = arrayOf(minH.toFloat(), midH.toFloat(), maxH.toFloat())
    Log.d("11111", "11111 ${anchors[0]}")

    Layout(
        modifier = Modifier.horizonSwiper(
            state = state,
            anchors = anchors
        ),
        content = content
    ) { measurables, constraints ->
        Log.d("33333", "33333")

        val w = constraints.maxWidth * 3
        val itemW = constraints.maxWidth
        val itemH = constraints.maxHeight

        minH = itemW / 2
        midH = itemW
        maxH = itemH

        state.updateBounds(
            lowerBound = 300f,
            upperBound = itemH.toFloat()
        )

        val items = measurables.map { measurable ->
            measurable.measure(Constraints(itemW, itemW, state.value.toInt(), state.value.toInt()))
        }

        layout(w, state.value.toInt()){
            items.forEachIndexed { i, item ->
                item.placeRelative(x = x_List[i], y = 0)
            }
        }
    }
}

fun Modifier.horizonSwiper(
    state: Animatable<Float, AnimationVector1D>,
    anchors: Array<Float>,
    fractionalThreshold: Float = 0.1f
) = then(
    Modifier.pointerInput(Unit) {

        val decay = splineBasedDecay<Float>(this)

        coroutineScope {
            while (true) {
                Log.d("22222", "${anchors[0]}")
                awaitPointerEventScope {

                    val down = awaitFirstDown()
                    val velocityTracker = VelocityTracker()

                    verticalDrag(down.id) { change ->
                        val a = change.previousPosition.y
                        val b = change.position.y

                        val plus = (b - a) * 3

                        launch {
                            state.animateTo(state.value + plus)
                        }
                        velocityTracker.addPosition(
                            change.uptimeMillis,
                            change.position
                        )
                    }
                    val velocity = velocityTracker.calculateVelocity().y / 3

                    val targetOffsetY = decay.calculateTargetValue(
                        state.value,
                        velocity
                    )
                    Log.d("55555", "${anchors[0]}")
                    launch {
                        if (state.value <= anchors[0]) {
                            state.animateTo(
                                targetValue = anchors[0],
                                initialVelocity = velocity
                            )
                        } else {
                            state.animateDecay(velocity, decay)
                        }
                    }
                }
            }
        }
    }
)


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