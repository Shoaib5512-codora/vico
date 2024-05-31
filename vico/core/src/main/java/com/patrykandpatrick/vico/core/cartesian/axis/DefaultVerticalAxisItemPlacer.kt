/*
 * Copyright 2024 by Patryk Goworowski and Patrick Michalik.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.patrykandpatrick.vico.core.cartesian.axis

import com.patrykandpatrick.vico.core.cartesian.CartesianDrawContext
import com.patrykandpatrick.vico.core.cartesian.CartesianMeasureContext
import com.patrykandpatrick.vico.core.common.ceil
import com.patrykandpatrick.vico.core.common.data.ExtraStore
import com.patrykandpatrick.vico.core.common.floor
import com.patrykandpatrick.vico.core.common.getDivisors
import com.patrykandpatrick.vico.core.common.half
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow

internal class DefaultVerticalAxisItemPlacer(
  private val mode: Mode,
  private val shiftTopLines: Boolean,
) : AxisItemPlacer.Vertical {
  override fun getShiftTopLines(context: CartesianDrawContext): Boolean = shiftTopLines

  override fun getLabelValues(
    context: CartesianDrawContext,
    axisHeight: Float,
    maxLabelHeight: Float,
    position: AxisPosition.Vertical,
  ) = getWidthMeasurementLabelValues(context, axisHeight, maxLabelHeight, position)

  override fun getWidthMeasurementLabelValues(
    context: CartesianMeasureContext,
    axisHeight: Float,
    maxLabelHeight: Float,
    position: AxisPosition.Vertical,
  ): List<Float> = mode.getLabelValues(context, axisHeight, maxLabelHeight, position)

  override fun getHeightMeasurementLabelValues(
    context: CartesianMeasureContext,
    position: AxisPosition.Vertical,
  ): List<Float> {
    val yRange = context.chartValues.getYRange(position)
    return listOf(yRange.minY, (yRange.minY + yRange.maxY).half, yRange.maxY)
  }

  override fun getTopVerticalAxisInset(
    context: CartesianMeasureContext,
    verticalLabelPosition: VerticalAxis.VerticalLabelPosition,
    maxLabelHeight: Float,
    maxLineThickness: Float,
  ) =
    when {
      !mode.insetsRequired(context) -> 0f
      verticalLabelPosition == VerticalAxis.VerticalLabelPosition.Top ->
        maxLabelHeight + (if (shiftTopLines) maxLineThickness else -maxLineThickness).half
      verticalLabelPosition == VerticalAxis.VerticalLabelPosition.Center ->
        (max(maxLabelHeight, maxLineThickness) +
            if (shiftTopLines) maxLineThickness else -maxLineThickness)
          .half
      else -> if (shiftTopLines) maxLineThickness else 0f
    }

  override fun getBottomVerticalAxisInset(
    context: CartesianMeasureContext,
    verticalLabelPosition: VerticalAxis.VerticalLabelPosition,
    maxLabelHeight: Float,
    maxLineThickness: Float,
  ): Float =
    when {
      !mode.insetsRequired(context) -> 0f
      verticalLabelPosition == VerticalAxis.VerticalLabelPosition.Top -> maxLineThickness
      verticalLabelPosition == VerticalAxis.VerticalLabelPosition.Center ->
        (maxOf(maxLabelHeight, maxLineThickness) + maxLineThickness).half
      else -> maxLabelHeight + maxLineThickness.half
    }

  sealed interface Mode {
    fun getSimpleLabelValues(
      context: CartesianMeasureContext,
      axisHeight: Float,
      maxLabelHeight: Float,
      position: AxisPosition.Vertical,
    ): List<Float>

    fun getMixedLabelValues(
      context: CartesianMeasureContext,
      axisHeight: Float,
      maxLabelHeight: Float,
      position: AxisPosition.Vertical,
    ): List<Float>

    fun insetsRequired(context: CartesianMeasureContext): Boolean = true

    fun getLabelValues(
      context: CartesianMeasureContext,
      axisHeight: Float,
      maxLabelHeight: Float,
      position: AxisPosition.Vertical,
    ) =
      if (context.chartValues.getYRange(position).run { minY * maxY } >= 0) {
        getSimpleLabelValues(context, axisHeight, maxLabelHeight, position)
      } else {
        getMixedLabelValues(context, axisHeight, maxLabelHeight, position)
      }

    class Step(private val step: (ExtraStore) -> Float?) : Mode {
      private fun CartesianMeasureContext.getStepOrThrow() =
        step(chartValues.model.extraStore)?.also {
          require(it > 0) { "`step` must return a positive value." }
        }

      private fun getPartialLabelValues(
        context: CartesianMeasureContext,
        minY: Float,
        maxY: Float,
        freeHeight: Float,
        maxLabelHeight: Float,
        multiplier: Int = 1,
      ): List<Float> {
        val values = mutableListOf<Float>()
        val requestedStep = context.getStepOrThrow() ?: 10f.pow(log10(maxY).floor - 1)
        val step =
          if (maxLabelHeight != 0f) {
            val minStep = (maxY - minY) / (freeHeight / maxLabelHeight).floor
            ((maxY - minY) / requestedStep)
              .takeIf { it == it.floor }
              ?.toInt()
              ?.getDivisors(includeDividend = false)
              ?.firstOrNull { it * requestedStep >= minStep }
              ?.let { it * requestedStep } ?: ((minStep / requestedStep).ceil * requestedStep)
          } else {
            requestedStep
          }
        repeat(((maxY - minY) / step).toInt()) { values += multiplier * (minY + (it + 1) * step) }
        return values
      }

      override fun getSimpleLabelValues(
        context: CartesianMeasureContext,
        axisHeight: Float,
        maxLabelHeight: Float,
        position: AxisPosition.Vertical,
      ): List<Float> =
        context.chartValues.getYRange(position).run {
          if (maxY > 0) {
            getPartialLabelValues(context, minY, maxY, axisHeight, maxLabelHeight) + minY
          } else {
            getPartialLabelValues(
              context = context,
              minY = abs(maxY),
              maxY = abs(minY),
              freeHeight = axisHeight,
              maxLabelHeight = maxLabelHeight,
              multiplier = -1,
            ) + maxY
          }
        }

      override fun getMixedLabelValues(
        context: CartesianMeasureContext,
        axisHeight: Float,
        maxLabelHeight: Float,
        position: AxisPosition.Vertical,
      ): List<Float> =
        context.chartValues.getYRange(position).run {
          val topLabelValues =
            getPartialLabelValues(
              context = context,
              minY = 0f,
              maxY = maxY,
              freeHeight = maxY / length * axisHeight,
              maxLabelHeight = maxLabelHeight,
            )
          val bottomLabelValues =
            getPartialLabelValues(
              context = context,
              minY = 0f,
              maxY = abs(minY),
              freeHeight = -minY / length * axisHeight,
              maxLabelHeight = maxLabelHeight,
              multiplier = -1,
            )
          topLabelValues + bottomLabelValues + 0f
        }
    }

    class Count(private val count: (ExtraStore) -> Int?) : Mode {
      private fun CartesianMeasureContext.getCountOrThrow() =
        count(chartValues.model.extraStore)?.also {
          require(it >= 0) { "`count` must return a nonnegative value." }
        }

      override fun getSimpleLabelValues(
        context: CartesianMeasureContext,
        axisHeight: Float,
        maxLabelHeight: Float,
        position: AxisPosition.Vertical,
      ): List<Float> {
        val values = mutableListOf<Float>()
        val requestedItemCount = context.getCountOrThrow()
        if (requestedItemCount == 0) return values
        val yRange = context.chartValues.getYRange(position)
        values += yRange.minY
        if (requestedItemCount == 1) return values
        if (maxLabelHeight == 0f) {
          values += yRange.maxY
          return values
        }
        var extraItemCount = (axisHeight / maxLabelHeight).toInt()
        if (requestedItemCount != null)
          extraItemCount = extraItemCount.coerceAtMost(requestedItemCount - 1)
        val step = yRange.length / extraItemCount
        repeat(extraItemCount) { values += yRange.minY + (it + 1) * step }
        return values
      }

      override fun getMixedLabelValues(
        context: CartesianMeasureContext,
        axisHeight: Float,
        maxLabelHeight: Float,
        position: AxisPosition.Vertical,
      ): List<Float> {
        val values = mutableListOf<Float>()
        val requestedItemCount = context.getCountOrThrow()
        if (requestedItemCount == 0) return values
        values += 0f
        if (requestedItemCount == 1) return values
        val yRange = context.chartValues.getYRange(position)
        if (maxLabelHeight == 0f) {
          values += yRange.minY
          values += yRange.maxY
          return values
        }
        val topHeight = yRange.maxY / yRange.length * axisHeight
        val bottomHeight = -yRange.minY / yRange.length * axisHeight
        val maxTopItemCount =
          if (requestedItemCount != null) (requestedItemCount - 1) * topHeight / axisHeight
          else null
        val maxBottomItemCount =
          if (requestedItemCount != null) (requestedItemCount - 1) * bottomHeight / axisHeight
          else null
        val topItemCountByHeight = topHeight / maxLabelHeight
        val bottomItemCountByHeight = bottomHeight / maxLabelHeight
        var topItemCount =
          topItemCountByHeight
            .let { if (maxTopItemCount != null) it.coerceAtMost(maxTopItemCount) else it }
            .toInt()
        var bottomItemCount =
          bottomItemCountByHeight
            .let { if (maxBottomItemCount != null) it.coerceAtMost(maxBottomItemCount) else it }
            .toInt()
        if (requestedItemCount == null || topItemCount + bottomItemCount + 1 < requestedItemCount) {
          val isTopNotDenser = topItemCount / topHeight <= bottomItemCount / bottomHeight
          val isTopFillable = topItemCountByHeight - topItemCount >= 1
          val isBottomFillable = bottomItemCountByHeight - bottomItemCount >= 1
          if (isTopFillable && (isTopNotDenser || !isBottomFillable)) {
            topItemCount++
          } else if (isBottomFillable) {
            bottomItemCount++
          }
        }
        if (topItemCount != 0) {
          val step = yRange.maxY / topItemCount
          repeat(topItemCount) { values += (it + 1) * step }
        }
        if (bottomItemCount != 0) {
          val step = yRange.minY / bottomItemCount
          repeat(bottomItemCount) { values += (it + 1) * step }
        }
        return values
      }

      override fun insetsRequired(context: CartesianMeasureContext): Boolean =
        context.getCountOrThrow() != 0
    }
  }
}
