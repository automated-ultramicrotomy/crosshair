package de.embl.cba.tables.command;

import de.embl.cba.tables.FileUtils;
import de.embl.cba.tables.Logger;
import de.embl.cba.tables.Tables;
import de.embl.cba.tables.table.ConcatenatedTableModel;
import net.imglib2.type.numeric.RealType;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.io.File;
import java.util.ArrayList;
import java.util.List;


@Plugin(type = Command.class, menuPath = "Plugins>Tables>Concatenate Tables" )
public class ConcatTablesCommand< R extends RealType< R > > implements Command
{
	@Parameter ( label = "Input directory", style = "directory" )
	public File directory;

	@Parameter ( label = "Output table file", style = "save" )
	public File outputTable;

	@Parameter ( label = "Regular expression" )
	public String regExp = ".*";

	@Override
	public void run()
	{
		final List< File > files = FileUtils.getFileList( directory, regExp, true );

		final ArrayList< TableModel > models = new ArrayList<>();
		for ( File file : files )
		{
			Logger.info( "Loading: " + file );
			models.add( Tables.loadTable( file.getAbsolutePath() ).getModel() );
		}

		final ConcatenatedTableModel concat = new ConcatenatedTableModel( models );

		Logger.info( "Saving: " + outputTable );
		Tables.saveTable( new JTable( concat ), outputTable );

		Logger.info( "Done!" );
	}


}
