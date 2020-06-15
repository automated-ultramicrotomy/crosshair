package de.embl.cba.target;

import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvStackSource;
import customnode.CustomTriangleMesh;
import ij.ImagePlus;
import ij.plugin.FolderOpener;
import ij3d.Content;
import ij3d.Image3DUniverse;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import org.scijava.vecmath.Point3f;

import java.util.ArrayList;

public class Universe3DExplorer
{
	public static final String INPUT_FOLDER = "/Users/tischer/Documents/targeting_3D/src/test/resources/target/mri-stack-slices/";
	private final Content meshContent;

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
		meshContent = universe.addCustomMesh( mesh, "planeA" );
		meshContent.setVisible( true );
		meshContent.setLocked( true );

		final Img wrap = ImageJFunctions.wrap( imagePlus );
		final BdvStackSource bdvStackSource = BdvFunctions.show( wrap, "raw" );
		bdvStackSource.setDisplayRange( 0, 255 );


		final BdvHandle bdvHandle = bdvStackSource.getBdvHandle();

		final Behaviours behaviours = new Behaviours( new InputTriggerConfig() );
		behaviours.install( bdvHandle.getTriggerbindings(), "target" );



		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
			final AffineTransform3D affineTransform3D = new AffineTransform3D();
			bdvHandle.getViewerPanel().getState().getViewerTransform( affineTransform3D );

			final ArrayList< double[] > viewerPoints = new ArrayList<>();

			viewerPoints.add( new double[]{ 0, 0, 0 });
			viewerPoints.add( new double[]{ 0, 100, 0 });
			viewerPoints.add( new double[]{ 100, 0, 0 });

			final double[] clickPositionInViewerWindow = { x, y, 0 };

			final ArrayList< double[] > globalPoints = new ArrayList<>();
			for ( int i = 0; i < 3; i++ )
			{
				globalPoints.add( new double[ 3 ] );
			}

			for ( int i = 0; i < 3; i++ )
			{
				affineTransform3D.inverse().apply( viewerPoints.get( i ), globalPoints.get( i ) );
			}

			System.out.println( "Hello" );

			// TODO: update this guy
			// meshContent

		}, "update target plane", "shift T" );

	}

	public static void main( String[] args )
	{
		new Universe3DExplorer();
	}
}
