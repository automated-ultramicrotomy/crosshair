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

/**
 * Class to manage all current planes
 */
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

    /**
     * Create a plane manager
     * @param bdvStackSource BigDataViewer stack source
     * @param universe universe of the 3D viewer
     * @param imageContent image content displayed in 3D viewer (This image content is used to define the
     *                     extent of planes (only shown within bounds of that image) and where points are shown
     *                     (again attached to that image))
     */
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

    /**
     * @return whether a plane is currently being tracked
     */
    public boolean isTrackingPlane() { return isTrackingPlane; }

    /**
     * @param tracking whether a plane is currently being tracked
     */
    public void setTrackingPlane( boolean tracking ) { isTrackingPlane = tracking; }

    public void setTrackedPlaneName(String trackedPlaneName) {
        this.trackedPlaneName = trackedPlaneName;
    }

    public String getTrackedPlaneName() {
        return trackedPlaneName;
    }

    /**
     * @return whether Crosshair is in 'point mode' i.e. click to add points to fit plane
     */
    public boolean isInPointMode() { return isInPointMode; }

    /**
     * @param isInPointMode whether Crosshair is in 'point mode' i.e. click to add points to fit plane
     */
    public void setPointMode( boolean isInPointMode ) {
        this.isInPointMode = isInPointMode;
        bdvHandle.getViewerPanel().requestRepaint();
    }

    /**
     * @return whether Crosshair is in 'vertex mode' i.e. click to add vertices to the block plane
     */
    public boolean isInVertexMode() {
        return isInVertexMode;
    }

    /**
     * @param isInVertexMode whether Crosshair is in 'vertex mode' i.e. click to add vertices to the block plane
     */
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

    /**
     * Add a plane with given settings
     * @param planeSettings plane settings
     */
    public void addPlane( PlaneSettings planeSettings ){
        Plane plane = planeCreator.createPlane( planeSettings );
        planeNameToPlane.put( planeSettings.name, plane);
    }

    /**
     * Add a plane with the given name and orientation
     * @param planeName plane name
     * @param planeNormal plane normal vector
     * @param planePoint plane point
     */
    public void addPlane( String planeName, Vector3d planeNormal, Vector3d planePoint ) {
        PlaneSettings planeSettings = new PlaneSettings();
        planeSettings.name = planeName;
        planeSettings.normal = planeNormal;
        planeSettings.point = planePoint;

        addPlane( planeSettings );
    }

    /**
     * Add a plane with point overlays, but no mesh generated
     * @param planeName name of plane
     */
    public void addPlane( String planeName ) {
        PlaneSettings settings = new PlaneSettings();
        settings.name = planeName;

        addPlane( settings );
    }

    /**
     * Add a plane with position + orientation matching the current view in the BigDataViewer window
     * @param planeName plane name
     */
    public void addPlaneAtCurrentView( String planeName ){
        ArrayList<Vector3d> planeDefinition = getPlaneDefinitionOfCurrentView();
        addPlane( planeName, planeDefinition.get(0), planeDefinition.get(1) );
    }

    /**
     * Add a block plane with given settings
     * @param blockPlaneSettings block plane settings
     */
    public void addBlockPlane( BlockPlaneSettings blockPlaneSettings ) {
        BlockPlane plane = planeCreator.createBlockPlane( blockPlaneSettings );
        planeNameToPlane.put( blockPlaneSettings.name, plane );
    }

    /**
     * Add block plane with the given name and orientation
     * @param planeName plane name
     * @param planeNormal plane normal vector
     * @param planePoint plane point
     */
    public void addBlockPlane( String planeName, Vector3d planeNormal, Vector3d planePoint ) {
        BlockPlaneSettings settings = new BlockPlaneSettings();
        settings.name = planeName;
        settings.normal = planeNormal;
        settings.point = planePoint;

        addBlockPlane( settings );
    }

    /**
     * Add block plane with point overlays, but no mesh generated
     * @param planeName plane name
     */
    public void addBlockPlane( String planeName ) {
        BlockPlaneSettings settings = new BlockPlaneSettings();
        settings.name = planeName;

        addBlockPlane( settings );
    }

    /**
     * Add a block plane with position + orientation matching the current view in the BigDataViewer window
     * @param planeName plane name
     */
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

    /**
     * Update orientation of named plane
     * @param planeNormal new plane normal
     * @param planePoint new plane point
     * @param planeName name of plane to update
     */
    public void updatePlane( Vector3d planeNormal, Vector3d planePoint, String planeName ) {
        if ( checkNamedPlaneExists( planeName ) ) {
            planeCreator.updatePlaneOrientation( getPlane( planeName ), planeNormal, planePoint );
        }
    }

    /**
     * Set colour of named plane to the aligned colour
     * @param planeName plane name
     */
    public void setPlaneColourToAligned( String planeName ) {
        Color3f currentColour = universe.getContent( planeName ).getColor();
        Color3f alignedColour = new Color3f( alignedPlaneColour );
        if ( currentColour != alignedColour ) {
            universe.getContent( planeName ).setColor( alignedColour );
        }
    }

    /**
     * Set colour of named plane to the unaligned colour
     * @param planeName plane name
     */
    public void setPlaneColourToUnaligned( String planeName ) {
        Color3f currentColour = universe.getContent( planeName ).getColor();
        Color3f notAlignedColour = new Color3f( planeNameToPlane.get(planeName).getColor() );
        if (currentColour != notAlignedColour) {
            universe.getContent( planeName ).setColor( notAlignedColour );
        }
    }

    /**
     * Update orientation of named plane to match given BigDataViewer affine transform
     * @param affineTransform3D 3D affine transform of BigDataViewer window
     * @param planeName plane name
     */
    public void updatePlaneOnTransformChange(AffineTransform3D affineTransform3D, String planeName) {
        ArrayList<Vector3d> planeDefinition = getPlaneDefinitionFromViewTransform(affineTransform3D);
        updatePlane(planeDefinition.get(0), planeDefinition.get(1), planeName);
    }

    /**
     * Update orientation of named plane to match current view in the BigDataViewer window
     * @param planeName plane name
     */
    public void updatePlaneCurrentView (String planeName) {
        ArrayList<Vector3d> planeDefinition = getPlaneDefinitionOfCurrentView();
        updatePlane(planeDefinition.get(0), planeDefinition.get(1), planeName);
    }

    /**
     * Re-draw all planes in the 3D viewer
     */
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

    /**
     * Fit named plane to the points in its 'points to fit plane display'
     * @param planeName plane name
     */
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

    /**
     * Get plane definition of the current view in the BigDataViewer window
     * @return ArrayList of 2 vectors: the first being the plane normal, and the second the plane point
     */
    public ArrayList<Vector3d> getPlaneDefinitionOfCurrentView () {
        final AffineTransform3D transform = new AffineTransform3D();
        bdvHandle.getViewerPanel().state().getViewerTransform( transform );

        return getPlaneDefinitionFromViewTransform(transform);
    }

    private ArrayList<Vector3d> getPlaneDefinitionFromViewTransform(AffineTransform3D affineTransform3D) {
        final ArrayList< double[] > viewerPoints = new ArrayList<>();

        viewerPoints.add( new double[]{ 0, 0, 0 });
        viewerPoints.add( new double[]{ 0, 100, 0 });
        viewerPoints.add( new double[]{ 100, 0, 0 });

        final ArrayList< double[] > globalPoints = new ArrayList<>();
        for ( int i = 0; i < 3; i++ )
        {
            globalPoints.add( new double[ 3 ] );
        }

        for ( int i = 0; i < 3; i++ )
        {
            affineTransform3D.inverse().apply( viewerPoints.get( i ), globalPoints.get( i ) );
        }

        ArrayList<Vector3d> planeDefinition = new ArrayList<>();

        Vector3d planeNormal = GeometryUtils.calculateNormalFromPoints(globalPoints);
        Vector3d planePoint = new Vector3d(globalPoints.get(0)[0], globalPoints.get(0)[1], globalPoints.get(0)[2]);
        planeDefinition.add(planeNormal);
        planeDefinition.add(planePoint);

        return planeDefinition;
    }

    /**
     * Get global point at centre of the current view in the BigDataViewer window
     * @return global centre point of BigDataViewer window
     */
    public double[] getGlobalViewCentre () {
        final AffineTransform3D transform = new AffineTransform3D();
        bdvHandle.getViewerPanel().state().getViewerTransform( transform );
        double[] centrePointView = getBdvWindowCentre(bdvStackSource);
        double[] centrePointGlobal = new double[3];
        transform.inverse().apply(centrePointView, centrePointGlobal);

        return centrePointGlobal;
    }

    /**
     * Move BigDataViewer window view to match the named plane
     * @param name plane name
     */
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

    /**
     * Remove plane with given name
     * @param name plane name
     */
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
