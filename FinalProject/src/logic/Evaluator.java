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
        score += isolatedPawnScore(state);
        score += passedPawnScore(state);
        score += bishopPairBonus(state);
        
        score += calculateThreatScore(state);
        score += mobilityScore(state); 
        score += kingAttackZoneScore(state);
        
        score += pieceDevelopmentScore(state);
        score += centerControlScore(state);
        score += pawnChainScore(state);

        if (isMidgamePhase) {
            score += kingSafetyScore(state); 
            score += kingPawnShieldScore(state); 
        } else {
            score += pawnAdvancementScore(state); 
            score += endgameMopUpScore(state);    
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
        byte[] types = {Piece.PAWN, Piece.ROOK, Piece.KNIGHT, Piece.BISHOP, Piece.QUEEN};
        // בודק איומים על כלים שחורים (שלנו)
        for (byte t : types) {
            for (Byte pos : state.getPiecePositions(t, Color.black)) {
                int r = GameState.getRowFromByte(pos);
                int c = GameState.getColFromByte(pos);
                Piece p = state.get_board()[r][c];
                if (p == null) continue;
                int pieceValue = getPieceValue(t);
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
            }
        }
        // בודק איומים על כלים לבנים (יריב)
        for (byte t : types) {
            for (Byte pos : state.getPiecePositions(t, Color.white)) {
                int r = GameState.getRowFromByte(pos);
                int c = GameState.getColFromByte(pos);
                Piece p = state.get_board()[r][c];
                if (p == null) continue;
                int pieceValue = getPieceValue(t);
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
        return threatScore;
    }

    public boolean isSquareDefended(GameState state, int row, int col, Color friendlyColor) {
        byte[] types = {Piece.PAWN, Piece.ROOK, Piece.KNIGHT, Piece.BISHOP, Piece.QUEEN, Piece.KING};
        Piece target = state.getPiecePlace(row, col);
        for (byte t : types) {
            for (Byte pos : state.getPiecePositions(t, friendlyColor)) {
                int r = GameState.getRowFromByte(pos);
                int c = GameState.getColFromByte(pos);
                if (r == row && c == col) continue;
                Piece ally = state.get_board()[r][c];
                if (ally == null) continue;
                if (ally.isValidMovement(r, c, row, col, target)
                        && !ally.isMoveOverAnotherPiece(state, r, c, row, col))
                    return true;
            }
        }
        return false;
    }

    public boolean isSquareUnderAttack(GameState state, int row, int col, Color friendlyColor) {
        Color enemyColor = (friendlyColor == Color.black) ? Color.white : Color.black;
        byte[] types = {Piece.PAWN, Piece.ROOK, Piece.KNIGHT, Piece.BISHOP, Piece.QUEEN, Piece.KING};
        Piece target = state.getPiecePlace(row, col);
        for (byte t : types) {
            for (Byte pos : state.getPiecePositions(t, enemyColor)) {
                int r = GameState.getRowFromByte(pos);
                int c = GameState.getColFromByte(pos);
                Piece enemy = state.get_board()[r][c];
                if (enemy == null) continue;
                if (enemy.isValidMovement(r, c, row, col, target)
                        && !enemy.isMoveOverAnotherPiece(state, r, c, row, col))
                    return true;
            }
        }
        return false;
    }

    public Piece getLowestValueAttacker(GameState state, int targetRow, int targetCol, Color attackerColor) {
        Piece lowestAttacker = null;
        int lowestValue = Integer.MAX_VALUE;
        byte[] types = {Piece.PAWN, Piece.ROOK, Piece.KNIGHT, Piece.BISHOP, Piece.QUEEN, Piece.KING};
        Piece target = state.getPiecePlace(targetRow, targetCol);
        for (byte t : types) {
            int typeVal = getPieceValue(t);
            if (typeVal >= lowestValue) continue; // early-exit: this type can't beat current best
            for (Byte pos : state.getPiecePositions(t, attackerColor)) {
                int r = GameState.getRowFromByte(pos);
                int c = GameState.getColFromByte(pos);
                Piece p = state.get_board()[r][c];
                if (p == null) continue;
                if (p.isValidMovement(r, c, targetRow, targetCol, target) &&
                    !p.isMoveOverAnotherPiece(state, r, c, targetRow, targetCol)) {
                    lowestAttacker = p;
                    lowestValue = typeVal;
                    break; // found one of this type, no need to check more of same type
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
            score += r * 30; 
            if (r == state.get_board().length - 1 || r == 0) score += 900; // הגעה לקצה=מלכה
        }
        for (Byte pos : state.getWhitePawns()) {
            int r = GameState.getRowFromByte(pos);
            score -= (7 - r) * 30;
            if (r == 0 || r == state.get_board().length - 1) score -= 900;
        }
        return score;
    }

    private int endgameMopUpScore(GameState state) {
        int score = 0;
        int materialAdvantage = materialScore(state);

        if (materialAdvantage > 200 && !state.getWhiteKings().isEmpty() && !state.getBlackKings().isEmpty()) {
            byte wKing = state.getWhiteKings().get(0);
            byte bKing = state.getBlackKings().get(0);
            
            int wRow = GameState.getRowFromByte(wKing);
            int wCol = GameState.getColFromByte(wKing);
            int bRow = GameState.getRowFromByte(bKing);
            int bCol = GameState.getColFromByte(bKing);

            int centerDistX = Math.max(3 - wCol, wCol - 4);
            int centerDistY = Math.max(3 - wRow, wRow - 4);
            int centerDistance = Math.max(0, centerDistX) + Math.max(0, centerDistY);
            score += centerDistance * 40; // 40 נקודות על כל צעד שהמלך הלבן נדחק אחורה!

            int kingDistance = Math.abs(wRow - bRow) + Math.abs(wCol - bCol);
            score += (14 - kingDistance) * 20; // ככל שהמלכים קרובים, הציון עולה
        }
        else if (materialAdvantage < -200 && !state.getBlackKings().isEmpty()) {
            byte bKing = state.getBlackKings().get(0);
            int bRow = GameState.getRowFromByte(bKing);
            int bCol = GameState.getColFromByte(bKing);
            
            int centerDistX = Math.max(3 - bCol, bCol - 4);
            int centerDistY = Math.max(3 - bRow, bRow - 4);
            int centerDistance = Math.max(0, centerDistX) + Math.max(0, centerDistY);
            score -= centerDistance * 40; // קנס ענק על בריחה לפינה, אנחנו נשאף להישאר במרכז!
        }

        return score;
    }
    private int bishopPairBonus(GameState state) {
        int score = 0;
        if (state.getBlackBishops().size() >= 2) score += 50; // הבוט חזק
        if (state.getWhiteBishops().size() >= 2) score -= 50; // הלבן חזק
        return score;
    }

    private int isolatedPawnScore(GameState state) {
        int score = 0;
        // קנס על רגלים מבודדים שלנו, ובונוס על רגלים מבודדים של הלבן
        score -= calculateIsolatedPawns(state, Color.black) * 20; 
        score += calculateIsolatedPawns(state, Color.white) * 20; 
        return score;
    }

    private int calculateIsolatedPawns(GameState state, Color color) {
        int isolatedCount = 0;
        ArrayList<Byte> pawns = (color == Color.black) ? state.getBlackPawns() : state.getWhitePawns();
        
        // יצירת מערך שזוכר באילו עמודות יש לנו רגלים
        boolean[] pawnsOnFile = new boolean[8];
        for (Byte pos : pawns) {
            pawnsOnFile[GameState.getColFromByte(pos)] = true;
        }

        for (Byte pos : pawns) {
            int col = GameState.getColFromByte(pos);
            boolean hasNeighbor = false;
            
            // בודק אם יש רגלי חבר בעמודה מימין או משמאל
            if (col > 0 && pawnsOnFile[col - 1]) hasNeighbor = true;
            if (col < 7 && pawnsOnFile[col + 1]) hasNeighbor = true;

            if (!hasNeighbor) {
                isolatedCount++;
            }
        }
        return isolatedCount;
    }

 
    private int mobilityScore(GameState state) {
        int blackMoves = 0;
        int whiteMoves = 0;

        // עוברים על כל הכלים בלוח וסופרים לכמה משבצות הם יכולים לזוז
        for (int r = 0; r < state.get_board().length; r++) {
            for (int c = 0; c < state.get_board()[r].length; c++) {
                Piece p = state.get_board()[r][c];
                if (p != null) {
                    if (p.getColor() == Color.black) {
                        blackMoves += p._allPossibleMoves.size();
                    } else {
                        whiteMoves += p._allPossibleMoves.size();
                    }
                }
            }
        }
        // כל אפשרות תנועה שווה 2 נקודות לטובת מי ששולט בלוח
        return (blackMoves - whiteMoves) * 2;
    }

  
    private int kingPawnShieldScore(GameState state) {
        int score = 0;
        score += evaluateKingShield(state, Color.black);
        score -= evaluateKingShield(state, Color.white);
        return score;
    }

    private int evaluateKingShield(GameState state, Color color) {
        int bonus = 0;
        ArrayList<Byte> kings = (color == Color.black) ? state.getBlackKings() : state.getWhiteKings();
        if (kings.isEmpty()) return 0;

        byte kingPos = kings.get(0);
        int kingRow = GameState.getRowFromByte(kingPos);
        int kingCol = GameState.getColFromByte(kingPos);

        // בודק אם המלך נמצא באחד הצדדים (לאחר הצרחה או מחסה)
        if (kingCol <= 2 || kingCol >= 5) {
            int pawnRow = (color == Color.black) ? kingRow + 1 : kingRow - 1; // השורה שמול המלך

            // מוודא שאנחנו לא יוצאים מגבולות הלוח
            if (pawnRow >= 0 && pawnRow <= 7) {
                // סורק את 3 המשבצות בדיוק מול המלך
                for (int c = Math.max(0, kingCol - 1); c <= Math.min(7, kingCol + 1); c++) {
                    Piece p = state.get_board()[pawnRow][c];
                    // אם יש שם רגלי מהצבע שלנו, נותן בונוס ענק!
                    if (p != null && p.getPieceType() == Piece.PAWN && p.getColor() == color) {
                        bonus += 15;
                    }
                }
            }
        }
        return bonus;
    }

    private int kingAttackZoneScore(GameState state) {
        int score = 0;
        
        if (!state.getWhiteKings().isEmpty()) {
            byte wKing = state.getWhiteKings().get(0);
            int kRow = GameState.getRowFromByte(wKing);
            int kCol = GameState.getColFromByte(wKing);
            
            // אנחנו סורקים רדיוס של משבצת אחת סביב המלך הלבן (סך הכל 9 משבצות)
            for (int r = Math.max(0, kRow - 1); r <= Math.min(7, kRow + 1); r++) {
                for (int c = Math.max(0, kCol - 1); c <= Math.min(7, kCol + 1); c++) {
                    // אם כלי שחור שלנו יכול להגיע למשבצת שקרובה למלך הלבן
                    if (isSquareUnderAttack(state, r, c, Color.white)) { 
                        score += 25; // בונוס! הבוט ירצה לרכז כוחות סביב המלך
                    }
                }
            }
        }
        return score;
    }
    
 // 1. ציון על שליטה במרכז הלוח (המשבצות החשובות ביותר)
    private int centerControlScore(GameState state) {
        int score = 0;
        for (int r = 3; r <= 4; r++) {
            for (int c = 3; c <= 4; c++) {
                Piece p = state.getPiecePlace(r, c);
                if (p != null) {
                    if (p.getColor() == Color.black) score += 40; // בונוס לנו
                    else score -= 40; // קנס אם היריב שולט במרכז
                }
            }
        }
        return score;
    }

    // 2. פיתוח כלים (עונש על כלים שנשארים בשורה האחורית)
    private int pieceDevelopmentScore(GameState state) {
        int score = 0;
        for (Byte pos : state.getBlackKnights()) {
            if (GameState.getRowFromByte(pos) == 0) score -= 25;
        }
        for (Byte pos : state.getBlackBishops()) {
            if (GameState.getRowFromByte(pos) == 0) score -= 25;
        }
        return score;
    }

    // 3. שרשראות רגלים (בונוס לרגלים שמגנים אחד על השני באלכסון)
    private int pawnChainScore(GameState state) {
        int score = 0;
        for (Byte pos : state.getBlackPawns()) {
            int r = GameState.getRowFromByte(pos);
            int c = GameState.getColFromByte(pos);
            // רגלי שחור מתקדם למטה (r+1). המגינים שלו נמצאים בשורה מעליו (r-1) באלכסונים.
            if (r > 0 && c > 0) {
                Piece p = state.getPiecePlace(r - 1, c - 1);
                if (p != null && p.getPieceType() == Piece.PAWN && p.getColor() == Color.black) score += 15;
            }
            if (r > 0 && c < 7) {
                Piece p = state.getPiecePlace(r - 1, c + 1);
                if (p != null && p.getPieceType() == Piece.PAWN && p.getColor() == Color.black) score += 15;
            }
        }
        return score;
    }
}