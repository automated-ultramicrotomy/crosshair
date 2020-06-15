package de.embl.cba.bdv.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileUtils
{
	public static ArrayList< File > getFileList( File directory, String fileNameRegExp, boolean recursive )
	{
		final ArrayList< File > files = new ArrayList<>();
		populateFileList( directory, fileNameRegExp,files, recursive );
		return files;
	}

	public static File changeExtension(File f, String newExtension) {
		if ( f.getName().contains( "." ) )
		{
			int i = f.getName().lastIndexOf( '.' );
			String name = f.getName().substring( 0, i );
			return new File( f.getParent() + "/" + name + newExtension );
		}
		else
		{
			return new File( f.toString() + ".jpg" );
		}
	}

	public static void populateFileList(
			File directory, String fileNameRegExp, List< File > files, boolean recursive ) {

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
					{
						files.add( file );
					}

				}
				else if ( file.isDirectory() )
				{
					if ( recursive )
						populateFileList( file, fileNameRegExp, files, recursive );
				}
			}
		}
	}

}
