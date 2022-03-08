package de.embl.schwab.crosshair.plane;

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvStackSource;
import bdv.viewer.SourceAndConverter;
import de.embl.schwab.crosshair.bdv.ModeOverlay;
import de.embl.schwab.crosshair.points.PointsToFitPlaneDisplay;
import de.embl.schwab.crosshair.points.VertexDisplay;
import de.embl.schwab.crosshair.points.overlays.Point3dOverlay;
import de.embl.schwab.crosshair.points.overlays.PointOverlay2d;
import de.embl.schwab.crosshair.settings.BlockPlaneSettings;
import de.embl.schwab.crosshair.settings.PlaneSettings;
import de.embl.schwab.crosshair.utils.BdvUtils;
import de.embl.schwab.crosshair.utils.GeometryUtils;
import ij.IJ;
import ij3d.Content;
import ij3d.Image3DUniverse;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Vector3d;

import java.util.*;

import static de.embl.cba.bdv.utils.BdvUtils.getBdvWindowCentre;
import static de.embl.cba.bdv.utils.BdvUtils.moveToPosition;
import static de.embl.schwab.crosshair.utils.GeometryUtils.getPlaneDefinitionFromViewTransform;

public class PlaneManager {

    private boolean isTrackingPlane = false;
    private String trackedPlaneName;

    private boolean isInPointMode = false;
    private boolean isInVertexMode = false;

    private final PlaneCreator planeCreator;
    private final Map<String, Plane> planeNameToPlane;

    private final BdvHandle bdvHandle;
    private final BdvStackSource bdvStackSource;
    private final Image3DUniverse universe;
    private final Point3dOverlay point3dOverlay;

    private final Color3f alignedPlaneColour = new Color3f(1, 0, 0);
    // TODO - make this threshold user definable - makes sense for microns, but possibly not for other units
    private final double distanceBetweenPlanesThreshold = 1E-10;

    // Given image content is used to define the extent of planes (only shown within bounds of that image)
    // and where points are shown (again attached to that image)
    public PlaneManager( BdvStackSource bdvStackSource, Image3DUniverse universe, Content imageContent ) {
        planeNameToPlane = new HashMap<>();

        this.bdvStackSource = bdvStackSource;
        BdvFunctions.showOverlay( new ModeOverlay( this), "mode_text",
                Bdv.options().addTo( bdvStackSource ) );
        this.bdvHandle = bdvStackSource.getBdvHandle();
        this.universe = universe;
        this.point3dOverlay = new Point3dOverlay( imageContent );

        this.planeCreator = new PlaneCreator( universe, imageContent, bdvStackSource, point3dOverlay );
    }

    public Plane getPlane( String planeName ) {
        return planeNameToPlane.get( planeName );
    }

    public BlockPlane getBlockPlane( String planeName ) {
        Plane plane = getPlane( planeName );
        if ( plane instanceof BlockPlane ) {
            return (BlockPlane) plane;
        } else {
            throw new UnsupportedOperationException( "Plane " + planeName + " is not a block plane" );
        }
    }

    public Set<String> getPlaneNames() {
        return planeNameToPlane.keySet();
    }

    public Collection<Plane> getPlanes() {
        return planeNameToPlane.values();
    }

    public boolean isTrackingPlane() { return isTrackingPlane; }

    public void setTrackingPlane( boolean tracking ) { isTrackingPlane = tracking; }

    public void setTrackedPlaneName(String trackedPlaneName) {
        this.trackedPlaneName = trackedPlaneName;
    }

    public String getTrackedPlaneName() {
        return trackedPlaneName;
    }

    public boolean isInPointMode() { return isInPointMode; }

    public void setPointMode( boolean isInPointMode ) {
        this.isInPointMode = isInPointMode;
        bdvHandle.getViewerPanel().requestRepaint();
    }

    public boolean isInVertexMode() {
        return isInVertexMode;
    }

    public void setVertexMode( boolean isInVertexMode ) {
        this.isInVertexMode = isInVertexMode;
        bdvHandle.getViewerPanel().requestRepaint();
    }

    public boolean checkNamedPlaneExists(String name) {
        return planeNameToPlane.containsKey( name );
    }

    public boolean checkNamedPlaneExistsAndOrientationIsSet( String name ) {
        if ( checkNamedPlaneExists( name )) {
            return getPlane( name ).isOrientationSet();
        } else {
            return false;
        }
    }

    public void addPlane( PlaneSettings planeSettings ){
        Plane plane = planeCreator.createPlane( planeSettings );
        planeNameToPlane.put( planeSettings.name, plane);
    }

    public void addPlane( String planeName, Vector3d planeNormal, Vector3d planePoint ) {
        PlaneSettings planeSettings = new PlaneSettings();
        planeSettings.name = planeName;
        planeSettings.normal = planeNormal;
        planeSettings.point = planePoint;

        addPlane( planeSettings );
    }

    // add block plane with point overlays, but no mesh generated
    public void addPlane( String planeName ) {
        PlaneSettings settings = new PlaneSettings();
        settings.name = planeName;

        addPlane( settings );
    }

    public void addPlaneAtCurrentView( String planeName ){
        ArrayList<Vector3d> planeDefinition = getPlaneDefinitionOfCurrentView();
        addPlane( planeName, planeDefinition.get(0), planeDefinition.get(1) );
    }

    public void addBlockPlane( BlockPlaneSettings blockPlaneSettings ) {
        BlockPlane plane = planeCreator.createBlockPlane( blockPlaneSettings );
        planeNameToPlane.put( blockPlaneSettings.name, plane );
    }

    public void addBlockPlane( String planeName, Vector3d planeNormal, Vector3d planePoint ) {
        BlockPlaneSettings settings = new BlockPlaneSettings();
        settings.name = planeName;
        settings.normal = planeNormal;
        settings.point = planePoint;

        addBlockPlane( settings );
    }

    // add block plane with point overlays, but no mesh generated
    public void addBlockPlane( String planeName ) {
        BlockPlaneSettings settings = new BlockPlaneSettings();
        settings.name = planeName;

        addBlockPlane( settings );
    }

    public void addBlockPlaneAtCurrentView( String planeName ) {
        ArrayList<Vector3d> planeDefinition = getPlaneDefinitionOfCurrentView();
        addBlockPlane( planeName, planeDefinition.get(0), planeDefinition.get(1) );
    }

    public PointsToFitPlaneDisplay getPointsToFitPlaneDisplay( String planeName ) {
        return getPlane(planeName).getPointsToFitPlaneDisplay();
    }

    public VertexDisplay getVertexDisplay( String planeName ) {
        return getBlockPlane( planeName ).getVertexDisplay();
    }

    public void updatePlane( Vector3d planeNormal, Vector3d planePoint, String planeName ) {
        if ( checkNamedPlaneExists( planeName ) ) {
            planeCreator.updatePlaneOrientation( getPlane( planeName ), planeNormal, planePoint );
        }
    }

    public void setPlaneColourToAligned( String planeName ) {
        Color3f currentColour = universe.getContent( planeName ).getColor();
        Color3f alignedColour = new Color3f( alignedPlaneColour );
        if ( currentColour != alignedColour ) {
            universe.getContent( planeName ).setColor( alignedColour );
        }
    }

    public void setPlaneColourToUnaligned( String planeName ) {
        Color3f currentColour = universe.getContent( planeName ).getColor();
        Color3f notAlignedColour = new Color3f( planeNameToPlane.get(planeName).getColor() );
        if (currentColour != notAlignedColour) {
            universe.getContent( planeName ).setColor( notAlignedColour );
        }
    }

    public void updatePlaneOnTransformChange(AffineTransform3D affineTransform3D, String planeName) {
        ArrayList<Vector3d> planeDefinition = getPlaneDefinitionFromViewTransform(affineTransform3D);
        updatePlane(planeDefinition.get(0), planeDefinition.get(1), planeName);
    }

    public void updatePlaneCurrentView (String planeName) {
        ArrayList<Vector3d> planeDefinition = getPlaneDefinitionOfCurrentView();
        updatePlane(planeDefinition.get(0), planeDefinition.get(1), planeName);
    }

    public void redrawCurrentPlanes () {
        for ( String planeName: planeNameToPlane.keySet() ) {
            Plane plane = planeNameToPlane.get( planeName );
            updatePlane( plane.getNormal(), plane.getPoint(), planeName );
        }
    }

    private void fitToPoints( String planeName, ArrayList<RealPoint> points ) {
        ArrayList<Vector3d> planeDefinition = GeometryUtils.fitPlaneToPoints( points );
        updatePlane( planeDefinition.get(0), planeDefinition.get(1), planeName );
        getPlane( planeName ).setVisible( true );
    }

    public void fitToPoints( String planeName ) {
        ArrayList<RealPoint> points = getPointsToFitPlaneDisplay( planeName ).getPointsToFitPlane();

        if ( points.size() >= 3 ) {
            fitToPoints( planeName, points );
        } else {
            IJ.log ("Need at least 3 points to fit plane");
        }
    }

    public ArrayList<PointOverlay2d> getAll2dPointOverlays() {
        ArrayList<PointOverlay2d> pointOverlays = new ArrayList<>();
        for ( Plane plane: planeNameToPlane.values() ) {
            pointOverlays.add( plane.getPointsToFitPlaneDisplay().getPoint2dOverlay() );
            if ( plane instanceof BlockPlane ) {
                pointOverlays.add( ((BlockPlane) plane).getVertexDisplay().get2dOverlay() );
            }
        }

        return pointOverlays;
    }

    public ArrayList<Vector3d> getPlaneDefinitionOfCurrentView () {
        final AffineTransform3D transform = new AffineTransform3D();
        bdvHandle.getViewerPanel().state().getViewerTransform( transform );

        return getPlaneDefinitionFromViewTransform(transform);
    }

    public double[] getGlobalViewCentre () {
        final AffineTransform3D transform = new AffineTransform3D();
        bdvHandle.getViewerPanel().state().getViewerTransform( transform );
        double[] centrePointView = getBdvWindowCentre(bdvStackSource);
        double[] centrePointGlobal = new double[3];
        transform.inverse().apply(centrePointView, centrePointGlobal);

        return centrePointGlobal;
    }

    public void moveViewToNamedPlane (String name) {
        // check if you're already at the plane
        ArrayList<Vector3d> planeDefinition = getPlaneDefinitionOfCurrentView();
        Vector3d currentPlaneNormal = planeDefinition.get(0);
        Vector3d currentPlanePoint = planeDefinition.get(1);

        Plane plane = planeNameToPlane.get( name );
        boolean normalsParallel = GeometryUtils.checkVectorsParallel( plane.getNormal(), currentPlaneNormal );
        double distanceToPlane = GeometryUtils.distanceFromPointToPlane( currentPlanePoint,
                plane.getNormal(), plane.getPoint() );

        // units may want to be more or less strict
        // necessary due to double precision, will very rarely get exactly the same value
        boolean pointInPlane = distanceToPlane < distanceBetweenPlanesThreshold;

        if (normalsParallel & pointInPlane) {
            IJ.log("Already at that plane");
        } else {
            double[] targetNormal = new double[3];
            plane.getNormal().get( targetNormal );

            double[] targetCentroid = new double[3];
            plane.getCentroid().get( targetCentroid );
            moveToPosition(bdvStackSource, targetCentroid, 0, 0);
            if (!normalsParallel) {
                BdvUtils.levelCurrentView(bdvStackSource, targetNormal);
            }
        }
    }

    private SourceAndConverter getMatchingBdvSource( String name ) {
        for ( SourceAndConverter sac: bdvHandle.getViewerPanel().state().getSources() ) {
            if (sac.getSpimSource().getName().equals(name)) {
                return sac;
            }
        }

        return null;
    }

    public void removeNamedPlane (String name) {
        if ( checkNamedPlaneExists(name) ) {
            PointsToFitPlaneDisplay pointsToFitPlaneDisplay = getPointsToFitPlaneDisplay( name );
            pointsToFitPlaneDisplay.removeAllPointsToFitPlane();
            bdvHandle.getViewerPanel().getDisplay().overlays().remove( pointsToFitPlaneDisplay.getPoint2dOverlay() );
            SourceAndConverter pointSource = getMatchingBdvSource( pointsToFitPlaneDisplay.getSourceName() );
            bdvHandle.getViewerPanel().state().removeSource( pointSource );

            if ( getPlane( name ) instanceof BlockPlane ) {
                VertexDisplay vertexDisplay = getVertexDisplay( name );
                vertexDisplay.removeAllVertices();
                bdvHandle.getViewerPanel().getDisplay().overlays().remove( vertexDisplay.get2dOverlay() );
                SourceAndConverter vertexSource = getMatchingBdvSource( vertexDisplay.getSourceName() );
                bdvHandle.getViewerPanel().state().removeSource( vertexSource );
            }

            planeNameToPlane.remove( name );
            universe.removeContent( name );
        }

    }
}
