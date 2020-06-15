package de.embl.cba.tables.command;

import de.embl.cba.tables.TableColumns;
import de.embl.cba.tables.cellprofiler.CellProfilerUtils;
import de.embl.cba.tables.cellprofiler.FolderAndFileColumn;
import de.embl.cba.tables.image.FileImageSourcesModel;
import de.embl.cba.tables.image.FileImageSourcesModelFactory;
import de.embl.cba.tables.imagesegment.SegmentProperty;
import de.embl.cba.tables.imagesegment.SegmentUtils;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import de.embl.cba.tables.view.combined.SegmentsTableAndBdvViews;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Deprecated
public class ExploreCellProfilerCommand< R extends RealType< R > & NativeType< R > >
		implements Command
{
	public static final String CELLPROFILER_FOLDER_COLUMN_PREFIX = "PathName_";
	public static final String CELLPROFILER_FILE_COLUMN_PREFIX = "FileName_";
	public static final String OBJECTS = "Objects_";
	public static final String COLUMN_NAME_OBJECT_LABEL = "Number_Object_Number";
	public static final String COLUMN_NAME_OBJECT_LOCATION_CENTER_X = "Location_Center_X";
	public static final String COLUMN_NAME_OBJECT_LOCATION_CENTER_Y = "Location_Center_Y";

	@Parameter ( label = "CellProfiler Objects Table with Image Paths" )
	public File inputTableFile;

	@Parameter ( label = "Apply Path Mapping" )
	public boolean isPathMapping = false;

	@Parameter ( label = "Image Path Mapping (In Table)" )
	public String imageRootPathInTable = "/Volumes/cba/exchange/Daja-Christian/20190116_for_classification_interphase_versus_mitotic";

	@Parameter ( label = "Image Path Mapping (On this Computer)" )
	public String imageRootPathOnThisComputer = "/Users/tischer/Documents/daja-schichler-nucleoli-segmentation--data/2019-01-31";


	private HashMap< String, FolderAndFileColumn > imageNameToFolderAndFileColumns;
	private Map< String, List< String > > columns;

	@Override
	public void run()
	{
		final List< TableRowImageSegment > tableRowImageSegments
				= createSegments( inputTableFile.getAbsolutePath() );

		final String tablePath = inputTableFile.toString();

		final FileImageSourcesModel imageSourcesModel =
				new FileImageSourcesModelFactory(
						tableRowImageSegments,
						tablePath,
						true ).getImageSourcesModel();

		new SegmentsTableAndBdvViews(
				tableRowImageSegments,
				imageSourcesModel,
				inputTableFile.getName() );
	}

	private List< TableRowImageSegment > createSegments( String tablePath )
	{
		columns = TableColumns.stringColumnsFromTableFile( tablePath );

		final List< String > pathColumnNames =
				CellProfilerUtils.replaceFolderAndFileColumnsByPathColumn( columns );

		final Map< SegmentProperty, List< String > > segmentPropertyToColumn
				= getSegmentPropertyToColumn( pathColumnNames );

		final List< TableRowImageSegment > segments
				= SegmentUtils.tableRowImageSegmentsFromColumns(
						columns,
						segmentPropertyToColumn,
						false );

		return segments;
	}

	private Map< SegmentProperty, List< String > > getSegmentPropertyToColumn(
			List< String > pathColumnNames )
	{
		final Map< SegmentProperty, List< String > > segmentPropertyToColumn
				= new HashMap<>();

		String labelImagePathColumnName = getLabelImagePathColumnName( pathColumnNames );

		segmentPropertyToColumn.put(
				SegmentProperty.LabelImage,
				columns.get( labelImagePathColumnName ));

		segmentPropertyToColumn.put(
				SegmentProperty.ObjectLabel,
				columns.get( COLUMN_NAME_OBJECT_LABEL ) );

		segmentPropertyToColumn.put(
				SegmentProperty.X,
				columns.get( COLUMN_NAME_OBJECT_LOCATION_CENTER_X ) );

		segmentPropertyToColumn.put(
				SegmentProperty.Y,
				columns.get( COLUMN_NAME_OBJECT_LOCATION_CENTER_Y ) );

		return segmentPropertyToColumn;
	}

	private String getLabelImagePathColumnName( List< String > pathColumnNames )
	{
		String labelImagePathColumnName = "";
		for ( String pathColumnName : pathColumnNames )
		{
			if ( pathColumnName.contains( OBJECTS ) )
			{
				labelImagePathColumnName = pathColumnName;
				break;
			}
		}
		return labelImagePathColumnName;
	}

}
