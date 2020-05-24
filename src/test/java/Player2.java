import com.codingame.paper_soccer.MoveCalculator;
import com.codingame.paper_soccer.Pitch;

import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class Player2 {

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
