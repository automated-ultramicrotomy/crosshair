package de.embl.cba.bdv.utils.selection;

import bdv.util.Bdv;
import de.embl.cba.bdv.utils.*;
import de.embl.cba.bdv.utils.behaviour.BehaviourRandomColorLutSeedChangeEventHandler;
import de.embl.cba.bdv.utils.converters.RandomARGBConverter;
import de.embl.cba.bdv.utils.converters.SelectableVolatileARGBConverter;
import de.embl.cba.bdv.utils.objects3d.ConnectedComponentExtractorAnd3DViewer;
import de.embl.cba.bdv.utils.sources.SelectableARGBConvertedRealSource;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class BdvSelectionEventHandler
{
	final Bdv bdv;
	final SelectableARGBConvertedRealSource source;
	final SelectableVolatileARGBConverter selectableConverter;
	final String sourceName;

	Behaviours behaviours;

	private String selectTrigger = "ctrl button1";
	private String selectNoneTrigger = "ctrl Q";
	private String iterateSelectionModeTrigger = "ctrl S";
	private String viewIn3DTrigger = "ctrl shift button1";

	private CopyOnWriteArrayList< BdvLabelSourceSelectionListener > bdvLabelSourceSelectionListeners;
	private List< SelectableVolatileARGBConverter.SelectionMode > selectionModes;
	private double resolution3DView;
	private static final int BACKGROUND = 0;

	/**
	 * Selection of argbconversion (objects) in a label source.
	 * @param bdv Bdv window in which the source is shown.
	 */
	public BdvSelectionEventHandler( Bdv bdv,
									 SelectableARGBConvertedRealSource selectableSource )
	{
		this.bdv = bdv;
		this.source = selectableSource;
		this.selectableConverter = selectableSource.getSelectableConverter();
		this.sourceName = source.getName();

		this.bdvLabelSourceSelectionListeners = new CopyOnWriteArrayList<>(  );
		this.selectionModes = Arrays.asList( SelectableVolatileARGBConverter.SelectionMode.values() );

		this.resolution3DView = 0.2;

		installBdvBehaviours();
	}

	public void set3DObjectViewResolution( double resolution3DView )
	{
		this.resolution3DView = resolution3DView;
	}

	private void installBdvBehaviours()
	{
		behaviours = new Behaviours( new InputTriggerConfig() );
		behaviours.install( bdv.getBdvHandle().getTriggerbindings(),  sourceName + "-bdv-selection-handler" );

		installSelectionBehaviour( );
		installSelectNoneBehaviour( );
		installSelectionModeIterationBehaviour( );
		installRandomColorShufflingBehaviour();

		if( is3D() ) install3DViewBehaviour();
	}

	private void installRandomColorShufflingBehaviour()
	{
		if ( selectableConverter.getWrappedConverter() instanceof RandomARGBConverter )
		{
			new BehaviourRandomColorLutSeedChangeEventHandler(
					bdv,
					( RandomARGBConverter ) selectableConverter.getWrappedConverter(),
					sourceName );

		}
	}

	private boolean is3D()
	{
		return source.getWrappedSource( 0, 0 ).numDimensions() == 3;
	}

	private void install3DViewBehaviour( )
	{
		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) ->
		{
			if ( BdvUtils.isActive( bdv, source ) )
			{
				viewIn3D();
			}
		}, sourceName + "-view-3d", viewIn3DTrigger );
	}

	private void viewIn3D()
	{
		new Thread( new Runnable()
		{
			@Override
			public void run()
			{
				new ConnectedComponentExtractorAnd3DViewer( source )
						.extractAndShowIn3D(
								BdvUtils.getGlobalMouseCoordinates( bdv ),
								resolution3DView );
			}
		} ).start();
	}

	private void installSelectionModeIterationBehaviour( )
	{
		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) ->
		{
			if ( BdvUtils.isActive( bdv, source ) )
			{
				iterateSelectionMode();
			}
		}, sourceName + "-iterate-selection", iterateSelectionModeTrigger );
	}

	private void iterateSelectionMode()
	{
		final int selectionModeIndex = selectionModes.indexOf( selectableConverter.getSelectionMode() );

		if ( selectionModeIndex < selectionModes.size() -1 )
		{
			selectableConverter.setSelectionMode( selectionModes.get( selectionModeIndex + 1 ) );
		}
		else
		{
			selectableConverter.setSelectionMode( selectionModes.get( 0 ) );
		}

		BdvUtils.repaint( bdv );
	}

	private void installSelectNoneBehaviour( )
	{
		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) ->
		{
			if ( BdvUtils.isActive( bdv, source ) )
			{
				selectNone();
			}
		}, sourceName + "-select-none", selectNoneTrigger );
	}

	public void selectNone()
	{
//		final Map< Integer, Collection< Double > > selections = selectableConverter.getSelections();
//
//		for ( final BdvLabelSourceSelectionListener s : bdvLabelSourceSelectionListeners )
//		{
//			for ( int timePoint : selections.keySet() )
//			{
//				for ( Double selection : selections.get( timePoint ) )
//				{
//					s.selectionChanged( selection, timePoint, false );
//				}
//			}
//		}

		//selectableConverter.clearSelections( );

		BdvUtils.repaint( bdv );
	}

	private void installSelectionBehaviour()
	{
		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) ->
		{
			if ( BdvUtils.isActive( bdv, source ) )
			{
				toggleSelectionAtMousePosition();
			}
		}, sourceName+"-toggle-selection", selectTrigger ) ;
	}

	private void toggleSelectionAtMousePosition()
	{
		final double selected = BdvUtils.getPixelValue(
				source,
				BdvUtils.getGlobalMouseCoordinates( bdv ),
				getCurrentTimepoint() );

		if ( selected == BACKGROUND ) return;

		final int currentTimepoint = getCurrentTimepoint();

		if ( isNewSelection( selected, currentTimepoint ) )
		{
			addSelectionAndNotifyListeners( selected, currentTimepoint );
		}
		else
		{
			removeSelectionAndNotifyListeners( selected, currentTimepoint );
		}

		requestRepaint();
	}

	private void removeSelectionAndNotifyListeners( double selected, int currentTimepoint )
	{
//		selectableConverter.selectionChanged( selected, currentTimepoint, false );
//
//		for ( final BdvLabelSourceSelectionListener s : bdvLabelSourceSelectionListeners )
//			s.selectionChanged( selected, currentTimepoint, false );
	}

	private void addSelectionAndNotifyListeners( double selected, int currentTimepoint )
	{
		selectionChanged( selected, currentTimepoint, true );

		for ( final BdvLabelSourceSelectionListener s : bdvLabelSourceSelectionListeners )
			s.selectionChanged( selected, currentTimepoint, true );
	}

	private int getCurrentTimepoint()
	{
		return bdv.getBdvHandle().getViewerPanel().getState().getCurrentTimepoint();
	}

	private boolean isNewSelection( double selected, int timepoint )
	{
//		if ( selectableConverter.getSelections() == null ) return true;
//
//		if ( selectableConverter.getSelections().get( timepoint ) == null ) return true;
//
//		if ( selectableConverter.getSelections().get( timepoint ).contains( selected ) ) return false;

		return true;
	}

	public void selectionChanged( double label, int timepoint, boolean selected )
	{
		if ( isNewSelection( label, timepoint  ) )
		{
//			selectableConverter.selectionChanged( label, timepoint, true );
		}
	}

	public void addSelectionEventListener( BdvLabelSourceSelectionListener s )
	{
		bdvLabelSourceSelectionListeners.add( s );
	}

	public Bdv getBdv()
	{
		return bdv;
	}

	public SelectableVolatileARGBConverter getSelectableConverter()
	{
		return selectableConverter;
	}

	public void requestRepaint()
	{
		BdvUtils.repaint( bdv );
	}


}

