package de.embl.cba.tables.measure;

import de.embl.cba.tables.TableRows;
import de.embl.cba.tables.Tables;
import de.embl.cba.tables.measure.SummaryStatistics;
import de.embl.cba.tables.tablerow.TableRow;
import ij.IJ;
import ij.gui.GenericDialog;

import javax.swing.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MeasureDistance< T extends TableRow >
{

	public static final String L1_NORM = "L1 Norm";
	public static final String L2_NORM = "L2 Norm";
	private static final String Z_SCORE = "Per Column Z-Score";


	final JTable table;
	final List< T > tableRows;

	private Map< String, SummaryStatistics > columnNameToSummaryStatistics;
	private String selectedMetric;
	private String selectedNorm;
	private double[] selectedColumnMeans;
	private double[] selectedColumnSigmas;
	private int[] selectedColumnIndices;
	private ArrayList< String > selectedColumnNames;
	private String newColumnName;
	private String columnSelectionRegExp;
	private Double[] distances;

	public MeasureDistance( JTable table, List< T > tableRows )
	{
		this.table = table;
		this.tableRows = tableRows;
		this.columnNameToSummaryStatistics = new LinkedHashMap<>();
	}

	public boolean showDialog( Set< T > selectedRows )
	{
		if ( ! initChoicesFromDialog() ) return false;

		measureDistanceToSelectedRows( selectedRows );

		return true;
	}

	public String getNewColumnName()
	{
		return newColumnName;
	}

	public void measureDistanceToSelectedRows( Set< T > selectedRows )
	{
		configSelectedColumns( );

		if ( selectedNorm.equals( Z_SCORE ))
			computeSelectedColumnMeansAndSigmas( );

		final double[] referenceVector = computeReferenceVector( selectedRows );

		if ( selectedMetric.equals( L2_NORM ) )
			distances = distances( referenceVector );

		Tables.addColumn( table, newColumnName, distances );
		TableRows.addColumn( tableRows, newColumnName, distances );
	}

	private boolean initChoicesFromDialog()
	{
		final String[] norms = new String[]
				{
						Z_SCORE
				};


		final String[] metrics = new String[]
				{
						L2_NORM
				};


		final GenericDialog gd = new GenericDialog( "Measure Distance to Selected Rows" );
		gd.addStringField( "Column select regular expression", ".*", 20 );

		if ( selectedMetric == null ) selectedMetric = metrics[ 0 ];
		gd.addChoice( "Distance metric", metrics, selectedMetric );

		if ( selectedNorm == null ) selectedNorm = norms[ 0 ];
		gd.addChoice( "Feature normalisation", norms, selectedMetric );

		gd.addStringField( "New distance column name", "Distance", 20 );

		gd.showDialog();
		if ( gd.wasCanceled() ) return false;

		columnSelectionRegExp = gd.getNextString();
		selectedMetric = gd.getNextChoice();
		newColumnName = gd.getNextString();

		return true;
	}

	private void computeSelectedColumnMeansAndSigmas( )
	{
		final int n = selectedColumnIndices.length;
		selectedColumnMeans = new double[ n ];
		selectedColumnSigmas = new double[ n ];

		for ( int i = 0; i < n; ++i )
		{
			final SummaryStatistics summaryStatistics =
					getSummaryStatistics( table, selectedColumnNames.get( i ) );

			selectedColumnMeans[ i ] = summaryStatistics.mean;
			selectedColumnSigmas[ i ] = summaryStatistics.sigma;
		}
	}

	private void configSelectedColumns( )
	{
		selectedColumnNames = getSelectedColumnNames( table, columnSelectionRegExp );

		final int n = selectedColumnNames.size();
		selectedColumnIndices = new int[ n ];

		for ( int i = 0; i < n; ++i )
		{
			selectedColumnIndices[ i ] =
					table.getColumnModel().getColumnIndex( selectedColumnNames.get( i ) );
		}

	}

	private double[] computeReferenceVector( Set< T > selectedRows )
	{
		final ArrayList< double[] > normVectors = new ArrayList<>();
		for ( T tableRow : selectedRows )
		{
			final double[] normVector = getZScoreNormalisedRowVector(
					table,
					tableRow.rowIndex(),
					selectedColumnIndices,
					selectedColumnMeans,
					selectedColumnSigmas );

			normVectors.add( normVector );
		}

		return computeAverageVector( normVectors );
	}

	private double[] computeAverageVector( ArrayList< double[] > vectors )
	{
		int numDimensions = vectors.get( 0 ).length;
		int numVectors = vectors.size();

		final double[] avgVector = new double[ numDimensions ];

		// add
		for( double[] vector : vectors )
			for ( int d = 0; d < numDimensions; ++d )
				avgVector[ d ] += vector[ d ];

		// divide by N
		for ( int d = 0; d < numDimensions; ++d )
			avgVector[ d ] /= numVectors;

		return avgVector;
	}

	private Double[] distances( double[] referenceVector )
	{
		final int rowCount = table.getRowCount();

		final Double[] distances = new Double[ rowCount ];

		for ( int rowIndex = 0; rowIndex < rowCount; ++rowIndex )
		{
			final double[] rowVector = getZScoreNormalisedRowVector(
					table, rowIndex, selectedColumnIndices, selectedColumnMeans, selectedColumnSigmas );

			double distance = l2Distance( rowVector, referenceVector );

			distances[ rowIndex ] = distance;
		}

		return distances;
	}

	private double l2Distance( double[] rowVector, double[] referenceVector )
	{
		int numDimensions = rowVector.length ;

		double distance = 0.0;
		for ( int d = 0; d < numDimensions; ++d )
			distance += Math.pow( rowVector[ d ] - referenceVector[ d ], 2 );
		distance = Math.sqrt( distance );

		return distance;
	}

	private double[] getZScoreNormalisedRowVector(
			final JTable table,
			final int rowIndex,
			final int[] selectedColumnIndices,
			final double[] means,
			final double[] sigmas )
	{
		final double[] rawVector = getRowVector( table, rowIndex, selectedColumnIndices );

		final double[] normVector = zScoreNormalisation( means, sigmas, rawVector );

		return normVector;
	}

	private double[] zScoreNormalisation(
			final double[] means,
			final double[] sigmas,
			final double[] rawVector )
	{
		int numDimension = rawVector.length;

		final double[] normVector = new double[ numDimension ];
		for ( int d = 0; d < numDimension; ++d )
			normVector[ d ] =  ( rawVector[ d ] - means[ d ] ) / sigmas[ d ];

		return normVector;
	}

	private double[] getRowVector( JTable table, int rowIndex, int[] selectedColumnIndices )
	{
		int n = selectedColumnIndices.length;
		final double[] rawVector = new double[ n ];
		for ( int i = 0; i < n; ++i )
			rawVector[ i ] = ( Double ) table.getValueAt( rowIndex, selectedColumnIndices[ i ] );
		return rawVector;
	}

	private SummaryStatistics getSummaryStatistics( JTable table, String columnName )
	{
		if ( ! columnNameToSummaryStatistics.containsKey( columnName ) )
		{
			final double[] meanSigma = Tables.meanSigma( columnName, table );
			final SummaryStatistics summaryStatistics = new SummaryStatistics(
					meanSigma[ 0 ], meanSigma[ 1 ]
			);

			columnNameToSummaryStatistics.put( columnName, summaryStatistics );
		}

		IJ.log( columnName + ": " + columnNameToSummaryStatistics.get( columnName ) );

		return columnNameToSummaryStatistics.get( columnName );
	}

	private ArrayList< String > getSelectedColumnNames( JTable table, String columnNameRegExp )
	{
		final List< String > columnNames = Tables.getColumnNames( table );

		final ArrayList< String > selectedColumnNames = new ArrayList<>();
		for ( String columnName : columnNames )
		{
			if ( Tables.isNumeric( table, columnName ) )
			{
				final Matcher matcher = Pattern.compile( columnNameRegExp ).matcher( columnName );

				if ( matcher.matches() )
					selectedColumnNames.add( columnName );
			}
		}

		return selectedColumnNames;
	}


}
