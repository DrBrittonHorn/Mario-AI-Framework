package levelGenerators.ttWFC;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import javax.imageio.ImageIO;

public class Helper {
    public static class BitmapData {
        public final int[] pixels;
        public final int width, height;
        public BitmapData(int[] pixels, int width, int height) {
            this.pixels = pixels;
            this.width = width;
            this.height = height;
        }
    }
    public static int Random(double[] weights, double r)
    {
        double sum = 0;
        for (int i = 0; i < weights.length; i++) sum += weights[i];
        double threshold = r * sum;

        double partialSum = 0;
        for (int i = 0; i < weights.length; i++)
        {
            partialSum += weights[i];
            if (partialSum >= threshold) return i;
        }
        return 0;
    }
    public static BitmapData loadBitmap(String filename) throws IOException {
        BufferedImage img = ImageIO.read(new File(filename));
        int w = img.getWidth();
        int h = img.getHeight();
        int[] pixels = new int[w * h];
        // getRGB fills the int[] in row-major order with 0xAARRGGBB values
        img.getRGB(0, 0, w, h, pixels, 0, w);
        return new BitmapData(pixels, w, h);
    }

    /** Writes an ARGB int[] (length = width*height) to a PNG file. */
    public static void saveBitmap(char[] chars, int width, int height, String filename) throws IOException {
        // BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        // out.setRGB(0, 0, width, height, pixels, 0, width);
        // ImageIO.write(out, "PNG", new File(filename));
        try (PrintWriter out = new PrintWriter(new FileWriter(filename + ".txt"))) {
        for (int y = 0; y < height; y++) {
            out.print(new String(chars, y * width, width));
            out.println();
        }
    }
    }
    
}
