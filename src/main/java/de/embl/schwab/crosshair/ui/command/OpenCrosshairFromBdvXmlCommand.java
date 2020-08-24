package de.embl.schwab.crosshair.ui.command;

import bdv.BigDataViewer;
import bdv.ij.BigDataViewerPlugIn;
import bdv.ij.util.ProgressWriterIJ;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import bdv.util.*;
import bdv.viewer.Source;
import bdv.viewer.SynchronizedViewerState;
import bdv.viewer.ViewerOptions;
import bdv.viewer.state.ViewerState;
import bdv.viewer.state.XmlIoViewerState;
import de.embl.schwab.crosshair.Crosshair;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij3d.Content;
import ij3d.Image3DUniverse;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.RealUnsignedByteConverter;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.Parameter;
import mpicbg.spim.data.XmlIoSpimData;

import java.io.File;
import java.util.List;

import static de.embl.cba.tables.ij3d.UniverseUtils.addSourceToUniverse;

@Plugin(type = Command.class, menuPath = "Plugins>Crosshair>Open Bdv File" )
public class OpenCrosshairFromBdvXmlCommand implements Command {

    private BigDataViewer bdv;

    @Parameter ( label = "Select Bdv Xml File:" )
    public File bdvXml;

    @Override
    public void run() {
        try {
            // Open xml as here: https://forum.image.sc/t/imglyb-bigdataviewer/28390/4
            final SpimDataMinimal spimData = new XmlIoSpimDataMinimal().load( bdvXml.getAbsolutePath() );
            BdvStackSource bdvStackSource = BdvFunctions.show( spimData ).get(0);
            bdvStackSource.setDisplayRange(0, 255);
        } catch (SpimDataException e) {
            e.printStackTrace();
        }

        Image3DUniverse universe = new Image3DUniverse();
        universe.show();

        // as here: https://forum.image.sc/t/bigdataviewer-bigdataserver-get-an-imageplus-image-of-the-current-slice/20138/7
        SynchronizedViewerState viewerState = bdv.getViewer().state();
        Source currentSource = viewerState.getSources().get(0).getSpimSource();
        addSourceToUniverse(universe, currentSource, 300 * 300 * 300, Content.VOLUME, null, 0.7f, 0, 255 );

        new Crosshair(bdvStackSource, universe);
    }

    public static void main( String[] args ) {
        try {
            // Open xml as here: https://forum.image.sc/t/imglyb-bigdataviewer/28390/4
            final SpimDataMinimal spimData = new XmlIoSpimDataMinimal().load( "C:\\Users\\meechan\\Documents\\test_3d_larger_anisotropic_xml\\test_3d_larger_anisotropic_xml.xml");
            BdvStackSource bdvStackSource = BdvFunctions.show( spimData ).get(0);
            bdvStackSource.setDisplayRange(0, 255);
        } catch (SpimDataException e) {
            e.printStackTrace();
        }
    }
}
