package seamcarving

import java.awt.Color
import java.awt.Component
import java.awt.Graphics
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JFrame
import kotlin.math.min
import kotlin.math.sqrt

var inFile = ""
var outFile = ""
var dWidth = 0
var dHeight = 0

fun main(args: Array<String>) {
    for (i in 0..args.lastIndex) {
        if (args[i] == "-in") inFile = args[i + 1]
        if (args[i] == "-out") outFile = args[i + 1]
        if (args[i] == "-width") dWidth = args[i + 1].toInt()
        if (args[i] == "-height") dHeight = args[i + 1].toInt()
    }
    try {
        val myImage = Image(ImageIO.read(File(inFile)))
        var newImg = myImage
        if (dWidth > 0) {
            repeat(dWidth) {
                newImg = newImg.verticalSeam()
            }
        }
        if (dHeight > 0) {
            repeat(dHeight) {
                newImg = newImg.horizontalSeam()
            }
        }

        ImageIO.write(newImg.img, "png", (File(outFile)))
        println("Message saved in $outFile image.")
    } catch (e: Exception) {
        println(e.message)
    }
}

class Image(val img: BufferedImage) {
    val height = img.height
    val width = img.width
    val arrE = Array(height) { DoubleArray(width) { 0.0 } }
    val maskSeam = Array(height) { IntArray(width) { 1 } }

    fun horizontalSeam(): Image {
        var new = img.rotate90()
        new = new.verticalSeam()
        new = new.img.rotate270()
        return new
    }

    fun verticalSeam(): Image {
        setEnergy()
        // calc min energy in arrE
        var dE = 0.0
        for (y in 1 until height) {
            for (x in 0 until width) {
                arrE[y][x] += arrE[y - 1][minNeighbor(y, x)]
            }
        }
        // find the min element in the last raw
        var minX = 0
        val minY = height - 1
        var minE = arrE[minY][minX]
        for (x in 1 until width) {
            if (arrE[minY][x] < minE) {
                minE = arrE[minY][x]
                minX = x
            }
        }
        // go backward and save seam as minPath in maskSeam
        maskSeam[minY][minX] = 0
        var x = minX
        for (y in height - 1 downTo 1) {
            x = minNeighbor(y, x)
            maskSeam[y - 1][x] = 0
        }
        // delete seam
        val new = BufferedImage(width - 1, height, BufferedImage.TYPE_INT_RGB)
        var x1 = 0
        for (y in 0 until height) {
            x1 = 0
            for (x in 0 until width) {
                if (maskSeam[y][x] == 1) {
                    new.setRGB(x1, y, img.getRGB(x, y))
                    x1++
                }
            }
        }
        return Image(new)
    }

    fun setEnergy() {
        var e = 0.0
        for (y in 0 until img.height) {
            for (x in 0 until img.width) {
                e = sqrt(this.xGrad(x, y).toDouble() + this.yGrad(x, y))
                arrE[y][x] = e
            }
        }
    }

    fun minNeighbor(y:Int, x: Int): Int {
        val row = arrE[y - 1]
        when (x) {
            0 -> return if (row[x] < row[x + 1]) x else x + 1
            img.width - 1 -> return if (row[x] < row[x - 1]) x else x - 1
            else -> {
                val v = minOf(row[x - 1], row[x], row[x + 1])
                when (v) {
                    row[x - 1] -> return x - 1
                    row[x + 1] -> return x + 1
                    else -> return x
                }
            }
        }
    }

    fun xGrad(x: Int, y: Int): Int {
     val x0 = x.coerceIn(1, img.width - 2)
     val c1 = Color(img.getRGB(x0 - 1, y))
     val c2 = Color(img.getRGB(x0 + 1, y))
     return ((c2.red - c1.red) * (c2.red - c1.red) +
             (c2.green - c1.green) * (c2.green - c1.green) +
             (c2.blue - c1.blue) * (c2.blue - c1.blue))
    }

    fun yGrad(x: Int, y: Int): Int {
     val y0 = y.coerceIn(1, img.height - 2)
     val c1 = Color(img.getRGB(x, y0 - 1))
     val c2 = Color(img.getRGB(x, y0 + 1))
     return ((c2.red - c1.red) * (c2.red - c1.red) +
             (c2.green - c1.green) * (c2.green - c1.green) +
             (c2.blue - c1.blue) * (c2.blue - c1.blue))
    }

    fun BufferedImage.rotate90(): Image {
        val image = BufferedImage(height, width, BufferedImage.TYPE_INT_RGB)
        for (y in 0 until height) {
            for (x in 0 until width) {
                image.setRGB(height - y - 1, x, this.getRGB(x, y))
            }
        }
        return Image(image)
    }

    fun BufferedImage.rotate270(): Image {
        val image = BufferedImage(height, width, BufferedImage.TYPE_INT_RGB)
        for (y in 0 until height) {
            for (x in 0 until width) {
                image.setRGB(y, width - x - 1, this.getRGB(x, y))
            }
        }
        return Image(image)
    }
 }