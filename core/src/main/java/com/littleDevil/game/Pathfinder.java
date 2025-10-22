package com.littleDevil.game;

import java.util.*;

public class Pathfinder {

    private GameWorld world;
    private boolean[][] currentGrid; // cached grid used for the current pathfinding run

    public Pathfinder(GameWorld world) {
        this.world = world;
    }

    /**
     * Finds a path between two points using the specified grid.
     * The grid can be either the coarse or detailed one (passed by reference).
     */
    public List<Node> findPath(int startX, int startY, int goalX, int goalY) {
        this.currentGrid = world.collisionGrid;

        // Bounds check early to avoid expensive pathfinding when out of range
        if (!isInBounds(startX, startY) || !isInBounds(goalX, goalY))
            return Collections.emptyList();

        Node start = new Node(startX, startY);
        Node goal = new Node(goalX, goalY);

        PriorityQueue<Node> open = new PriorityQueue<>();
        HashSet<Node> closed = new HashSet<>();
        open.add(start);

        while (!open.isEmpty()) {
            Node current = open.poll();

            if (current.equals(goal))
                return reconstructPath(current);

            closed.add(current);

            for (Node neighbor : getNeighbors(current)) {
                if (!isInBounds(neighbor.x, neighbor.y)) continue;
                if (isBlocked(neighbor.x, neighbor.y, 1)) continue; // 1-tile offset for collision padding
                if (closed.contains(neighbor)) continue;

                float newCost = current.gCost + distance(current, neighbor);

                Optional<Node> existing = open.stream()
                    .filter(n -> n.equals(neighbor))
                    .findFirst();

                if (!existing.isPresent() || newCost < existing.get().gCost) {
                    neighbor.gCost = newCost;
                    neighbor.hCost = heuristic(neighbor, goal);
                    neighbor.parent = current;

                    if (!existing.isPresent()) open.add(neighbor);
                }
            }
        }

        return Collections.emptyList(); // no path found
    }

    // ========================
    // --- HELPER FUNCTIONS ---
    // ========================

    private boolean isInBounds(int x, int y) {
        return y >= 0 && y < currentGrid.length && x >= 0 && x < currentGrid[0].length;
    }

    private boolean isBlocked(int tileX, int tileY, int offset) {
        // Check a square around (tileX, tileY) with size offset
        for (int y = tileY - offset; y <= tileY + offset; y++) {
            for (int x = tileX - offset; x <= tileX + offset; x++) {
                if (!isInBounds(x, y)) continue;
                if (currentGrid[y][x]) return true;
            }
        }
        return false;
    }

    private float heuristic(Node a, Node b) {
        // Euclidean distance gives smooth diagonals
        float dx = a.x - b.x;
        float dy = a.y - b.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private float distance(Node a, Node b) {
        // Diagonals cost slightly more than straight movement
        int dx = Math.abs(a.x - b.x);
        int dy = Math.abs(a.y - b.y);
        return (dx + dy == 2) ? 1.4142f : 1f; // √2 for diagonals
    }

    private List<Node> getNeighbors(Node node) {
        List<Node> result = new ArrayList<>(8);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                result.add(new Node(node.x + dx, node.y + dy));
            }
        }
        return result;
    }

    private List<Node> reconstructPath(Node goal) {
        List<Node> path = new ArrayList<>();
        Node current = goal;
        while (current != null) {
            path.add(current);
            current = current.parent;
        }
        Collections.reverse(path);
        return path;
    }
}
