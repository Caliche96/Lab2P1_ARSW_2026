package co.eci.snake.core;

import java.util.ArrayDeque;
import java.util.Deque;

public final class Snake {
    private final Deque<Position> body = new ArrayDeque<>();
    private Direction direction;
    private int maxLength = 5;

    private Snake(Position start, Direction dir) {
        body.addFirst(start);
        this.direction = dir;
    }
    public synchronized int length() { return body.size(); }

    public static Snake of(int x, int y, Direction dir) {
        return new Snake(new Position(x, y), dir);
    }

    public synchronized Direction direction() { return direction; }

    public synchronized void turn(Direction dir) {
        if (isOpposite(direction, dir)) return;
        this.direction = dir;
    }

    private boolean isOpposite(Direction a, Direction b) {
        return (a == Direction.UP && b == Direction.DOWN) ||
                (a == Direction.DOWN && b == Direction.UP) ||
                (a == Direction.LEFT && b == Direction.RIGHT) ||
                (a == Direction.RIGHT && b == Direction.LEFT);
    }

    public synchronized Position head() { return body.peekFirst(); }

    public synchronized Deque<Position> snapshot() { return new ArrayDeque<>(body); }

    public synchronized void advance(Position newHead, boolean grow) {
        body.addFirst(newHead);
        if (grow) maxLength++;
        while (body.size() > maxLength) body.removeLast();
    }
}