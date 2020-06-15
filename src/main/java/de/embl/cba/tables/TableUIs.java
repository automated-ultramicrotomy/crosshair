package de.embl.cba.tables;

import de.embl.cba.tables.github.GitHubUtils;
import de.embl.cba.tables.github.GitLocation;
import de.embl.cba.tables.view.TableRowsTableView;
import ij.gui.GenericDialog;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static de.embl.cba.tables.FileUtils.resolveTableURL;


public class TableUIs
{

	public static final String PROJECT = "Project";
	public static final String FILE_SYSTEM = "File system";

	public static void addColumnUI( TableRowsTableView tableView )
	{
		final GenericDialog gd = new GenericDialog( "Add Custom Column" );
		gd.addStringField( "Column Name", "Column", 30 );
		gd.addStringField( "Default Value", "None", 30 );

		gd.showDialog();
		if( gd.wasCanceled() ) return;

		final String columnName = gd.getNextString();
		final String defaultValueString = gd.getNextString();

		Object defaultValue;
		try	{
			defaultValue = Utils.parseDouble( defaultValueString );
		}
		catch ( Exception e )
		{
			defaultValue = defaultValueString;
		}

		tableView.addColumn( columnName, defaultValue );
	}

	public static String selectColumnNameUI( JTable table, String text )
	{
		final String[] columnNames = Tables.getColumnNamesAsArray( table );
		final GenericDialog gd = new GenericDialog( "" );
		gd.addChoice( text, columnNames, columnNames[ 0 ] );
		gd.showDialog();
		if ( gd.wasCanceled() ) return null;
		final String columnName = gd.getNextChoice();
		return columnName;
	}

	public static ArrayList< String > selectColumnNamesUI( JTable table, String text )
	{
		final String[] columnNames = Tables.getColumnNamesAsArray( table );
		final int n = (int) Math.ceil( Math.sqrt( columnNames.length ) );
		final GenericDialog gd = new GenericDialog( "" );
		boolean[] booleans = new boolean[ columnNames.length ];
		gd.addCheckboxGroup( n, n, columnNames, booleans );
		gd.showDialog();
		if ( gd.wasCanceled() ) return null;

		final ArrayList< String > selectedColumns = new ArrayList<>();
		for ( int i = 0; i < columnNames.length; i++ )
			if ( gd.getNextBoolean() )
				selectedColumns.add( columnNames[ i ] );

		return selectedColumns;
	}

	public static void saveTableUI( JTable table )
	{
		final JFileChooser jFileChooser = new JFileChooser( "" );

		if ( jFileChooser.showSaveDialog( null ) == JFileChooser.APPROVE_OPTION )
		{
			final File selectedFile = jFileChooser.getSelectedFile();

			Tables.saveTable( table, selectedFile );
		}
	}

	public static void saveColumns( JTable table )
	{
		final ArrayList< String > selectedColumns
				= selectColumnNamesUI( table, "Select columns" );

		final JTable newTable = Tables.createNewTableFromSelectedColumns( table, selectedColumns );

		final JFileChooser jFileChooser = new JFileChooser( "" );

		if ( jFileChooser.showSaveDialog( null ) == JFileChooser.APPROVE_OPTION )
		{
			final File selectedFile = jFileChooser.getSelectedFile();

			Tables.saveTable( newTable, selectedFile );
		}
	}

	public static Map< String, List< String > > openTableUI( )
	{
		final JFileChooser jFileChooser = new JFileChooser( "" );

		if ( jFileChooser.showOpenDialog( null ) == JFileChooser.APPROVE_OPTION )
		{
			final File selectedFile = jFileChooser.getSelectedFile();

			return TableColumns.stringColumnsFromTableFile( selectedFile.toString() );
		}

		return null;
	}

	// TODO: make own class: ColumnsLoader
	public static Map< String, List< String > > loadColumns( JTable table,
															 String tablesDirectory,
															 String mergeByColumnName ) throws IOException
	{
		String tablesLocation = null;
		if ( tablesDirectory != null )
		{
			final GenericDialog gd = new GenericDialog( "Choose columns source" );
			gd.addChoice( "Load columns from", new String[]{ PROJECT, FILE_SYSTEM }, PROJECT );
			gd.showDialog();
			if ( gd.wasCanceled() ) return null;
			tablesLocation = gd.getNextChoice();
		}

		String tablesPath = null;
		if ( tablesDirectory != null && tablesLocation.equals( PROJECT ) && tablesDirectory.contains( "raw.githubusercontent" ) )
		{
			tablesPath = selectGitHubTablePath( tablesDirectory );
			if ( tablesPath == null ) return null;
		}
		else
		{
			final JFileChooser jFileChooser = new JFileChooser( tablesDirectory );

			if ( jFileChooser.showOpenDialog( null ) == JFileChooser.APPROVE_OPTION )
				tablesPath = jFileChooser.getSelectedFile().getAbsolutePath();
		}

		if ( tablesPath == null ) return null;

		if ( tablesPath.startsWith( "http" ) )
			tablesPath = resolveTableURL( URI.create( tablesPath ) );

		Map< String, List< String > > columns = TableColumns.openAndOrderNewColumns( table, mergeByColumnName, tablesPath );

		return columns;
	}

	public static String selectGitHubTablePath( String tablesLocation ) throws IOException
	{
//		final String[] tableNames = getTableNamesFromFile( tablesLocation, "additional_tables.txt" );
		final GitLocation gitLocation = GitHubUtils.rawUrlToGitLocation( tablesLocation );
		final ArrayList< String > filePaths = GitHubUtils.getFilePaths( gitLocation );
		final String[] fileNames = filePaths.stream().map( File::new ).map( File::getName ).toArray( String[]::new );


		final GenericDialog gd = new GenericDialog( "Select Table" );
		gd.addChoice( "Table", fileNames, fileNames[ 0 ] );
		gd.showDialog();
		if ( gd.wasCanceled() ) return null;
		final String tableFileName = gd.getNextChoice();
		String newTablePath = FileAndUrlUtils.combinePath( tablesLocation, tableFileName );

		return newTablePath;
	}

	public static String[] getTableNamesFromFile( String tablesLocation, String additionalTableNamesFile ) throws IOException
	{
		String additionalTablesUrl = getAdditionalTablesUrl( tablesLocation, additionalTableNamesFile );

		final BufferedReader reader = Tables.getReader( additionalTablesUrl );

		final ArrayList< String > lines = new ArrayList<>();
		String line = reader.readLine();
		while ( line != null )
		{
			lines.add( line );
			line = reader.readLine();
		}
		return lines.toArray( new String[]{} );
	}

	public static String getAdditionalTablesUrl( String tablesLocation, String additionalTableNamesFile )
	{
		String additionalTablesUrl;
		if ( tablesLocation.endsWith( "/" ) )
		{
			additionalTablesUrl = tablesLocation + additionalTableNamesFile;
		}
		else
		{
			additionalTablesUrl = tablesLocation + "/" + additionalTableNamesFile;
		}
		return additionalTablesUrl;
	}

}
