package de.embl.cba.tables.mesh;

import customnode.CustomTriangleMesh;
import org.scijava.vecmath.Point3f;

import java.util.ArrayList;

public class MeshUtils
{
	public static CustomTriangleMesh asCustomTriangleMesh(
			final float[] meshCoordinates )
	{
		CustomTriangleMesh mesh;
		final ArrayList< Point3f > points = new ArrayList<>();
		for ( int i = 0; i < meshCoordinates.length; )
			points.add( new Point3f(
					meshCoordinates[ i++ ],
					meshCoordinates[ i++ ],
					meshCoordinates[ i++ ] ) );
		mesh = new CustomTriangleMesh( points );

		return mesh;
	}
}
