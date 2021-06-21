package de.embl.schwab.crosshair.points;

// TODO - all 3D points / vertices for all planes are currently in the same point list. This means that overlapping
// points are not allowed (while this is allowed in the 2D viewer, as each plane gets its own 2D overlay).
// This is a limitation of ImageJ 3D Viewer. All point lists must be assocated with an imagecontent, and only one per
// imageContent. Swapping to SciView at some point would be a good plan
// (this can be done with point meshes in the 3d viewer, but points can't be named, and are harder to remove)
public class Point3dOverlay {
}
