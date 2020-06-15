package de.embl.cba.tables.command;

import de.embl.cba.tables.Logger;
import de.embl.cba.tables.TableColumns;
import de.embl.cba.tables.cellprofiler.CellProfilerUtils;
import de.embl.cba.tables.image.FileImageSourcesModel;
import de.embl.cba.tables.image.FileImageSourcesModelFactory;
import de.embl.cba.tables.imagesegment.SegmentProperty;
import de.embl.cba.tables.imagesegment.SegmentPropertyColumnsSelectionDialog;
import de.embl.cba.tables.imagesegment.SegmentUtils;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import de.embl.cba.tables.view.combined.SegmentsTableAndBdvViews;
import de.embl.cba.tables.view.combined.SegmentsTableBdvAnd3dViews;
import ij.gui.GenericDialog;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static de.embl.cba.tables.imagesegment.SegmentPropertyColumnsSelectionDialog.NO_COLUMN_SELECTED;

@Plugin(type = Command.class, menuPath =
		"Plugins>Segmentation>Explore>Explore Objects Table" )
public class ExploreObjectsTableCommand implements Command
{
	public static final String DEFAULT = "Path_";
	public static final String IMAGE_PATH_COLUMNS_ID_CELL_PROFILER = "\'FileName_\' and \'PathName_\' (CellProfiler)";
	public static final String IMAGE_PATH_COLUMNS_ID_DEFAULT = "Path_";

	@Parameter
	public LogService logService;

	@Parameter ( label = "Table" )
	public File tableFile;

	@Parameter ( label = "Image path column identifier", choices = { DEFAULT })
	public String imagePathColumnsId = DEFAULT;

	@Parameter ( label = "Paths to images are relative" )
	public boolean isRelativeImagePath = true;

	@Parameter ( label = "Parent folder (for relative image paths)",
			required = false, style = "directory")
	public File imageRootFolder;

	@Parameter ( label = "Log Image Paths", callback = "logImagePaths")
	private Button logImagePathsButton;

	@Parameter ( label = "Images are 2D" )
	public boolean is2D = true;

	@Parameter ( label = "Time points are one-based" )
	public boolean isOneBasedTimePoint;

	//	@Parameter ( label = "Apply Path Mapping" )
	private boolean isPathMapping = false;

	private Map< String, List< String > > columns;

	// Can be set programmatically
	public Map< SegmentProperty, String > propertyToColumnName = null;

	public void run()
	{
		if ( ! isRelativeImagePath ) imageRootFolder = new File("" );

		Logger.info("Opening table: " + tableFile );
		final List< TableRowImageSegment > tableRowImageSegments
				= createSegments( tableFile.getAbsolutePath() );

		Logger.info("Creating image sources model..." );
		final FileImageSourcesModelFactory< TableRowImageSegment > factory =
				new FileImageSourcesModelFactory(
						tableRowImageSegments,
						imageRootFolder.toString(),
						is2D );

		if ( ! showImageChoiceDialog( factory ) ) return;

		final FileImageSourcesModel imageSourcesModel = factory.getImageSourcesModel();

		showViews( tableRowImageSegments, imageSourcesModel );
	}

	public void showViews(
			List< TableRowImageSegment > tableRowImageSegments,
			FileImageSourcesModel imageSourcesModel )
	{
		if ( is2D )
		{
			final SegmentsTableAndBdvViews views = new SegmentsTableAndBdvViews(
					tableRowImageSegments,
					imageSourcesModel,
					tableFile.getName() );

			if ( views.getSegmentsBdvView().getSourceSetIds().size() > 1 )
				views.getSegmentsBdvView().showSourceSetSelectionDialog();
		}
		else
		{
			final SegmentsTableBdvAnd3dViews views = new SegmentsTableBdvAnd3dViews(
					tableRowImageSegments,
					imageSourcesModel,
					tableFile.getName() );

			if ( views.getSegmentsBdvView().getSourceSetIds().size() > 1 )
				views.getSegmentsBdvView().showSourceSetSelectionDialog();
		}
	}

	public boolean showImageChoiceDialog( FileImageSourcesModelFactory< TableRowImageSegment > factory )
	{
		final Map< String, String > imageNameToPathColumnName = factory.getImageNameToPathColumnName();
		final GenericDialog gd = new GenericDialog( "Show images" );
		for( String image : imageNameToPathColumnName.keySet() )
			gd.addCheckbox( image, true );
		gd.showDialog();
		if ( gd.wasCanceled() ) return false;
		for( String image : imageNameToPathColumnName.keySet()  )
			if( ! gd.getNextBoolean() )
				factory.excludeImage( image );
		return true;
	}

	private List< TableRowImageSegment > createSegments(
			String tablePath )
	{
		if ( columns == null )
			loadColumnsFromFile( tablePath );

		if ( propertyToColumnName == null )
		{
			final SegmentPropertyColumnsSelectionDialog selectionDialog
					= new SegmentPropertyColumnsSelectionDialog( columns.keySet() );
			propertyToColumnName = selectionDialog.fetchUserInput();
		}

		final Map< SegmentProperty, List< String > > propertyToColumn
				= createPropertyToColumnMap( columns.keySet() );

		final List< TableRowImageSegment > segments
				= SegmentUtils.tableRowImageSegmentsFromColumns(
						columns, propertyToColumn, isOneBasedTimePoint );

		return segments;
	}

	private void loadColumnsFromFile( String tablePath )
	{
		columns = TableColumns.stringColumnsFromTableFile( tablePath );

		if ( imagePathColumnsId.equals( IMAGE_PATH_COLUMNS_ID_CELL_PROFILER ) )
			CellProfilerUtils.replaceFolderAndFileColumnsByPathColumn( columns );

		if ( isPathMapping )
		{
			// TODO
		}
	}

	private Map< SegmentProperty, List< String > > createPropertyToColumnMap(
			Set< String > columnNames )
	{
		final Map< SegmentProperty, List< String > > propertyToColumn
				= new LinkedHashMap<>();

		for( SegmentProperty property : propertyToColumnName.keySet() )
		{
			if ( propertyToColumnName.get( property ).equals( NO_COLUMN_SELECTED ) )
				continue;

			propertyToColumn.put(
					property,
					this.columns.get( propertyToColumnName.get( property ) ) );
		}

		return propertyToColumn;
	}


}
