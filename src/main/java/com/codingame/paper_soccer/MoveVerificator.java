package com.codingame.paper_soccer;

public class MoveVerificator {
    private Pitch pitch;

    public MoveVerificator() {
        pitch = new Pitch();
    }

    public void verifyMove(Pitch currentPitch, String move) throws InvalidMove {
        char[] chars = move.toCharArray();
        for (char c : chars) {
            switch(c) {
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                    break;
                default:
                    throw new InvalidMove("Invalid characters in move");
            }
        }
        pitch.setPitch(currentPitch);
        for (int i=0; i < chars.length; i++) {
            char c = chars[i];
            if (pitch.isGameOver() || (i > 0 && !pitch.passNextDone())) {
                throw new InvalidMove("Should finish turn, but there are still more characters in input");
            }
            int n = nextNode(c);
            if (n == -1) {
                throw new InvalidMove("Trying to move out of pitch boundary");
            }
            if (pitch.existsEdge(pitch.ball, n)) {
                throw new InvalidMove("Trying to move on already existing edge");
            }
            pitch.addEdge(pitch.ball, n);
            pitch.ball = n;
        }
        if (!pitch.isGameOver() && pitch.passNextDone()) {
            throw new InvalidMove("Turn ended prematurely, ball must still move");
        }
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




}
