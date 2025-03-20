package de.embl.schwab.crosshair.ui.command;

import bdv.cache.SharedQueue;
import de.embl.schwab.crosshair.Crosshair;
import org.embl.mobie.io.imagedata.N5ImageData;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * ImageJ command to open Crosshair from remote ome-zarr file e.g. in an S3 bucket
 */
@Plugin(type = Command.class, menuPath = "Plugins>Crosshair>Open>Target Ome-Zarr File (remote)" )
public class OpenCrosshairFromOmeZarrRemote implements Command {

    @Parameter(
            label = "Ome-zarr URI",
            description = "S3 address to one OME-Zarr multi-scale image."
    )
    public String zarrURI = "https://s3...";

    @Parameter (
            label = "S3 Access Key (if required)",
            description = "Optional. Access key for a protected S3 bucket.",
            persist = false,
            required = false,
            style = "password"
    )
    public String s3AccessKey;

    @Parameter (
            label = "S3 Secret Key (if required)",
            description = "Optional. Secret key for a protected S3 bucket.",
            persist = false,
            required = false ,
            style = "password"
    )
    public String s3SecretKey;

    @Override
    public void run() {
        String[] s3AccessAndSecretKey = null;
        if (s3AccessKey != null && s3SecretKey != null) {
            s3AccessAndSecretKey = new String[]{s3AccessKey, s3SecretKey};
        }

        N5ImageData< ? > imageData = new N5ImageData<>(
                zarrURI,
                new SharedQueue( Math.max( 1, Runtime.getRuntime().availableProcessors() / 2 ) ),
                s3AccessAndSecretKey
        );
        new Crosshair(imageData);
    }
}
