package develop.openexamples;

import de.embl.schwab.crosshair.ui.command.OpenCrosshairFromBdvXmlCommand;

import java.io.File;

public class OpenExampleBlock {

    public void open() {
        OpenCrosshairFromBdvXmlCommand command = new OpenCrosshairFromBdvXmlCommand();
        ClassLoader classLoader = this.getClass().getClassLoader();
        File headFile = new File(classLoader.getResource("exampleBlock.xml").getFile());
        command.bdvXmlFilePath = headFile.getAbsolutePath();
        command.openPathInCrosshair();
    }

    public static void main( String[] args )
    {
        new OpenExampleBlock().open();
    }
}
