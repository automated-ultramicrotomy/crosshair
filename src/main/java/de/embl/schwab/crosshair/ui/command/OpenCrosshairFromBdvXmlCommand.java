package de.embl.schwab.crosshair.ui.command;

import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import bdv.util.*;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import de.embl.schwab.crosshair.Crosshair;
import ij3d.Content;
import ij3d.Image3DUniverse;
import mpicbg.spim.data.SpimDataException;
import net.imglib2.type.numeric.ARGBType;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.Parameter;

import java.io.File;

import static de.embl.cba.tables.ij3d.UniverseUtils.addSourceToUniverse;

@Plugin(type = Command.class, menuPath = "Plugins>Crosshair>Open Bdv File" )
public class OpenCrosshairFromBdvXmlCommand implements Command {

    private BdvStackSource bdvStackSource;

    @Parameter ( label = "Select Bdv Xml File:" )
    public File bdvXml;

    @Override
    public void run() {
        try {
            // Open xml as here: https://forum.image.sc/t/imglyb-bigdataviewer/28390/4
            final SpimDataMinimal spimData = new XmlIoSpimDataMinimal().load( bdvXml.getAbsolutePath() );
            bdvStackSource = BdvFunctions.show( spimData ).get(0);
            bdvStackSource.setDisplayRange(0, 255);
        } catch (SpimDataException e) {
            e.printStackTrace();
        }

        Image3DUniverse universe = new Image3DUniverse();
        universe.show();

        // as here: https://forum.image.sc/t/bigdataviewer-bigdataserver-get-an-imageplus-image-of-the-current-slice/20138/7
        SourceAndConverter currentSourceConverter = (SourceAndConverter) bdvStackSource.getSources().get(0);
        Source currentSource = currentSourceConverter.getSpimSource();
        // Set to arbitrary colour
        ARGBType colour =  new ARGBType( ARGBType.rgba( 0, 0, 0, 0 ) );
        Content imageContent = addSourceToUniverse(universe, currentSource, 300 * 300 * 300, Content.VOLUME, colour, 0.7f, 0, 255 );
        // Reset colour to default for 3D viewer
        imageContent.setColor(null);

        new Crosshair(bdvStackSource, universe, imageContent);
    }

    public static void main( String[] args ) {
        try {
            // Open xml as here: https://forum.image.sc/t/imglyb-bigdataviewer/28390/4
            final SpimDataMinimal spimData = new XmlIoSpimDataMinimal().load( "C:\\Users\\meechan\\Documents\\test_3d_larger_anisotropic_xml\\test_3d_larger_anisotropic_xml.xml" );
            BdvStackSource bdvStackSource = BdvFunctions.show( spimData ).get(0);
            bdvStackSource.setDisplayRange(0, 255);
            Image3DUniverse universe = new Image3DUniverse();
            universe.show();

            // as here: https://forum.image.sc/t/bigdataviewer-bigdataserver-get-an-imageplus-image-of-the-current-slice/20138/7
            SourceAndConverter currentSourceConverter = (SourceAndConverter) bdvStackSource.getSources().get(0);
            Source currentSource = currentSourceConverter.getSpimSource();
            // Set to arbitrary colour
            ARGBType colour =  new ARGBType( ARGBType.rgba( 0, 0, 0, 0 ) );
            Content imageContent = addSourceToUniverse(universe, currentSource, 300 * 300 * 300, Content.VOLUME, colour, 0.7f, 0, 255 );
            // Reset colour to default for 3D viewer
            imageContent.setColor(null);

            new Crosshair(bdvStackSource, universe, imageContent);
        } catch (SpimDataException e) {
            e.printStackTrace();
        }
    }
}
