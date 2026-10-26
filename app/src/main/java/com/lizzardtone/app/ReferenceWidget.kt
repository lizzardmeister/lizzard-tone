package com.lizzardtone.app

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartService
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

class ReferenceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ReferenceWidget()
}

class ReferenceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        TuningSettings.get(context)
        provideContent {
            // Lido como estado do Compose: a sessão do Glance fica aberta por um tempo e
            // só recompõe; um valor lido fora daqui ficaria congelado na recomposição.
            val state by TuningSettings.state.collectAsState()
            GlanceTheme {
                WidgetContent(state)
            }
        }
    }
}

/** Botões de oitava: soma [DELTA_KEY] à oitava atual. */
class AdjustOctaveAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val delta = parameters[DELTA_KEY] ?: return
        TuningSettings.update(context) {
            it.copy(octave = (it.octave + delta).coerceIn(Notes.MIN_OCTAVE, Notes.MAX_OCTAVE))
        }
    }

    companion object {
        val DELTA_KEY = ActionParameters.Key<Int>("delta")
    }
}

/** Botão de tom do instrumento: avança para o próximo da lista. */
class CycleTranspositionAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        TuningSettings.update(context) { it.copy(transposition = it.transposition.next()) }
    }
}

@Composable
private fun WidgetContent(state: TuningState) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(16.dp)
            .padding(8.dp),
    ) {
        Header(state)
        Spacer(GlanceModifier.height(6.dp))
        KeyRow(state, Notes.SHARPS_ROW, sharps = true, modifier = GlanceModifier.defaultWeight())
        Spacer(GlanceModifier.height(4.dp))
        KeyRow(state, Notes.NATURALS, sharps = false, modifier = GlanceModifier.defaultWeight())
    }
}

@Composable
private fun Header(state: TuningState) {
    val status = when {
        state.isSounding && state.activeWrittenMidi != state.activeSoundingMidi ->
            "${Notes.name(state.activeWrittenMidi!!)} → ${Notes.name(state.activeSoundingMidi!!)}"
        state.isSounding -> Notes.name(state.activeSoundingMidi!!)
        else -> "Oitava ${state.octave}"
    }
    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        SmallButton(
            text = "−",
            onClick = actionRunCallback<AdjustOctaveAction>(actionParametersOf(AdjustOctaveAction.DELTA_KEY to -1)),
        )
        Text(
            text = status,
            modifier = GlanceModifier.defaultWeight(),
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            ),
        )
        SmallButton(
            text = "+",
            onClick = actionRunCallback<AdjustOctaveAction>(actionParametersOf(AdjustOctaveAction.DELTA_KEY to 1)),
        )
        Spacer(GlanceModifier.width(6.dp))
        SmallButton(
            text = "Tom ${state.transposition.label}",
            onClick = actionRunCallback<CycleTranspositionAction>(),
        )
    }
}

@Composable
private fun SmallButton(text: String, onClick: androidx.glance.action.Action) {
    Button(
        text = text,
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = GlanceTheme.colors.secondaryContainer,
            contentColor = GlanceTheme.colors.onSecondaryContainer,
        ),
    )
}

@Composable
private fun KeyRow(state: TuningState, pitchClasses: List<Int?>, sharps: Boolean, modifier: GlanceModifier) {
    val context = androidx.glance.LocalContext.current
    Row(modifier = modifier.fillMaxWidth()) {
        pitchClasses.forEach { pitchClass ->
            if (pitchClass == null) {
                Spacer(GlanceModifier.defaultWeight())
                return@forEach
            }
            val written = Notes.midi(pitchClass, state.octave)
            val active = state.activeWrittenMidi == written
            Button(
                text = Notes.NAMES[pitchClass],
                onClick = actionStartService(
                    TonePlaybackService.toggleIntent(context, written),
                    isForegroundService = true,
                ),
                modifier = GlanceModifier.defaultWeight().fillMaxHeight().padding(horizontal = 1.dp),
                colors = keyColors(active, sharps),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun keyColors(active: Boolean, sharps: Boolean) = when {
    active -> ButtonDefaults.buttonColors(
        backgroundColor = ColorProvider(Color(0xFFF2B33D)),
        contentColor = ColorProvider(Color(0xFF1C1300)),
    )
    sharps -> ButtonDefaults.buttonColors(
        backgroundColor = ColorProvider(Color(0xFF2B2B2B)),
        contentColor = ColorProvider(Color(0xFFF5F5F5)),
    )
    else -> ButtonDefaults.buttonColors(
        backgroundColor = ColorProvider(Color(0xFFFAFAFA)),
        contentColor = ColorProvider(Color(0xFF1A1A1A)),
    )
}
