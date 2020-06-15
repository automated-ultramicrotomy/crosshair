package de.embl.cba.transforms.utils;

import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.interpolation.randomaccess.ClampingNLinearInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.RealViews;
import net.imglib2.realtransform.Scale;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

import static de.embl.cba.transforms.utils.ImageCreators.*;
import static de.embl.cba.transforms.utils.Transforms.createTransformedInterval;

public abstract class Scalings
{

	public static < T extends RealType< T > & NativeType< T > >
	RandomAccessibleInterval< T > createRescaledArrayImg(
			RandomAccessibleInterval< T > input,
			double[] scalingFactors )
	{
		assert scalingFactors.length == input.numDimensions();

		/**
		 * - In principle, writing a function that computes weighted averages
		 *   of an appropriate number of neighboring (not only nearest) pixels
		 *   around each requested (real) position in the new image appears to me
		 *   the most straight-forward way of rescaling.
		 * - However, in practice, blurring and subsequent re-sampling seems to be more commonly done,
		 *   maybe for implementation efficiency?
		 * - http://imagej.1557.x6.nabble.com/downsampling-methods-td3690444.html
		 * - https://github.com/axtimwalde/mpicbg/blob/050bc9110a186394ea15190fd326b3e32829e018/mpicbg/src/main/java/mpicbg/ij/util/Filter.java#L424
		 * - https://imagej.net/Downsample
		 */

		/*
		 * Blur image
		 */

		final RandomAccessibleInterval< T > blurred =
				createOptimallyBlurredArrayImg( input, scalingFactors );

		/*
		 * Sample values from blurred image
		 */

		final RandomAccessibleInterval< T > resampled =
				createResampledArrayImg( blurred, scalingFactors );

		return resampled;
	}

	public static < T extends RealType< T > & NativeType< T > >
	RandomAccessibleInterval< T > createRescaledCellImg(
			RandomAccessibleInterval< T > input,
			double[] scalingFactors )
	{
		assert scalingFactors.length == input.numDimensions();

		/**
		 * - In principle, writing a function that computes weighted averages
		 *   of an appropriate number of neighboring (not only nearest) pixels
		 *   around each requested (real) position in the new image appears to me
		 *   the most straight-forward way of rescaling.
		 * - However, in practice, blurring and subsequent re-sampling seems to be more commonly done,
		 *   maybe for implementation efficiency?
		 * - http://imagej.1557.x6.nabble.com/downsampling-methods-td3690444.html
		 * - https://github.com/axtimwalde/mpicbg/blob/050bc9110a186394ea15190fd326b3e32829e018/mpicbg/src/main/java/mpicbg/ij/util/Filter.java#L424
		 * - https://imagej.net/Downsample
		 */

		/*
		 * Blur image
		 */

		final RandomAccessibleInterval< T > blurred =
				createOptimallyBlurredCellImg( input, scalingFactors );

		/*
		 * Sample values from blurred image
		 */

		final RandomAccessibleInterval< T > resampled =
				createResampledArrayImg( blurred, scalingFactors );

		return resampled;
	}

	public static < T extends RealType< T > & NativeType< T > >
	RandomAccessibleInterval< T > createResampledArrayImg(
			RandomAccessibleInterval< T > input,
			double[] scalingFactors )
	{
		// Convert to RealRandomAccessible such that we can obtain values at (infinite) non-integer coordinates
		RealRandomAccessible< T > rra =
				Views.interpolate( Views.extendBorder( input ),
						new ClampingNLinearInterpolatorFactory<>() );

		// Change scale such that we can sample from integer coordinates (for raster function below)
		Scale scale = new Scale( scalingFactors );
		RealRandomAccessible< T > rescaledRRA  = RealViews.transform( rra, scale );

		// Create view sampled at integer coordinates
		final RandomAccessible< T > rastered = Views.raster( rescaledRRA );

		// Put an interval to make it a finite "normal" image again
		final RandomAccessibleInterval< T > finiteRastered =
				Views.interval( rastered, createTransformedInterval( input, scale ) );

		// Convert from View to a "conventional" image in RAM
		// - Above code would also run on, e.g. 8 TB image, within ms
		// - Now, we really force it to create the image
		// (we actually might now have to, depends...)
		final RandomAccessibleInterval< T > output = copyAsArrayImg( finiteRastered );

		return output;
	}

	private static < T extends RealType< T > & NativeType< T > >
	RandomAccessibleInterval< T > createOptimallyBlurredArrayImg(
			RandomAccessibleInterval< T > input,
			double[] scalingFactors )
	{
		/**
		 * - https://en.wikipedia.org/wiki/Decimation_(signal_processing)
		 * - Optimal blurring is 0.5 / M, where M is the downsampling factor
		 */

		final double[] sigmas = new double[input.numDimensions() ];

		for ( int d = 0; d < input.numDimensions(); ++d )
			sigmas[ d ] = 0.5 / scalingFactors[ d ];

		// allocate output image
		RandomAccessibleInterval< T > output = createEmptyArrayImg( input );

		// blur input image and write into output image
		Gauss3.gauss( sigmas, Views.extendBorder( input ), output ) ;

		return output;
	}

	private static < T extends RealType< T > & NativeType< T > >
	RandomAccessibleInterval< T > createOptimallyBlurredCellImg(
			RandomAccessibleInterval< T > input,
			double[] scalingFactors )
	{
		/**
		 * - https://en.wikipedia.org/wiki/Decimation_(signal_processing)
		 * - Optimal blurring is 0.5 / M, where M is the downsampling factor
		 */

		final double[] sigmas = new double[input.numDimensions() ];

		for ( int d = 0; d < input.numDimensions(); ++d )
			sigmas[ d ] = 0.5 / scalingFactors[ d ];

		// allocate output image
		RandomAccessibleInterval< T > output = createEmptyCellImg( input );

		// blur input image and write into output image
		Gauss3.gauss( sigmas, Views.extendBorder( input ), output ) ;

		return output;
	}



}
