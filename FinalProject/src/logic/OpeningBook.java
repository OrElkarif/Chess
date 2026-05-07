package logic;

import java.awt.Color;
import piece.Piece;

public class OpeningBook {//ספר פתיחות קבוע

    private int[][] currentOpening;
    private int openingIndex;
    private boolean openingDone;

    private static final int[][] OPENING_CENTER = {
        {1, 4, 3, 4}, {0, 6, 2, 5}, {0, 5, 3, 2}, {0, 1, 2, 2}, {0, 4, 0, 6}
    };
    private static final int[][] OPENING_QUICK_ATTACK = {
        {1, 4, 3, 4}, {0, 5, 3, 2}, {0, 3, 2, 5}, {0, 1, 2, 2}
    };
    private static final int[][] OPENING_SICILIAN = {
        {1, 2, 3, 2}, {0, 1, 2, 2}, {1, 6, 2, 6}, {0, 5, 1, 6}, {0, 6, 2, 5}, {0, 4, 0, 6}
    };
    private static final int[][] OPENING_FRENCH = {
        {1, 4, 2, 4}, {1, 3, 3, 3}, {0, 6, 2, 5}, {0, 5, 1, 4}
    };
    private static final int[][] OPENING_SCANDINAVIAN = {
        {1, 3, 3, 3}, {0, 1, 2, 2}, {0, 2, 3, 5}, {0, 6, 2, 5}
    };

    public OpeningBook() {
        int random = (int)(Math.random() * 5);
        if (random == 0) currentOpening = OPENING_CENTER;
        else if (random == 1) currentOpening = OPENING_QUICK_ATTACK;
        else if (random == 2) currentOpening = OPENING_SICILIAN;
        else if (random == 3) currentOpening = OPENING_FRENCH;
        else currentOpening = OPENING_SCANDINAVIAN;

        openingIndex = 0;
        openingDone = false;
    }

    public boolean isOpeningDone() { return openingDone; }//אומרת לבוט שהספר נגמר ועכשיו הוא לבד

    public Move getNextMove(GameState state, Evaluator evaluator) {//שולפת מהספר את המהלך המדויק
        if (openingDone) return null;

        for (Byte pos : state.getWhiteQueens()) {//אם המלכה הלבנה זזה, סיים פתיחה
            if (GameState.getRowFromByte(pos) <= 4) {
                openingDone = true; return null;
            }
        }

        if (openingIndex >= currentOpening.length) {
            openingDone = true; return null;
        }

        int[] mv = currentOpening[openingIndex];
        Piece p = state.get_board()[mv[0]][mv[1]];

        if (p == null || p.getColor() != Color.black) {
            openingDone = true; return null;
        }

        Move move = new Move(p, mv[1], mv[0], mv[3], mv[2]);
        if (!move.CanMove(state)) {
            openingDone = true; return null;
        }

        if (evaluator.isSquareUnderAttack(state, mv[2], mv[3], Color.black)) {//
            openingDone = true; return null; 
        }

        openingIndex++;
        return move;
    }
}