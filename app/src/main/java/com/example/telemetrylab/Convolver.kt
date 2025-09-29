package com.example.telemetrylab

class Convolver {

    fun convolve2D(input: Array<FloatArray>, kernel: Array<FloatArray>): Array<FloatArray> {
        val kernelSize = kernel.size
        val kernelRadius = kernelSize / 2
        val rows = input.size
        val cols = input[0].size

        val output = Array(rows) { FloatArray(cols) }

        for (i in 0 until rows) {
            for (j in 0 until cols) {
                var sum = 0f
                for (ki in 0 until kernelSize) {
                    for (kj in 0 until kernelSize) {
                        val row = i + ki - kernelRadius
                        val col = j + kj - kernelRadius
                        if (row in 0 until rows && col in 0 until cols) {
                            sum += input[row][col] * kernel[ki][kj]
                        }
                    }
                }
                output[i][j] = sum
            }
        }

        return output
    }
}
