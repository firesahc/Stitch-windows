package soko.ekibun.stitch.capture

import java.awt.Rectangle
import java.awt.Robot
import java.awt.Toolkit
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class ScreenCapture {

    private val robot = Robot()

    fun captureFullScreen(): BufferedImage {
        val screenSize = Toolkit.getDefaultToolkit().screenSize
        return robot.createScreenCapture(Rectangle(screenSize))
    }

    fun captureRegion(x: Int, y: Int, width: Int, height: Int): BufferedImage {
        return robot.createScreenCapture(Rectangle(x, y, width, height))
    }

    fun saveCapture(image: BufferedImage, file: File) {
        ImageIO.write(image, "png", file)
    }
}
