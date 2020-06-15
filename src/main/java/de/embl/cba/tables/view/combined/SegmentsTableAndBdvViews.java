package de.embl.cba.tables.view.combined;

import bdv.util.BdvHandle;
import de.embl.cba.bdv.utils.lut.GlasbeyARGBLut;
import de.embl.cba.tables.color.LazyCategoryColoringModel;
import de.embl.cba.tables.color.SelectionColoringModel;
import de.embl.cba.tables.image.ImageSourcesModel;
import de.embl.cba.tables.imagesegment.DefaultImageSegmentsModel;
import de.embl.cba.tables.select.DefaultSelectionModel;
import de.embl.cba.tables.select.SelectionModel;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import de.embl.cba.tables.view.SegmentsBdvView;
import de.embl.cba.tables.view.TableRowsTableView;

import java.util.List;

public class SegmentsTableAndBdvViews
{
	private final List< TableRowImageSegment > tableRowImageSegments;
	private final ImageSourcesModel imageSourcesModel;
	private final String viewName;
	private SegmentsBdvView< TableRowImageSegment > segmentsBdvView;
	private TableRowsTableView< TableRowImageSegment > tableRowsTableView;
	private SelectionColoringModel< TableRowImageSegment > selectionColoringModel;
	private SelectionModel< TableRowImageSegment > selectionModel;
	private LazyCategoryColoringModel< TableRowImageSegment > coloringModel;

	public SegmentsTableAndBdvViews(
			List< TableRowImageSegment > tableRowImageSegments,
			ImageSourcesModel imageSourcesModel,
			String viewName )
	{
		this( tableRowImageSegments, imageSourcesModel, viewName, null );
	}

	public SegmentsTableAndBdvViews(
			List< TableRowImageSegment > tableRowImageSegments,
			ImageSourcesModel imageSourcesModel,
			String viewName,
			BdvHandle bdv )
	{
		this.tableRowImageSegments = tableRowImageSegments;
		this.imageSourcesModel = imageSourcesModel;
		this.viewName = viewName;
		show( bdv );
	}

	private void show( BdvHandle bdv )
	{
		selectionModel = new DefaultSelectionModel<>();

		coloringModel = new LazyCategoryColoringModel<>( new GlasbeyARGBLut( 255 ) );

		selectionColoringModel = new SelectionColoringModel<>(
				coloringModel,
				selectionModel );

		final DefaultImageSegmentsModel< TableRowImageSegment > imageSegmentsModel
				= new DefaultImageSegmentsModel<>( tableRowImageSegments, viewName );

		segmentsBdvView = new SegmentsBdvView< TableRowImageSegment >(
				tableRowImageSegments,
				selectionModel,
				selectionColoringModel,
				imageSourcesModel,
				bdv );

		tableRowsTableView = new TableRowsTableView< TableRowImageSegment >(
				tableRowImageSegments,
				selectionModel,
				selectionColoringModel );

		tableRowsTableView.showTableAndMenu( segmentsBdvView.getBdv().getViewerPanel() );
	}

	public SelectionModel< TableRowImageSegment > getSelectionModel()
	{
		return selectionModel;
	}

	public SelectionColoringModel< TableRowImageSegment > getSelectionColoringModel()
	{
		return selectionColoringModel;
	}

	public SegmentsBdvView< TableRowImageSegment > getSegmentsBdvView()
	{
		return segmentsBdvView;
	}

	public TableRowsTableView< TableRowImageSegment > getTableRowsTableView()
	{
		return tableRowsTableView;
	}

	public void close()
	{
		segmentsBdvView.close();
		tableRowsTableView.close();

		segmentsBdvView = null;
		tableRowsTableView = null;

		System.gc();
	}
}
