package de.embl.cba.target;

import ij.ImagePlus;
import ij.plugin.FolderOpener;

public class Universe3DExplorer
{
	public static final String INPUT_FOLDER = "/Users/tischer/Documents/targeting_3D/src/test/resources/target/mri-stack-slices/";

	public Universe3DExplorer()
	{
		final ImagePlus imagePlus = FolderOpener.open( INPUT_FOLDER, "" );
		imagePlus.show();
	}

	public static void main( String[] args )
	{
		new Universe3DExplorer();
	}
}
