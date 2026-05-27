package logic;

import java.awt.Color;
import java.util.ArrayList;
import piece.Piece;

public class Evaluator {//מחלקה שמעריכה כמה הלוח טוב לכל שחקן ונותנת ציון מספרי כדי שהבוט ידע מה לבחור

    private static final int[][] KNIGHT_TABLE = {//טווח לא גדול מידי ויחסי להכל כדי שהבוט לא יחליט את הניקוד רק על סמך המיקום בלבד
        {-50,-40,-30,-30,-30,-30,-40,-50}, 
        {-40,-20,  0,  0,  0,  0,-20,-40},
        {-30,  0, 10, 15, 15, 10,  0,-30}, 
        {-30,  5, 15, 20, 20, 15,  5,-30},
        {-30,  5, 15, 20, 20, 15,  5,-30}, 
        {-30,  0, 10, 15, 15, 10,  0,-30},
        {-40,-20,  0,  5,  5,  0,-20,-40}, 
        {-50,-40,-30,-30,-30,-30,-40,-50}
    };
    private static final int[][] BISHOP_TABLE = {
        {-20,-10,-10,-10,-10,-10,-10,-20},
        {-10,  0,  0,  0,  0,  0,  0,-10},
        {-10,  0,  5, 10, 10,  5,  0,-10}, 
        {-10,  5,  5, 10, 10,  5,  5,-10},
        {-10,  0, 10, 10, 10, 10,  0,-10}, 
        {-10, 10, 10, 10, 10, 10, 10,-10},
        {-10,  5,  0,  0,  0,  0,  5,-10}, 
        {-20,-10,-10,-10,-10,-10,-10,-20}
    };
    private static final int[][] ROOK_TABLE = {
        {  0,  0,  0,  0,  0,  0,  0,  0},
        { 10, 20, 20, 20, 20, 20, 20, 10},
        { -5,  0,  0,  0,  0,  0,  0, -5},
        { -5,  0,  0,  0,  0,  0,  0, -5},
        { -5,  0,  0,  0,  0,  0,  0, -5}, 
        { -5,  0,  0,  0,  0,  0,  0, -5},
        { -5,  0,  0,  0,  0,  0,  0, -5},
        {  0,  0,  0,  5,  5,  0,  0,  0}
    };
    private static final int[][] PAWN_TABLE_BLACK = {
        {  0,  0,  0,  0,  0,  0,  0,  0}, 
        {  5, 10, 10,-20,-20, 10, 10,  5},
        {  5, -5,-10,  0,  0,-10, -5,  5}, 
        {  0,  0,  0, 20, 20,  0,  0,  0},
        {  5,  5, 10, 25, 25, 10,  5,  5}, 
        { 10, 10, 20, 30, 30, 20, 10, 10},
        { 50, 50, 50, 50, 50, 50, 50, 50},
        {  0,  0,  0,  0,  0,  0,  0,  0}
    };
    private static final int[][] PAWN_TABLE_WHITE = {
        {  0,  0,  0,  0,  0,  0,  0,  0}, 
        { 50, 50, 50, 50, 50, 50, 50, 50},
        { 10, 10, 20, 30, 30, 20, 10, 10}, 
        {  5,  5, 10, 25, 25, 10,  5,  5},
        {  0,  0,  0, 20, 20,  0,  0,  0}, 
        {  5, -5,-10,  0,  0,-10, -5,  5},
        {  5, 10, 10,-20,-20, 10, 10,  5}, 
        {  0,  0,  0,  0,  0,  0,  0,  0}
    };
    private static final int[][] KING_MIDDLE_TABLE = {
        {-30,-40,-40,-50,-50,-40,-40,-30},
        {-30,-40,-40,-50,-50,-40,-40,-30},
        {-30,-40,-40,-50,-50,-40,-40,-30}, 
        {-30,-40,-40,-50,-50,-40,-40,-30},
        {-20,-30,-30,-40,-40,-30,-30,-20}, 
        {-10,-20,-20,-20,-20,-20,-20,-10},
        { 20, 20,  0,  0,  0,  0, 20, 20}, 
        { 20, 30, 10,  0,  0, 10, 30, 20}
    };
    private static final int[][] KING_ENDGAME_TABLE = {
        {-50,-40,-30,-20,-20,-30,-40,-50}, 
        {-30,-20,-10,  0,  0,-10,-20,-30},
        {-30,-10, 20, 30, 30, 20,-10,-30}, 
        {-30,-10, 30, 40, 40, 30,-10,-30},
        {-30,-10, 30, 40, 40, 30,-10,-30},
        {-30,-10, 20, 30, 30, 20,-10,-30},
        {-30,-30,  0,  0,  0,  0,-30,-30}, 
        {-50,-30,-30,-30,-30,-30,-30,-50}
    };

    public int evaluate(GameState state, boolean isMidgamePhase) {//הפונקציה המרכזית שמשקללת את כל הציונים של חומר מיקום ואסטרטגיה יחד ומחזירה ציון סופי
        int score = 0;
        
        score += materialScore(state);
        score += positionalScore(state, isMidgamePhase);
        
        score += pawnStructureScore(state);
        score += passedPawnScore(state);
        
        score += calculateThreatScore(state);
        score += mobilityScore(state); 
        
        score += pieceDevelopmentScore(state);
        score += centerControlScore(state);
        score += pawnChainScore(state);

        if (isMidgamePhase) {
            score += kingSafetyScore(state); 
        } else {
            score += pawnAdvancementScore(state); 
        }
        
        return score;
    }

    public int getPieceValue(byte type) {//מחזירה כמה נקודות שווה כל כלי בשחמט כדי לדעת מה יותר שווה לאכול
        switch(type) {
            case Piece.PAWN:   return 100;
            case Piece.KNIGHT: return 300;
            case Piece.BISHOP: return 300;
            case Piece.ROOK:   return 500;
            case Piece.QUEEN:  return 900;
            default:           return 0;
        }
    }

    private int materialScore(GameState state) {//מחשבת מי מוביל בכלים לפי כמות
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

    private int positionalScore(GameState state, boolean isMidgame) {//יתרון במיקומים של הכלים כמו שליטה במרכז לפי טבלאות מיקום
        int score = 0;
        for (Byte pos : state.getBlackKnights()) score += KNIGHT_TABLE[GameState.getRowFromByte(pos)][GameState.getColFromByte(pos)];
        for (Byte pos : state.getWhiteKnights()) score -= KNIGHT_TABLE[7 - GameState.getRowFromByte(pos)][GameState.getColFromByte(pos)];
        
        for (Byte pos : state.getBlackBishops()) score += BISHOP_TABLE[GameState.getRowFromByte(pos)][GameState.getColFromByte(pos)];
        for (Byte pos : state.getWhiteBishops()) score -= BISHOP_TABLE[7 - GameState.getRowFromByte(pos)][GameState.getColFromByte(pos)];
        
        for (Byte pos : state.getBlackRooks()) score += ROOK_TABLE[GameState.getRowFromByte(pos)][GameState.getColFromByte(pos)];
        for (Byte pos : state.getWhiteRooks()) score -= ROOK_TABLE[7 - GameState.getRowFromByte(pos)][GameState.getColFromByte(pos)];
        
        for (Byte pos : state.getBlackPawns()) score += PAWN_TABLE_BLACK[GameState.getRowFromByte(pos)][GameState.getColFromByte(pos)];
        for (Byte pos : state.getWhitePawns()) score -= PAWN_TABLE_WHITE[GameState.getRowFromByte(pos)][GameState.getColFromByte(pos)];

        int[][] kingTable = isMidgame ? KING_MIDDLE_TABLE : KING_ENDGAME_TABLE;
        for (Byte pos : state.getBlackKings()) score += kingTable[GameState.getRowFromByte(pos)][GameState.getColFromByte(pos)];
        for (Byte pos : state.getWhiteKings()) score -= kingTable[7 - GameState.getRowFromByte(pos)][GameState.getColFromByte(pos)];
        
        return score;
    }

    private int pawnStructureScore(GameState state) {//בודקת כמה טובים הרגלים אם הם מסודרים טוב
        int score = 0;
        int[] blackPerCol = new int[8];
        int[] whitePerCol = new int[8];

        for (Byte pos : state.getBlackPawns()) blackPerCol[GameState.getColFromByte(pos)]++;
        for (Byte pos : state.getWhitePawns()) whitePerCol[GameState.getColFromByte(pos)]++;

        for (int col = 0; col < 8; col++) {
            if (blackPerCol[col] > 1) score -= 20 * (blackPerCol[col] - 1);//אם יש יותר מרגלי אחד בעמודה זה לא טוב
            if (whitePerCol[col] > 1) score += 20 * (whitePerCol[col] - 1); 
        }
        return score;
    }

    private int calculateThreatScore(GameState state) {
        int threatScore = 0;
        byte[] types = {Piece.PAWN, Piece.ROOK, Piece.KNIGHT, Piece.BISHOP, Piece.QUEEN};
        Color[] colors = {Color.black, Color.white}; 

        for (Color color : colors) {
            // הגדרת הצבע הנגדי והמכפיל לפי התור הנוכחי בלולאה
            Color enemyColor = (color == Color.black) ? Color.white : Color.black;
            int multiplier   = (color == Color.black) ? -1 : 1; 

            for (byte t : types) {
                for (Byte pos : state.getPiecePositions(t, color)) {
                    int r = GameState.getRowFromByte(pos);
                    int c = GameState.getColFromByte(pos);
                    Piece p = state.get_board()[r][c];
                    if (p == null) continue;
                    
                    int pieceValue = getPieceValue(t);
                    
                    if (isSquareUnderAttack(state, r, c, color)) {
                        Piece attacker = getLowestValueAttacker(state, r, c, enemyColor);
                        int attackerVal = (attacker != null) ? getPieceValue(attacker.getPieceType()) : 0;
                        
                        int penalty = 0; // נחשב את הקנס כערך מוחלט, ורק בסוף נכפיל בפלוס או מינוס
                        if (!isSquareDefended(state, r, c, color)) {
                            penalty = pieceValue;
                        } else {
                            if (attackerVal > 0 && attackerVal < pieceValue) {
                                penalty = pieceValue;
                            } else {
                                penalty = (pieceValue / 10);
                            }
                        }
                        
                        threatScore += (multiplier * penalty); 
                    }
                }
            }
        }
        return threatScore;
    }

    public boolean isSquareDefended(GameState state, int row, int col, Color friendlyColor) {//מסייעת לבוט לדעת אם משבצת מסוימת מוגנת על ידי חבר לקבוצה
        byte[] types = {Piece.PAWN, Piece.ROOK, Piece.KNIGHT, Piece.BISHOP, Piece.QUEEN, Piece.KING};
        Piece target = state.getPiecePlace(row, col);
        for (byte t : types) {
            for (Byte pos : state.getPiecePositions(t, friendlyColor)) {//עובר על הכלים שלנו
                int r = GameState.getRowFromByte(pos);
                int c = GameState.getColFromByte(pos);
                if (r == row && c == col) continue;
                Piece ally = state.get_board()[r][c];
                if (ally == null) continue;
                if (ally.isValidMovement(r, c, row, col, target)//עוקף את החוקים של move שאומרים שאי אפשר לאכול כלי שלנו
                        && !ally.isMoveOverAnotherPiece(state, r, c, row, col))
                    return true;
            }
        }
        return false;
    }

    public boolean isSquareUnderAttack(GameState state, int row, int col, Color friendlyColor) {//בודקת אם משבצת מסוימת בסכנה ויש כלי יריב שמאיים עליה
        Color enemyColor = (friendlyColor == Color.black) ? Color.white : Color.black;//עובר על הכלים של היריב
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

    public Piece getLowestValueAttacker(GameState state, int targetRow, int targetCol, Color attackerColor) {//מוצאת את הכלי הכי זול שמאיים על המשבצת
        Piece lowestAttacker = null;
        int lowestValue = Integer.MAX_VALUE;
        byte[] types = {Piece.PAWN, Piece.ROOK, Piece.KNIGHT, Piece.BISHOP, Piece.QUEEN, Piece.KING};
        Piece target = state.getPiecePlace(targetRow, targetCol);
        
        for (byte t : types) {
            int typeVal = getPieceValue(t);
            if (typeVal >= lowestValue) continue; 
            for (Byte pos : state.getPiecePositions(t, attackerColor)) {
                int r = GameState.getRowFromByte(pos);
                int c = GameState.getColFromByte(pos);
                Piece p = state.get_board()[r][c];
                if (p == null) continue;
                if (p.isValidMovement(r, c, targetRow, targetCol, target) &&//אם מצאתי את האיום הכי זול צא מהפונקציה ושלח אותו זה מייעל את הפונקציה
                    !p.isMoveOverAnotherPiece(state, r, c, targetRow, targetCol)) {
                    lowestAttacker = p;
                    lowestValue = typeVal;
                    break; 
                }
            }
        }
        return lowestAttacker;
    }

    private int passedPawnScore(GameState state) {
        int score = 0;
        Color[] colors = {Color.black, Color.white};

        for (Color color : colors) {
            java.util.ArrayList<Byte> pawns = (color == Color.black) ? state.getBlackPawns() : state.getWhitePawns();
            int sign = (color == Color.black) ? 1 : -1; // שחור מוסיף לציון, לבן מוריד מהציון

            for (Byte pos : pawns) {
                int r = GameState.getRowFromByte(pos);
                int c = GameState.getColFromByte(pos);

                if (isPassedPawn(state, r, c, color)) {
                    // חישוב ההתקדמות: לשחור זה פשוט השורה (r), ללבן זה 7 פחות השורה
                    int rowsAdvanced = (color == Color.black) ? r : (7 - r);
                    
                    score += sign * (200 + (rowsAdvanced * 30));
                }
            }
        }
        return score;
    }

    private boolean isPassedPawn(GameState state, int row, int col, Color color) {//פונקציית עזר שמזהה ספציפית אם רגלי יכול לרוץ
        int dir = (color == Color.black) ? 1 : -1;
        for(int r = row + dir; r >= 0 && r < 8; r += dir) {
            Piece p = state.get_board()[r][col];
            if (p != null && p.getPieceType() == Piece.PAWN && p.getColor() != color) return false;//בודק שאין רגלי מלפניו
            if (col > 0) {
                p = state.get_board()[r][col-1];
                if (p != null && p.getPieceType() == Piece.PAWN && p.getColor() != color) return false;//בודק שאין רגלי מצד שמאל שלו
            }
            if (col < 7) {
                p = state.get_board()[r][col+1];
                if (p != null && p.getPieceType() == Piece.PAWN && p.getColor() != color) return false;//בודק שאין רגלי מצד ימין שלו
            }
        }
        return true;
    }

    private int mobilityScore(GameState state) {//נותנת בונוס לשחקן שיש לו המון אפשרויות לזוז לעומת שחקן שתקוע
    	    int score = 0;
    	    byte[] types = {Piece.PAWN, Piece.ROOK, Piece.KNIGHT, Piece.BISHOP, Piece.QUEEN, Piece.KING};
    	    Color[] colors = {Color.black, Color.white}; // מערך הצבעים
    	    
    	    for (byte t : types) {
    	        for (Color color : colors) {
    	            int sign = (color == Color.black) ? 1 : -1; // שחור זה פלוס, לבן זה מינוס
    	            
    	            for (Byte pos : state.getPiecePositions(t, color)) {
    	                int r = GameState.getRowFromByte(pos);
    	                int c = GameState.getColFromByte(pos);
    	                Piece p = state.get_board()[r][c];
    	                
    	                if (p != null && p._allPossibleMoves != null) {
    	                    score += sign * (p._allPossibleMoves.size() * 2);//על כל משבצת פנויה שכלי יכול לזוז אליה, אני מוסיף 2 נקודות
    	                }
    	            }
    	        }
    	    }
    	    return score;
    	}

    private int kingSafetyScore(GameState state) {//עריכה כמה המלך בטוח
        int score = 0;
        score += evaluateKingShield(state, Color.black);
        score -= evaluateKingShield(state, Color.white);
        return score;
    }

    private int evaluateKingShield(GameState state, Color color) {//בודקת ממש שורות וכלים מסביב למלך כדי לראות אם יש עליו מגן טוב
        ArrayList<Byte> kings = (color == Color.black) ? state.getBlackKings() : state.getWhiteKings();
        if (kings.isEmpty()) return 0;

        int kRow = GameState.getRowFromByte(kings.get(0));
        int kCol = GameState.getColFromByte(kings.get(0));

        if (kCol <= 2 || kCol >= 5) {//האם המלך בפינה
            int pRow = (color == Color.black) ? kRow + 1 : kRow - 1;//לבדוק אם השורה של הרגלים היא שורה קדימה או אחורה לפי הצבעים
            if (pRow >= 0 && pRow <= 7) {
                int bonus = 0;
                for (int c = Math.max(0, kCol - 1); c <= Math.min(7, kCol + 1); c++) {//אם המלך בעמודה 0, Math.max(0, -1) יחזיר 0, וכך המחשב לא ינסה לחפש מחוץ למערך
                	//כאן אני בודק רק את 3 המשבצות שמעל המלך
                    Piece p = state.get_board()[pRow][c];
                    if (p != null && p.getPieceType() == Piece.PAWN && p.getColor() == color) {
                        bonus += 15;
                    }
                }
                return bonus;
            }
        }
        return 0;
    }

    private int pawnAdvancementScore(GameState state) {
        int score = 0;
        for (Byte pos : state.getBlackPawns()) {
            score += GameState.getRowFromByte(pos) * 10;
        }
        for (Byte pos : state.getWhitePawns()) {
            score -= (7 - GameState.getRowFromByte(pos)) * 10;
        }
        return score;
    }

    private int pieceDevelopmentScore(GameState state) {//מעודדת את הבוט להוציא את הכלים הגדולים שלו
        int score = 0;
        for (Byte pos : state.getBlackKnights()) if (GameState.getRowFromByte(pos) == 0) score -= 25;
        for (Byte pos : state.getBlackBishops()) if (GameState.getRowFromByte(pos) == 0) score -= 25;
        
        for (Byte pos : state.getWhiteKnights()) if (GameState.getRowFromByte(pos) == 7) score += 25;
        for (Byte pos : state.getWhiteBishops()) if (GameState.getRowFromByte(pos) == 7) score += 25;
        return score;
    }

    private int centerControlScore(GameState state) {//בודקת איזה שחקן דומיננטי בארבע המשבצות שבדיוק במרכז הלוח
        int score = 0;
        int[][] center = {{3,3}, {3,4}, {4,3}, {4,4}};
        for (int[] sq : center) {
            Piece p = state.get_board()[sq[0]][sq[1]];
            if (p != null) {
                if (p.getColor() == Color.black) score += 40;
                else score -= 40;
            }
        }
        return score;
    }

    private int pawnChainScore(GameState state) {
        int score = 0;
        Color[] colors = {Color.black, Color.white};
        
        for (Color color : colors) {
            // קביעת כיוונים וסימנים לפי הצבע
            int sign = (color == Color.black) ? 1 : -1; 
            int backRowDir = (color == Color.black) ? -1 : 1; //המגנים של שחור נמצאים שורה מעליו  (-1)
            
            java.util.ArrayList<Byte> pawns = (color == Color.black) ? state.getBlackPawns() : state.getWhitePawns();
            
            for (Byte pos : pawns) {
                int r = GameState.getRowFromByte(pos);
                int c = GameState.getColFromByte(pos);
                int defRow = r + backRowDir; // חישוב השורה שבה אמורים לעמוד המגנים
                
                // הגנה מחריגה מגבולות השורות של הלוח
                if (defRow >= 0 && defRow <= 7) {
                    // בדיקת מגן באלכסון השמאלי (בתנאי שאנחנו לא בקצה השמאלי של הלוח)
                    if (c > 0) {
                        Piece pLeft = state.get_board()[defRow][c - 1];
                        if (pLeft != null && pLeft.getPieceType() == Piece.PAWN && pLeft.getColor() == color) {
                            score += sign * 15;
                        }
                    }
                    // בדיקת מגן באלכסון הימני (בתנאי שאנחנו לא בקצה הימני של הלוח)
                    if (c < 7) {
                        Piece pRight = state.get_board()[defRow][c + 1];
                        if (pRight != null && pRight.getPieceType() == Piece.PAWN && pRight.getColor() == color) {
                            score += sign * 15;
                        }
                    }
                }
            }
        }
        return score;
    }
}