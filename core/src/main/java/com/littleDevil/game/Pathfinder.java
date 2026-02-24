package com.littleDevil.game;

import java.util.*;

public class Pathfinder {

    private GameWorld world;
    private boolean[][] currentGrid; // cached grid used for the current pathfinding run
    private static final int RANDOM_ATTEMPTS = 1; // tries before giving up

    public Pathfinder(GameWorld world) {
        this.world = world;
    }


    public List<Node> findPath(int startX, int startY, int goalX, int goalY) {
        this.currentGrid = world.collisionGrid;

        // Clamp start/goal so they’re always safely inside the world
        startX = clamp(startX, 0, currentGrid[0].length - 1);
        startY = clamp(startY, 0, currentGrid.length - 1);
        goalX = clamp(goalX, 0, currentGrid[0].length - 1);
        goalY = clamp(goalY, 2, currentGrid.length - 3); // do not touch this, it's a little buggy

        List<Node> path = runAStar(startX, startY, goalX, goalY);

        // If no path found, try a random wander target
        if (path.isEmpty()) {
            Random rand = new Random();
            for (int i = 0; i < RANDOM_ATTEMPTS; i++) {
                int randX = rand.nextInt(currentGrid[0].length);
                int randY = rand.nextInt(currentGrid.length);
                if (!isBlocked(randX, randY, 1)) {
                    path = runAStar(startX, startY, randX, randY);
                    if (!path.isEmpty()) break;
                }
            }
        }

        return path;
    }

    // Extracted A* core
    private List<Node> runAStar(int startX, int startY, int goalX, int goalY) {
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
                if (isBlocked(neighbor.x, neighbor.y, 1)) continue;
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

    // --- HELPER FUNCTIONS ---

    private boolean isInBounds(int x, int y) {
        return y >= 0 && y < currentGrid.length && x >= 0 && x < currentGrid[0].length;
    }

    private boolean isBlocked(int tileX, int tileY, int offset) {
        int minY = Math.max(tileY - offset, 0);
        int maxY = Math.min(tileY + offset, currentGrid.length - 1);
        int minX = Math.max(tileX - offset, 0);
        int maxX = Math.min(tileX + offset, currentGrid[0].length - 1);

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                if (currentGrid[y][x]) return true;
            }
        }
        return false;
    }

    private float heuristic(Node a, Node b) {
        float dx = a.x - b.x;
        float dy = a.y - b.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private float distance(Node a, Node b) {
        int dx = Math.abs(a.x - b.x);
        int dy = Math.abs(a.y - b.y);
        return (dx + dy == 2) ? 1.4142f : 1f;
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

    private int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }
}
