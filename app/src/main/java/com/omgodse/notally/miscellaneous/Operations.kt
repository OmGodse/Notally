package com.omgodse.notally.miscellaneous

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Build
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.ContextCompat
import com.google.android.material.chip.ChipGroup
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.RelativeCornerSize
import com.google.android.material.shape.RoundedCornerTreatment
import com.google.android.material.shape.ShapeAppearanceModel
import com.omgodse.notally.BuildConfig
import com.omgodse.notally.R
import com.omgodse.notally.databinding.LabelBinding
import com.omgodse.notally.preferences.TextSize
import com.omgodse.notally.room.Color
import com.omgodse.notally.room.ListItem
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.text.DateFormat

object Operations {

    const val extraCharSequence = "com.omgodse.notally.extra.charSequence"

    fun getLog(app: Application): File {
        val folder = File(app.filesDir, "logs")
        folder.mkdir()
        return File(folder, "Log.v1.txt")
    }

    fun log(app: Application, throwable: Throwable) {
        val file = getLog(app)
        val output = FileOutputStream(file, true)
        val writer = PrintWriter(OutputStreamWriter(output, Charsets.UTF_8))

        val formatter = DateFormat.getDateTimeInstance()
        val time = formatter.format(System.currentTimeMillis())

        writer.println("[Start]")
        throwable.printStackTrace(writer)
        writer.println("Version code : " + BuildConfig.VERSION_CODE)
        writer.println("Version name : " + BuildConfig.VERSION_NAME)
        writer.println("Model : " + Build.MODEL)
        writer.println("Device : " + Build.DEVICE)
        writer.println("Brand : " + Build.BRAND)
        writer.println("Manufacturer : " + Build.MANUFACTURER)
        writer.println("Android : " + Build.VERSION.SDK_INT)
        writer.println("Time : $time")
        writer.println("[End]")

        writer.close()
    }


    fun extractColor(color: Color, context: Context): Int {
        val id = when (color) {
            Color.DEFAULT -> R.color.Default
            Color.CORAL -> R.color.Coral
            Color.ORANGE -> R.color.Orange
            Color.SAND -> R.color.Sand
            Color.STORM -> R.color.Storm
            Color.FOG -> R.color.Fog
            Color.SAGE -> R.color.Sage
            Color.MINT -> R.color.Mint
            Color.DUSK -> R.color.Dusk
            Color.FLOWER -> R.color.Flower
            Color.BLOSSOM -> R.color.Blossom
            Color.CLAY -> R.color.Clay
        }
        return ContextCompat.getColor(context, id)
    }


    fun shareNote(context: Context, title: String, body: CharSequence) {
        val text = body.toString()
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(extraCharSequence, body)
        intent.putExtra(Intent.EXTRA_TEXT, text)
        intent.putExtra(Intent.EXTRA_TITLE, title)
        intent.putExtra(Intent.EXTRA_SUBJECT, title)
        val chooser = Intent.createChooser(intent, null)
        context.startActivity(chooser)
    }


    fun getBody(list: List<ListItem>) = buildString {
        for (item in list) {
            val check = if (item.checked) "[✓]" else "[ ]"
            appendLine("$check ${item.body}")
        }
    }


    fun bindLabels(group: ChipGroup, labels: List<String>, textSize: String) {
        if (labels.isEmpty()) {
            group.visibility = View.GONE
        } else {
            group.visibility = View.VISIBLE
            group.removeAllViews()

            val inflater = LayoutInflater.from(group.context)
            val labelSize = TextSize.getDisplayBodySize(textSize)

            for (label in labels) {
                val view = LabelBinding.inflate(inflater, group, true).root
                view.background = getOutlinedDrawable(group.context)
                view.setTextSize(TypedValue.COMPLEX_UNIT_SP, labelSize)
                view.text = label
            }
        }
    }

    private fun getOutlinedDrawable(context: Context): MaterialShapeDrawable {
        val model = ShapeAppearanceModel.builder()
            .setAllCorners(RoundedCornerTreatment())
            .setAllCornerSizes(RelativeCornerSize(0.5f))
            .build()

        val drawable = MaterialShapeDrawable(model)
        drawable.fillColor = ColorStateList.valueOf(0)
        drawable.strokeWidth = context.resources.displayMetrics.density
        drawable.strokeColor = ContextCompat.getColorStateList(context, R.color.chip_stroke)

        return drawable
    }
}