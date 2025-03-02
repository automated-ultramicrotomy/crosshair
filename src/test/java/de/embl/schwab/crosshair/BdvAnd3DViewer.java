package de.embl.schwab.crosshair;

import bdv.util.BdvStackSource;
import ij3d.Content;
import ij3d.Image3DUniverse;
import net.imglib2.realtransform.AffineTransform3D;

public class BdvAnd3DViewer {
    public BdvStackSource bdvStackSource;
    public AffineTransform3D initialViewerTransform;
    public Image3DUniverse universe;
    public Content imageContent;
}