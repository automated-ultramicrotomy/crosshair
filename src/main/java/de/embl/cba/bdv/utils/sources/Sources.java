package de.embl.cba.bdv.utils.sources;

import bdv.viewer.Source;

import java.util.Map;
import java.util.WeakHashMap;

public abstract class Sources
{
	public static Map< Source< ? >, Metadata > sourceToMetadata = new WeakHashMap();
}
