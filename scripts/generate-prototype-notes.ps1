$ErrorActionPreference = 'Stop'

$outputDir = Join-Path $PSScriptRoot '..\app\src\main\res\raw'
New-Item -ItemType Directory -Path $outputDir -Force | Out-Null

$notes = @(
    @{ Name = 'c4'; Frequency = 261.625565 },
    @{ Name = 'e4'; Frequency = 329.627557 },
    @{ Name = 'g4'; Frequency = 391.995436 },
    @{ Name = 'a4'; Frequency = 440.0 }
)

$sampleRate = 44100
$seconds = 1.35
$sampleCount = [int]($sampleRate * $seconds)

foreach ($note in $notes) {
    $path = Join-Path $outputDir ($note.Name + '.wav')
    $stream = [System.IO.File]::Create($path)
    $writer = [System.IO.BinaryWriter]::new($stream)
    try {
        $dataBytes = $sampleCount * 2
        $writer.Write([System.Text.Encoding]::ASCII.GetBytes('RIFF'))
        $writer.Write([int](36 + $dataBytes))
        $writer.Write([System.Text.Encoding]::ASCII.GetBytes('WAVE'))
        $writer.Write([System.Text.Encoding]::ASCII.GetBytes('fmt '))
        $writer.Write([int]16)
        $writer.Write([int16]1)
        $writer.Write([int16]1)
        $writer.Write([int]$sampleRate)
        $writer.Write([int]($sampleRate * 2))
        $writer.Write([int16]2)
        $writer.Write([int16]16)
        $writer.Write([System.Text.Encoding]::ASCII.GetBytes('data'))
        $writer.Write([int]$dataBytes)

        for ($i = 0; $i -lt $sampleCount; $i++) {
            $t = $i / $sampleRate
            $attack = [Math]::Min(1.0, $t / 0.008)
            $release = [Math]::Min(1.0, ($seconds - $t) / 0.16)
            $envelope = $attack * $release * [Math]::Exp(-1.5 * $t)
            $phase = 2.0 * [Math]::PI * $note.Frequency * $t
            $tone = [Math]::Sin($phase) + 0.35 * [Math]::Sin(2 * $phase) + 0.16 * [Math]::Sin(3 * $phase)
            $sample = [int16][Math]::Round(18000 * $envelope * $tone / 1.51)
            $writer.Write($sample)
        }
    } finally {
        $writer.Dispose()
        $stream.Dispose()
    }
}
