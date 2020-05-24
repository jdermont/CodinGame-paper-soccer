import java.util.*;
import java.util.stream.Collectors;
import java.awt.Point;

class Player {

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);

        Random random = new Random();
        Pitch pitch = new Pitch();
        MoveCalculator moveCalculator = new MoveCalculator(pitch);

        int myId = in.nextInt();
        Pitch.PLAYER me = myId == 0 ? Pitch.PLAYER.ONE : Pitch.PLAYER.TWO;

        while (true) {
            int opponentMoveLength = in.nextInt();
            if (in.hasNextLine()) {
                in.nextLine();
            }
            String opponentMove = in.next();

            if ("-".equals(opponentMove)) {
                // first turn, ignore
            } else {
                pitch.makeMove(opponentMove);
            }
            System.err.println(myId+" "+opponentMove);

            List<String> moves = moveCalculator.getPossibleMoves(me);
            String move = moves.get(random.nextInt(moves.size()));
            pitch.makeMove(move);
            System.out.println(move);
        }
    }
}

class MoveCalculator {
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
                    int score = 0;
                    if (goal != Pitch.PLAYER.NONE) {
                        // empty
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

class Pitch {
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