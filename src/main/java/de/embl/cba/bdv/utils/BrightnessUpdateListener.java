package de.embl.cba.bdv.utils;

import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.SliderPanelDouble;
import bdv.util.BoundedValueDouble;

import java.util.ArrayList;

public class BrightnessUpdateListener implements BoundedValueDouble.UpdateListener
{
	final private ArrayList< ConverterSetup > converterSetups;
 	final private BoundedValueDouble min;
	final private BoundedValueDouble max;
	private final SliderPanelDouble minSlider;
	private final SliderPanelDouble maxSlider;

	public BrightnessUpdateListener( BoundedValueDouble min,
									 BoundedValueDouble max,
									 SliderPanelDouble minSlider,
									 SliderPanelDouble maxSlider,
									 ArrayList< ConverterSetup > converterSetups )
	{
		this.min = min;
		this.max = max;
		this.minSlider = minSlider;
		this.maxSlider = maxSlider;
		this.converterSetups = converterSetups;
	}

	@Override
	public void update()
	{
		minSlider.update();
		maxSlider.update();
		for ( ConverterSetup converterSetup : converterSetups )
		{
			converterSetup.setDisplayRange( min.getCurrentValue(), max.getCurrentValue() );
		}
	}
}
