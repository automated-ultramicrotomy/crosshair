package de.embl.cba.tables.github;

import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;

import java.util.ArrayList;

public abstract class GitHubUtils
{
	public static GitLocation rawUrlToGitLocation( String rawUrl )
	{
		final GitLocation gitLocation = new GitLocation();
		final String[] split = rawUrl.split( "/" );
		final String user = split[ 3 ];
		final String repo = split[ 4 ];
		gitLocation.branch = split[ 5 ];
		gitLocation.repoUrl = "https://github.com/" + user + "/" + repo;
		gitLocation.path = "";
		for ( int i = 6; i < split.length; i++ )
		{
			gitLocation.path += split[ i ] + "/";
		}
		return gitLocation;
	}

	public static ArrayList< String > getFilePaths( GitLocation gitLocation )
	{
		final GitHubContentGetter contentGetter = new GitHubContentGetter( gitLocation.repoUrl, gitLocation.path, gitLocation.branch, null );
		final String json = contentGetter.getContent();

		GsonBuilder builder = new GsonBuilder();

		final ArrayList< String > bookmarkPaths = new ArrayList<>();
		ArrayList< LinkedTreeMap > linkedTreeMaps = ( ArrayList< LinkedTreeMap >) builder.create().fromJson( json, Object.class );
		for ( LinkedTreeMap linkedTreeMap : linkedTreeMaps )
		{
			final String downloadUrl = ( String ) linkedTreeMap.get( "download_url" );
			bookmarkPaths.add( downloadUrl );
		}
		return bookmarkPaths;
	}
}
