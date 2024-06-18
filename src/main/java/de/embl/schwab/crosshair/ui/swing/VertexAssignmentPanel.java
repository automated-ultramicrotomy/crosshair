package de.embl.schwab.crosshair.ui.swing;

import de.embl.schwab.crosshair.Crosshair;
import de.embl.schwab.crosshair.plane.PlaneManager;
import de.embl.schwab.crosshair.points.VertexPoint;

import javax.swing.*;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

/**
 * Class for UI Panel allowing assignment of vertices as top left / top right etc...
 */
public class VertexAssignmentPanel extends CrosshairPanel {

    private PlaneManager planeManager;
    private Map<String, JButton> buttons;
    private CrosshairFrame crosshairFrame;

    public VertexAssignmentPanel() {}

    /**
     * Initialise panel from settings in main Crosshair UI
     * @param crosshairFrame main crosshair UI
     */
    public void initialisePanel ( CrosshairFrame crosshairFrame ) {
        this.crosshairFrame = crosshairFrame;
        this.planeManager = crosshairFrame.getCrosshair().getPlaneManager();
        buttons = new HashMap<>();

        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Assign Vertex"),
                BorderFactory.createEmptyBorder(5,5,5,5)));

        setLayout(new GridLayout(1, 4));
        ActionListener vertexListener = new vertexPointListener();

        String[] pointAssignments = {"Top Left", "Top Right", "Bottom Left", "Bottom Right"};
        for (String pointAssignment : pointAssignments) {
            addButton(this, pointAssignment, vertexListener);
        }
    }

    public void enableButtons () {
        for (String buttonName : buttons.keySet()) {
            buttons.get(buttonName).setEnabled(true);
        }
    }

    public void disableButtons () {
        for (String buttonName : buttons.keySet()) {
            buttons.get(buttonName).setEnabled(false);
        }
    }

    private void addButton(JPanel panel, String pointAssignment, ActionListener vertexListener) {
        JButton b = new JButton(pointAssignment);
        b.setActionCommand(pointAssignment);
        b.addActionListener(vertexListener);
        panel.add(b);
        buttons.put(pointAssignment, b);
    }

    class vertexPointListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if ( planeManager.checkNamedPlaneExists( Crosshair.block )) {
                VertexPoint vertexPoint = VertexPoint.fromString( e.getActionCommand() );
                planeManager.getVertexDisplay( Crosshair.block ).assignSelectedVertex( vertexPoint );
            }
        }
    }

}

