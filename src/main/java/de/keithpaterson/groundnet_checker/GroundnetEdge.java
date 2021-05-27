package de.keithpaterson.groundnet_checker;

import org.jgrapht.graph.DefaultEdge;
import org.w3c.dom.Element;

public class GroundnetEdge extends DefaultEdge {
    private final boolean isPushback;

    public GroundnetEdge(Element n) {
        isPushback = "1".equals(n.getAttribute("isPushBackRoute"));
    }

    public boolean isPushback() {
        return isPushback;
    }
}
