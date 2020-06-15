package de.embl.cba.tables.imagesegment;

import de.embl.cba.tables.tablerow.ColumnBasedTableRow;
import de.embl.cba.tables.tablerow.DefaultColumnBasedTableRow;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import de.embl.cba.tables.tablerow.TableRowMap;
import net.imglib2.util.ValuePair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SegmentUtils
{

	public static final String BB_MIN_X = "bb_min_x";
	public static final String BB_MIN_Y = "bb_min_y";
	public static final String BB_MIN_Z = "bb_min_z";
	public static final String BB_MAX_X = "bb_max_x";
	public static final String BB_MAX_Y = "bb_max_y";
	public static final String BB_MAX_Z = "bb_max_z";

	@Deprecated
	public static String getKey( Double label )
	{
		return getKey( label, 0 );
	}

	@Deprecated
	public static String getKey( Double label, Integer timePoint )
	{
		return "L"+label.toString() + "_T" + timePoint.toString();
	}

	public static DefaultImageSegment segmentFromFeatures(
			Map< SegmentProperty, String > coordinateColumnMap,
			HashMap< String, Object > columnValueMap,
			DefaultImageSegmentBuilder segmentBuilder )
	{

		for( SegmentProperty coordinate : coordinateColumnMap.keySet() )
		{
			final String colName = coordinateColumnMap.get( coordinate );

			columnValueMap.get( colName );

			switch ( coordinate )
			{
				case X:
					segmentBuilder.setX( ( double ) columnValueMap.get( colName ) );
					break;
				case Y:
					segmentBuilder.setY( ( double ) columnValueMap.get( colName ) );
					break;
				case Z:
					segmentBuilder.setZ( ( double ) columnValueMap.get( colName ) );
					break;
				case T:
					segmentBuilder.setTimePoint( ( int ) columnValueMap.get( colName ) );
					break;
				case ObjectLabel:
					segmentBuilder.setLabel( ( double ) columnValueMap.get( colName ) );
					break;
				case LabelImage:
					segmentBuilder.setImageId( columnValueMap.get( colName ).toString() );
					break;

			}
		}

		return segmentBuilder.build();
	}


	public static DefaultImageSegment segmentFromTableRowMap(
			final Map< SegmentProperty, String > coordinateColumnMap,
			final TableRowMap tableRowMap,
			final DefaultImageSegmentBuilder segmentBuilder )
	{

		for( SegmentProperty coordinate : coordinateColumnMap.keySet() )
		{
			final String colName = coordinateColumnMap.get( coordinate );

			tableRowMap.get( colName );

			switch ( coordinate )
			{
				case X:
					segmentBuilder.setX( Double.parseDouble( (String) tableRowMap.get( colName ) ) );
					break;
				case Y:
					segmentBuilder.setY( Double.parseDouble(  (String) tableRowMap.get( colName ) ));
					break;
				case Z:
					segmentBuilder.setZ( Double.parseDouble( (String) tableRowMap.get( colName ) ) );
					break;
				case T:
					segmentBuilder.setTimePoint( Integer.parseInt( (String) tableRowMap.get( colName ) ));
					break;
				case ObjectLabel:
					segmentBuilder.setLabel(  Double.parseDouble((String)  tableRowMap.get( colName ) ));
					break;
				case LabelImage:
					segmentBuilder.setImageId( tableRowMap.get( colName ).toString() );

					break;

			}
		}

		return segmentBuilder.build();
	}


	public static DefaultImageSegment segmentFromFeaturesIndexBased(
			Map< SegmentProperty, ValuePair< String, Integer > > coordinateColumnMap,
			String[] rowEntries )
	{
		final DefaultImageSegmentBuilder segmentBuilder = new DefaultImageSegmentBuilder();

		for( SegmentProperty coordinate : coordinateColumnMap.keySet() )
		{
			final Integer col = coordinateColumnMap.get( coordinate ).getB();

			switch ( coordinate )
			{
				case X:
					segmentBuilder.setX(
							Double.parseDouble(
									rowEntries[ col ] ) );
					break;
				case Y:
					segmentBuilder.setY(
							Double.parseDouble(
									rowEntries[ col ] ) );
					break;
				case Z:
					segmentBuilder.setZ(
							Double.parseDouble(
									rowEntries[ col ] ) );
					break;
				case T:
					segmentBuilder.setTimePoint(
							Integer.parseInt(
									rowEntries[ col ] ) );
					break;
				case ObjectLabel:
					segmentBuilder.setLabel(
							Double.parseDouble(
									rowEntries[ col ] ) );
					break;
				case LabelImage:
					segmentBuilder.setImageId( rowEntries[ col ] );
					break;

			}
		}

		return segmentBuilder.build();
	}

	public static List< TableRowImageSegment > tableRowImageSegmentsFromColumns(
			final Map< String, List< String > > columns,
			final Map< SegmentProperty, List< String > > segmentPropertiesToColumn,
			boolean isOneBasedTimePoint )
	{
		final List< TableRowImageSegment > columnBasedTableRowImageSegments
				= new ArrayList<>();

		final int numRows = columns.values().iterator().next().size();

		for ( int row = 0; row < numRows; row++ )
		{
			final ColumnBasedTableRowImageSegment segment
					= new ColumnBasedTableRowImageSegment(
							row,
							columns,
							segmentPropertiesToColumn,
							isOneBasedTimePoint );

			columnBasedTableRowImageSegments.add( segment );
		}

		return columnBasedTableRowImageSegments;
	}

	public static List< ColumnBasedTableRow > columnBasedTableRowsFromColumns( final Map< String, List< String > > columnNamesToColumns )
	{
		final List< ColumnBasedTableRow > columnBasedTableRows = new ArrayList<>();

		final int numRows = columnNamesToColumns.values().iterator().next().size();

		for ( int row = 0; row < numRows; row++ )
		{
			final DefaultColumnBasedTableRow tableRow = new DefaultColumnBasedTableRow( row, columnNamesToColumns );

			columnBasedTableRows.add( tableRow );
		}

		return columnBasedTableRows;
	}

	public static < T extends ImageSegment >
	HashMap< LabelFrameAndImage, T > createSegmentMap( List< T > segments )
	{
		final HashMap< LabelFrameAndImage, T > labelFrameAndImageToSegment
				= new HashMap<>();

		for ( T segment : segments )
			labelFrameAndImageToSegment.put( new LabelFrameAndImage( segment ), segment );

		return labelFrameAndImageToSegment;
	}

	public static void putDefaultBoundingBoxMapping(
			Map< SegmentProperty, List< String > > segmentPropertyToColumn,
			Map< String, List< String > > columns )
	{
		segmentPropertyToColumn.put(
				SegmentProperty.BoundingBoxXMin,
				columns.get( BB_MIN_X ) );

		segmentPropertyToColumn.put(
				SegmentProperty.BoundingBoxYMin,
				columns.get( BB_MIN_Y ) );

		segmentPropertyToColumn.put(
				SegmentProperty.BoundingBoxZMin,
				columns.get( BB_MIN_Z ) );

		segmentPropertyToColumn.put(
				SegmentProperty.BoundingBoxXMax,
				columns.get( BB_MAX_X ) );

		segmentPropertyToColumn.put(
				SegmentProperty.BoundingBoxYMax,
				columns.get( BB_MAX_Y ) );

		segmentPropertyToColumn.put(
				SegmentProperty.BoundingBoxZMax,
				columns.get( BB_MAX_Z ) );
	}
}
