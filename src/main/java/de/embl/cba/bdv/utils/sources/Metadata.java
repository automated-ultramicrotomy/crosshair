package de.embl.cba.bdv.utils.sources;

import bdv.util.BdvStackSource;
import de.embl.cba.tables.view.combined.SegmentsTableBdvAnd3dViews;
import ij3d.Content;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Metadata
{
	// TODO: refactor this, e.g. separate in basic metadata and other metadata that extends the basic metadata
	// TODO: also it really now became a SourceState as well
	public String displayName = "Image";
	public String color = null;
	public double[] contrastLimits;
	public String imageId = null;
	public List< String > imageSetIDs = new ArrayList<>();
	public Type type = Type.Image;
	public Modality modality = Modality.FM;
	public int numSpatialDimensions = 3;
	public boolean showInitially = false;
	public BdvStackSource< ? > bdvStackSource = null;
	public Content content = null; // 3D
	public String segmentsTablePath = null;
	public List< String > additionalSegmentTableNames = new ArrayList<>(  );
	public String colorByColumn = null;
	public List< Double > selectedSegmentIds = new ArrayList<>(  );
	public boolean showSelectedSegmentsIn3d = false;
	public boolean showImageIn3d = false;
	public String xmlLocation = null;
	public SegmentsTableBdvAnd3dViews views = null;

	public enum Modality
	{
		@Deprecated
		Segmentation,
		FM,
		EM,
		XRay
	}

	public enum Type
	{
		Image,
		Segmentation,
		Mask
	}

	public Metadata( String imageId )
	{
		this.imageId = imageId;
		imageSetIDs.add( this.imageId );
	}
}
