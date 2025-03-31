package develop.openexamples;

import com.formdev.flatlaf.FlatLightLaf;
import de.embl.schwab.crosshair.ui.command.OpenCrosshairFromBdvXmlCommand;
import de.embl.schwab.crosshair.ui.command.OpenCrosshairFromOmeZarrLocal;

import javax.swing.*;
import java.io.File;

public class OpenExampleZarrLocalBlock {
    public void open() {

        // Match Fiji's new default look and feel
        try {
            UIManager.setLookAndFeel(new FlatLightLaf() );
        } catch (UnsupportedLookAndFeelException e) {
            throw new RuntimeException(e);
        }

        OpenCrosshairFromOmeZarrLocal command = new OpenCrosshairFromOmeZarrLocal();
        ClassLoader classLoader = this.getClass().getClassLoader();
        File headFile = new File(classLoader.getResource("exampleBlock.ome.zarr").getFile());
        command.omeZarrFilePath = headFile.getAbsolutePath();
        command.openPathInCrosshair();
    }

    public static void main( String[] args )
    {
        new OpenExampleZarrLocalBlock().open();
    }
}
