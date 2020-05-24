package com.codingame.game;

import com.codingame.gameengine.core.AbstractPlayer.TimeoutException;
import com.codingame.gameengine.core.AbstractReferee;
import com.codingame.gameengine.core.GameManager;
import com.codingame.gameengine.core.MultiplayerGameManager;
import com.codingame.gameengine.module.entities.*;
import com.codingame.paper_soccer.InvalidMove;
import com.codingame.paper_soccer.MoveVerificator;
import com.codingame.paper_soccer.Pitch;
import com.codingame.view.PitchView;
import com.google.inject.Inject;
import com.google.inject.Provider;

import java.util.Random;

public class Referee extends AbstractReferee {
    @Inject private MultiplayerGameManager<Player> gameManager;
    @Inject private GraphicEntityModule graphicEntityModule;
    @Inject private Provider<PitchView> pitchViewProvider;

    private String lastMove = null;
    private Random random;

    private PitchView pitchView;
    private MoveVerificator moveVerificator;
    private Rectangle background;
    
    @Override
    public void init() {
        random = new Random(gameManager.getSeed());

        moveVerificator = new MoveVerificator();

        drawBackground();
        drawHud();
        drawPitch();

        gameManager.setFrameDuration(600);
        gameManager.setMaxTurns(66);
        gameManager.setTurnMaxTime(200);

        for(Player player : gameManager.getPlayers()) {
            player.sendInputLine(String.format("%d", player.getIndex()));
        }
    }

    private void drawBackground() {
        graphicEntityModule.createRectangle()
                .setFillColor(0x000000)
                .setWidth(1920)
                .setHeight(1080);

        background = graphicEntityModule.createRectangle()
                .setFillColor(0x000000)
                .setWidth(1920)
                .setHeight(1080)
                .setAlpha(0.0);
    }

    private void drawPitch() {
        pitchView.draw();
    }
    
    private void drawHud() {
        pitchView = pitchViewProvider.get();
        int[] colors = new int[2];

        for (Player player : gameManager.getPlayers()) {
            int x = player.getIndex() == 0 ? 280 : 1920 - 280;
            int y = player.getIndex() == 0 ? 1080 - 220 : 220;

            colors[player.getIndex()] = player.getColorToken();

            graphicEntityModule
                    .createRectangle()
                    .setWidth(140)
                    .setHeight(140)
                    .setX(x - 70)
                    .setY(y - 70)
                    .setLineWidth(0)
                    .setFillColor(player.getColorToken());

            graphicEntityModule
                    .createRectangle()
                    .setWidth(120)
                    .setHeight(120)
                    .setX(x - 60)
                    .setY(y - 60)
                    .setLineWidth(0)
                    .setFillColor(0xffffff);

            Text text = graphicEntityModule.createText(player.getNicknameToken())
                    .setX(x)
                    .setY(y + 120)
                    .setZIndex(20)
                    .setFontSize(40)
                    .setFillColor(0xffffff)
                    .setAnchor(0.5);

            Text moveText = graphicEntityModule.createText("")
                    .setX(x)
                    .setY(y - 120)
                    .setZIndex(20)
                    .setFontSize(40)
                    .setFillColor(0xffffff)
                    .setAnchor(0.5);

            Text winnerText = graphicEntityModule.createText("")
                    .setX(x)
                    .setY(y + 180)
                    .setZIndex(20)
                    .setFontSize(40)
                    .setFillColor(0xffff00)
                    .setFontWeight(Text.FontWeight.BOLD)
                    .setAnchor(0.5);

            Sprite avatar = graphicEntityModule.createSprite()
                    .setX(x)
                    .setY(y)
                    .setZIndex(20)
                    .setImage(player.getAvatarToken())
                    .setAnchor(0.5)
                    .setBaseHeight(116)
                    .setBaseWidth(116);

            player.hud = graphicEntityModule.createGroup(text, moveText, winnerText, avatar);
            player.moveText = moveText;
            player.winnerText = winnerText;
        }

        pitchView.init(colors[0], colors[1]);
    }

    @Override
    public void gameTurn(int turn) {
        Player player = gameManager.getPlayer(turn % gameManager.getPlayerCount());
        Pitch.PLAYER pitchPlayer = player.getIndex() == 0 ? Pitch.PLAYER.ONE : Pitch.PLAYER.TWO;

        if (lastMove != null) {
            player.sendInputLine(String.valueOf(lastMove.length()));
            player.sendInputLine(lastMove);
        } else {
            player.sendInputLine(String.valueOf(1));
            player.sendInputLine("-");
        }

        player.execute();

        try {
            String move = player.getMove();
            gameManager.addToGameSummary(String.format("Player %s played %s", player.getNicknameToken(), move));

            // moveVerificator
            moveVerificator.verifyMove(pitchView.getPitch(), move);

            lastMove = move;

            gameManager.getPlayers().get(player.getIndex()).moveText.setText(move);
            gameManager.getPlayers().get(player.getIndex()).hud.setAlpha(1.0, Curve.NONE);
            gameManager.getPlayers().get((player.getIndex()+1)%2).hud.setAlpha(0.5, Curve.NONE);
            graphicEntityModule.commitEntityState(0.0, gameManager.getPlayers().get(player.getIndex()).moveText.setText(move), gameManager.getPlayers().get(player.getIndex()).hud, gameManager.getPlayers().get((player.getIndex()+1)%2).hud);
            double lastTime = pitchView.makeMove(move, pitchPlayer);

            if (pitchView.isGameOver()) {
                setWinner(pitchPlayer, lastTime);
                endGame();
            }
        } catch (TimeoutException e) {
            gameManager.addToGameSummary("\n"+GameManager.formatErrorMessage(player.getNicknameToken() + " timeout!"));
            setWinnerFromInvalid(player, "TIMEOUT");
            player.deactivate(player.getNicknameToken() + " timeout!");
            endGame();
        } catch (InvalidMove e) {
            gameManager.addToGameSummary("\n"+GameManager.formatErrorMessage(player.getNicknameToken() + " Invalid move: " + e.getMessage()));
            setWinnerFromInvalid(player, "INVALID MOVE");
            player.deactivate(player.getNicknameToken() + " invalid action!");
            endGame();
        }
    }

    private void setWinnerFromInvalid(Player loserPlayer, String reason) {
        Player winnerPlayer = gameManager.getPlayer((loserPlayer.getIndex()+1) % 2);
        winnerPlayer.setScore(1);
        loserPlayer.setScore(-1);
        loserPlayer.winnerText.setFillColor(0xFF0000, Curve.NONE);
        loserPlayer.winnerText.setText(reason);
        graphicEntityModule.commitEntityState(0.0, loserPlayer.winnerText);
        winnerPlayer.hud.setAlpha(1.0);
        loserPlayer.hud.setAlpha(0.5);
    }

    private void setWinner(Pitch.PLAYER currentPlayer, double lastTime) {
        Pitch.PLAYER winner = pitchView.getWinner(currentPlayer);
        Player winnerPlayer = gameManager.getPlayer(winner == Pitch.PLAYER.ONE ? 0 : 1);
        winnerPlayer.setScore(1);
        Player loserPlayer = gameManager.getPlayer(winner == Pitch.PLAYER.ONE ? 1 : 0);
        loserPlayer.setScore(-1);
        if (pitchView.getPitch().goal(pitchView.getPitch().ball) == Pitch.PLAYER.NONE) { // blocked
            gameManager.addToGameSummary("\n"+GameManager.formatErrorMessage(loserPlayer.getNicknameToken() + " is blocked!"));
            gameManager.addTooltip(loserPlayer, loserPlayer.getNicknameToken() + " is blocked!");
            loserPlayer.winnerText.setFillColor(0xFF0000, Curve.NONE);
            loserPlayer.winnerText.setText("BLOCK");
            graphicEntityModule.commitEntityState(lastTime, loserPlayer.winnerText);
        } else {
            if (pitchView.getPitch().goal(pitchView.getPitch().ball) == currentPlayer) { // own goal
                gameManager.addToGameSummary("\n"+GameManager.formatErrorMessage(loserPlayer.getNicknameToken() + " scored an own goal!"));
                gameManager.addTooltip(loserPlayer, loserPlayer.getNicknameToken() + " scored an own goal!");
                winnerPlayer.winnerText.setText("GOAL");
                graphicEntityModule.commitEntityState(lastTime, winnerPlayer.winnerText);
            } else { // opponent goal
                gameManager.addToGameSummary("\n"+GameManager.formatSuccessMessage(winnerPlayer.getNicknameToken() + " scored a goal!"));
                gameManager.addTooltip(winnerPlayer, winnerPlayer.getNicknameToken() + " scored a goal!");
                winnerPlayer.winnerText.setText("GOAL");
                graphicEntityModule.commitEntityState(lastTime, winnerPlayer.winnerText);
            }
        }
        winnerPlayer.hud.setAlpha(1.0);
        loserPlayer.hud.setAlpha(0.5);
    }

    private void endGame() {
        gameManager.endGame();
    }
}
