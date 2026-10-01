package com.lizzardtone.app

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartService
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.compose.ui.unit.dp

class ReferenceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ReferenceWidget()
}

class ReferenceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: android.content.Context, id: GlanceId) {
        provideContent {
            WidgetContent()
        }
    }
}

@Composable
private fun WidgetContent() {
    val context = LocalContext.current
    Column(modifier = GlanceModifier.fillMaxSize().padding(8.dp)) {
        Text("Notas de referência · Lá4 440 Hz")
        Spacer(GlanceModifier.height(6.dp))
        ReferenceNote.entries.chunked(2).forEach { rowNotes ->
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                rowNotes.forEach { note ->
                    val intent = Intent(context, NotePlaybackService::class.java)
                        .setAction(NotePlaybackService.ACTION_PLAY)
                        .setData(Uri.parse("lizzardtone://note/${note.name}"))
                        .putExtra(NotePlaybackService.EXTRA_NOTE, note.name)
                    Button(
                        text = note.label,
                        onClick = actionStartService(intent, isForegroundService = true),
                        modifier = GlanceModifier.defaultWeight(),
                    )
                }
            }
            Spacer(GlanceModifier.height(4.dp))
        }
    }
}
