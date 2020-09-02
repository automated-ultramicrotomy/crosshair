package de.embl.schwab.crosshair;

import bdv.util.*;
import de.embl.schwab.crosshair.bdv.BdvBehaviours;
import de.embl.schwab.crosshair.bdv.PointsOverlaySizeChange;
import de.embl.schwab.crosshair.microtome.MicrotomeManager;
import de.embl.schwab.crosshair.ui.swing.CrosshairFrame;
import ij3d.Content;
import ij3d.Image3DUniverse;

import javax.swing.*;
import java.awt.Window;

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
		PointsOverlaySizeChange pointOverlay = new PointsOverlaySizeChange();
		pointOverlay.setPoints(planeManager.getPointsToFitPlane(), planeManager.getBlockVertices(),
				planeManager.getSelectedVertex(), planeManager.getNamedVertices());
		planeManager.setPointOverlay(pointOverlay);
		BdvFunctions.showOverlay(pointOverlay, "PointOverlay", Bdv.options().addTo(bdvStackSource));
		new BdvBehaviours(bdvHandle, planeManager, microtomeManager);

		CrosshairFrame crosshairFrame = new CrosshairFrame(universe, imageContent, planeManager, microtomeManager, pointOverlay, bdvHandle, unit);

		// Space out windows like here:
		// https://github.com/mobie/mobie-viewer-fiji/blob/9f7367902cc0bd01e089f7ce40cdcf0ee0325f1e/src/main/java/de/embl/cba/mobie/ui/viewer/SourcesPanel.java#L369
		Window viewFrame = SwingUtilities.getWindowAncestor(bdvHandle.getViewerPanel());
		viewFrame.setLocation(
						crosshairFrame.getLocationOnScreen().x + crosshairFrame.getWidth(),
						crosshairFrame.getLocationOnScreen().y );

		universe.getWindow().setLocation(viewFrame.getLocationOnScreen().x + viewFrame.getWidth(),
				viewFrame.getLocation().y);

	}
}
