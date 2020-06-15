package de.embl.cba.tables.color;

import de.embl.cba.bdv.utils.lut.ARGBLut;
import de.embl.cba.bdv.utils.lut.BlueWhiteRedARGBLut;
import de.embl.cba.bdv.utils.lut.GlasbeyARGBLut;
import de.embl.cba.bdv.utils.lut.ViridisARGBLut;
import de.embl.cba.tables.Logger;
import de.embl.cba.tables.Tables;
import de.embl.cba.tables.tablerow.TableRow;
import ij.gui.GenericDialog;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

public class ColumnColoringModelCreator< T extends TableRow >
{
	private final JTable table;

	private String selectedColumnName;
	private String selectedColoringMode;
	private boolean isZeroTransparent = false;

	private Map< String, double[] > columnNameToMinMax;
	private HashMap< String, double[] > columnNameToRangeSettings;

	public static final String[] COLORING_MODES = new String[]
	{
			ColoringLuts.BLUE_WHITE_RED,
			ColoringLuts.VIRIDIS,
			ColoringLuts.GLASBEY,
	};

	public ColumnColoringModelCreator( JTable table )
	{
		this.table = table;

		this.columnNameToMinMax = new HashMap<>();
		this.columnNameToRangeSettings = new HashMap<>();
	}

	public ColoringModel< T > showDialog()
	{
		final String[] columnNames = Tables.getColumnNamesAsArray( table );

		final GenericDialog gd = new GenericDialog( "Color by Column" );

		if ( selectedColumnName == null ) selectedColumnName = columnNames[ 0 ];
		gd.addChoice( "Column", columnNames, selectedColumnName );

		if ( selectedColoringMode == null ) selectedColoringMode = COLORING_MODES[ 0 ];
		gd.addChoice( "Coloring Mode", COLORING_MODES, selectedColoringMode );

		gd.addCheckbox( "Paint Zero Transparent", isZeroTransparent );

		gd.showDialog();
		if ( gd.wasCanceled() ) return null;

		selectedColumnName = gd.getNextChoice();
		selectedColoringMode = gd.getNextChoice();
		isZeroTransparent = gd.getNextBoolean();

		if ( isZeroTransparent )
			selectedColoringMode += ColoringLuts.ZERO_TRANSPARENT;

		return createColoringModel( selectedColumnName, selectedColoringMode, null, null );
	}

	public ColoringModel< T > createColoringModel(
			String selectedColumnName,
			String coloringLut,
			Double min,
			Double max)
	{
		rememberChoices( selectedColumnName, coloringLut );

		switch ( coloringLut )
		{
			case ColoringLuts.BLUE_WHITE_RED:
				return createLinearColoringModel(
						selectedColumnName,
						false,
						min, max,
						new BlueWhiteRedARGBLut( 1000 ) );
			case ColoringLuts.BLUE_WHITE_RED + ColoringLuts.ZERO_TRANSPARENT:
				return createLinearColoringModel(
						selectedColumnName,
						true,
						min, max,
						new BlueWhiteRedARGBLut( 1000 ) );
			case ColoringLuts.VIRIDIS:
				return createLinearColoringModel(
						selectedColumnName,
						false,
						min, max,
						new ViridisARGBLut() );
			case ColoringLuts.VIRIDIS + ColoringLuts.ZERO_TRANSPARENT:
				return createLinearColoringModel(
						selectedColumnName,
						true,
						min, max,
						new ViridisARGBLut() );
			case ColoringLuts.GLASBEY:
				return createCategoricalColoringModel(
						selectedColumnName,
						false, new GlasbeyARGBLut() );
			case ColoringLuts.GLASBEY + ColoringLuts.ZERO_TRANSPARENT:
				return createCategoricalColoringModel(
						selectedColumnName,
						true, new GlasbeyARGBLut() );
		}

		return null;
	}

	public void rememberChoices( String selectedColumnName, String selectedColoringMode )
	{
		this.selectedColumnName = selectedColumnName;
		this.selectedColoringMode = selectedColoringMode;

		if ( selectedColoringMode.contains( ColoringLuts.ZERO_TRANSPARENT ) )
			this.isZeroTransparent = true;
		else
			this.isZeroTransparent = false;
	}

	public CategoryTableRowColumnColoringModel< T > createCategoricalColoringModel(
			String selectedColumnName,
			boolean isZeroTransparent,
			ARGBLut argbLut )
	{
		final CategoryTableRowColumnColoringModel< T > coloringModel
				= new CategoryTableRowColumnColoringModel< >(
						selectedColumnName,
						argbLut );

		if ( isZeroTransparent )
		{
			coloringModel.putInputToFixedColor( "0", CategoryTableRowColumnColoringModel.TRANSPARENT );
			coloringModel.putInputToFixedColor( "0.0", CategoryTableRowColumnColoringModel.TRANSPARENT );
		}

		return coloringModel;
	}

	private NumericTableRowColumnColoringModel< T > createLinearColoringModel(
			String selectedColumnName,
			boolean isZeroTransparent,
			Double min,
			Double max,
			ARGBLut argbLut )
	{
		if ( ! Tables.isNumeric( table, selectedColumnName ) )
		{
			Logger.error( "This coloring mode is only available for numeric columns.\n" +
					"The selected " + selectedColumnName + " column however appears to contain non-numeric values.");
			return null; // TODO: Make this work without null pointer exception
		}

		final double[] valueRange = getValueRange( table, selectedColumnName );
		double[] valueSettings = getValueSettings( selectedColumnName, valueRange );

		final NumericTableRowColumnColoringModel< T > coloringModel
				= new NumericTableRowColumnColoringModel(
						selectedColumnName,
						argbLut,
						valueSettings,
						valueRange,
						isZeroTransparent );

		if ( min != null )
			coloringModel.setMin( min );

		if ( max != null )
			coloringModel.setMax( max );

		SwingUtilities.invokeLater( () ->
				new NumericColoringModelDialog( selectedColumnName, coloringModel, valueRange ) );

		return coloringModel;
	}

	private double[] getValueSettings( String columnName, double[] valueRange )
	{
		double[] valueSettings;

		if ( columnNameToRangeSettings.containsKey( columnName ) )
			valueSettings = columnNameToRangeSettings.get( columnName );
		else
			valueSettings = valueRange.clone();

		columnNameToRangeSettings.put( columnName, valueSettings );

		return valueSettings;
	}

	private double[] getValueRange( JTable table, String column )
	{
		if ( ! columnNameToMinMax.containsKey( column ) )
		{
			final double[] minMaxValues = Tables.minMax( column, table );
			columnNameToMinMax.put( column, minMaxValues );
		}

		return columnNameToMinMax.get( column );
	}
}
