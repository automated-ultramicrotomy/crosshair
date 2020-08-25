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

//TODO - more sensible placement of varibles / structure
//TODO - make plane update as efficient as possible
//Add some buttons for e.g. reset view, cnetre view for microtome, centre view for sample etc
//check points are on block plane
//TODO need extra requirement that that the out of block normal points towards knife
//TODO - no plane updates when they aren't visible
//TODO - maybe explicitly round in microtome manager to 4dp (otherwise a longer number typed is transmitted, but isn't
// displayed - could be confusing
//TODO stop view shifting when you move the microtome
//TODO - initial point - not general to case where target plane intersects with block face e.g. you're just chipping off a
// corner - some vertex points above, some below. Need to think about approaching from a distance.
// TODO - check angle updates - wrote very quickly
//TODO - colour change on alignment, only set, if not already that colour?
// TODO - align microtome view when enter
// TODO - perhaps add another plane entry for cutting plane so can change colour / visiblity etc
// TODO -make sure selected point deselected in all removals
// TODO - add cutting-plane to target distance in cutting mode (would be nice check for me for distances, and could be useful for folks to plan their runs)
// TODO - command for loading bdv files
//TODO - view changes if change planes after exit microtome mode
// TODO - make GOTOs match normals properly? Issue is imglib2 uses a coordinate system from top left so normal vector t calculates is into page
// not out of it, like our target normals are set?

public class Crosshair {

	public Crosshair (BdvStackSource bdvStackSource, Image3DUniverse universe, Content imageContent) {

		BdvHandle bdvHandle = bdvStackSource.getBdvHandle();
		imageContent.setLocked(true);
		imageContent.showPointList(true);
		universe.getPointListDialog().setVisible(false);

		// the global min of the image is often not (0,0,0), looks like this is calculated only from pixels != 0
		// as here: https://github.com/fiji/3D_Viewer/blob/ed05e4b2275ad6ad7c94b0e22f4789ebd3472f4d/src/main/java/voltex/VoltexGroup.java
		// Still places so (0,0) of image == (0,0) in global coordinate system, just bounding box is wrapped tight to
		// only regions of the image > 0

		PlaneManager planeManager = new PlaneManager(bdvStackSource, universe, imageContent);
		MicrotomeManager microtomeManager = new MicrotomeManager(planeManager, universe, imageContent, bdvStackSource);
		PointsOverlaySizeChange pointOverlay = new PointsOverlaySizeChange();
		pointOverlay.setPoints(planeManager.getPointsToFitPlane(), planeManager.getBlockVertices(),
				planeManager.getSelectedVertex(), planeManager.getNamedVertices());
		BdvFunctions.showOverlay(pointOverlay, "PointOverlay", Bdv.options().addTo(bdvStackSource));
		new BdvBehaviours(bdvHandle, planeManager, microtomeManager);

		CrosshairFrame crosshairFrame = new CrosshairFrame(universe, imageContent, planeManager, microtomeManager, pointOverlay, bdvHandle);

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
