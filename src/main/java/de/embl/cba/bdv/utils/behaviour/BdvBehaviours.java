package de.embl.cba.bdv.utils.behaviour;

import bdv.ij.util.ProgressWriterIJ;
import bdv.tools.boundingbox.TransformedRealBoxSelectionDialog;
import bdv.util.BdvHandle;
import de.embl.cba.bdv.utils.BdvDialogs;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.bdv.utils.Logger;
import de.embl.cba.bdv.utils.capture.BdvViewCaptures;
import de.embl.cba.bdv.utils.capture.ViewCaptureDialog;
import de.embl.cba.bdv.utils.export.BdvRealSourceToVoxelImageExporter;
import ij.IJ;
import net.imglib2.FinalRealInterval;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.util.Behaviours;

import javax.swing.*;
import java.util.List;

import static de.embl.cba.bdv.utils.export.BdvRealSourceToVoxelImageExporter.*;

// TODO:
// - remove logging, return things

public class BdvBehaviours
{
	public static void addPositionAndViewLoggingBehaviour(
			BdvHandle bdv,
			Behaviours behaviours,
			String trigger )
	{
		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {

			(new Thread( () -> {
				Logger.log( "\nBigDataViewer position: " + BdvUtils.getGlobalMousePositionString( bdv ) );
				Logger.log( "BigDataViewer transform: " + BdvUtils.getBdvViewerTransformString( bdv ) );
			} )).start();

		}, "Print position and view", trigger ) ;
	}

	public static void addViewCaptureBehaviour(
			BdvHandle bdvHandle,
			Behaviours behaviours,
			String trigger )
	{
		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) ->
		{
			new Thread( () -> {
				new ViewCaptureDialog( bdvHandle ).run();
			}).start();
		}, "capture raw view", trigger ) ;
	}

	public static void addSimpleViewCaptureBehaviour(
			BdvHandle bdv,
			Behaviours behaviours,
			String trigger )
	{
		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) ->
		{
			new Thread( () -> {
				SwingUtilities.invokeLater( () -> {
					final JFileChooser jFileChooser = new JFileChooser();
					if ( jFileChooser.showSaveDialog( bdv.getViewerPanel() ) == JFileChooser.APPROVE_OPTION )
					{
						BdvViewCaptures.saveScreenShot(
								jFileChooser.getSelectedFile(),
								bdv.getViewerPanel() );
					}
				});
			}).start();

		}, "capture simple view", trigger ) ;
	}


	public static void addExportSourcesToVoxelImagesBehaviour(
			BdvHandle bdvHandle,
			Behaviours behaviours,
			String trigger )
	{
		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) ->
		{
			new Thread( () ->
			{
				final FinalRealInterval maximalRangeInterval = BdvUtils.getRealIntervalOfVisibleSources( bdvHandle );

				final TransformedRealBoxSelectionDialog.Result result =
						BdvDialogs.showBoundingBoxDialog(
								bdvHandle,
								maximalRangeInterval );

				BdvUtils.getVoxelDimensionsOfCurrentSource( bdvHandle ).dimensions( Dialog.outputVoxelSpacings );

				if ( ! Dialog.showDialog() ) return;

				final BdvRealSourceToVoxelImageExporter exporter =
						new BdvRealSourceToVoxelImageExporter(
								bdvHandle,
								BdvUtils.getVisibleSourceIndices( bdvHandle ),
								result.getInterval(),
								result.getMinTimepoint(),
								result.getMaxTimepoint(),
								Dialog.interpolation,
								Dialog.outputVoxelSpacings,
								Dialog.exportModality,
								Dialog.exportDataType,
								Dialog.numProcessingThreads,
								new ProgressWriterIJ()
						);

				if ( Dialog.exportModality.equals( ExportModality.SaveAsTiffVolumes ) )
				{
					final String outputDirectory = IJ.getDirectory( "Choose and output directory" );
					exporter.setOutputDirectory( outputDirectory );
				}

				exporter.export();

			}).start();
		}, "ExportSourcesToVoxelImages", trigger ) ;
	}

	public static void addDisplaySettingsBehaviour(
			BdvHandle bdv,
			Behaviours behaviours,
			String trigger )
	{
		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) ->
						BdvDialogs.showDisplaySettingsDialogForSourcesAtMousePosition(
								bdv,
								false,
								true ),
				"show display settings dialog",
				trigger ) ;
	}

	public static void addSourceBrowsingBehaviour( BdvHandle bdv, Behaviours behaviours  )
	{
		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {

			(new Thread( () -> {
				final int currentSource = bdv.getViewerPanel().getVisibilityAndGrouping().getCurrentSource();
				if ( currentSource == 0 ) return;
				bdv.getViewerPanel().getVisibilityAndGrouping().setCurrentSource( currentSource - 1 );
			} )).start();

		}, "Go to previous source", "J" ) ;

		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> {

			(new Thread( () -> {
				final int currentSource = bdv.getViewerPanel().getVisibilityAndGrouping().getCurrentSource();
				if ( currentSource == bdv.getViewerPanel().getVisibilityAndGrouping().numSources() - 1  ) return;
				bdv.getViewerPanel().getVisibilityAndGrouping().setCurrentSource( currentSource + 1 );
			} )).start();

		}, "Go to next source", "K" ) ;
	}
}
