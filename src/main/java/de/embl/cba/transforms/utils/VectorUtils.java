package de.embl.cba.transforms.utils;

public class VectorUtils
{
	static double[] getPerpendicularVector( double[] vector )
	{
		final double[] perpendicularVector = new double[ 3 ];
		if ( vector[ 2 ] != 0 )
		{
			perpendicularVector[ 1 ] = 1;
			perpendicularVector[ 2 ] = - vector[ 1 ] / vector[ 2 ];
		}
		else if ( vector[ 1 ] != 0 )
		{
			perpendicularVector[ 0 ] = 1;
			perpendicularVector[ 1 ] = - vector[ 0 ] / vector[ 1 ];
		}
		else if ( vector[ 0 ] != 0 )
		{
			perpendicularVector[ 2 ] = 1;
			perpendicularVector[ 0 ] = - vector[ 2 ] / vector[ 0 ];
		}

		return perpendicularVector;
	}
}
