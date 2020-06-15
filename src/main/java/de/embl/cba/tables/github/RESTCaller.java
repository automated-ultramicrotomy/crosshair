package de.embl.cba.tables.github;

import de.embl.cba.tables.Logger;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class RESTCaller
{
	private int issueNumber;
	private String status;

	public RESTCaller()
	{
	}

	public void put( String url, String requestMethod, String content, String accessToken )
	{
		try
		{
			URL obj = new URL( url );
			HttpURLConnection httpURLConnection = ( HttpURLConnection ) obj.openConnection();

			httpURLConnection.setRequestMethod( requestMethod );
			httpURLConnection.setRequestProperty( "Content-Type", "application/json" );
			httpURLConnection.setRequestProperty( "Authorization", "Token " + accessToken );

			httpURLConnection.setDoOutput( true );
			DataOutputStream wr = new DataOutputStream( httpURLConnection.getOutputStream() );
			wr.writeBytes( content );
			wr.flush();
			wr.close();

			parseResponse( httpURLConnection );
		}
		catch( Exception e )
		{
			Logger.error( "Please see the error in the console" );
			System.err.println( e );
		}
	}


	public String get(
			String url,
			String requestMethod,
			String accessToken // nullable
	)
	{
		try
		{
			URL obj = new URL( url );
			HttpURLConnection httpURLConnection = ( HttpURLConnection ) obj.openConnection();

			httpURLConnection.setRequestMethod( requestMethod );
			httpURLConnection.setRequestProperty( "Content-Type", "application/json" );
			if ( accessToken != null )
				httpURLConnection.setRequestProperty( "Authorization", "Token " + accessToken );

			return parseResponse( httpURLConnection );
		}
		catch( Exception e )
		{
			Logger.error( "Please see the error in the console" );
			System.err.println( e );
			return null;
		}
	}


	private String parseResponse( HttpURLConnection httpURLConnection ) throws IOException
	{
		StringBuilder builder = getResponse( httpURLConnection );

		int responseCode = httpURLConnection.getResponseCode();
		if ( ! ( responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED ) )
		{
			Logger.error( "Unexpected response code: " + responseCode + "\n"+status+
					"\nPlease see the log window for more details.");
			Logger.info( "\n" + builder.toString() );

			return null;
		}
		else
		{
			final BufferedReader bufferedReader = new BufferedReader( new InputStreamReader( ( httpURLConnection.getInputStream() ) ) );
			final StringBuilder stringBuilder = new StringBuilder();
			String output;
			while ((output = bufferedReader.readLine()) != null) {
				stringBuilder.append(output);
			}
			final String response = stringBuilder.toString();
			return response;
		}
	}

	private StringBuilder getResponse( HttpURLConnection httpURLConnection ) throws IOException
	{
		StringBuilder builder = new StringBuilder();
		builder.append(httpURLConnection.getResponseCode())
				.append(" ")
				.append(httpURLConnection.getResponseMessage())
				.append("\n");

		Map<String, List<String> > map = httpURLConnection.getHeaderFields();
		for (Map.Entry<String, List<String>> entry : map.entrySet())
		{
			if (entry.getKey() == null)
				continue;
			builder.append( entry.getKey())
					.append(": ");

			List<String> headerValues = entry.getValue();
			Iterator<String> it = headerValues.iterator();
			if (it.hasNext()) {
				builder.append(it.next());

				while (it.hasNext()) {
					builder.append(", ")
							.append(it.next());
				}
			}

			if (entry.getKey().equals( "Location" ) )
			{
				final String[] split = entry.getValue().get( 0 ).split( "/" );
				issueNumber = Integer.parseInt( split[ split.length - 1 ] );
			}

			if (entry.getKey().equals( "Status" ) )
			{
				status = entry.getValue().get( 0 );
			}

			builder.append("\n");
		}
		return builder;
	}

	public int getIssueNumber()
	{
		return issueNumber;
	}
}
