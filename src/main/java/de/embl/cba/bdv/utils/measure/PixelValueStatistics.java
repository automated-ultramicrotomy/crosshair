package de.embl.cba.bdv.utils.measure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PixelValueStatistics
{
	public long numVoxels;
	public double mean;
	public double sdev;

	@Override
	public String toString()
	{
		final ObjectMapper mapper = new ObjectMapper();
		try
		{
			return mapper.writeValueAsString( this );
		} catch ( JsonProcessingException e )
		{
			e.printStackTrace();
			throw new RuntimeException( "Could not parse PixelValueStatistics to String" );
		}
	}
}
