package de.embl.cba.tables;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FileAndUrlUtils
{
	public static List< File > getFileList( File directory, String fileNameRegExp )
	{
		final ArrayList< File > files = new ArrayList<>();
		populateFileList( directory, fileNameRegExp,files );
		return files;
	}

	public static void populateFileList( File directory, String fileNameRegExp, List< File > files ) {

		// Get all the files from a directory.
		File[] fList = directory.listFiles();

		if( fList != null )
		{
			for ( File file : fList )
			{
				if ( file.isFile() )
				{
					final Matcher matcher = Pattern.compile( fileNameRegExp ).matcher( file.getName() );

					if ( matcher.matches() )
						files.add( file );
				}
				else if ( file.isDirectory() )
				{
					populateFileList( file, fileNameRegExp, files );
				}
			}
		}
	}

	public static List< String > getFiles( File inputDirectory, String filePattern )
	{
		final List< File > fileList =
				de.embl.cba.tables.FileUtils.getFileList(
						inputDirectory, filePattern, false );

		Collections.sort( fileList, new FileAndUrlUtils.SortFilesIgnoreCase() );

		final List< String > paths = fileList.stream().map( x -> x.toString() ).collect( Collectors.toList() );

		return paths;
	}

	public static String getSeparator( String location )
	{
		String separator = null;
		if ( location.startsWith( "http" ) )
			separator = "/";
		else
			separator = File.separator;
		return separator;
	}

	public static String combinePath( String... paths )
	{
		final String separator = getSeparator( paths[ 0 ] );

		String combined = paths[ 0 ];
		for ( int i = 1; i < paths.length; i++ )
		{
			if ( combined.endsWith( separator ) )
				combined = combined + paths[ i ];
			else
				combined = combined + separator + paths[ i ];
		}

		return combined;
	}

	public static String removeTrailingSlash( String path )
	{
		if ( path.endsWith( "/" ) ) path = path.substring(0, path.length() - 1);
		return path;
	}

	public static InputStream getInputStream( String filePath ) throws IOException
	{
		InputStream is;
		if ( filePath.startsWith( "http" ) )
		{
			URL url = new URL( filePath );
			is = url.openStream();
		}
		else
		{
			is = new FileInputStream( new File( filePath ) );
		}
		return is;
	}

	public static String getParentLocation( String path )
	{
		if ( path.startsWith( "http" ) )
		{
			try
			{
				URI uri = new URI(path );
				URI parent = uri.getPath().endsWith("/") ? uri.resolve("..") : uri.resolve(".");
				return parent.toString();
			} catch ( URISyntaxException e )
			{
				throw new RuntimeException( "Invalid URL Syntax: " + path );
			}
		}
		else
		{
			return new File( path ).getParent();
		}

//		String tablesLocation = new File( path ).getParent();
//		if ( tablesLocation.contains( ":/" ) && ! tablesLocation.contains( "://" ) )
//			tablesLocation = tablesLocation.replace( ":/", "://" );
	}

	public static class SortFilesIgnoreCase implements Comparator<File>
	{
		public int compare( File o1, File o2 )
		{
			String s1 = o1.getName();
			String s2 = o2.getName();
			return s1.toLowerCase().compareTo(s2.toLowerCase());
		}
	}

	public static void openURI( String uri )
	{
		try
		{
			java.awt.Desktop.getDesktop().browse( new URI( uri ));
		} catch ( IOException e )
		{
			e.printStackTrace();
		} catch ( URISyntaxException e )
		{
			e.printStackTrace();
		}
	}

}
