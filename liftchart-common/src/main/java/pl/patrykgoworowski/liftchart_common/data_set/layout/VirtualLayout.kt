package pl.patrykgoworowski.liftchart_common.data_set.layout

import android.graphics.RectF
import pl.patrykgoworowski.liftchart_common.axis.AxisManager
import pl.patrykgoworowski.liftchart_common.data_set.DataSetRenderer
import pl.patrykgoworowski.liftchart_common.data_set.entry.collection.EntriesModel
import pl.patrykgoworowski.liftchart_common.dimensions.Dimensions
import pl.patrykgoworowski.liftchart_common.dimensions.floatDimensions

public open class VirtualLayout(
    var isLTR: Boolean
) {

    val axesDimensions: Dimensions<Float> = floatDimensions()
    public open fun <Model> getMeasuredWidth(
        dataSet: DataSetRenderer<Model>,
        model: Model,
        axisManager: AxisManager,
    ): Int =
        dataSet
            .getMeasuredWidth(model)
            .plus(axisManager.getAxisWidth(dataSet.getAxisModel(model)).toInt())

    public open fun <Model: EntriesModel> setBounds(
        contentBounds: RectF,
        dataSet: DataSetRenderer<Model>,
        model: Model,
        axisManager: AxisManager,
    ) {
        val axisModel = dataSet.getAxisModel(model)
        axisManager.getAxesDimensions(axesDimensions, axisModel)
        axisManager.isLTR = isLTR

        dataSet.setBounds(
            left = contentBounds.left + axesDimensions.getLeft(isLTR),
            top = contentBounds.top + axesDimensions.top,
            right = contentBounds.right - axesDimensions.getRight(isLTR),
            bottom = contentBounds.bottom - axesDimensions.bottom
        )

        axisManager.setAxesBounds(contentBounds, dataSet.bounds, axisModel)
    }

}