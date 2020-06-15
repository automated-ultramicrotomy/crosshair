package de.embl.cba.tables.cellprofiler;

import de.embl.cba.tables.cellprofiler.FolderAndFileColumn;
import de.embl.cba.tables.command.ExploreCellProfilerCommand;

import java.io.File;
import java.util.*;

public abstract class CellProfilerUtils
{
	public static List< String > replaceFolderAndFileColumnsByPathColumn(
			Map< String, List< String > > columns )
	{
		final int numRows = columns.values().iterator().next().size();
		HashMap< String, FolderAndFileColumn >
				imageNameToFolderAndFileColumns = fetchFolderAndFileColumns( columns.keySet() );

		final List< String > pathColumnNames = new ArrayList<>();

		for ( String imageName : imageNameToFolderAndFileColumns.keySet() )
		{
			final String fileColumnName =
					imageNameToFolderAndFileColumns.get( imageName ).fileColumn();
			final String folderColumnName =
					imageNameToFolderAndFileColumns.get( imageName ).folderColumn();
			final List< ? > fileColumn = columns.get( fileColumnName );
			final List< ? > folderColumn = columns.get( folderColumnName );

			final List< String > pathColumn = new ArrayList<>();

			for ( int row = 0; row < numRows; row++ )
			{
				String imagePath = folderColumn.get( row )
						+ File.separator + fileColumn.get( row );

				pathColumn.add( imagePath );
			}

			columns.remove( fileColumnName );
			columns.remove( folderColumnName );

			final String pathColumnName = getPathColumnName( imageName );
			columns.put( pathColumnName, pathColumn );
			pathColumnNames.add( pathColumnName );
		}

		return pathColumnNames;
	}

	public static HashMap< String, FolderAndFileColumn > fetchFolderAndFileColumns(
			Set< String > columns )
	{
		final HashMap< String, FolderAndFileColumn > imageNameToFolderAndFileColumns
				= new HashMap<>();

		for ( String column : columns )
		{
			if ( column.contains( ExploreCellProfilerCommand.CELLPROFILER_FOLDER_COLUMN_PREFIX ) )
			{
				final String image =
						column.split( ExploreCellProfilerCommand.CELLPROFILER_FOLDER_COLUMN_PREFIX )[ 1 ];
				String fileColumn = getMatchingFileColumn( image, columns );
				imageNameToFolderAndFileColumns.put(
						image,
						new FolderAndFileColumn( column, fileColumn ) );
			}
		}

		return imageNameToFolderAndFileColumns;
	}

	public static String getMatchingFileColumn( String image, Set< String > columns )
	{
		String matchingFileColumn = null;

		for ( String column : columns )
		{
			if ( column.contains( ExploreCellProfilerCommand.CELLPROFILER_FILE_COLUMN_PREFIX ) && column.contains( image ) )
			{
				matchingFileColumn = column;
				break;
			}
		}

		return matchingFileColumn;
	}

	public static String getPathColumnName( String imageName )
	{
		return "Path_" + imageName;
	}

}
