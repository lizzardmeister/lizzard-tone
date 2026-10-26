package com.lizzardtone.app

import java.util.Locale
import kotlin.math.pow

/** Nomes, números MIDI e frequências das notas. Dó central = MIDI 60 = Dó4. */
object Notes {
    val NAMES = listOf("Dó", "Dó♯", "Ré", "Ré♯", "Mi", "Fá", "Fá♯", "Sol", "Sol♯", "Lá", "Lá♯", "Si")

    /** Classes de altura das teclas brancas, na ordem do teclado. */
    val NATURALS = listOf(0, 2, 4, 5, 7, 9, 11)

    /**
     * Linha das teclas pretas, alinhada coluna a coluna com [NATURALS]:
     * cada sustenido fica acima da tecla branca que ele altera. `null` é vão.
     */
    val SHARPS_ROW = listOf(1, 3, null, 6, 8, 10, null)

    const val MIN_OCTAVE = 2
    const val MAX_OCTAVE = 6

    fun midi(pitchClass: Int, octave: Int): Int = (octave + 1) * 12 + pitchClass

    fun pitchClass(midi: Int): Int = Math.floorMod(midi, 12)

    fun octave(midi: Int): Int = Math.floorDiv(midi, 12) - 1

    fun name(midi: Int): String = NAMES[pitchClass(midi)] + octave(midi)

    fun frequency(midi: Int, a4Hz: Double): Double = a4Hz * 2.0.pow((midi - 69) / 12.0)

    fun formatHz(hz: Double): String = String.format(Locale("pt", "BR"), "%.1f Hz", hz)
}

/**
 * Tom do instrumento. A tecla rotulada "Dó" soa a nota [label] do instrumento:
 * [semitones] é o deslocamento da nota escrita para a nota real.
 */
enum class Transposition(val label: String, val semitones: Int, val instruments: String) {
    C("Dó", 0, "piano, voz, flauta, oboé, violino"),
    BB("Si♭", -2, "clarinete, trompete, sax soprano e tenor"),
    A("Lá", -3, "clarinete em Lá"),
    F("Fá", -7, "trompa, corne inglês"),
    EB("Mi♭", -9, "sax alto e barítono");

    fun next(): Transposition = entries[(ordinal + 1) % entries.size]

    companion object {
        fun fromName(name: String?): Transposition = entries.firstOrNull { it.name == name } ?: C
    }
}
