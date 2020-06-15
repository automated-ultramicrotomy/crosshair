package de.embl.cba.target;

import bdv.util.BdvFunctions;
import bdv.util.BdvStackSource;
import customnode.CustomTriangleMesh;
import ij.ImagePlus;
import ij.plugin.FolderOpener;
import ij3d.Content;
import ij3d.Image3DUniverse;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import org.scijava.vecmath.Point3f;

import java.util.ArrayList;

public class Universe3DExplorer
{
	public static final String INPUT_FOLDER = "/Users/tischer/Documents/targeting_3D/src/test/resources/target/mri-stack-slices/";

	public Universe3DExplorer()
	{
		final ImagePlus imagePlus = FolderOpener.open( INPUT_FOLDER, "" );
		imagePlus.show();

		final Image3DUniverse universe = new Image3DUniverse();
		final Content imageContent = universe.addContent( imagePlus, Content.VOLUME );
		imageContent.setTransparency( 0.7F );
		imageContent.setLocked( true );
		universe.show();

		final ArrayList< Point3f > points = new ArrayList<>();
		points.add( new Point3f( 16, 64, 134 ) );
		points.add( new Point3f( 176, 70, 134 ) );
		points.add( new Point3f( 91, 210, 134 ) );
		final CustomTriangleMesh mesh = new CustomTriangleMesh( points );
		final Content meshContent = universe.addCustomMesh( mesh, "planeA" );
		meshContent.setVisible( true );
		meshContent.setLocked( true );

		final Img wrap = ImageJFunctions.wrap( imagePlus );
		final BdvStackSource bdvStackSource = BdvFunctions.show( wrap, "raw" );
		bdvStackSource.setDisplayRange( 0, 255 );

	}

	public static void main( String[] args )
	{
		new Universe3DExplorer();
	}
}
