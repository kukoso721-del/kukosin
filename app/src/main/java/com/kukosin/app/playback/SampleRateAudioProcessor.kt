package com.kukosin.app.playback

import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer

@UnstableApi
class SampleRateAudioProcessor(
    private var targetSampleRateHz: Int = 0,
) : BaseAudioProcessor() {

    fun setTargetSampleRate(sampleRateHz: Int) {
        if (targetSampleRateHz != sampleRateHz) {
            targetSampleRateHz = sampleRateHz
        }
    }

    override fun onConfigure(
        inputAudioFormat: AudioProcessor.AudioFormat,
    ): AudioProcessor.AudioFormat {
        if (targetSampleRateHz <= 0 || inputAudioFormat.sampleRate == targetSampleRateHz) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        return AudioProcessor.AudioFormat(
            targetSampleRateHz,
            inputAudioFormat.channelCount,
            inputAudioFormat.encoding,
        )
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val position = inputBuffer.position()
        val limit = inputBuffer.limit()
        val frameCount = (limit - position) / inputAudioFormat.bytesPerFrame
        if (frameCount <= 0) return

        val outputFrameCount = (frameCount.toLong() * targetSampleRateHz / inputAudioFormat.sampleRate).toInt()
        val outputBytes = outputFrameCount * outputAudioFormat.bytesPerFrame
        val buffer = replaceOutputBuffer(outputBytes)

        val srcStep = inputAudioFormat.sampleRate.toDouble() / targetSampleRateHz
        var srcPos = 0.0
        val bytesPerSample = inputAudioFormat.bytesPerFrame / inputAudioFormat.channelCount

        for (i in 0 until outputFrameCount) {
            val srcIndex = srcPos.toInt().coerceIn(0, frameCount - 1)
            val srcByteOffset = position + srcIndex * inputAudioFormat.bytesPerFrame
            for (c in 0 until inputAudioFormat.channelCount) {
                val sampleOffset = srcByteOffset + c * bytesPerSample
                for (b in 0 until bytesPerSample) {
                    if (sampleOffset + b < limit) {
                        buffer.put(inputBuffer.get(sampleOffset + b))
                    } else {
                        buffer.put(0)
                    }
                }
            }
            srcPos += srcStep
        }
        inputBuffer.position(limit)
    }
}
