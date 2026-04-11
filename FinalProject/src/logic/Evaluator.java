package logic;

import java.awt.Color;
import java.util.ArrayList;
import piece.Piece;

public class Evaluator {

    private static final int[][] KNIGHT_TABLE = {
        {-50,-40,-30,-30,-30,-30,-40,-50}, {-40,-20,  0,  0,  0,  0,-20,-40},
        {-30,  0, 10, 15, 15, 10,  0,-30}, {-30,  5, 15, 20, 20, 15,  5,-30},
        {-30,  5, 15, 20, 20, 15,  5,-30}, {-30,  0, 10, 15, 15, 10,  0,-30},
        {-40,-20,  0,  5,  5,  0,-20,-40}, {-50,-40,-30,-30,-30,-30,-40,-50}
    };
    private static final int[][] BISHOP_TABLE = {
        {-20,-10,-10,-10,-10,-10,-10,-20}, {-10,  0,  0,  0,  0,  0,  0,-10},
        {-10,  0,  5, 10, 10,  5,  0,-10}, {-10,  5,  5, 10, 10,  5,  5,-10},
        {-10,  0, 10, 10, 10, 10,  0,-10}, {-10, 10, 10, 10, 10, 10, 10,-10},
        {-10,  5,  0,  0,  0,  0,  5,-10}, {-20,-10,-10,-10,-10,-10,-10,-20}
    };
    private static final int[][] ROOK_TABLE = {
        {  0,  0,  0,  0,  0,  0,  0,  0}, { 10, 20, 20, 20, 20, 20, 20, 10},
        { -5,  0,  0,  0,  0,  0,  0, -5}, { -5,  0,  0,  0,  0,  0,  0, -5},
        { -5,  0,  0,  0,  0,  0,  0, -5}, { -5,  0,  0,  0,  0,  0,  0, -5},
        { -5,  0,  0,  0,  0,  0,  0, -5}, {  0,  0,  0,  5,  5,  0,  0,  0}
    };
    private static final int[][] PAWN_TABLE_BLACK = {
        {  0,  0,  0,  0,  0,  0,  0,  0}, {  5, 10, 10,-20,-20, 10, 10,  5},
        {  5, -5,-10,  0,  0,-10, -5,  5}, {  0,  0,  0, 20, 20,  0,  0,  0},
        {  5,  5, 10, 25, 25, 10,  5,  5}, { 10, 10, 20, 30, 30, 20, 10, 10},
        { 50, 50, 50, 50, 50, 50, 50, 50}, {  0,  0,  0,  0,  0,  0,  0,  0}
    };
    private static final int[][] PAWN_TABLE_WHITE = {
        {  0,  0,  0,  0,  0,  0,  0,  0}, { 50, 50, 50, 50, 50, 50, 50, 50},
        { 10, 10, 20, 30, 30, 20, 10, 10}, {  5,  5, 10, 25, 25, 10,  5,  5},
        {  0,  0,  0, 20, 20,  0,  0,  0}, {  5, -5,-10,  0,  0,-10, -5,  5},
        {  5, 10, 10,-20,-20, 10, 10,  5}, {  0,  0,  0,  0,  0,  0,  0,  0}
    };
    private static final int[][] KING_MIDDLE_TABLE = {
        {-30,-40,-40,-50,-50,-40,-40,-30}, {-30,-40,-40,-50,-50,-40,-40,-30},
        {-30,-40,-40,-50,-50,-40,-40,-30}, {-30,-40,-40,-50,-50,-40,-40,-30},
        {-20,-30,-30,-40,-40,-30,-30,-20}, {-10,-20,-20,-20,-20,-20,-20,-10},
        { 20, 20,  0,  0,  0,  0, 20, 20}, { 20, 30, 10,  0,  0, 10, 30, 20}
    };
    private static final int[][] KING_ENDGAME_TABLE = {
        {-50,-40,-30,-20,-20,-30,-40,-50}, {-30,-20,-10,  0,  0,-10,-20,-30},
        {-30,-10, 20, 30, 30, 20,-10,-30}, {-30,-10, 30, 40, 40, 30,-10,-30},
        {-30,-10, 30, 40, 40, 30,-10,-30}, {-30,-10, 20, 30, 30, 20,-10,-30},
        {-30,-30,  0,  0,  0,  0,-30,-30}, {-50,-30,-30,-30,-30,-30,-30,-50}
    };

    public int evaluate(GameState state, boolean isMidgamePhase) {
        int score = 0;
        score += materialScore(state);
        score += positionalScore(state, isMidgamePhase);
        score += pawnStructureScore(state);
        score += calculateThreatScore(state);
        score += passedPawnScore(state);

        if (isMidgamePhase) score += kingSafetyScore(state);
        else {
            score += pawnAdvancementScore(state);
            score += kingOppositionScore(state);
        }
        return score;
    }

    public int getPieceValue(byte type) {
        switch(type) {
            case Piece.PAWN:   return 100;
            case Piece.KNIGHT: return 300;
            case Piece.BISHOP: return 300;
            case Piece.ROOK:   return 500;
            case Piece.QUEEN:  return 900;
            default:           return 0;
        }
    }

    private int materialScore(GameState state) {
        int score = 0;
        score += state.getBlackPawns().size() * 100;
        score += state.getBlackKnights().size() * 300;
        score += state.getBlackBishops().size() * 300;
        score += state.getBlackRooks().size() * 500;
        score += state.getBlackQueens().size() * 900;
        score -= state.getWhitePawns().size() * 100;
        score -= state.getWhiteKnights().size() * 300;
        score -= state.getWhiteBishops().size() * 300;
        score -= state.getWhiteRooks().size() * 500;
        score -= state.getWhiteQueens().size() * 900;
        return score;
    }

    private int positionalScore(GameState state, boolean isMidgame) {
        int score = 0;
        for (Byte pos : state.getBlackKnights()) score += KNIGHT_TABLE[GameState.getRowFromByte(pos)][GameState.getColFromByte(pos)];
        for (Byte pos : state.getWhiteKnights()) score -= KNIGHT_TABLE[7 - GameState.getRowFromByte(pos)][GameState.getColFromByte(pos)];
        for (Byte pos : state.getBlackBishops()) score += BISHOP_TABLE[GameState.getRowFromByte(pos)][GameState.getColFromByte(pos)];
        for (Byte pos : state.getWhiteBishops()) score -= BISHOP_TABLE[7 - GameState.getRowFromByte(pos)][GameState.getColFromByte(pos)];
        for (Byte pos : state.getBlackRooks()) {
            score += ROOK_TABLE[GameState.getRowFromByte(pos)][GameState.getColFromByte(pos)];
            score += rookOpenFileBonus(state, GameState.getColFromByte(pos), Color.black);
        }
        for (Byte pos : state.getWhiteRooks()) {
            score -= ROOK_TABLE[7 - GameState.getRowFromByte(pos)][GameState.getColFromByte(pos)];
            score -= rookOpenFileBonus(state, GameState.getColFromByte(pos), Color.white);
        }
        for (Byte pos : state.getBlackPawns()) score += PAWN_TABLE_BLACK[GameState.getRowFromByte(pos)][GameState.getColFromByte(pos)];
        for (Byte pos : state.getWhitePawns()) score -= PAWN_TABLE_WHITE[GameState.getRowFromByte(pos)][GameState.getColFromByte(pos)];

        int[][] kingTable = isMidgame ? KING_MIDDLE_TABLE : KING_ENDGAME_TABLE;
        for (Byte pos : state.getBlackKings()) score += kingTable[GameState.getRowFromByte(pos)][GameState.getColFromByte(pos)];
        for (Byte pos : state.getWhiteKings()) score -= kingTable[7 - GameState.getRowFromByte(pos)][GameState.getColFromByte(pos)];
        return score;
    }

    private int rookOpenFileBonus(GameState state, int col, Color color) {
        ArrayList<Byte> ownPawns = (color == Color.black) ? state.getBlackPawns() : state.getWhitePawns();
        for (Byte pos : ownPawns) {
            if (GameState.getColFromByte(pos) == col) return 0;
        }
        return 15;
    }

    private int pawnStructureScore(GameState state) {
        int score = 0;
        int[] blackPerCol = new int[state.get_board()[0].length];
        int[] whitePerCol = new int[state.get_board()[0].length];

        for (Byte pos : state.getBlackPawns()) blackPerCol[GameState.getColFromByte(pos)]++;
        for (Byte pos : state.getWhitePawns()) whitePerCol[GameState.getColFromByte(pos)]++;

        for (int col = 0; col < state.get_board()[0].length; col++) {
            if (blackPerCol[col] > 1) score -= 20 * (blackPerCol[col] - 1);
            if (whitePerCol[col] > 1) score += 20 * (whitePerCol[col] - 1);
        }
        return score;
    }

    private int calculateThreatScore(GameState state) {
        int threatScore = 0;
        for (int r = 0; r < state.get_board().length; r++) {
            for (int c = 0; c < state.get_board()[r].length; c++) {
                Piece p = state.get_board()[r][c];
                if (p != null) {
                    int pieceValue = getPieceValue(p.getPieceType());
                    if (p.getColor() == Color.black) { 
                        if (isSquareUnderAttack(state, r, c, Color.black)) {
                            Piece attacker = getLowestValueAttacker(state, r, c, Color.white);
                            int attackerVal = (attacker != null) ? getPieceValue(attacker.getPieceType()) : 0;
                            if (!isSquareDefended(state, r, c, Color.black)) {
                                threatScore -= pieceValue;
                            } else {
                                if (attackerVal > 0 && attackerVal < pieceValue) threatScore -= pieceValue; 
                                else threatScore -= (pieceValue / 10);
                            }
                        }
                    } else {
                        if (isSquareUnderAttack(state, r, c, Color.white)) {
                            Piece attacker = getLowestValueAttacker(state, r, c, Color.black);
                            int attackerVal = (attacker != null) ? getPieceValue(attacker.getPieceType()) : 0;
                            if (!isSquareDefended(state, r, c, Color.white)) {
                                threatScore += pieceValue; 
                            } else {
                                if (attackerVal > 0 && attackerVal < pieceValue) threatScore += pieceValue;
                                else threatScore += (pieceValue / 10); 
                            }
                        }
                    }
                }
            }
        }
        return threatScore;
    }

    public boolean isSquareDefended(GameState state, int row, int col, Color friendlyColor) {
        for (int r = 0; r < state.get_board().length; r++) {
            for (int c = 0; c < state.get_board()[r].length; c++) {
                Piece ally = state.get_board()[r][c];
                if (ally == null || ally.getColor() != friendlyColor) continue;
                if (r == row && c == col) continue;
                if (ally.isValidMovement(r, c, row, col, state.getPiecePlace(row, col))
                        && !ally.isMoveOverAnotherPiece(state, r, c, row, col)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isSquareUnderAttack(GameState state, int row, int col, Color friendlyColor) {
        Color enemyColor = (friendlyColor == Color.black) ? Color.white : Color.black;
        for (int r = 0; r < state.get_board().length; r++) {
            for (int c = 0; c < state.get_board()[r].length; c++) {
                Piece enemy = state.get_board()[r][c];
                if (enemy == null || enemy.getColor() != enemyColor) continue;
                if (enemy.isValidMovement(r, c, row, col, state.getPiecePlace(row, col))
                        && !enemy.isMoveOverAnotherPiece(state, r, c, row, col)) {
                    return true;
                }
            }
        }
        return false;
    }

    public Piece getLowestValueAttacker(GameState state, int targetRow, int targetCol, Color attackerColor) {
        Piece lowestAttacker = null;
        int lowestValue = Integer.MAX_VALUE;
        for (int r = 0; r < state.get_board().length; r++) {
            for (int c = 0; c < state.get_board()[r].length; c++) {
                Piece p = state.get_board()[r][c];
                if (p != null && p.getColor() == attackerColor) {
                    if (p.isValidMovement(r, c, targetRow, targetCol, state.getPiecePlace(targetRow, targetCol)) &&
                        !p.isMoveOverAnotherPiece(state, r, c, targetRow, targetCol)) {
                        int val = getPieceValue(p.getPieceType());
                        if (val < lowestValue) {
                            lowestValue = val;
                            lowestAttacker = p;
                        }
                    }
                }
            }
        }
        return lowestAttacker;
    }

    private int passedPawnScore(GameState state) {
        int score = 0;
        for (Byte pos : state.getBlackPawns()) {
            int r = GameState.getRowFromByte(pos);
            int c = GameState.getColFromByte(pos);
            if (isPassedPawn(state, r, c, Color.black)) score += 200 + (r * 30);
        }
        for (Byte pos : state.getWhitePawns()) {
            int r = GameState.getRowFromByte(pos);
            int c = GameState.getColFromByte(pos);
            if (isPassedPawn(state, r, c, Color.white)) score -= 200 + ((7 - r) * 30);
        }
        return score;
    }

    private boolean isPassedPawn(GameState state, int row, int col, Color color) {
        int direction = (color == Color.black) ? 1 : -1;
        int r = row + direction;
        while (r >= 0 && r < state.get_board().length) {
            Piece p = state.get_board()[r][col];
            if (p != null && p.getPieceType() == Piece.PAWN && p.getColor() != color) return false;
            if (col > 0) {
                p = state.get_board()[r][col - 1];
                if (p != null && p.getPieceType() == Piece.PAWN && p.getColor() != color) return false;
            }
            if (col < state.get_board()[r].length - 1) {
                p = state.get_board()[r][col + 1];
                if (p != null && p.getPieceType() == Piece.PAWN && p.getColor() != color) return false;
            }
            r += direction;
        }
        return true;
    }

    private int kingSafetyScore(GameState state) {
        return kingShelterBonus(state, Color.black) - kingShelterBonus(state, Color.white);
    }

    private int kingShelterBonus(GameState state, Color color) {
        ArrayList<Byte> kings = (color == Color.black) ? state.getBlackKings() : state.getWhiteKings();
        ArrayList<Byte> pawns = (color == Color.black) ? state.getBlackPawns() : state.getWhitePawns();
        if (kings.isEmpty()) return 0;
        int kingRow = GameState.getRowFromByte(kings.get(0));
        int kingCol = GameState.getColFromByte(kings.get(0));
        int shelterRow = (color == Color.black) ? kingRow + 1 : kingRow - 1;
        int bonus = 0;
        for (Byte pos : pawns) {
            int pRow = GameState.getRowFromByte(pos);
            int pCol = GameState.getColFromByte(pos);
            if (pRow == shelterRow && Math.abs(pCol - kingCol) <= 1) bonus += 15;
        }
        return bonus;
    }

    private int pawnAdvancementScore(GameState state) {
        int score = 0;
        for (Byte pos : state.getBlackPawns()) {
            int r = GameState.getRowFromByte(pos);
            score += r * 5;
            if (r == state.get_board().length - 1 || r == 0) score += 800;
        }
        for (Byte pos : state.getWhitePawns()) {
            int r = GameState.getRowFromByte(pos);
            score -= (7 - r) * 5;
            if (r == 0 || r == state.get_board().length - 1) score -= 800;
        }
        return score;
    }

    private int kingOppositionScore(GameState state) {
        if (state.getBlackKings().isEmpty() || state.getWhiteKings().isEmpty()) return 0;
        byte bPos = state.getBlackKings().get(0);
        byte wPos = state.getWhiteKings().get(0);
        int rowDiff = Math.abs(GameState.getRowFromByte(bPos) - GameState.getRowFromByte(wPos));
        int colDiff = Math.abs(GameState.getColFromByte(bPos) - GameState.getColFromByte(wPos));
        if ((rowDiff == 2 && colDiff == 0) || (rowDiff == 0 && colDiff == 2)) return 30;
        if (rowDiff == 2 && colDiff == 2) return 15;
        return 0;
    }
}