package com.littleDevil.game;

public class Node implements Comparable<Node> {
    public int x, y;
    public float gCost; // cost from start
    public float hCost; // heuristic to goal
    public Node parent;

    public Node(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public float fCost() {
        return gCost + hCost;
    }

    @Override
    public int compareTo(Node other) {
        return Float.compare(this.fCost(), other.fCost());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Node)) return false;
        Node n = (Node)obj;
        return this.x == n.x && this.y == n.y;
    }

    @Override
    public int hashCode() {
        return 31 * x + y;
    }
}
