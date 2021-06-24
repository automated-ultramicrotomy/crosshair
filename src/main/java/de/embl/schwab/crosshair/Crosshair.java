package de.embl.schwab.crosshair;

import bdv.util.*;
import de.embl.schwab.crosshair.bdv.BdvBehaviours;
import de.embl.schwab.crosshair.points.PointOverlay2d;
import de.embl.schwab.crosshair.microtome.MicrotomeManager;
import de.embl.schwab.crosshair.plane.PlaneManager;
import de.embl.schwab.crosshair.ui.swing.CrosshairFrame;
import ij3d.Content;
import ij3d.Image3DUniverse;

import javax.swing.*;
import java.awt.Window;

import static de.embl.schwab.crosshair.utils.Utils.spaceOutWindows;

// TODO - neaten up code structure, possibly clearer labelling of which coordinate system & units are being used
// TODO - check degree symbols work on mac

// Possible improvements to add
// TODO - add cutting-plane to target distance in cutting mode (would be nice check for me for distances, and could be useful for folks to plan their runs)
// TODO - make plane update as efficient as possible
// TODO - Add some buttons for e.g. reset view, centre view for microtome, centre view for sample etc
// TODO - Add a check that target plane is behind block face (or intersects it)
// TODO - make GOTOs match normals properly? Issue is imglib2 uses a coordinate system from top left so normal vector t calculates is into page
// not out of it, like our target normals are set?
// TODO - make generic for any bit depth - currently only accepts 8 bit
// TODO - make it so transparency panel doesn't appear if plane not initialised
// TODO - do checks for legitimacy of values when load from settings? e.g. that vertices lie on the block plane
// TODO - make so doesn't show windows until all loaded?
// TODO - check against original blender solution. Make mock file for case of intersection with block face - check how two solutions compare


public class Crosshair {

	public static final String target = "target";
	public static final String block = "block";

	public Crosshair (BdvStackSource bdvStackSource, Image3DUniverse universe, Content imageContent, String unit) {

		BdvHandle bdvHandle = bdvStackSource.getBdvHandle();
		imageContent.setLocked(true);
		imageContent.showPointList(true);
		universe.getPointListDialog().setVisible(false);

		// the global min of the image is often not (0,0,0), looks like this is calculated only from pixels != 0
		// as here: https://github.com/fiji/3D_Viewer/blob/ed05e4b2275ad6ad7c94b0e22f4789ebd3472f4d/src/main/java/voltex/VoltexGroup.java
		// Still places so (0,0) of image == (0,0) in global coordinate system, just bounding box is wrapped tight to
		// only regions of the image > 0

		PlaneManager planeManager = new PlaneManager(bdvStackSource, universe, imageContent);

		MicrotomeManager microtomeManager = new MicrotomeManager(planeManager, universe, imageContent, bdvStackSource, unit);
		new BdvBehaviours(bdvHandle, planeManager, microtomeManager);

		CrosshairFrame crosshairFrame = new CrosshairFrame(universe, imageContent, planeManager, microtomeManager, bdvHandle, unit);

		spaceOutWindows( bdvHandle, crosshairFrame, universe );
	}
}
