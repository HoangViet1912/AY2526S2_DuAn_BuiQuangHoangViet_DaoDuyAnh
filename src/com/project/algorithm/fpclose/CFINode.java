package com.project.algorithm.fpclose;

import java.util.HashMap;
import java.util.Map;

public class CFINode {
    private final int item;
    private double esup;
    private final CFINode parent;
    private CFINode nodeLink;
    private final Map<Integer, CFINode> children;

    public CFINode(int item, CFINode parent) {
        this.item = item;
        this.parent = parent;
        this.esup = -1.0;
        this.nodeLink = null;
        this.children = new HashMap<>();
    }

    // --- GETTERS & SETTERS ---
    public int getItem() 
    { 
        return item; 
    }

    public double getEsup() 
    { 
        return esup; 
    }

    public void setEsup(double esup) 
    { 
        this.esup = esup; 
    }

    public CFINode getParent() 
    { 
        return parent; 
    }

    public CFINode getNodeLink() 
    { 
        return nodeLink; 
    }

    public void setNodeLink(CFINode nodeLink) 
    { 
        this.nodeLink = nodeLink; 
    }

    public CFINode getChild(int item) 
    { 
        return children.get(item); 
    }

    public void addChild(int item, CFINode child) 
    { 
        children.put(item, child); 
    }
    
    public Map<Integer, CFINode> getChildren() 
    { 
        return children; 
    }
}