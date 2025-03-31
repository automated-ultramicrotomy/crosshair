package develop.openexamples;

import com.formdev.flatlaf.FlatLightLaf;
import de.embl.schwab.crosshair.ui.command.OpenCrosshairFromBdvXmlCommand;

import javax.swing.*;
import java.io.File;

public class OpenExampleBdvXmlBlock {

    public void open() {

        // Match Fiji's new default look and feel
        try {
            UIManager.setLookAndFeel(new FlatLightLaf() );
        } catch (UnsupportedLookAndFeelException e) {
            throw new RuntimeException(e);
        }

        OpenCrosshairFromBdvXmlCommand command = new OpenCrosshairFromBdvXmlCommand();
        ClassLoader classLoader = this.getClass().getClassLoader();
        File headFile = new File(classLoader.getResource("exampleBlock.xml").getFile());
        command.bdvXmlFilePath = headFile.getAbsolutePath();
        command.openPathInCrosshair();
    }

    public static void main( String[] args )
    {
        new OpenExampleBdvXmlBlock().open();
    }
}
