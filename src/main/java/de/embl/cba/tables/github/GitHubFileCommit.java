package de.embl.cba.tables.github;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Base64;

public class GitHubFileCommit
{
	public String message = "My commit message";
	public String content = Base64.getEncoder().encodeToString( "Hello World".getBytes() );;

	public GitHubFileCommit( String message, String base64String )
	{
		this.message = message;
		content = base64String;
	}

	@Override
	public String toString()
	{
		final ObjectMapper objectMapper = new ObjectMapper();
		try
		{
			return objectMapper.writeValueAsString( this );
		} catch ( JsonProcessingException e )
		{
			e.printStackTrace();
			throw new RuntimeException( "Could not build Json string" );
		}
	}
}
