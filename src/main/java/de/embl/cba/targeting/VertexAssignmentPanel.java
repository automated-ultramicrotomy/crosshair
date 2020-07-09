package de.embl.cba.targeting;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

// TODO - stop free resizing of buttons

public class VertexAssignmentPanel extends JPanel {

    private final PlaneManager planeManager;

    public VertexAssignmentPanel(PlaneManager planeManager) {
        this.planeManager = planeManager;

        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Assign Vertex"),
                BorderFactory.createEmptyBorder(5,5,5,5)));

        setLayout(new GridLayout(2, 2));
        ActionListener vertexListener = new vertexPointListener();

        String[] pointAssignments = {"Top Left", "Top Right", "Bottom Left", "Bottom Right"};
        for (String pointAssignment : pointAssignments) {
            addButton(this, pointAssignment, vertexListener);
        }
    }


    private void addButton(JPanel panel, String pointAssignment, ActionListener vertexListener) {
        JButton b = new JButton(pointAssignment);
        b.setPreferredSize(
                new Dimension(200, 100));
        b.setActionCommand(pointAssignment);
        b.addActionListener(vertexListener);
        panel.add(b);
    }

    class vertexPointListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            planeManager.nameVertex(e.getActionCommand());
        }
    }

}

