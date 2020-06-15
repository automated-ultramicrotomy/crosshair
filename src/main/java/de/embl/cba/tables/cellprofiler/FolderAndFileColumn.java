package de.embl.cba.tables.cellprofiler;

public class FolderAndFileColumn
{
	final String folderColumn;
	final String fileColumn;

	public FolderAndFileColumn( String folderColumn, String fileColumn )
	{
		this.folderColumn = folderColumn;
		this.fileColumn = fileColumn;
	}

	public String fileColumn()
	{
		return fileColumn;
	}

	public String folderColumn()
	{
		return folderColumn;
	}

}
