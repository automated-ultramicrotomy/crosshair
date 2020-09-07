package de.embl.schwab.crosshair.ui.command;

import bdv.util.BdvFunctions;
import bdv.util.BdvStackSource;
import de.embl.cba.bdv.utils.sources.LazySpimSource;
import de.embl.schwab.crosshair.Crosshair;
import ij3d.Content;
import ij3d.Image3DUniverse;
import net.imglib2.type.numeric.ARGBType;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import static de.embl.cba.tables.ij3d.UniverseUtils.addSourceToUniverse;

@Plugin(type = Command.class, menuPath = "Plugins>Crosshair>Target Bdv File" )
public class OpenCrosshairFromBdvXmlCommand implements Command {

    String bdvXmlFilePath;

    // Can't use @Parameter for File, as this seems to affect the appearance of the Swing panels for crosshair,
    // instead use JFileChooser

    @Override
    public void run() {

        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("xml", "xml"));
        int returnVal = chooser.showOpenDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            bdvXmlFilePath = chooser.getSelectedFile().getAbsolutePath();

            final LazySpimSource imageSource = new LazySpimSource("raw", bdvXmlFilePath);
            BdvStackSource bdvStackSource = BdvFunctions.show(imageSource, 1);
            bdvStackSource.setDisplayRange(0, 255);

            Image3DUniverse universe = new Image3DUniverse();
            universe.show();

            String unit = imageSource.getVoxelDimensions().unit();

            // Set to arbitrary colour
            ARGBType colour =  new ARGBType( ARGBType.rgba( 0, 0, 0, 0 ) );
            Content imageContent = addSourceToUniverse(universe, imageSource, 300 * 300 * 300, Content.VOLUME, colour, 0.7f, 0, 255 );
            // Reset colour to default for 3D viewer
            imageContent.setColor(null);

            new Crosshair(bdvStackSource, universe, imageContent, unit);

        }
    }

    public static void main( String[] args ) {

        final LazySpimSource imageSource = new LazySpimSource("raw", "C:\\Users\\meechan\\Documents\\test_images\\Flipped_imaged_before.xml");
        BdvStackSource bdvStackSource = BdvFunctions.show(imageSource, 1);
        bdvStackSource.setDisplayRange(0, 255);

        Image3DUniverse universe = new Image3DUniverse();
        universe.show();

        String unit = imageSource.getVoxelDimensions().unit();

        // Set to arbitrary colour
        ARGBType colour =  new ARGBType( ARGBType.rgba( 0, 0, 0, 0 ) );
        Content imageContent = addSourceToUniverse(universe, imageSource, 300 * 300 * 300, Content.VOLUME, colour, 0.7f, 0, 255 );
        // Reset colour to default for 3D viewer
        imageContent.setColor(null);

        new Crosshair(bdvStackSource, universe, imageContent, unit);
    }
}
