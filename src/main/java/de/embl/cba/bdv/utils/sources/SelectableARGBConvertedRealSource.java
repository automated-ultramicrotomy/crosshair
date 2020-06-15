package de.embl.cba.bdv.utils.sources;

import bdv.viewer.Source;
import de.embl.cba.bdv.utils.converters.SelectableVolatileARGBConverter;
import net.imglib2.RandomAccessible;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.ClampingNLinearInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.volatiles.VolatileARGBType;

public class SelectableARGBConvertedRealSource < R extends RealType< R > > extends ARGBConvertedRealSource
{
    private SelectableVolatileARGBConverter selectableVolatileARGBConverter;
    final private InterpolatorFactory< VolatileARGBType, RandomAccessible< VolatileARGBType > >[] interpolatorFactories;

    private AffineTransform3D[] mipmapTransforms;
    {
        interpolatorFactories = new InterpolatorFactory[]{
                new NearestNeighborInterpolatorFactory< VolatileARGBType >(),
                new ClampingNLinearInterpolatorFactory< VolatileARGBType >()
        };
    }

    public SelectableARGBConvertedRealSource( Source< R > source )
    {
        super( source, new SelectableVolatileARGBConverter(  ) );
        this.selectableVolatileARGBConverter = ( SelectableVolatileARGBConverter ) super.getConverter();
    }

    public SelectableARGBConvertedRealSource(
            Source< R > source,
            SelectableVolatileARGBConverter selectableVolatileARGBConverter )
    {
        super( source, selectableVolatileARGBConverter );
        this.selectableVolatileARGBConverter = selectableVolatileARGBConverter;
    }

    public void setSelectableConverter( SelectableVolatileARGBConverter selectableVolatileARGBConverter )
    {
        this.selectableVolatileARGBConverter = selectableVolatileARGBConverter;
    }

    public SelectableVolatileARGBConverter getSelectableConverter()
    {
        return selectableVolatileARGBConverter;
    }

}
