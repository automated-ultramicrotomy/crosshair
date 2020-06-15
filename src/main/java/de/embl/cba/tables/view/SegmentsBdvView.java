package de.embl.cba.tables.view;

import bdv.tools.brightness.ConverterSetup;
import bdv.util.*;
import bdv.viewer.Source;
import bdv.viewer.state.SourceState;
import bdv.viewer.state.ViewerState;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.bdv.utils.objects3d.ConnectedComponentExtractorAnd3DViewer;
import de.embl.cba.bdv.utils.overlays.BdvGrayValuesOverlay;
import de.embl.cba.bdv.utils.popup.BdvPopupMenus;
import de.embl.cba.bdv.utils.sources.ARGBConvertedRealSource;
import de.embl.cba.bdv.utils.sources.ImagePlusFileSource;
import de.embl.cba.bdv.utils.sources.Metadata;
import de.embl.cba.bdv.utils.sources.ModifiableRandomAccessibleIntervalSource4D;
import de.embl.cba.lazyalgorithm.RandomAccessibleIntervalNeighborhoodFilter;
import de.embl.cba.lazyalgorithm.converter.NeighborhoodNonZeroBoundariesConverter;
import de.embl.cba.tables.Logger;
import de.embl.cba.tables.color.*;
import de.embl.cba.tables.image.ImageSourcesModel;
import de.embl.cba.tables.image.SourceAndMetadata;
import de.embl.cba.tables.imagesegment.ImageSegment;
import de.embl.cba.tables.imagesegment.LabelFrameAndImage;
import de.embl.cba.tables.imagesegment.SegmentUtils;
import de.embl.cba.tables.select.SelectionListener;
import de.embl.cba.tables.select.SelectionModel;
import de.embl.cba.tables.view.dialogs.BdvViewSourcesBrowsingAndActionsDialog;
import ij.gui.GenericDialog;
import net.imglib2.RealPoint;
import net.imglib2.algorithm.neighborhood.HyperSphereShape;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;

import java.util.*;

import static de.embl.cba.bdv.utils.converters.SelectableVolatileARGBConverter.BACKGROUND;

// TODO: reconsider what a "segment" needs to be here
public class SegmentsBdvView < T extends ImageSegment >
{
	private String selectTrigger = "ctrl button1";
	private String selectNoneTrigger = "ctrl shift N";
	private String nextImageSetTrigger = "ctrl N";
	private String previousImageSetTrigger = "ctrl P";
	private String incrementCategoricalLutRandomSeedTrigger = "ctrl L";
	private String labelMaskAsBinaryMaskTrigger = "ctrl M";
	private String labelMaskAsBoundaryTrigger = "ctrl B";
	private String iterateSelectionModeTrigger = "ctrl S";
	private String viewIn3DTrigger = "ctrl shift button1";

	private final SelectionModel< T > selectionModel;
	private final SelectionColoringModel< T > selectionColoringModel;

	private final ImageSourcesModel imageSourcesModel;
	private Behaviours behaviours;

	private BdvHandle bdv;
	private String segmentsName;
	private BdvOptions bdvOptions;
	private SourceAndMetadata labelsSource;
	private LabelsARGBConverter labelsSourceConverter;

	private T recentFocus;
	private ViewerState recentViewerState;
	private List< ConverterSetup > recentConverterSetups;
	private double voxelSpacing3DView;
	private Set< SourceAndMetadata< ? extends RealType< ? > > > currentSources;
	private boolean grayValueOverlayWasFirstSource;
	private HashMap< LabelFrameAndImage, T > labelFrameAndImageToSegment;
	private List< T > segments;
	private int segmentFocusAnimationDurationMillis;
	private List< String > labelSourceIds;
	private ARGBType labelSourceSingleColor;
	private boolean isLabelMaskShownAsBinaryMask;
	private boolean isLabelMaskShownAsBoundaries;
	private int labelMaskBoundaryThickness;

	public SegmentsBdvView(
			final List< T > segments,
			final SelectionModel< T > selectionModel,
			final SelectionColoringModel< T > selectionColoringModel,
			final ImageSourcesModel imageSourcesModel )
	{
		this( segments, selectionModel, selectionColoringModel, imageSourcesModel, null );
	}

	/**
	 *
	 * There can be multiple label images, both in the segments list as well as in
	 * the imageSourcesModel. However, only one of the label images can be displayed at
	 * a time. If a segment is selected in the segments list, the appropriate label
	 * source is identified and shown.
	 *
	 * @param segments
	 * @param selectionModel
	 * @param selectionColoringModel
	 * @param imageSourcesModel
	 * @param bdv
	 */
	public SegmentsBdvView(
			final List< T > segments,
			final SelectionModel< T > selectionModel,
			final SelectionColoringModel< T > selectionColoringModel,
			final ImageSourcesModel imageSourcesModel,
			BdvHandle bdv )
	{
		this.selectionModel = selectionModel;
		this.selectionColoringModel = selectionColoringModel;
		this.imageSourcesModel = imageSourcesModel;
		this.bdv = bdv;

		this.labelSourceSingleColor = new ARGBType( ARGBType.rgba( 255, 255, 255, 255 ) );;
		this.isLabelMaskShownAsBinaryMask = false;

		this.voxelSpacing3DView = 0.1;
		this.segmentFocusAnimationDurationMillis = 750;
		this.currentSources = new HashSet<>( );

		initSegments( segments );
		initBdvOptions();
		showInitialSources();
		initLabelSourceIds();
//		addGrayValueOverlay(); // TODO: If added, this is the image name shown in Bdv...which is bad

		registerAsSelectionListener( this.selectionModel );
		registerAsColoringListener( this.selectionColoringModel );

		installBdvBehaviours();
	}

	public void initSegments( List< T > segments )
	{
		if ( segments != null )
		{
			this.segments = segments;
			this.labelFrameAndImageToSegment = SegmentUtils.createSegmentMap( this.segments );
		}
	}

	public List< String > getSourceSetIds()
	{
		return labelSourceIds;
	}

	public void showSourceSetSelectionDialog()
	{
		new BdvViewSourcesBrowsingAndActionsDialog( this );
	}

	public void addGrayValueOverlay()
	{
		// TODO: put this to lower right corner not to interfere with the scale bar
		new BdvGrayValuesOverlay( bdv, 20 ).getBdvOverlaySource();
		grayValueOverlayWasFirstSource = false;
		//hasGrayValueOverlay = true;
	}

	public ImageSourcesModel getImageSourcesModel()
	{
		return imageSourcesModel;
	}

	private void showInitialSources()
	{
		boolean isShownNone = true;

		for ( SourceAndMetadata< ? > sourceAndMetadata : imageSourcesModel.sources().values() )
		{
			if ( sourceAndMetadata.metadata().showInitially )
			{
				showSourceSet( sourceAndMetadata, false );
				isShownNone = false;
			}
		}

		if ( isShownNone )
		{
			showSourceSet(
					// TODO: how get an entry of map values?
					imageSourcesModel.sources().values().iterator().next(),
					false );
		}

	}

	public BdvHandle getBdv()
	{
		return bdv;
	}

	public void registerAsColoringListener( ColoringModel< T > coloringModel )
	{
		coloringModel.listeners().add( () -> BdvUtils.repaint( bdv ) );
	}

	public void registerAsSelectionListener( SelectionModel< T > selectionModel )
	{
		selectionModel.listeners().add( new SelectionListener< T >()
		{
			@Override
			public void selectionChanged()
			{
				BdvUtils.repaint( bdv );
			}

			@Override
			public void focusEvent( T selection )
			{
				if ( recentFocus != null && selection == recentFocus )
				{
					return;
				}
				else
				{
					recentFocus = selection;
					centerBdvOnSegment( selection );
				}
			}
		} );
	}

	public synchronized void centerBdvOnSegment( ImageSegment imageSegment )
	{
		updateImageSet( imageSegment.imageId() );

		final double[] position = new double[ 3 ];
		imageSegment.localize( position );

		BdvUtils.moveToPosition(
				bdv,
				position,
				imageSegment.timePoint(),
				segmentFocusAnimationDurationMillis );
	}

	public void setSegmentFocusAnimationDurationMillis( int duration )
	{
		this.segmentFocusAnimationDurationMillis = duration;
	}

	public synchronized void updateImageSet( String imageId )
	{
		if ( labelsSource.metadata().imageId.equals( imageId ) )
		{
			// Nothing to be done, the imageSegment belongs to the
			// currently shown image set.
			return;
		}
		else
		{
			// Replace sources, because selected image segment
			// belongs to another image set of displayed data set

			final SourceAndMetadata sourceAndMetadata
					= imageSourcesModel.sources().get( imageId );

			showSourceSet( sourceAndMetadata, true );
		}
	}

	public void setVoxelSpacing3DView( double voxelSpacing3DView )
	{
		this.voxelSpacing3DView = voxelSpacing3DView;
	}

	/**
	 * ...will show more sources if required by metadata...
	 *
	 * @param sourceAndMetadata
	 * @param removeOtherSources
	 */
	public void showSourceSet( SourceAndMetadata sourceAndMetadata, boolean removeOtherSources )
	{
		final List< String > imageSetIDs = sourceAndMetadata.metadata().imageSetIDs;

		if ( bdv != null && removeOtherSources )
			removeSources();

		for ( int associatedSourceIndex = 0;
			  associatedSourceIndex < imageSetIDs.size();
			  associatedSourceIndex++ )
		{
			final SourceAndMetadata associatedSourceAndMetadata =
					imageSourcesModel.sources().get( imageSetIDs.get( associatedSourceIndex ) );

			// Note: this needs to be done here, because if it is a Lazy Source it might
			// still change its metadata
			final int numTimePoints = getNumTimePoints( associatedSourceAndMetadata.source() );

			applyRecentDisplaySettings( associatedSourceIndex, associatedSourceAndMetadata );
			showSource( associatedSourceAndMetadata, numTimePoints );
		}

		applyRecentViewerSettings();
	}

	public void applyRecentViewerSettings( )
	{
		if ( recentViewerState != null )
		{
			applyViewerStateTransform( recentViewerState );
			applyViewerStateVisibility( recentViewerState );
		}
	}

	public void applyViewerStateVisibility( ViewerState recentViewerState )
	{
		final int numSources = bdv.getViewerPanel().getVisibilityAndGrouping().numSources();
		final List< Integer > visibleSourceIndices = recentViewerState.getVisibleSourceIndices();

		for ( int i = 1; i < numSources; i++ )
		{
			int recentSourceIndex = i;

			if ( ! grayValueOverlayWasFirstSource )
				recentSourceIndex--;

			bdv.getViewerPanel().getVisibilityAndGrouping().
					setSourceActive(
							i,
							visibleSourceIndices.contains( recentSourceIndex ) );
		}
	}


	public void applyViewerStateTransform( ViewerState viewerState )
	{
		final AffineTransform3D transform3D = new AffineTransform3D();
		viewerState.getViewerTransform( transform3D );
		bdv.getViewerPanel().setCurrentViewerTransform( transform3D );
	}

	private void applyRecentDisplaySettings( int associatedSourceIndex,
											 SourceAndMetadata associatedSourceAndMetadata )
	{
		if ( recentConverterSetups != null )
		{
			int recentConverterSetupIndex = associatedSourceIndex;

			if ( grayValueOverlayWasFirstSource )
				recentConverterSetupIndex++;

			final double displayRangeMax = recentConverterSetups.get( recentConverterSetupIndex ).getDisplayRangeMax();
			final double displayRangeMin = recentConverterSetups.get( recentConverterSetupIndex ).getDisplayRangeMin();

			associatedSourceAndMetadata.metadata().contrastLimits = new double[]{ displayRangeMin, displayRangeMax };
		}
	}

	public BdvStackSource showSource( SourceAndMetadata sourceAndMetadata, int numTimePoints )
	{
		final Metadata metadata = sourceAndMetadata.metadata();
		Source< ? > source = sourceAndMetadata.source();

		if ( isLabelSource( metadata) )
		{
			source = asLabelsSource( sourceAndMetadata );
			if ( isLabelMaskShownAsBoundaries ) showLabelMaskAsBoundaries();
		}

		final BdvStackSource bdvStackSource = null;

		bdvStackSource.setActive( true );

		if ( metadata.contrastLimits != null )
			bdvStackSource.setDisplayRange( metadata.contrastLimits[ 0 ], metadata.contrastLimits[ 1 ] );

		// TODO: Implement auto-contrast; Take code from bdp2

		bdv = bdvStackSource.getBdvHandle();

		bdvOptions = bdvOptions.addTo( bdv );

		metadata.bdvStackSource = bdvStackSource;

		currentSources.add( sourceAndMetadata );

		if ( labelsSourceConverter != null )
			bdv.getViewerPanel().addTimePointListener( labelsSourceConverter );

		return bdvStackSource;
	}

	// TODO: clean this up! There should only be one way to decide whether this is a labelSource
	public boolean isLabelSource( Metadata metadata )
	{
		if ( metadata.type != null && metadata.type.equals( Metadata.Type.Segmentation ) )
		{
				return true;
		}
		else if ( metadata.modality == Metadata.Modality.Segmentation  )
		{
			return true;
		}
		else
		{
			return false;
		}

	}

	public int getNumTimePoints( Source< ? > source )
	{
		int numTimePoints = 0;
		while ( source.isPresent( numTimePoints++ ) ){}
		return numTimePoints - 1;
	}

	public void removeSource( BdvStackSource bdvStackSource )
	{
		currentSources.remove( bdvStackSource );
		BdvUtils.removeSource( bdv, bdvStackSource );
	}

	public List< SourceAndMetadata< ? extends RealType< ? > > > getCurrentSources()
	{
		return new ArrayList<>( currentSources );
	}

	private void removeSources()
	{
		recentViewerState = bdv.getViewerPanel().getState();
		recentConverterSetups = new ArrayList<>( bdv.getSetupAssignments().getConverterSetups() );

		final List< SourceState< ? > > sources = recentViewerState.getSources();
		final int numSources = sources.size();

		for ( int i = 0; i < numSources; ++i )
		{
			final Source< ? > source = sources.get( i ).getSpimSource();
			if ( source instanceof PlaceHolderSource )
			{
				if ( i == 0 )
					grayValueOverlayWasFirstSource = true;
			}
			else
			{
				bdv.getViewerPanel().removeSource( source );
				bdv.getSetupAssignments().removeSetup( recentConverterSetups.get( i ) );
			}
		}
	}


	/**
	 * Currently, the logic is that there can be only one labels source
	 * within this SegmentsBdvView class. Thus, creation of the labels source
	 * also registers it as "the" labelsSource
	 *
	 * @param sourceAndMetadata
	 * @return
	 */
	private Source asLabelsSource(
			SourceAndMetadata< ? extends RealType< ? > > sourceAndMetadata )
	{
		this.labelsSource = sourceAndMetadata;

		final LabelsARGBConverter labelsARGBConverter =
				createLabelsARGBConverter( labelsSource );

		this.labelsSourceConverter = labelsARGBConverter;

		final ARGBConvertedRealSource convertedRealSource =
				new ARGBConvertedRealSource( sourceAndMetadata.source(), labelsARGBConverter );

		return convertedRealSource;
	}

	private LabelsARGBConverter createLabelsARGBConverter( SourceAndMetadata labelsSource )
	{
		LabelsARGBConverter labelsARGBConverter;

		if ( labelFrameAndImageToSegment == null )
		{
			labelsARGBConverter = new LazyLabelsARGBConverter();
		}
		else
		{
			labelsARGBConverter =
					new SegmentsARGBConverter(
							labelFrameAndImageToSegment,
							labelsSource.metadata().imageId,
							selectionColoringModel );

		}

		return labelsARGBConverter;
	}

	private void initBdvOptions( )
	{
		bdvOptions = BdvOptions.options();


		if ( imageSourcesModel.is2D() )
			bdvOptions = bdvOptions.is2D();

		if ( bdv != null )
			bdvOptions = bdvOptions.addTo( bdv );
	}

	private void installBdvBehaviours()
	{
		segmentsName = segments.toString();

		behaviours = new Behaviours( new InputTriggerConfig() );
		behaviours.install(
				bdv.getBdvHandle().getTriggerbindings(),
				segmentsName + "-bdv-select-handler" );

		installSelectionBehaviour();
		installSelectNoneBehaviour();
		installSelectionColoringModeBehaviour();
		installRandomColorShufflingBehaviour();
		installShowLabelMaskAsBinaryMaskBehaviour();
		installShowLabelMaskAsBoundaryBehaviour();
		install3DViewBehaviour();
		installImageSetNavigationBehaviour( );

		BdvPopupMenus.addAction( bdv,
				"Change animation settings...",
				( x, y ) -> new Thread( () -> changeAnimationSettingsUI() ).start()
			);
	}

	private void changeAnimationSettingsUI()
	{
		final GenericDialog genericDialog = new GenericDialog( "Animation settings" );
		genericDialog.addNumericField( "Animation duration [ms]", segmentFocusAnimationDurationMillis, 0 );
		genericDialog.showDialog();
		if ( genericDialog.wasCanceled() ) return;
		segmentFocusAnimationDurationMillis = ( int ) genericDialog.getNextNumber();
	}

	private void installRandomColorShufflingBehaviour()
	{
		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) ->
						new Thread( () -> shuffleRandomColors() ).start(),
					segmentsName + "-change-color-random-seed",
						incrementCategoricalLutRandomSeedTrigger );
	}

	private void installShowLabelMaskAsBinaryMaskBehaviour()
	{
		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) ->
						new Thread( () -> toggleLabelMaskAsBinaryMask() ).start(),
				segmentsName + "-asMask",
				labelMaskAsBinaryMaskTrigger );
	}

	private void installShowLabelMaskAsBoundaryBehaviour()
	{
		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) ->
						new Thread( () -> toggleLabelMaskAsBoundaries() ).start(),
				segmentsName + "-asBoundaries",
				labelMaskAsBoundaryTrigger );
	}

	private synchronized void shuffleRandomColors()
	{
		if ( ! isLabelSourceActive() ) return;

		final ColoringModel< T > coloringModel =
				selectionColoringModel.getColoringModel();

		if ( coloringModel instanceof CategoryColoringModel )
		{
			( ( CategoryColoringModel ) coloringModel ).incRandomSeed();
			BdvUtils.repaint( bdv );
		}
	}

	private synchronized void toggleLabelMaskAsBinaryMask()
	{
		if ( ! isLabelSourceActive() ) return;

		if ( isLabelMaskShownAsBinaryMask )
			labelsSourceConverter.setSingleColor( null );
		else
			labelsSourceConverter.setSingleColor( labelSourceSingleColor );

		isLabelMaskShownAsBinaryMask = !isLabelMaskShownAsBinaryMask;

		BdvUtils.repaint( bdv );
	}

	private synchronized void toggleLabelMaskAsBoundaries()
	{
		if ( ! isLabelSourceActive() ) return;

		final ModifiableRandomAccessibleIntervalSource4D modifiableSource = getModifiableSource( labelsSource.source() );

		if ( modifiableSource == null ) return;

		if ( ! isLabelMaskShownAsBoundaries )
		{
			final GenericDialog gd = new GenericDialog( "Boundary thickness" );
			gd.addNumericField( "Boundary thickness [pixels]", 1, 1 );
			gd.showDialog();
			if ( gd.wasCanceled() ) return;
			labelMaskBoundaryThickness = (int) gd.getNextNumber();
		}

		final RandomAccessibleIntervalNeighborhoodFilter filter = new RandomAccessibleIntervalNeighborhoodFilter(
				new NeighborhoodNonZeroBoundariesConverter( ),
				new HyperSphereShape( labelMaskBoundaryThickness ) );

		if ( isLabelMaskShownAsBoundaries )
			modifiableSource.setFilter( null );
		else
			modifiableSource.setFilter( filter );

		isLabelMaskShownAsBoundaries = ! isLabelMaskShownAsBoundaries;

		BdvUtils.repaint( bdv );
	}

	private void showLabelMaskAsBoundaries()
	{
		final ModifiableRandomAccessibleIntervalSource4D modifiableSource = getModifiableSource( labelsSource.source() );
		if ( modifiableSource == null ) return;
		final RandomAccessibleIntervalNeighborhoodFilter filter = new RandomAccessibleIntervalNeighborhoodFilter(
				new NeighborhoodNonZeroBoundariesConverter( ),
				new HyperSphereShape( labelMaskBoundaryThickness ) );
		modifiableSource.setFilter( filter );
	}

	public ModifiableRandomAccessibleIntervalSource4D getModifiableSource( Source< ? > source )
	{
		if ( source instanceof ModifiableRandomAccessibleIntervalSource4D )
			return ( ModifiableRandomAccessibleIntervalSource4D ) source;
		else if ( source instanceof ImagePlusFileSource )
			return  ( ModifiableRandomAccessibleIntervalSource4D ) ( ( ImagePlusFileSource ) source ).getWrappedSource()   ;
		else
		{
			Logger.warn( "Cannot create boundaries of label mask of type: " + labelsSource.source().getClass().toString() );
			return null;
		}
	}

	public void setLabelSourceSingleColor( ARGBType labelSourceSingleColor )
	{
		this.labelSourceSingleColor = labelSourceSingleColor;
		labelsSourceConverter.setSingleColor( labelSourceSingleColor );
		isLabelMaskShownAsBinaryMask = true;
		BdvUtils.repaint( bdv );
	}

	private void installSelectNoneBehaviour( )
	{
		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) ->
				new Thread( () -> selectNone() ).start(),
				segmentsName + "-select-none", selectNoneTrigger );
	}

	public synchronized void selectNone()
	{
		if ( ! isLabelSourceActive() ) return;

		selectionModel.clearSelection( );

		BdvUtils.repaint( bdv );
	}

	private void installSelectionBehaviour()
	{
		behaviours.behaviour(
				( ClickBehaviour ) ( x, y ) ->
						new Thread( () -> toggleSelectionAtMousePosition() ).start(),
				segmentsName + "-toggle-select", selectTrigger ) ;
	}


	private void installImageSetNavigationBehaviour()
	{
		behaviours.behaviour(
				( ClickBehaviour ) ( x, y ) ->
						new Thread( () -> changeImageSet( +1 ) ).start(),
				segmentsName + "-next-image-set", nextImageSetTrigger ) ;


		behaviours.behaviour(
				( ClickBehaviour ) ( x, y ) ->
						new Thread( () -> changeImageSet( -1 ) ).start(),
				segmentsName + "-previous-image-set", previousImageSetTrigger ) ;

	}

	private synchronized void changeImageSet( int di )
	{
		int i = labelSourceIds.indexOf( labelsSource.metadata().imageId );

		i += di;
		if ( i >= 0 && i < labelSourceIds.size() )
			updateImageSet( labelSourceIds.get( i ) );
	}

	private void initLabelSourceIds()
	{
		final ArrayList< String > sourceIds =
				new ArrayList( imageSourcesModel.sources().keySet() );
		labelSourceIds = new ArrayList<>();

		for ( String sourceId : sourceIds )
			if ( imageSourcesModel.sources().get( sourceId ).metadata()
					.modality.equals( Metadata.Modality.Segmentation ) )
				labelSourceIds.add( sourceId );
	}

	private synchronized void toggleSelectionAtMousePosition()
	{
		if ( segments == null ) return;

		if ( ! isLabelSourceActive() ) return;

		final double labelId = getLabelIdAtCurrentMouseCoordinates( labelsSource );

		selectAndFocus( labelId );
	}

	public void selectAndFocus( double labelId )
	{
		if ( labelId == BACKGROUND ) return;

		final String labelImageId = labelsSource.metadata().imageId;

		final LabelFrameAndImage labelFrameAndImage =
				new LabelFrameAndImage( labelId, getCurrentTimePoint(),labelImageId );

		final T segment = labelFrameAndImageToSegment.get( labelFrameAndImage );

		selectionModel.toggle( segment );

		if ( selectionModel.isSelected( segment ) )
		{
			recentFocus = segment;
			selectionModel.focus( segment );
		}
	}

	public void select( List< Double > labelIds )
	{
		List< T > segments = getSegments( labelIds );

		selectionModel.setSelected( segments, true );
	}

	private List< T > getSegments( List< Double > labelIds )
	{
		final String labelImageId = labelsSource.metadata().imageId;

		ArrayList< T > segments = new ArrayList<>(  );

		for ( Double labelId : labelIds )
		{
			final LabelFrameAndImage labelFrameAndImage =
					new LabelFrameAndImage( labelId, getCurrentTimePoint(), labelImageId );

			segments.add( labelFrameAndImageToSegment.get( labelFrameAndImage ) );
		}
		return segments;
	}

	private boolean isLabelSourceActive()
	{
		final Source< ? > source
				= labelsSource.metadata().bdvStackSource
				.getSources().get( 0 ).getSpimSource();

		final boolean active = BdvUtils.isActive( bdv, source );

		return active;
	}

	private double getLabelIdAtCurrentMouseCoordinates( SourceAndMetadata labelSource )
	{
		final RealPoint globalMouseCoordinates =
				BdvUtils.getGlobalMouseCoordinates( bdv );

		final Double value = BdvUtils.getPixelValue(
				labelSource.source(),
				globalMouseCoordinates,
				getCurrentTimePoint() );

		if ( value == null )
			System.err.println(
					"Could not find pixel value at position " + globalMouseCoordinates);

		return value;
	}

	private void installSelectionColoringModeBehaviour( )
	{
		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) ->
				new Thread( () ->
				{
					if ( ! isLabelSourceActive() ) return;
					selectionColoringModel.iterateSelectionMode();
					BdvUtils.repaint( bdv );
				} ).start(),
				segmentsName + "-iterate-select", iterateSelectionModeTrigger );
	}

	private void install3DViewBehaviour()
	{
		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) ->

				new Thread( () -> {

					if ( labelsSource == null ) return;
					if ( ! isLabelSourceActive() ) return;
					if ( getLabelIdAtCurrentMouseCoordinates( labelsSource ) == BACKGROUND ) return;

					viewObjectAtCurrentMouseCoordinatesIn3D( labelsSource );

				}).start(),
				segmentsName + "-view-3d", viewIn3DTrigger );
	}

	private synchronized void viewObjectAtCurrentMouseCoordinatesIn3D(
			SourceAndMetadata activeLabelSource )
	{
		new ConnectedComponentExtractorAnd3DViewer( activeLabelSource.source() )
				.extractAndShowIn3D(
						BdvUtils.getGlobalMouseCoordinates( bdv ),
						voxelSpacing3DView );
	}

	private int getCurrentTimePoint()
	{
		return bdv.getBdvHandle().getViewerPanel().getState().getCurrentTimepoint();
	}

	public void close()
	{
		// TODO
	}
}
