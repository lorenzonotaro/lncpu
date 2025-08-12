package com.lnc.cc.codegen;

import com.mxgraph.layout.mxCircleLayout;
import com.mxgraph.model.mxGraphModel;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static java.awt.event.KeyEvent.VK_ENTER;
import static javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW;

public class InterferenceGraphVisualizer extends JFrame {
    final Object lock = new Object();

    private static final InterferenceGraphVisualizer _instance = new InterferenceGraphVisualizer();

    private static final Map<Register, String> COLOR_MAP = Map.of(
            Register.RA, "green",
            Register.RB, "red",
            Register.RC, "blue",
            Register.RD, "yellow",
            Register.RCRD, "orange");

    private final mxGraph mxGraph;
    private final JPanel content;
    private final mxCircleLayout layout;
    private final mxGraphComponent component;
    private Collection<InterferenceGraph.Node> graph;

    private InterferenceGraphVisualizer() {
        setTitle("Interference Graph Visualizer");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        this.setContentPane(this.content = new JPanel(new BorderLayout()));
        this.content.add(this.component = new mxGraphComponent(this.mxGraph = new mxGraph()), BorderLayout.CENTER);
        this.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        this.layout = new com.mxgraph.layout.mxCircleLayout(mxGraph);
        this.layout.setRadius(100);


        this.getRootPane().getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ENTER"), "continue");
        this.getRootPane().getActionMap().put("continue", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                synchronized (lock) {
                    lock.notify();
                }
            }
        });
    }

    public static void showVisualizer() {
        _instance.remakeGraph();
        _instance.setVisible(true);
        try {
            synchronized (_instance.lock){
                _instance.lock.wait();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void setGraph(Collection<InterferenceGraph.Node> graph) {
        _instance.graph = graph;
        update();
    }

    public static void update(){
        _instance.remakeGraph();
        _instance.repaint();
    }

    private void remakeGraph() {
        // first, erase the graph
        mxGraph.getModel().beginUpdate();
        try {
            mxGraph.setModel(new mxGraphModel());
            if (graph == null || graph.isEmpty()) {
                return;
            }

            Map<InterferenceGraph.Node, Object> vertexMap = new LinkedHashMap<>();

            // add nodes
            for (InterferenceGraph.Node node : graph) {
                var nodeN = String.valueOf(node.vr.getRegisterNumber());
                Register nodeReg = node.isPhysical() ? node.phys : node.assigned;
                String color = nodeReg == null ? "gray" : COLOR_MAP.getOrDefault(nodeReg, "gray");
                var v = mxGraph.insertVertex(null, nodeN, "r" + nodeN, 0, 0, 80, 30,
                        "strokeColor=%s;fillColor=%s;".formatted(color, color));
                vertexMap.put(node, v);
            }

            // add edges
            for (InterferenceGraph.Node node : graph) {
                for (InterferenceGraph.Node neighbor : node.adj) {
                    if (neighbor != null && vertexMap.containsKey(neighbor)) {
                        mxGraph.insertEdge(null, node.vr.getRegisterNumber() + "-" + neighbor.vr.getRegisterNumber(), "", Objects.requireNonNull(vertexMap.get(node)), Objects.requireNonNull(vertexMap.get(neighbor)));
                    }
                }
            }
        } finally {
            mxGraph.getModel().endUpdate();
            this.layout.execute(mxGraph.getDefaultParent());
            content.setSize(this.component.getSize());
        }
    }
}
