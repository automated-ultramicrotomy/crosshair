package de.embl.cba.tables.github;

import com.drew.lang.annotations.Nullable;

public class GitHubContentGetter
{
	private String repository;
	private String path;
	private String branch;
	private String accessToken;

	/**
	 * https://developer.github.com/v3/repos/contents/
	 *
	 */
	public GitHubContentGetter(
			String repository,
			String path,
			@Nullable String branch,
			@Nullable String accessToken
	)
	{
		this.repository = repository;
		this.path = path;
		this.branch = branch;
		this.accessToken = accessToken;
	}

	public String getContent()
	{
		// GET /repos/:owner/:repo/contents/:path?ref=:branch

		String url = createGetContentApiUrl( path );
		final String requestMethod = "GET";
		final RESTCaller restCaller = new RESTCaller();
		return restCaller.get( url, requestMethod, accessToken );
	}

	private String createGetContentApiUrl( String path )
	{
		String url = repository.replace( "github.com", "api.github.com/repos" );
		if ( ! url.endsWith( "/" ) ) url += "/";
		if ( ! path.startsWith( "/" ) ) path = "/" + path;
		url += "contents" + path;
		if ( branch != null )
			url += "?ref=" + branch;
		return url;
	}

	public static void main( String[] args )
	{
		final GitHubContentGetter contentGetter = new GitHubContentGetter( "https://github.com/platybrowser/platybrowser", "data/1.0.1/misc/bookmarks" , "mobie", null );

		System.out.println( contentGetter.getContent() );
	}
}
