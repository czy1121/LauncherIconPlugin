package ezy.plugin.launchericon

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs

import javax.imageio.ImageIO
import java.awt.*
import java.awt.image.BufferedImage

class LauncherIconTask extends DefaultTask {

    @InputFiles
    FileCollection sources

    @Input
    String launcher;

    @Input
    String text

    @OutputDirectory
    File target

    @TaskAction
    def generate(IncrementalTaskInputs inputs) {

        for (File input : sources) {
            def dpi = input.toPath().parent.toFile().name
            def parent = new File(target, dpi);
            def output = new File(parent, launcher)
//            println(output.toString())
            try {
                parent.mkdirs()
                output.createNewFile()
                process(input, output, densityOf(dpi), text)
            } catch (Exception e) {
                e.printStackTrace()
            }
        }
    }
    static String[] sDpiNames = ["mipmap-mdpi", "mipmap-hdpi", "mipmap-xhdpi", "mipmap-xxhdpi", "mipmap-xxxhdpi", "drawable-mdpi", "drawable-hdpi", "drawable-xhdpi", "drawable-xxhdpi", "drawable-xxxhdpi"];
    static float[] sDpiValues = [1, 1.5f, 2, 3, 4, 1, 1.5f, 2, 3, 4];

    static float densityOf(String path) {
        int index = sDpiNames.findIndexOf { it -> it == path};
        return index > -1 ? sDpiValues[index] : 1;
    }

    static void process(File input, File output, float density, String text) {


        String[] lines = text.split("\n");
        int lineHeight = 5 * density;
        int paddingTop = 2 * density;
        int paddingBottom = 4 * density


        BufferedImage launcher = ImageIO.read(input);
        Graphics2D g = launcher.createGraphics();

        int width = launcher.getWidth();
        int height = launcher.getHeight();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, lineHeight));


        int textHeight = lineHeight * lines.length;
        int textY = height - textHeight - paddingBottom;

        // fill background
        g.setColor(new Color(0, 0, 0, 0x88));
        g.fillRect(0, height - textHeight - paddingTop - paddingBottom, width, width);

        // draw text
        g.setColor(Color.white);
        for (String line : lines) {
            int lineWidth = g.getFontMetrics().stringWidth(line);
            g.drawString(line, (width - lineWidth) / 2, textY += lineHeight);
        }

        // save to file
        ImageIO.write(launcher, "PNG", output);
    }
}