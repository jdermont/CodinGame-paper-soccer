package com.codingame.paper_soccer;

import java.util.*;
import java.util.stream.Collectors;

public class MoveCalculator {
    private static final int SIZE_LIMIT = 10;
    private static final int INFINITY = 1000;

    private Pitch pitch;

    public MoveCalculator(Pitch pitch) {
        this.pitch = pitch;
    }

    public List<String> getPossibleMoves(Pitch.PLAYER player) {
        List<Move> moves = new ArrayList<>();
        Deque<Pair<Integer,List<Path>>> deque = new ArrayDeque<>();
        List<Integer> vertices = new ArrayList<>();
        List<Integer> pathCycles = new ArrayList<>();
        List<Integer> blockedMoves = new ArrayList<>();
        StringBuilder str = new StringBuilder();

        vertices.add(pitch.ball);
        deque.push(new Pair<>(pitch.ball, new ArrayList<>()));

        while (!deque.isEmpty() && moves.size() < SIZE_LIMIT) {
            Pair<Integer, List<Path>> v_paths = deque.pollFirst();
            int t = v_paths.first;
            List<Path> paths = v_paths.second;
            for (Path p : paths) {
                str.append(pitch.getDistanceChar(p.a, p.b));
                pitch.addEdge(p.a, p.b);
                vertices.add(p.b);
            }
            pitch.ball = t;
            List<Integer> freeNeighbours = pitch.getFreeNeighbours(t);
            for (int n : freeNeighbours) {
                Pitch.PLAYER goal = pitch.goal(n);
                if (pitch.passNext(n) && !pitch.isAlmostBlocked(n) && goal == Pitch.PLAYER.NONE) {
                    List<Path> newPaths = new ArrayList<>(paths);
                    newPaths.add(new Path(t,n));
                    if (vertices.contains(n)) {
                        int newPathsHash = hashPaths(newPaths);
                        if (!pathCycles.contains(newPathsHash)) {
                            pathCycles.add(newPathsHash);
                            deque.push(new Pair<>(n, newPaths));
                        }
                    } else {
                        deque.push(new Pair<>(n, newPaths));
                    }
                } else {
                    pitch.addEdge(t, n);
                    pitch.ball = n;
                    int score;
                    if (goal != Pitch.PLAYER.NONE) {
                        score = player == goal ? (-INFINITY + 5) : INFINITY;
                    } else if (pitch.isBlocked(n)) {
                        List<Path> newPaths = new ArrayList<>(paths);
                        newPaths.add(new Path(t,n));
                        int newPathsHash = hashPaths(newPaths);
                        if (blockedMoves.contains(newPathsHash)) {
                            pitch.ball = t;
                            pitch.removeEdge(t, n);
                            continue;
                        }
                        blockedMoves.add(newPathsHash);
                        score = -INFINITY;
                    } else {
                        score = player == Pitch.PLAYER.ONE ? -pitch.getPosition(n).y : pitch.getPosition(n).y;
                    }

                    pitch.ball = t;
                    pitch.removeEdge(t, n);
                    str.append(pitch.getDistanceChar(t, n));
                    moves.add(new Move(str.toString(), score));
                    str.setLength(str.length()-1);
                    if (moves.size() >= SIZE_LIMIT) break;
                }
            }

            vertices = vertices.subList(0, 1);
            str.setLength(0);
            for (Path p : paths) {
                pitch.removeEdge(p.a, p.b);
            }
            pitch.ball = vertices.get(0);
        }

        int maxScore = Collections.max(moves).score;

        return moves.stream().filter(move -> move.score == maxScore).map(move -> move.move).collect(Collectors.toList());
    }

    private static int hashPaths(List<Path> paths) {
        int output = 0;
        for (Path path : paths) {
            int h = 78901*path.hashCode();
            h ^= h << 13;
            h ^= h >> 17;
            h ^= h << 5;
            output += h;
        }
        return output;
    }

    private static class Move implements Comparable<Move> {
        public String move;
        public int score;

        public Move(String move, int score) {
            this.move = move;
            this.score = score;
        }

        @Override
        public int compareTo(Move move) {
            return Integer.compare(score, move.score);
        }
    }

    private static class Pair<A,B> {
        public A first;
        public B second;

        public Pair(A first, B second) {
            this.first = first;
            this.second = second;
        }
    }

    private static class Path {
        public int a;
        public int b;

        public Path(int a, int b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Path path = (Path) o;
            return (a == path.a && b == path.b) || (a == path.b && b == path.a);
        }

        @Override
        public int hashCode() {
            return a > b ? ((b << 16) + a) : ((a << 16) + b);
        }
    }
}

