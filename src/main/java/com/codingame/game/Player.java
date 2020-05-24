package com.codingame.game;

import com.codingame.gameengine.core.AbstractMultiplayerPlayer;
import com.codingame.gameengine.module.entities.Group;
import com.codingame.gameengine.module.entities.Text;
import com.codingame.paper_soccer.InvalidMove;

import java.util.List;

public class Player extends AbstractMultiplayerPlayer {
    public Group hud;
    public Text moveText;
    public Text winnerText;

    private int expectedOutput = 1;
    
    @Override
    public int getExpectedOutputLines() {
        return expectedOutput;
    }

    public String getMove() throws TimeoutException, InvalidMove {
        List<String> outputs = getOutputs();
        if (outputs == null || outputs.isEmpty()) {
            throw new InvalidMove("Output empty!");
        }
        String output = outputs.get(0);
        if (output == null || output.isEmpty()) {
            throw new InvalidMove("Output empty!");
        }
        return output;
    }
}
