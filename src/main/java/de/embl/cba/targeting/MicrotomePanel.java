package de.embl.cba.targeting;

import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.SliderPanel;
import bdv.tools.brightness.SliderPanelDouble;
import bdv.util.*;
import bdv.viewer.Interpolation;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.bdv.utils.BrightnessUpdateListener;
import de.embl.cba.bdv.utils.Logger;
import de.embl.cba.bdv.utils.sources.ARGBConvertedRealSource;
import de.embl.cba.bdv.utils.sources.Metadata;
import de.embl.cba.tables.FileAndUrlUtils;
import de.embl.cba.tables.FileUtils;
import de.embl.cba.tables.TableColumns;
import de.embl.cba.tables.color.ColorUtils;
import de.embl.cba.tables.color.LazyLabelsARGBConverter;
import de.embl.cba.tables.ij3d.UniverseUtils;
import de.embl.cba.tables.image.DefaultImageSourcesModel;
import de.embl.cba.tables.image.SourceAndMetadata;
import de.embl.cba.tables.tablerow.TableRowImageSegment;
import de.embl.cba.tables.view.Segments3dView;
import de.embl.cba.tables.view.SegmentsBdvView;
import de.embl.cba.tables.view.TableRowsTableView;
import de.embl.cba.tables.view.combined.SegmentsTableBdvAnd3dViews;
import ij3d.Content;
import ij3d.ContentConstants;
import ij3d.Image3DUniverse;
import net.imglib2.type.numeric.ARGBType;
import org.scijava.java3d.Transform3D;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import org.scijava.vecmath.*;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.util.*;
import java.util.List;


// similar to mobie source panel - https://github.com/mobie/mobie-viewer-fiji/blob/master/src/main/java/de/embl/cba/mobie/ui/viewer/SourcesPanel.java

public class MicrotomePanel extends JPanel {

    private final MicrotomeManager microtomeManager;
    private final JSlider initialKnifeAngle;
    private final JSlider initialTiltAngle;
    private final JSlider knifeAngle;
    private final JSlider tiltAngle;
    private final JSlider rotationAngle;

    public MicrotomePanel(MicrotomeManager microtomeManager) {
        this.microtomeManager = microtomeManager;

        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Microtome Controls"),
                BorderFactory.createEmptyBorder(5,5,5,5)));

        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        ActionListener blockListener = new blockListener();
        JButton initialiseBlock = new JButton("Initialise block");
        initialiseBlock.setActionCommand("initialise_block");
        initialiseBlock.addActionListener(blockListener);
        this.add(initialiseBlock);

        // Initial knife angle
        initialKnifeAngle =
                addSliderToPanel(this, "Initial Knife Angle", -30, 30, 0, 10, 1);

        // Initial tilt angle
        initialTiltAngle =
                addSliderToPanel(this, "Initial tilt angle", -20, 20, 0, 10, 1);

        // Knife Rotation
        ChangeListener knifeListener = new KnifeListener();
        knifeAngle = addSliderToPanel(this, "Knife Rotation",  -30, 30, 0, 10,
                1, knifeListener);

        // Arc Tilt
        ChangeListener holderBackListener = new HolderBackListener();
        tiltAngle =
                addSliderToPanel(this, "Arc Tilt", -20, 20, 0, 10, 1, holderBackListener);

        // Holder Rotation
        ChangeListener holderFrontListener = new HolderFrontListener();
        rotationAngle =
                addSliderToPanel(this, "Holder rotation", -180, 180, 0, 60, 1, holderFrontListener);

//        Orientation of axes matches those in original blender file, object positions also match
//        Interactive transform setter in 3d viewer: https://github.com/fiji/3D_Viewer/blob/master/src/main/java/ij3d/gui/InteractiveTransformDialog.java
    }

    public JSlider getKnifeAngle() {
        return knifeAngle;
    }

    public JSlider getTiltAngle() {
        return tiltAngle;
    }

    public JSlider getRotationAngle() {
        return rotationAngle;
    }

    private JSlider addSliderToPanel(JPanel panel, String sliderName, int min, int max, int currentValue, int majorTickSpacing,
                                     int minorTickSpacing, ChangeListener changeListener) {

        panel.add( new JLabel(sliderName));
        JSlider s = new JSlider(JSlider.HORIZONTAL, min, max, currentValue);
        s.addChangeListener(changeListener);
        s.setMajorTickSpacing(majorTickSpacing);
        s.setMinorTickSpacing(minorTickSpacing);
        s.setPaintTicks(true);
        s.setPaintLabels(true);
        panel.add(s);
        refreshGui();
        return s;
    }

    private JSlider addSliderToPanel(JPanel panel, String sliderName, int min, int max, int currentValue, int majorTickSpacing,
                                  int minorTickSpacing) {
        panel.add( new JLabel(sliderName));
        JSlider s = new JSlider(JSlider.HORIZONTAL, min, max, currentValue);
        s.setMajorTickSpacing(majorTickSpacing);
        s.setMinorTickSpacing(minorTickSpacing);
        s.setPaintTicks(true);
        s.setPaintLabels(true);
        panel.add(s);
        refreshGui();
        return s;
    }

    private void refreshGui() {
        this.revalidate();
        this.repaint();
    }


    class blockListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            microtomeManager.initialiseMicrotome(initialKnifeAngle.getValue(), initialTiltAngle.getValue());
        }
    }

    class KnifeListener implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
            JSlider source = (JSlider) e.getSource();
            double knifeTilt = (double) source.getValue();
            microtomeManager.setKnifeAngle(knifeTilt);
        }
    }

    class HolderBackListener implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
            JSlider source = (JSlider) e.getSource();
            double tilt = (double) source.getValue();
            microtomeManager.setTilt(tilt);
        }
    }

    class HolderFrontListener implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
            JSlider source = (JSlider) e.getSource();
            double rotation = (double) source.getValue();
            microtomeManager.setRotation(rotation);
        }
    }

}

