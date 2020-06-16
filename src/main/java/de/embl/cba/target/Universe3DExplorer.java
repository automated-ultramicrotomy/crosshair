package de.embl.cba.target;

import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvStackSource;
import customnode.CustomTriangleMesh;
import ij.ImagePlus;
import ij.plugin.FolderOpener;
import ij3d.Content;
import ij3d.Image3DUniverse;
import ij3d.behaviors.InteractiveBehavior;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import org.scijava.vecmath.Point3f;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

public class Universe3DExplorer
{
//	public static final String INPUT_FOLDER = "Z:\\Kimberly\\Projects\\Targeting\\Data\\Raw\\MicroCT\\Targeting\\Course-1\\flipped";
	public static final String INPUT_FOLDER = "Z:\\Kimberly\\Projects\\Targeting\\Data\\Derived\\test_stack";
	private final Content meshContent;

	public Universe3DExplorer()
	{
		final ImagePlus imagePlus = FolderOpener.open( INPUT_FOLDER, "" );
		imagePlus.show();

		final Image3DUniverse universe = new Image3DUniverse();
		final Content imageContent = universe.addContent( imagePlus, Content.VOLUME );
		universe.addInteractiveBehavior(new CustomBehaviour(universe, imageContent));

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
//
//		final Img wrap = ImageJFunctions.wrap( imagePlus );
//		final BdvStackSource bdvStackSource = BdvFunctions.show( wrap, "raw" );
//		bdvStackSource.setDisplayRange( 0, 255 );

//
//		final BdvHandle bdvHandle = bdvStackSource.getBdvHandle();
//
//		final Behaviours behaviours = new Behaviours( new InputTriggerConfig() );
//		behaviours.install( bdvHandle.getTriggerbindings(), "target" );
//
//
//
//		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {
//			final AffineTransform3D affineTransform3D = new AffineTransform3D();
//			bdvHandle.getViewerPanel().getState().getViewerTransform( affineTransform3D );
//
//			final ArrayList< double[] > viewerPoints = new ArrayList<>();
//
//			viewerPoints.add( new double[]{ 0, 0, 0 });
//			viewerPoints.add( new double[]{ 0, 100, 0 });
//			viewerPoints.add( new double[]{ 100, 0, 0 });
//
//			final double[] clickPositionInViewerWindow = { x, y, 0 };
//
//			final ArrayList< double[] > globalPoints = new ArrayList<>();
//			for ( int i = 0; i < 3; i++ )
//			{
//				globalPoints.add( new double[ 3 ] );
//			}
//
//			for ( int i = 0; i < 3; i++ )
//			{
//				affineTransform3D.inverse().apply( viewerPoints.get( i ), globalPoints.get( i ) );
//			}
//
//			System.out.println( "Hello" );
//
//			// TODO: update this guy
//			// meshContent
//
//		}, "update target plane", "shift T" );

	}

//	behaviours for 3d window
	private class CustomBehaviour extends InteractiveBehavior {

		private Content imageContent;

		CustomBehaviour(Image3DUniverse universe, Content imageContent) {
			super(universe);
			this.imageContent = imageContent;
		}

		public void doProcess(KeyEvent e) {
			int id = e.getID();
			int key = e.getKeyCode();

			if (!((id == KeyEvent.KEY_RELEASED && e.isControlDown() && key == KeyEvent.VK_S) ||
					(id == KeyEvent.KEY_RELEASED && e.isControlDown() && key == KeyEvent.VK_U))) {
				super.doProcess(e);
				return;
			}

			switch (key) {
				case KeyEvent.VK_S:
					save_current_lut(imageContent, "Z:\\Kimberly\\Projects\\Targeting\\Data\\Derived\\test_stack\\test_lut.txt");
					System.out.println("Saved current LUT to file");
					break;
				case KeyEvent.VK_U:
					set_lut_from_file(imageContent, "Z:\\Kimberly\\Projects\\Targeting\\Data\\Derived\\test_stack\\test_lut.txt");
					System.out.println("Set LUT from file");
					break;
			}

		}
	}

	private void save_current_lut(Content imageContent, String filepath) {

		int[] red_lut = new int[256];
		int[] green_lut = new int[256];
		int[] blue_lut = new int[256];
		int[] alpha_lut = new int[256];

		//	get LUT copies the current lut into the given array
		imageContent.getRedLUT(red_lut);
		imageContent.getGreenLUT(green_lut);
		imageContent.getBlueLUT(blue_lut);
		imageContent.getAlphaLUT(alpha_lut);

		try (FileOutputStream f = new FileOutputStream(filepath);
			ObjectOutput s = new ObjectOutputStream(f);) {
			s.writeObject(red_lut);
			s.writeObject(green_lut);
			s.writeObject(blue_lut);
			s.writeObject(alpha_lut);
		} catch (FileNotFoundException e) {
			System.out.println("File not found");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Error writing to file");
			e.printStackTrace();
		}
	}

	private void set_lut_from_file (Content imageContent, String filepath) {

		try (FileInputStream in = new FileInputStream(filepath);
			 ObjectInputStream s = new ObjectInputStream(in);) {
			int[] red_lut = (int[]) s.readObject();
			int[] green_lut = (int[]) s.readObject();
			int[] blue_lut = (int[]) s.readObject();
			int[] alpha_lut = (int[]) s.readObject();
			imageContent.setLUT(red_lut, green_lut, blue_lut, alpha_lut);

		} catch (FileNotFoundException e) {
			System.out.println("File not found");
			e.printStackTrace();
		} catch (IOException | ClassNotFoundException e) {
			System.out.println("Error reading from file");
			e.printStackTrace();
		}
	}

	public static void main( String[] args )
	{
		new Universe3DExplorer();
	}
}
