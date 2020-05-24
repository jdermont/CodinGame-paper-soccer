package com.codingame.paper_soccer;

import com.codingame.game.Player;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class Pitch {
    public enum PLAYER { ONE, TWO, NONE }

    public static class Line {
        public Point a,b;
        public PLAYER player;

        public Line(Point a, Point b) {
            this(a,b, PLAYER.NONE);
        }

        public Line(Point a, Point b, PLAYER player) {
            this.a = a;
            this.b = b;
            this.player = player;
        }

        public Line(Line line) {
            this.a = new Point(line.a);
            this.b = new Point(line.b);
            this.player = line.player;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof Line) {
                Line other = (Line)o;
                return a.equals(other.a) && b.equals(other.b) && player == other.player;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return 2*a.hashCode()+b.hashCode();
        }

        @Override
        public String toString() {
            return a.toString()+","+b.toString();
        }
    }

    public static class Edge {
        public int a;
        public int b;
        public PLAYER player;

        public Edge(int a, int b, PLAYER player) {
            this.a = a;
            this.b = b;
            this.player = player;
        }

        public Line toLine(Pitch field) {
            return new Line(field.getPosition(a),field.getPosition(b));
        }
    }

    public int mWidth, mHeight,mSize;
    public int ball;

    private int[][] matrix;
    private int[] matrixNodes;
    private int[][] matrixNeibghours;
    private PLAYER[] goalArray;
    private PLAYER[] almostGoalArray;

    public Pitch() {
        mWidth = 8;
        mHeight = 10;

        int w = mWidth +1;
        int h = mHeight +1;

        int wh = w*h;
        mSize = wh+6;
        matrix = new int[mSize][mSize];
        matrixNodes = new int[mSize];

        // assign 2D points into matrix
        for (int i=0;i<wh;i++) {
            int x = i/h;
            int y = i%h;
            matrix[i][i] = (x<<8)+y;
        }
        // goals
        matrix[wh][wh] = ((mWidth /2-1)<<8) + 0xFF;
        matrix[wh+1][wh+1] = ((mWidth /2)<<8) + 0xFF;
        matrix[wh+2][wh+2] = ((mWidth /2+1)<<8) + 0xFF;
        matrix[wh+3][wh+3] = ((mWidth /2-1)<<8) + h;
        matrix[wh+4][wh+4] = ((mWidth /2)<<8) + h;
        matrix[wh+5][wh+5] = ((mWidth /2+1)<<8) + h;

        // make Neibghours
        for (int i=0;i< mSize;i++) {
            makeNeibghours(i);
        }
        // remove some 'adjacency' from goals
        removeAdjacency(wh,h*(mWidth /2-2));
        removeAdjacency(wh+2,h*(mWidth /2+2));
        removeAdjacency(wh+3,h*(mWidth /2-1)-1);
        removeAdjacency(wh+5,h*(mWidth /2+3)-1);

        // add edges except for goal
        for (int i=0;i<wh-1;i++) {
            for (int j=i+1;j<wh;j++) {
                Point p = getPosition(i);
                Point p2 = getPosition(j);
                if ((p.x == 0 && p2.x == 0) && distance(p,p2)<=1) addEdge(i,j);
                else if ((p.x == mWidth && p2.x == mWidth) && distance(p,p2)<=1) addEdge(i,j);
                else if ((p.y == 0 && p2.y == 0) && distance(p,p2)<=1) {
                    if (p.x < mWidth /2-1 || p.x >= mWidth /2+1) addEdge(i,j);
                } else if ((p.y == mHeight && p2.y == mHeight) && distance(p,p2)<=1) {
                    if (p.x < mWidth /2-1 || p.x >= mWidth /2+1) addEdge(i,j);
                }
            }
        }
        // and goals
        addEdge(wh,wh+1);
        addEdge(wh+1,wh+2);
        addEdge(wh,h*(mWidth /2-1));
        addEdge(wh+2,h*(mWidth /2+1));
        addEdge(wh+3,wh+4);
        addEdge(wh+4,wh+5);
        addEdge(wh+3,h*(mWidth /2)-1);
        addEdge(wh+5,h*(mWidth /2+2)-1);

        // create adjacency list
        matrixNeibghours = new int[mSize][];
        for (int i=0;i< mSize;i++) {
            matrixNeibghours[i] = getNeibghours(i);
            matrixNodes[i] += matrixNeibghours[i].length<<4;
        }

        // create auxiliary arrays for goals
        goalArray = new PLAYER[mSize];
        almostGoalArray = new PLAYER[mSize];
        for (int i=0;i<wh;i++) {
            goalArray[i] = PLAYER.NONE;
            almostGoalArray[i] = goalArray[i];
        }
        for (int i=wh;i< mSize;i++) {
            goalArray[i] = (i-wh)/3==0? PLAYER.TWO: PLAYER.ONE;
            almostGoalArray[i] = goalArray[i];
        }
        almostGoalArray[h*(mWidth/2-1)] = PLAYER.TWO;
        almostGoalArray[h*(mWidth/2+1)] = PLAYER.TWO;
        almostGoalArray[h*(mWidth/2)-1] = PLAYER.ONE;
        almostGoalArray[h*(mWidth/2+2)-1] = PLAYER.ONE;

        // ball in center
        ball = mWidth /2 * h + h/2;
    }

    public void setPitch(Pitch pitch) {
        ball = pitch.ball;
        System.arraycopy(pitch.matrixNodes, 0, matrixNodes, 0, pitch.matrixNodes.length);
        for (int i=0; i < mSize; i++) {
            System.arraycopy(pitch.matrix[i], 0, matrix[i], 0, pitch.matrix[i].length);
            System.arraycopy(pitch.matrixNeibghours[i], 0, matrixNeibghours[i], 0, pitch.matrixNeibghours[i].length);
        }
    }

    private void makeNeibghours(int index) {
        Point point = getPosition(index);

        for (int i=0;i<mSize;i++) {
            if (i == index) continue;
            Point p = getPosition(i);
            if (distance(point,p) <= 1) {
                matrixNodes[i]++;
                matrix[index][i] = 1;
                matrix[i][index] = 1;
            }
        }
    }

    private static int distance(Point p1, Point p2) {
        return (int) Math.sqrt((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y) * (p1.y - p2.y));
    }

    public Point getPosition(int index) {
        int x = (matrix[index][index]>>8)&0xFF;
        int y = matrix[index][index]&0xFF;
        if (y == 0xFF) y = -1;
        return new Point(x,y);
    }

    private void removeAdjacency(int a, int b) {
        matrix[a][b] = 0;
        matrix[b][a] = 0;
        matrixNodes[a]--;
        matrixNodes[b]--;
    }

    public boolean passNextDone() {
        return (matrixNodes[ball]&0x0F) < (matrixNodes[ball]>>4)-1;
    }

    public boolean passNext(int index) {
        return (matrixNodes[index]&0x0F) < (matrixNodes[index]>>4);
    }

    public boolean existsEdge(int a, int b) {
        return (matrix[a][b] & 2) != 0;
    }

    public boolean isAlmostBlocked(int index) {
        return (matrixNodes[index]&0x0F) <= 1;
    }

    public boolean isBlocked(int index) {
        return (matrixNodes[index]&0x0F) == 0;
    }

    public void addEdge(int a, int b) {
        matrix[a][b] |= 2;
        matrix[b][a] |= 2;
        matrixNodes[a]--;
        matrixNodes[b]--;
    }

    public void addEdge(int a, int b, PLAYER player) {
        matrix[a][b] |= 2|(player== PLAYER.ONE?4:8);
        matrix[b][a] |= 2|(player== PLAYER.ONE?4:8);
        matrixNodes[a]--;
        matrixNodes[b]--;
    }

    public void removeEdge(int a, int b) {
        matrix[a][b] = 1;
        matrix[b][a] = 1;
        matrixNodes[a]++;
        matrixNodes[b]++;
    }

    public PLAYER goal(int index) {
        return goalArray[index];
    }

    private int[] getNeibghours(int index) {
        List<Integer> neibghoursList = new ArrayList<Integer>();
        for (int i=0;i<mSize;i++) {
            if (i == index) continue;
            if ((matrix[index][i]&1) == 1) neibghoursList.add(i);
        }
        int[] output = new int[neibghoursList.size()];
        for (int i=0;i<neibghoursList.size();i++) output[i] = neibghoursList.get(i);
        return output;
    }

    public int getNeibghour(int dx, int dy) {
        Point point = getPosition(ball);
        for (int n : matrixNeibghours[ball]) {
            Point p = getPosition(n);
            if (point.x+dx == p.x && point.y+dy == p.y) return n;
        }
        return -1;
    }

    public List<Line> getLines() {
        List<Line> lines = new ArrayList<Line>();

        for (int i=0;i<mSize;i++) {
            for (int j=0;j<i;j++) {
                if ((matrix[i][j]&2) > 0) {
                    Point a = getPosition(i);
                    Point b = getPosition(j);
                    Line line = new Line(a,b);
                    if ((matrix[i][j]&4) > 0) line.player = PLAYER.ONE;
                    else if ((matrix[i][j]&8) > 0) line.player = PLAYER.TWO;
                    lines.add(line);
                }
            }
        }
        return lines;
    }

    public List<Integer> getFreeNeighbours(int index) {
        int n = matrixNodes[index]&0x0F;
        List<Integer> ns = new ArrayList<>();
        for (int i=0,j=0;j<n;i++) {
            if (matrix[index][matrixNeibghours[index][i]] == 1) {
                ns.add(matrixNeibghours[index][i]);
                j++;
            }
        }
        return ns;
    }

    public void makeMove(String move) {
        for (char c : move.toCharArray()) {
            int n;
            switch (c) {
                case '0': n = getNeibghour(0, -1); break;
                case '1': n = getNeibghour(1, -1); break;
                case '2': n = getNeibghour(1, 0); break;
                case '3': n = getNeibghour(1, 1); break;
                case '4': n = getNeibghour(0, 1); break;
                case '5': n = getNeibghour(-1, 1); break;
                case '6': n = getNeibghour(-1, 0); break;
                case '7': n = getNeibghour(-1, -1); break;
                default: n = -1;
            }
            addEdge(ball, n);
            ball = n;
        }
    }

    public boolean isGameOver() {
        return goalArray[ball] != PLAYER.NONE || isBlocked(ball);
    }

    public Pitch.PLAYER getWinner(Pitch.PLAYER currentPlayer) {
        if (goalArray[ball] != PLAYER.NONE) {
            return goalArray[ball] == PLAYER.ONE ? PLAYER.TWO : PLAYER.ONE;
        }
        return currentPlayer == PLAYER.ONE ? PLAYER.TWO : PLAYER.ONE;
    }



    public char getDistanceChar(int a, int b) {
        Point p1 = getPosition(a);
        Point p2 = getPosition(b);
        int dx = p2.x-p1.x;
        int dy = p2.y-p1.y;

        if (dx == 0) {
            if (dy == -1) return '0';
            return '4';
        } else if (dx == 1) {
            if (dy == -1) return '1';
            if (dy == 0) return '2';
            return '3';
        } else {
            if (dy == -1) return '7';
            if (dy == 0) return '6';
            if (dy == 1) return '5';
        }
        return (char)0;
    }
}