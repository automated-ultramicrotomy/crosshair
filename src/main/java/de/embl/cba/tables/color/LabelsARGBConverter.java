package de.embl.cba.tables.color;

import bdv.viewer.TimePointListener;
import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.volatiles.VolatileARGBType;

public interface LabelsARGBConverter extends Converter< RealType, VolatileARGBType >, TimePointListener
{
	void setSingleColor( ARGBType argbType );
}
