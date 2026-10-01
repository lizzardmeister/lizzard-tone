package com.lizzardtone.app

import androidx.annotation.RawRes

enum class ReferenceNote(val label: String, val frequencyHz: Double, @RawRes val soundRes: Int) {
    C4("Dó4", 261.625565, R.raw.c4),
    E4("Mi4", 329.627557, R.raw.e4),
    G4("Sol4", 391.995436, R.raw.g4),
    A4("Lá4", 440.0, R.raw.a4);

    companion object {
        fun fromId(id: String?): ReferenceNote? = entries.firstOrNull { it.name == id }
    }
}
