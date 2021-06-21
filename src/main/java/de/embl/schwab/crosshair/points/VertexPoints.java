package de.embl.schwab.crosshair.points;

public enum VertexPoints {

    TopLeft,
    TopRight,
    BottomLeft,
    BottomRight;

    @Override
    public String toString()
    {
        switch ( this )
        {
            case TopLeft:
                return "Top Left";
            case TopRight:
                return "Top Right";
            case BottomLeft:
                return "Bottom Left";
            case BottomRight:
                return "Bottom Right";
            default:
                throw new UnsupportedOperationException("Unknown vertex");
        }
    }

    public String toShortString()
    {
        switch ( this )
        {
            case TopLeft:
                return "TL";
            case TopRight:
                return "TR";
            case BottomLeft:
                return "BL";
            case BottomRight:
                return "BR";
            default:
                throw new UnsupportedOperationException("Unknown vertex");
        }
    }
}
