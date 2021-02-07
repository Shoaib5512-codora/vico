package pl.patrykgoworowski.liftchart_view.data_set.bar

import android.graphics.Canvas
import android.graphics.RectF
import pl.patrykgoworowski.liftchart_common.AnyEntry
import pl.patrykgoworowski.liftchart_common.data_set.bar.MergeMode
import pl.patrykgoworowski.liftchart_common.data_set.bar.MergedBarDataSetRenderer
import pl.patrykgoworowski.liftchart_common.data_set.bar.path.BarPathCreator
import pl.patrykgoworowski.liftchart_common.data_set.entry.collection.multi.MultiEntriesModel
import pl.patrykgoworowski.liftchart_common.data_set.entry.collection.multi.emptyMultiEntriesModel
import pl.patrykgoworowski.liftchart_common.defaults.DEF_BAR_SPACING
import pl.patrykgoworowski.liftchart_common.defaults.DEF_BAR_WIDTH
import pl.patrykgoworowski.liftchart_common.defaults.DEF_MERGED_BAR_INNER_SPACING
import pl.patrykgoworowski.liftchart_view.common.UpdateRequestListener
import pl.patrykgoworowski.liftchart_view.data_set.ViewDataSetRenderer
import pl.patrykgoworowski.liftchart_view.extension.dp

class MergedBarDataSet<Entry: AnyEntry>(
    barWidth: Float = DEF_BAR_WIDTH.dp,
    barSpacing: Float = DEF_BAR_SPACING.dp,
    barInnerSpacing: Float = DEF_MERGED_BAR_INNER_SPACING.dp,
    mergeMode: MergeMode = MergeMode.Stack,
    colors: List<Int> = emptyList(),
    barPathCreators: List<BarPathCreator> = emptyList(),
) : MergedBarDataSetRenderer<Entry>(colors, barWidth, barSpacing, barInnerSpacing), ViewDataSetRenderer {

    private val listeners = ArrayList<UpdateRequestListener>()

    var model: MultiEntriesModel<Entry> = emptyMultiEntriesModel()
        set(value) {
            field = value
            listeners.forEach { it() }
        }

    init {
        setColors(colors)
        this.barPathCreators.addAll(barPathCreators)
        this.groupMode = mergeMode
    }

    override fun setBounds(bounds: RectF) {
        setBounds(bounds, model)
    }

    override fun draw(canvas: Canvas) {
        draw(canvas, model)
    }

    override fun addListener(listener: UpdateRequestListener) {
        listeners += listener
    }

    override fun removeListener(listener: UpdateRequestListener) {
        listeners -= listener
    }

    override fun getMeasuredWidth(): Int = getMeasuredWidth(model)
}