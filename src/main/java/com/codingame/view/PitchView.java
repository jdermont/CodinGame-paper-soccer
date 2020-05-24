package com.codingame.view;

import com.codingame.game.Player;
import com.codingame.gameengine.core.MultiplayerGameManager;
import com.codingame.gameengine.module.entities.Circle;
import com.codingame.gameengine.module.entities.Curve;
import com.codingame.gameengine.module.entities.GraphicEntityModule;
import com.codingame.gameengine.module.entities.Line;
import com.codingame.paper_soccer.Pitch;
import com.google.inject.Inject;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class PitchView {
    private static final int BLOCK_SIZE = 80;
    private static final int MARGIN_X = (1920 - 9 * BLOCK_SIZE) / 2;
    private static final int MARGIN_Y = (1080 - 13 * BLOCK_SIZE) / 2;

    @Inject
    private GraphicEntityModule graphicEntityModule;

    @Inject
    private MultiplayerGameManager<Player> gameManager;

    private int colorOne;
    private int colorTwo;

    private Pitch pitch;
    private Circle ball;

    private List<Line> lastLines;

    public PitchView() {

    }

    public Pitch getPitch() {
        return pitch;
    }

    public void init(int colorOne, int colorTwo) {
        this.colorOne = colorOne;
        this.colorTwo = colorTwo;
        pitch = new Pitch();
    }

    public double makeMove(String move, Pitch.PLAYER player) {
        if (lastLines != null) {
            for (Line l : lastLines) {
                l.setLineWidth(4, Curve.NONE);
                graphicEntityModule.commitEntityState(0.0, l);
            }
        }
        lastLines = new ArrayList<>();
        int moveTime = 400 * move.length();
        int allTime = moveTime + 1000;
        gameManager.setFrameDuration(allTime);
        double frac = (double)moveTime / allTime;

        double lastTime = 0.0;

        char[] moveArray = move.toCharArray();
        for (int i=0; i < moveArray.length; i++) {
            char c = moveArray[i];
            int next = nextNode(c);
            Pitch.Line line = new Pitch.Edge(pitch.ball,next,player).toLine(pitch);
            pitch.addEdge(pitch.ball, next, player);
            pitch.ball = next;
            Line l = graphicEntityModule.createLine()
                    .setX(convertX(line.a.x))
                    .setX2(convertX(line.b.x))
                    .setY(convertY(line.a.y))
                    .setY2(convertY(line.b.y))
                    .setLineWidth(8)
                    .setLineColor(player == Pitch.PLAYER.ONE ? colorOne : colorTwo)
                    .setAlpha(0.0, Curve.NONE);
            lastLines.add(l);
            graphicEntityModule.commitEntityState(0.0, l);
            l.setAlpha(1.0, Curve.NONE);
            graphicEntityModule.commitEntityState(frac * i / moveArray.length, l);
            Point ballPosition = pitch.getPosition(pitch.ball);
            ball.setX(convertX(ballPosition.x), Curve.NONE);
            ball.setY(convertY(ballPosition.y), Curve.NONE);
            if (i == moveArray.length-1 && !pitch.isGameOver()) {
                ball.setFillColor(player != Pitch.PLAYER.ONE ? colorOne : colorTwo, Curve.NONE);
            } else {
                ball.setFillColor(player == Pitch.PLAYER.ONE ? colorOne : colorTwo, Curve.NONE);
            }
            graphicEntityModule.commitEntityState(frac * i / moveArray.length, ball);
            lastTime = frac * i / moveArray.length;
            //graphicEntityModule.commitEntityState(1.0, ball);
        }

        return lastTime;
    }

    public boolean isGameOver() {
        return pitch.isGameOver();
    }

    public Pitch.PLAYER getWinner(Pitch.PLAYER currentPlayer) {
        return pitch.getWinner(currentPlayer);
    }

    private int nextNode(char c) {
        switch(c) {
            case '5': return pitch.getNeibghour(-1, 1);
            case '4': return pitch.getNeibghour(0,1);
            case '3': return pitch.getNeibghour(1,1);
            case '6': return pitch.getNeibghour(-1,0);
            case '2': return pitch.getNeibghour(1,0);
            case '7': return pitch.getNeibghour(-1,-1);
            case '0': return pitch.getNeibghour(0,-1);
            case '1': return pitch.getNeibghour(1,-1);
        }
        return -1;
    }

    public void draw() {
        graphicEntityModule.createRectangle()
                .setWidth(9 * BLOCK_SIZE)
                .setHeight(13 * BLOCK_SIZE)
                .setFillColor(0xffffff)
                .setX(MARGIN_X)
                .setY(MARGIN_Y);

        for (int i = 0; i < pitch.mWidth + 1; i++) {
            graphicEntityModule.createLine()
                    .setX(MARGIN_X + BLOCK_SIZE / 2 + BLOCK_SIZE * i)
                    .setX2(MARGIN_X + BLOCK_SIZE / 2 + BLOCK_SIZE * i)
                    .setY(MARGIN_Y)
                    .setY2(MARGIN_Y + 13 * BLOCK_SIZE)
                    .setLineWidth(2)
                    .setLineColor(0xC0C0C0);
        }
        for (int i = 0; i < pitch.mHeight + 3; i++) {
            graphicEntityModule.createLine()
                    .setX(MARGIN_X)
                    .setX2(MARGIN_X + 9 * BLOCK_SIZE)
                    .setY(MARGIN_Y + BLOCK_SIZE / 2 + BLOCK_SIZE * i)
                    .setY2(MARGIN_Y + BLOCK_SIZE / 2 + BLOCK_SIZE * i)
                    .setLineWidth(2)
                    .setLineColor(0xC0C0C0);
        }

        graphicEntityModule.createRectangle()
                .setWidth(2 * BLOCK_SIZE)
                .setHeight(BLOCK_SIZE / 4)
                .setFillColor(colorTwo)
                .setAlpha(0.9)
                .setX(MARGIN_X + BLOCK_SIZE / 2 + BLOCK_SIZE * 3)
                .setY(MARGIN_Y + BLOCK_SIZE / 2);

        graphicEntityModule.createRectangle()
                .setWidth(2 * BLOCK_SIZE)
                .setHeight(BLOCK_SIZE / 4)
                .setFillColor(colorOne)
                .setAlpha(0.9)
                .setX(MARGIN_X + BLOCK_SIZE / 2 + BLOCK_SIZE * 3)
                .setY(MARGIN_Y + BLOCK_SIZE / 2 + 3 * BLOCK_SIZE / 4 + BLOCK_SIZE * 11);

        for (Pitch.Line line : pitch.getLines()) {
            if (line.player != Pitch.PLAYER.NONE) {
                continue;
            }
            graphicEntityModule.createLine()
                    .setX(convertX(line.a.x))
                    .setX2(convertX(line.b.x))
                    .setY(convertY(line.a.y))
                    .setY2(convertY(line.b.y))
                    .setLineWidth(4)
                    .setLineColor(0x000000);
        }

        Point p = pitch.getPosition(pitch.ball);

        ball = graphicEntityModule.createCircle()
                .setRadius(10)
                .setFillColor(colorOne)
                .setX(convertX(p.x))
                .setY(convertY(p.y))
                .setZIndex(1000);
    }

    private int convertX(int x) {
        return MARGIN_X + BLOCK_SIZE / 2 + BLOCK_SIZE * x;
    }

    private int convertY(int y) {
        return MARGIN_Y + 3 * BLOCK_SIZE / 2 + BLOCK_SIZE * y;
    }
}
