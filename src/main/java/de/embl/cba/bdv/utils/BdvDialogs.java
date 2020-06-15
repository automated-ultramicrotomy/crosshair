package de.embl.cba.bdv.utils;

import bdv.tools.boundingbox.BoxSelectionOptions;
import bdv.tools.boundingbox.TransformedRealBoxSelectionDialog;
import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.SliderPanelDouble;
import bdv.util.*;
import bdv.viewer.Source;
import bdv.viewer.VisibilityAndGrouping;
import bdv.viewer.state.SourceState;
import de.embl.cba.bdv.utils.converters.LinearARGBConverter;
import ij.IJ;
import ij.gui.GenericDialog;
import net.imglib2.FinalRealInterval;
import net.imglib2.RealInterval;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static de.embl.cba.bdv.utils.BdvUtils.getSourceIndicesAtSelectedPoint;

public abstract class BdvDialogs
{
	private static JFrame displaySettingsFrame;

	public static void addSourceDisplaySettingsUI( JPanel panel,
												   String name,
												   Bdv bdv,
												   Integer sourceIndex,
												   Color color )
	{

		final ArrayList< Integer > indices = new ArrayList<>();
		indices.add( sourceIndex );

		addSourcesDisplaySettingsUI(  panel,
				 name,
				 bdv,
				 indices,
				 color, 0.0, 65535.0 );

	}

	public static JPanel getSourceDisplaySettingsPanel(
			Bdv bdv,
			String name,
			Integer sourceIndex,
			Color color,
			double rangeMin,
			double rangeMax )
	{
		final ArrayList< Integer > indices = new ArrayList<>();
		indices.add( sourceIndex );

		final JPanel panel = getSourcesDisplaySettingsPanel(
				bdv,
				name,
				indices,
				color,
				rangeMin,
				rangeMax );

		return panel;
	}

	/**
	 * Show display setting for single source
	 *
	 * @param bdv
	 * @param name
	 * @param sourceIndex
	 * @param color
	 * @param rangeMin
	 * @param rangeMax
	 * @return
	 */
	public static JPanel getSourceDisplaySettingsPanel(
			Bdv bdv,
			String name,
			Integer sourceIndex,
			ARGBType color,
			double rangeMin,
			double rangeMax )
	{
		final ArrayList< Integer > indices = new ArrayList<>();
		indices.add( sourceIndex );

		final JPanel panel = getSourcesDisplaySettingsPanel(
				bdv,
				name,
				indices,
				new Color( color.get() ),
				rangeMin,
				rangeMax );

		return panel;
	}


	public static JPanel getSourceDisplaySettingsPanel(
			Bdv bdv,
			Integer sourceIndex,
			double rangeMin,
			double rangeMax )
	{
		final JPanel panel = getSourceDisplaySettingsPanel(
				bdv,
				BdvUtils.getSourceName( bdv, sourceIndex ),
				sourceIndex,
				BdvUtils.getSourceColor( bdv, sourceIndex ),
				rangeMin,
				rangeMax );

		return panel;
	}

	public static JPanel addSourcesDisplaySettingsUI( JPanel parentPanel,
													  String name,
													  Bdv bdv,
													  ArrayList< Integer > sourceIndices,
													  Color color,
													  double rangeMin,
													  double rangeMax )
	{
		JPanel panel =
				getSourcesDisplaySettingsPanel(
						bdv,
						name,
						sourceIndices,
						color,
						rangeMin,
						rangeMax );

		parentPanel.add( panel );

		return panel;
	}

	/**
	 * Show display settings for multiple sources.
	 * There is only one UI, but the settings will be applied to all sources.
	 *
	 * @param bdv
	 * @param name
	 * @param sourceIndices
	 * @param color
	 * @param rangeMin
	 * @param rangeMax
	 * @return
	 */
	public static JPanel getSourcesDisplaySettingsPanel(
			Bdv bdv,
			String name,
			ArrayList< Integer > sourceIndices,
			Color color,
			double rangeMin,
			double rangeMax )
	{
		JPanel panel = new JPanel();

		panel.setLayout( new BoxLayout( panel, BoxLayout.LINE_AXIS ) );
		panel.setBorder( BorderFactory.createEmptyBorder(
				0,10,0,10) );
		panel.add( Box.createHorizontalGlue() );
		panel.setOpaque( true );

		if ( color.equals( Color.BLACK ) )
			panel.setBackground( Color.WHITE );
		else
			panel.setBackground( color );

		JLabel jLabel = new JLabel( name );
		jLabel.setHorizontalAlignment( SwingConstants.CENTER );

		panel.add( jLabel );

		int[] buttonDimensions = new int[]{ 50, 30 };

		panel.add( createColorButton( panel, buttonDimensions,
				bdv, sourceIndices ) );

		panel.add( createBrightnessButton( buttonDimensions, name,
				bdv, sourceIndices, rangeMin, rangeMax ) );

		panel.add( createVisibilityCheckbox( buttonDimensions, bdv,
				sourceIndices ) );
		return panel;
	}

	public static JButton createColorButton( JComponent component,
											 int[] buttonDimensions,
											 Bdv bdv,
											 ArrayList< Integer > sourceIndices )
	{
		JButton colorButton = new JButton( "C" );
		colorButton.setPreferredSize( new Dimension(
				buttonDimensions[ 0 ], buttonDimensions[ 1 ] ) );

		colorButton.addActionListener( e -> {

			Color color = JColorChooser.showDialog(
					null, "", null );
			if ( color == null ) return;

			for ( int i : sourceIndices )
			{
				bdv.getBdvHandle().getSetupAssignments()
						.getConverterSetups().get( i )
						.setColor( BdvUtils.asArgbType( color ) );
			}

			component.setBackground( color );
		} );


		return colorButton;
	}

	public static JButton createColorButton( JPanel panel,
											 int[] buttonDimensions,
											 BdvSource bdvSource )
	{
		JButton colorButton;
		colorButton = new JButton( "C" );

		colorButton.setPreferredSize(
				new Dimension( buttonDimensions[ 0 ], buttonDimensions[ 1 ] ) );

		colorButton.addActionListener( e -> {
			Color color = JColorChooser.showDialog( null, "", null );
			if ( color == null ) return;
			bdvSource.setColor( BdvUtils.asArgbType( color ) );
			panel.setBackground( color );
		} );

		return colorButton;
	}

	public static JButton createColorButtonWithColoredBackground(
			int[] buttonDimensions,
			BdvSource bdvSource,
			Color initialColor )
	{
		JButton colorButton;
		colorButton = new JButton( " " );
		colorButton.setBackground( initialColor );

		colorButton.setPreferredSize(
				new Dimension( buttonDimensions[ 0 ], buttonDimensions[ 1 ] ) );

		colorButton.addActionListener( e -> {
			Color color = JColorChooser.showDialog( null, "", null );
			if ( color == null ) return;
			bdvSource.setColor( BdvUtils.asArgbType( color ) );
			colorButton.setBackground( color );
		} );

		return colorButton;
	}

	public static JButton createBrightnessButton( int[] buttonDimensions,
												  String name, Bdv bdv,
												  Integer sourceIndex,
												  double rangeMin,
												  double rangeMax )
	{
		final ArrayList< Integer > indices = new ArrayList<>();
		indices.add( sourceIndex );
		return createBrightnessButton( buttonDimensions, name, bdv,
				indices, rangeMin, rangeMax );
	}



	public static JButton createBrightnessButton( int[] buttonDimensions,
												  String name, Bdv bdv,
												  ArrayList< Integer > sourceIndices,
												  double rangeMin,
												  double rangeMax )
	{
		JButton button = new JButton( "B" );
		button.setPreferredSize(
				new Dimension(
						buttonDimensions[ 0 ],
						buttonDimensions[ 1 ] ) );

		button.addActionListener( e -> {
			final ArrayList< ConverterSetup > converterSetups = new ArrayList<>();

			for ( int i : sourceIndices )
			{
				converterSetups.add( bdv.getBdvHandle()
						.getSetupAssignments()
						.getConverterSetups().get( i ) );
			}

			showBrightnessDialog( name,
					converterSetups,
					rangeMin,
					rangeMax );
		} );

		return button;
	}


	public static JButton createBrightnessButton( int[] buttonDimensions,
												  String name,
												  BdvStackSource bdvStackSource,
												  final double rangeMin,
												  final double rangeMax )
	{
		JButton button = new JButton( "B" );
		button.setPreferredSize( new Dimension(
				buttonDimensions[ 0 ],
				buttonDimensions[ 1 ] ) );

		button.addActionListener( e ->
		{
			final ArrayList< ConverterSetup > converterSetups
					= getConverterSetups( bdvStackSource );

			showBrightnessDialog(
					name,
					converterSetups,
					rangeMin,
					rangeMax );
		} );

		return button;
	}

	private static ArrayList< ConverterSetup > getConverterSetups(
			BdvStackSource bdvStackSource )
	{
		bdvStackSource.setCurrent();
		final int sourceIndex = bdvStackSource.getBdvHandle()
				.getViewerPanel().getVisibilityAndGrouping().getCurrentSource();
		final ArrayList< ConverterSetup > converterSetups = new ArrayList<>();
		converterSetups.add( bdvStackSource.getBdvHandle()
				.getSetupAssignments().getConverterSetups().get( sourceIndex ) );
		return converterSetups;
	}


//	public static void showBrightnessDialog( String name,
//											 ConverterSetup converterSetup )
//	{
//		final ArrayList< ConverterSetup > converterSetups = new ArrayList<>();
//		converterSetups.add( converterSetup );
//
//		showBrightnessDialog(
//				name,
//				converterSetups,
//				0,
//				3 * currentRangeMax );
//	}

	public static void showBrightnessDialog(
			String name,
			ArrayList< ConverterSetup > converterSetups,
			double rangeMin,
			double rangeMax )
	{
		JFrame frame = new JFrame( name );
		frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );

		final double currentRangeMin = converterSetups.get( 0 ).getDisplayRangeMin();
		final double currentRangeMax = converterSetups.get( 0 ).getDisplayRangeMax();

		final BoundedValueDouble min =
				new BoundedValueDouble(
						rangeMin,
						rangeMax,
						currentRangeMin );

		final BoundedValueDouble max =
				new BoundedValueDouble(
						rangeMin,
						rangeMax,
						currentRangeMax );

		double spinnerStepSize = ( currentRangeMax - currentRangeMin ) / 100.0;

		JPanel panel = new JPanel();
		panel.setLayout( new BoxLayout( panel, BoxLayout.PAGE_AXIS ) );
		final SliderPanelDouble minSlider =
				new SliderPanelDouble( "Min", min, spinnerStepSize );
		minSlider.setNumColummns( 7 );
		minSlider.setDecimalFormat( "####E0" );

		final SliderPanelDouble maxSlider =
				new SliderPanelDouble( "Max", max, spinnerStepSize );
		maxSlider.setNumColummns( 7 );
		maxSlider.setDecimalFormat( "####E0" );

		final BrightnessUpdateListener brightnessUpdateListener =
				new BrightnessUpdateListener(
						min, max, minSlider, maxSlider, converterSetups );

		min.setUpdateListener( brightnessUpdateListener );
		max.setUpdateListener( brightnessUpdateListener );

		panel.add( minSlider );
		panel.add( maxSlider );

		frame.setContentPane( panel );

		//Display the window.
		frame.setBounds( MouseInfo.getPointerInfo().getLocation().x,
				MouseInfo.getPointerInfo().getLocation().y,
				120, 10);
		frame.setResizable( false );
		frame.pack();
		frame.setVisible( true );

	}

	public static void showBrightnessDialog(
			Bdv bdv,
			String name,
			LinearARGBConverter linearARGBConverter )
	{
		JFrame frame = new JFrame( name );

		frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );

		final BoundedValueDouble min = new BoundedValueDouble(
				linearARGBConverter.getMin(),
				linearARGBConverter.getMax(),
				linearARGBConverter.getMin() );

		final BoundedValueDouble max = new BoundedValueDouble(
				linearARGBConverter.getMin(),
				linearARGBConverter.getMax(),
				linearARGBConverter.getMax() );

		JPanel panel = new JPanel();
		panel.setLayout( new BoxLayout( panel, BoxLayout.PAGE_AXIS ) );
		final SliderPanelDouble minSlider = new SliderPanelDouble( "Min", min, 1 );
		final SliderPanelDouble maxSlider = new SliderPanelDouble( "Max", max, 1 );

		class UpdateListener implements BoundedValueDouble.UpdateListener
		{
			@Override
			public void update()
			{
				linearARGBConverter.setMin( min.getCurrentValue() );
				linearARGBConverter.setMax( max.getCurrentValue() );
				BdvUtils.repaint( bdv );
			}
		}

		final UpdateListener updateListener = new UpdateListener();

		min.setUpdateListener( updateListener );
		max.setUpdateListener( updateListener );

		panel.add( minSlider );
		panel.add( maxSlider );

		frame.setContentPane( panel );

		//Display the window.
		frame.setBounds( MouseInfo.getPointerInfo().getLocation().x,
				MouseInfo.getPointerInfo().getLocation().y,
				120, 10);
		frame.setResizable( false );
		frame.pack();
		frame.setVisible( true );

	}

	public static void showGenericBrightnessDialog( ConverterSetup converterSetup )
	{
		GenericDialog gd = new GenericDialog( "LUT max value" );
		gd.addNumericField( "LUT max value: ", converterSetup.getDisplayRangeMax(), 0 );
		gd.showDialog();
		converterSetup.setDisplayRange( converterSetup.getDisplayRangeMin(), ( int ) gd.getNextNumber() );
	}

	public static JCheckBox createVisibilityCheckbox( int[] buttonDimensions,
													  Bdv bdv,
													  ArrayList< Integer > sourceIndices )
	{
		JCheckBox checkBox = new JCheckBox( "" );
		checkBox.setSelected( BdvUtils.isActive( bdv, sourceIndices.get( 0 ) ) );
		checkBox.setPreferredSize(
				new Dimension(
						buttonDimensions[ 0 ],
						buttonDimensions[ 1 ] ) );

		checkBox.addActionListener( e -> {

			final VisibilityAndGrouping visibilityAndGrouping
					= bdv.getBdvHandle().getViewerPanel().getVisibilityAndGrouping();

			for ( int sourceIndex : sourceIndices )
				visibilityAndGrouping.setSourceActive( sourceIndex, checkBox.isSelected() );

		} );

		return checkBox;
	}


	public static JCheckBox createVisibilityCheckbox(
			int[] dims,
			BdvSource bdvSource,
			boolean isVisible )
	{
		JCheckBox checkBox = new JCheckBox( "" );
		checkBox.setSelected( isVisible );
		checkBox.setPreferredSize( new Dimension( dims[ 0 ], dims[ 1 ] ) );

		checkBox.addActionListener( e ->
				bdvSource.setActive( checkBox.isSelected() ) );

		return checkBox;
	}

	public static void addDisplaySettingsUI( Bdv bdv, JPanel panel )
	{
		final java.util.List< ConverterSetup > converterSetups =
				bdv.getBdvHandle().getSetupAssignments().getConverterSetups();
		final java.util.List< SourceState< ? > > sources =
				bdv.getBdvHandle().getViewerPanel().getState().getSources();
		final List< Integer > nonOverlaySourceIndices =
				BdvUtils.getNonOverlaySourceIndices( bdv, sources );
		ArrayList< Color > defaultColors = BdvUtils.getColors( nonOverlaySourceIndices );

		final HashMap< String, JPanel > nameToDisplayPanel = new HashMap<>();

		int iColor = 0;
		for ( int sourceIndex : nonOverlaySourceIndices )
		{
			final Color color = defaultColors.get( iColor++ );

			converterSetups.get( sourceIndex ).setColor( BdvUtils.asArgbType( color ) );

			final ArrayList< Integer > indices = new ArrayList<>( );
			indices.add( sourceIndex );

			String name = BdvUtils.getSourceName( bdv, sourceIndex );

			addSourcesDisplaySettingsUI( panel, name, bdv, indices, color, 0.0, 65535.0 );
		}

	}

	public static void showDisplaySettingsDialogForSourcesAtMousePosition(
			BdvHandle bdv,
			boolean allowMultipleDialogs,
			boolean evalSourcesAtPointIn2D )
	{
		final RealPoint point = BdvUtils.getGlobalMouseCoordinates( bdv );
		final ArrayList< Integer > indices =
				getSourceIndicesAtSelectedPoint( bdv, point, evalSourcesAtPointIn2D );

		if ( indices.size() == 0 ) return;

		JPanel panel = new JPanel();
		panel.setLayout( new BoxLayout( panel, BoxLayout.PAGE_AXIS ) );

		for ( int index : indices )
		{
			final JPanel settingsPanel = getSourceDisplaySettingsPanel(
					bdv,
					index,
					0.0,  // TODO: how to set?
					65535.0 // TODO: how to set?
			);

			panel.add( settingsPanel );
		}


		JFrame frame = getDisplaySettingsFrame( allowMultipleDialogs );

		frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
		frame.setContentPane( panel );
		frame.setBounds(
				MouseInfo.getPointerInfo().getLocation().x,
				MouseInfo.getPointerInfo().getLocation().y,
				120,
				10 );
		frame.setResizable( false );
		frame.pack();
		frame.setVisible( true );
	}

	public static JFrame getDisplaySettingsFrame( boolean allowMultipleDialogs )
	{
		if ( allowMultipleDialogs )
			return new JFrame( "Adjust Display Settings" );
		else
		{
			if ( displaySettingsFrame != null )
				displaySettingsFrame.dispose();

			displaySettingsFrame = new JFrame( "Adjust Display Settings" );
			return displaySettingsFrame;
		}
	}

	/**
	 *
	 * The source is used to select the maximal range of the selection.
	 *
	 * @param bdvHandle
	 * @param maximalRangeInterval
	 * @return
	 */
	public static TransformedRealBoxSelectionDialog.Result showBoundingBoxDialog(
			BdvHandle bdvHandle,
			FinalRealInterval maximalRangeInterval )
	{
		final FinalRealInterval viewerInterval =
				BdvUtils.getViewerGlobalBoundingInterval( bdvHandle );

		final FinalRealInterval initialInterval =
				getInitialBoundingBoxInterval( viewerInterval, maximalRangeInterval );

		final AffineTransform3D boxTransform = new AffineTransform3D();

		final int currentTimepoint =
				bdvHandle.getViewerPanel().getState().getCurrentTimepoint();

		final int numTimepoints =
				bdvHandle.getViewerPanel().getState().getNumTimepoints();

		final TransformedRealBoxSelectionDialog.Result result = BdvFunctions.selectRealBox(
				bdvHandle,
				boxTransform,
				initialInterval,
				maximalRangeInterval,
				BoxSelectionOptions.options()
						.title( "Select Region" )
						.initialTimepointRange( currentTimepoint, currentTimepoint )
						.selectTimepointRange( 0, numTimepoints )
		);

		// TODO: remove this once, this issue is resolved:
		// https://github.com/bigdataviewer/bigdataviewer-core/issues/63
		IJ.wait( 100 );

		return result;
	}

	private static FinalRealInterval getInitialBoundingBoxInterval( RealInterval viewerInterval, RealInterval sourceInterval )
	{
		double[] center = new double[ 3 ];
		double[] size = new double[ 3 ];

		for (int d = 0; d < 2; d++)
		{
			center[ d ] = ( viewerInterval.realMax( d ) + viewerInterval.realMin( d ) ) / 2.0;
			size[ d ] = ( viewerInterval.realMax( d ) - viewerInterval.realMin( d ) ) / 2.0;
		}

		center[ 2 ] = viewerInterval.realMin( 2 );
		size[ 2 ] = ( sourceInterval.realMax( 2 ) - sourceInterval.realMin( 2 ) ) / 2.0;

		double[] min = new double[ 3 ];
		double[] max = new double[ 3 ];

		for (int d = 0; d < 3; d++)
		{
			min[ d ] = center[ d ] - size[ d ] / 2;
			max[ d ] = center[ d ] + size[ d ] / 2;
		}

		return new FinalRealInterval( min, max );
	}

}
