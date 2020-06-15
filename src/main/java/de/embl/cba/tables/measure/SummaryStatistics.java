package de.embl.cba.tables.measure;

public class SummaryStatistics
{
	public final double mean;
	public final double sigma;

	public SummaryStatistics( double mean, double sigma )
	{
		this.mean = mean;
		this.sigma = sigma;
	}

	@Override
	public String toString()
	{
		String s = "";
		s += "mean = " + mean;
		s += ", sigma = " + sigma;
		return s;
	}

}
