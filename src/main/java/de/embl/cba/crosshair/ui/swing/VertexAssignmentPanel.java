package de.embl.cba.crosshair.ui.swing;

import de.embl.cba.crosshair.PlaneManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

public class VertexAssignmentPanel extends CrosshairPanel {

    private PlaneManager planeManager;
    private Map<String, JButton> buttons;
    private CrosshairFrame crosshairFrame;

    public VertexAssignmentPanel(CrosshairFrame crosshairFrame) {
        this.crosshairFrame = crosshairFrame;
    }

    public void initialisePanel () {
        this.planeManager = crosshairFrame.getPlaneManager();
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
//        b.setPreferredSize(
//                new Dimension(200, 100));
        b.setActionCommand(pointAssignment);
        b.addActionListener(vertexListener);
        panel.add(b);
        buttons.put(pointAssignment, b);
    }

    class vertexPointListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            planeManager.nameSelectedVertex(e.getActionCommand());
        }
    }

}

