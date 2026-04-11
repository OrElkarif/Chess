package logic;

import java.awt.Color;
import java.util.ArrayList;
import piece.Piece;

public class AIPlayer {

    public enum GamePhase { OPENING, MIDDLE, ENDGAME }

    // =========================================================
    // 1. תשתית העץ - צומת בסיסי לפי מבנה רשימה מקושרת גנרית
    //    left = "כן" / ענף פעולה,   right = "לא" / fallback
    // =========================================================

    public abstract class DecisionNode {
        public DecisionNode left;
        public DecisionNode right;

        public DecisionNode(DecisionNode left, DecisionNode right) {
            this.left = left;
            this.right = right;
        }

        public abstract boolean isActionNode();
        public boolean checkCondition(GameState state) { return false; }
        public Move calculateMove(GameState state) { return null; }
    }

    // צומת שאלה – בודק תנאי ומחזיר true/false
    public abstract class QuestionNode extends DecisionNode {
        public QuestionNode(DecisionNode yesNode, DecisionNode noNode) {
            super(yesNode, noNode);
        }
        @Override
        public boolean isActionNode() { return false; }
    }

    // צומת פעולה – מחשב ומחזיר מהלך; right הוא fallback אם לא נמצא מהלך
    public abstract class ActionNode extends DecisionNode {
        public ActionNode(DecisionNode fallbackNode) {
            super(null, fallbackNode);
        }
        @Override
        public boolean isActionNode() { return true; }
    }

    // =========================================================
    // 2. מחלקות ספציפיות – כל אחת מממשת צומת אחד בעץ
    // =========================================================

    // שאלה: האם אנחנו בשלב הפתיחה?
    class IsOpeningNode extends QuestionNode {
        public IsOpeningNode(DecisionNode yes, DecisionNode no) { super(yes, no); }
        @Override public boolean checkCondition(GameState state) { return isOpeningPhase(state); }
    }

    // שאלה: האם אנחנו באמצע המשחק (ולא סיום)?
    class IsMidgameNode extends QuestionNode {
        public IsMidgameNode(DecisionNode yes, DecisionNode no) { super(yes, no); }
        @Override public boolean checkCondition(GameState state) { return isMidgamePhase(state); }
    }

    // פעולת פתיחה: ביצוע מהלך מסדרת הפתיחה שנבחרה
    class OpeningActionNode extends ActionNode {
        public OpeningActionNode(DecisionNode fallback) { super(fallback); }
        @Override public Move calculateMove(GameState state) { return actionOpening(state); }
    }

    // פעולת הגנה: טיפול בשח ובכלים מותקפים
    class DefenseActionNode extends ActionNode {
        public DefenseActionNode(DecisionNode fallback) { super(fallback); }
        @Override public Move calculateMove(GameState state) { return actionDefense(state); }
    }

    // פעולת תקיפה/חילוף: אכילת כלי רווחית
    class CaptureActionNode extends ActionNode {
        public CaptureActionNode(DecisionNode fallback) { super(fallback); }
        @Override public Move calculateMove(GameState state) { return actionCapture(state); }
    }

    // פעולה פוזיציונית: שיפור מיקום כלים (הצרחה, סוסים, צריחים)
    class PositionalActionNode extends ActionNode {
        public PositionalActionNode(DecisionNode fallback) { super(fallback); }
        @Override public Move calculateMove(GameState state) { return actionPositional(state); }
    }

    // פעולת ברירת מחדל: הכי טוב שנמצא מכל המהלכים
    class DefaultActionNode extends ActionNode {
        public DefaultActionNode(DecisionNode fallback) { super(fallback); }
        @Override public Move calculateMove(GameState state) { return actionDefaultBestMove(state); }
    }

    // פעולת סיום משחק: הכתרת רגלי / דחיקת מלך / מרכוז
    class EndgameActionNode extends ActionNode {
        public EndgameActionNode(DecisionNode fallback) { super(fallback); }
        @Override public Move calculateMove(GameState state) { return actionEndgame(state); }
    }

    // =========================================================
    // 3. משתני המחלקה וטבלאות ערכים פוזיציוניים
    // =========================================================

    private DecisionNode rootNode;
    private int[][] currentOpening;
    private int openingIndex;
    private boolean openingDone;

    // ערך בונוס/קנס לפי מיקום הכלי על הלוח (מנקודת מבט שחור)
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

    // טבלת מלך – אמצע המשחק: המלך צריך להיות מוגן בצד
    private static final int[][] KING_MIDDLE_TABLE = {
        {-30,-40,-40,-50,-50,-40,-40,-30}, {-30,-40,-40,-50,-50,-40,-40,-30},
        {-30,-40,-40,-50,-50,-40,-40,-30}, {-30,-40,-40,-50,-50,-40,-40,-30},
        {-20,-30,-30,-40,-40,-30,-30,-20}, {-10,-20,-20,-20,-20,-20,-20,-10},
        { 20, 20,  0,  0,  0,  0, 20, 20}, { 20, 30, 10,  0,  0, 10, 30, 20}
    };

    // טבלת מלך – סיום: המלך צריך להיות פעיל במרכז
    private static final int[][] KING_ENDGAME_TABLE = {
        {-50,-40,-30,-20,-20,-30,-40,-50}, {-30,-20,-10,  0,  0,-10,-20,-30},
        {-30,-10, 20, 30, 30, 20,-10,-30}, {-30,-10, 30, 40, 40, 30,-10,-30},
        {-30,-10, 30, 40, 40, 30,-10,-30}, {-30,-10, 20, 30, 30, 20,-10,-30},
        {-30,-30,  0,  0,  0,  0,-30,-30}, {-50,-30,-30,-30,-30,-30,-30,-50}
    };

    // =========================================================
    // פתיחות לשחקן השחור – כל מהלך הוא {fromRow, fromCol, toRow, toCol}
    // =========================================================

    // פתיחה קלאסית: פיתוח רגלי מרכז, סוסים ורצים
    private static final int[][] OPENING_CENTER = {
        {1, 4, 3, 4}, // e7-e5 רגלי מרכז
        {0, 6, 2, 5}, // Ng8-f6 סוס ימני למרכז
        {0, 5, 3, 2}, // Bf8-c5 רץ ימני
        {0, 1, 2, 2}, // Nb8-c6 סוס שמאלי
        {0, 4, 0, 6}  // O-O הצרחה ימנית
    };

    // פתיחת תקיפה מהירה: מלכה ורץ קדמה
    private static final int[][] OPENING_QUICK_ATTACK = {
        {1, 4, 3, 4}, // e7-e5
        {0, 5, 3, 2}, // Bf8-c5 רץ ימני
        {0, 3, 2, 5}, // Qd8-f6 מלכה לתקיפה
        {0, 1, 2, 2}  // Nb8-c6 תמיכה
    };

    // פתיחה סיציליאנית: c5, דרגון
    private static final int[][] OPENING_SICILIAN = {
        {1, 2, 3, 2}, // c7-c5
        {0, 1, 2, 2}, // Nb8-c6
        {1, 6, 2, 6}, // g7-g6 הכנה לפיאנקטו
        {0, 5, 1, 6}, // Bf8-g7 רץ ארוך
        {0, 6, 2, 5}, // Ng8-f6
        {0, 4, 0, 6}  // O-O הצרחה
    };
    
 // פתיחה צרפתית: e6 ואז d5
    private static final int[][] OPENING_FRENCH = {
        {1, 4, 2, 4}, // e7-e6
        {1, 3, 3, 3}, // d7-d5
        {0, 6, 2, 5}, // Ng8-f6
        {0, 5, 1, 4}  // Bf8-e7
    };

    // פתיחה סקנדינבית: d5 מוקדם (אגרסיבי)
    private static final int[][] OPENING_SCANDINAVIAN = {
        {1, 3, 3, 3}, // d7-d5
        {0, 1, 2, 2}, // Nb8-c6
        {0, 2, 3, 5}, // Bc8-f5
        {0, 6, 2, 5}  // Ng8-f6
    };

    // =========================================================
    // 4. בנאי המחלקה – בחירת פתיחה אקראית ובניית עץ ההחלטות
    // =========================================================

    public AIPlayer() {
        int random = (int)(Math.random() * 5); // שונה מ-3 ל-5

        if (random == 0) {
            currentOpening = OPENING_CENTER;
        } else if (random == 1) {
            currentOpening = OPENING_QUICK_ATTACK;
        } else if (random == 2) {
            currentOpening = OPENING_SICILIAN;
        } else if (random == 3) {
            currentOpening = OPENING_FRENCH;
        } else {
            currentOpening = OPENING_SCANDINAVIAN;
        }

        openingIndex = 0;
        openingDone = false;

        buildStrategyTree();
    }
    // =========================================================
    // בניית העץ – מקריאה לצמתים בסדר מלמטה למעלה
    // =========================================================
    private void buildStrategyTree() {
        DecisionNode endgameAction  = new EndgameActionNode(null);
        DecisionNode defaultAction  = new DefaultActionNode(null);

        DecisionNode positionAction = new PositionalActionNode(defaultAction);
        DecisionNode captureAction  = new CaptureActionNode(positionAction);
        DecisionNode defenseAction  = new DefenseActionNode(captureAction);

        DecisionNode isMidgameNode  = new IsMidgameNode(defenseAction, endgameAction);

        DecisionNode openingAction  = new OpeningActionNode(isMidgameNode);
        
        // --- התיקון הקריטי: הגנה קודמת לפתיחה ---
        // אם יש סכנה (שח או כלי מותקף) הבוט יגן. רק אם אין סכנה, הוא ימשיך בפתיחה.
        DecisionNode openingDefense = new DefenseActionNode(openingAction); 

        // צומת השורש מפנה כעת קודם כל להגנה של שלב הפתיחה
        DecisionNode isOpeningNode  = new IsOpeningNode(openingDefense, isMidgameNode);

        this.rootNode = isOpeningNode;
    }

    // =========================================================
    // 5. מעבר על העץ בלולאה – ללא רקורסיה
    // =========================================================

    public Move chooseBestMove(GameState state) {
        // חישוב כל המהלכים האפשריים של שחור לפני שעוברים על העץ
        state.getAllPossibleMoves(Color.black);
        DecisionNode current = rootNode;

        while (current != null) {
            if (current.isActionNode()) {
                Move move = current.calculateMove(state);
                if (move != null) {
                    return move; // מצאנו מהלך – יוצאים
                } else {
                    current = current.right; // fallback לצומת הבא
                }
            } else {
                // צומת שאלה – הולכים ל-left (כן) או right (לא)
                current = current.checkCondition(state) ? current.left : current.right;
            }
        }
        return null;
    }

    // =========================================================
    // 6. זיהוי שלב המשחק
    // =========================================================

    private boolean isOpeningPhase(GameState state) {
        return !openingDone; // הפתיחה נגמרת כשסיימנו את המערך או נחסמנו
    }

    // אמצע משחק = עדיין יש לפחות 3 כלים כבדים על הלוח
    private boolean isMidgamePhase(GameState state) {
        int heavyPieces = state.getBlackQueens().size() + state.getWhiteQueens().size()
                        + state.getBlackRooks().size()  + state.getWhiteRooks().size();
        return heavyPieces >= 3;
    }

    // =========================================================
    // 7. פונקציות האסטרטגיה – לוגיקה מלאה לפי עץ ההחלטות
    // =========================================================

    // ---- שלב פתיחה ----
    private Move actionOpening(GameState state) {
        // אם המלכה הלבנה כבר יצאה להתקפה – נצא מהפתיחה ונתגונן
        for (Byte pos : state.getWhiteQueens()) {
            if (GameState.getRowFromByte(pos) <= 4) {
                openingDone = true;
                return null;
            }
        }

        if (openingIndex >= currentOpening.length) {
            openingDone = true;
            return null;
        }

        int[] mv = currentOpening[openingIndex];
        int fromRow = mv[0], fromCol = mv[1], toRow = mv[2], toCol = mv[3];
        Piece p = state.get_board()[fromRow][fromCol];

        // אם הכלי נאכל או נע – יוצאים מהפתיחה
        if (p == null || p.getColor() != Color.black) {
            openingDone = true;
            return null;
        }

        Move move = new Move(p, fromCol, fromRow, toCol, toRow);

        if (!move.CanMove(state)) {
            openingDone = true;
            return null;
        }


        // --- התיקון: בדיקה שהבוט לא מקריב כלי בחינם בפתיחה ---
        if (isSquareUnderAttack(state, toRow, toCol, Color.black)) {
            openingDone = true; // הלבן הפתיע אותנו! מבטלים את הפתיחה הקבועה
            return null; // מעביר את הבוט ללוגיקה של אמצע משחק
        }

        openingIndex++;
        return move;
    }

    // ---- שלב אמצע משחק – ענף א': הגנה ----
    private Move actionDefense(GameState state) {
        // מסלול שח: המלך בסכנה
        if (state.isKingInCheck(Color.black)) {
            ArrayList<Move> allMoves = getAllLegalMoves(state);

            // נסה לברוח עם המלך עצמו
            Move kingEscape = findKingEscapeMove(state, allMoves);
            if (kingEscape != null) return kingEscape;

            // אם אי אפשר לברוח – נסה לחסום
            Move blockMove = findBlockCheckMove(state, allMoves);
            if (blockMove != null) return blockMove;

            // אם אי אפשר לחסום – אכול את התוקף
            Move captureAttacker = findCaptureAttackerMove(state, allMoves);
            if (captureAttacker != null) return captureAttacker;

            // שח מט – כל מהלך חוקי
            return getBestMoveFromList(state, allMoves);
        }

        // מסלול הצלת כלי: כלי שחור חשוב מותקף?
        Move saveMove = findSavePieceMove(state);
        if (saveMove != null) return saveMove;

        // אין סכנה – ממשיך ל-fallback (חילוף)
        return null;
    }

    // מציאת מהלך בריחה של המלך עצמו
    private Move findKingEscapeMove(GameState state, ArrayList<Move> allMoves) {
        for (Move move : allMoves) {
            if (move.get_piece().getPieceType() == Piece.KING) {
                return move; // כל מהלך של המלך בבריחה
            }
        }
        return null;
    }

    // מציאת מהלך שחוסם את השח (כלי בין התוקף למלך)
    private Move findBlockCheckMove(GameState state, ArrayList<Move> allMoves) {
        // נסמלץ כל מהלך ונבדוק אם אחריו המלך לא בשח
        for (Move move : allMoves) {
            if (move.get_piece().getPieceType() == Piece.KING) continue; // בריחה נבדקה כבר
            int oldRow = move.get_piece().getRow();
            int oldCol = move.get_piece().getCol();
            Piece captured = simulateMoveLocally(state, move);
            boolean stillInCheck = state.isKingInCheck(Color.black);
            undoMoveSimulationLocally(state, move, captured, oldRow, oldCol);
            if (!stillInCheck) return move; // מהלך זה מסיר את השח
        }
        return null;
    }

    // מציאת מהלך שאוכל את הכלי שנותן שח
    private Move findCaptureAttackerMove(GameState state, ArrayList<Move> allMoves) {
        for (Move move : allMoves) {
            Piece target = state.get_board()[move.get_toRow()][move.get_toCol()];
            if (target != null && target.getColor() == Color.white) {
                // בדוק אם אכילת הכלי הזה מסירה את השח
                int oldRow = move.get_piece().getRow();
                int oldCol = move.get_piece().getCol();
                Piece captured = simulateMoveLocally(state, move);
                boolean stillInCheck = state.isKingInCheck(Color.black);
                undoMoveSimulationLocally(state, move, captured, oldRow, oldCol);
                if (!stillInCheck) return move;
            }
        }
        return null;
    }

    // מציאת כלי שחור חשוב שמותקף ע"י לבן – מנסה לברוח / לאכול את התוקף
    private Move findSavePieceMove(GameState state) {
        ArrayList<Move> allMoves = getAllLegalMoves(state);

        for (Move move : allMoves) {
            Piece piece = move.get_piece();
            // מתמקד בכלים יקרים (מלכה, צריח, רץ, סוס)
            if (getPieceValue(piece.getPieceType()) < 300) continue;

            // בדוק אם הכלי מותקף במיקומו הנוכחי
            if (isSquareUnderAttack(state, piece.getRow(), piece.getCol(), Color.black)) {
                // נסה להציל: אם יש מהלך שמוציא אותו ממשבצת מותקפת
                if (!isSquareUnderAttack(state, move.get_toRow(), move.get_toCol(), Color.black)) {
                    return move; // מהלך הצלה
                }
            }
        }
        return null;
    }

    // ---- שלב אמצע משחק – ענף ב': חילוף והתקפה ----
    private Move actionCapture(GameState state) {
        ArrayList<Move> allMoves = getAllLegalMoves(state);
        ArrayList<Move> profitableMoves = new ArrayList<>();

        for (Move move : allMoves) {
            Piece target = state.get_board()[move.get_toRow()][move.get_toCol()];
            if (target == null) continue; // לא אכילה

            int targetValue  = getPieceValue(target.getPieceType());
            int attackerValue = getPieceValue(move.get_piece().getPieceType());

            // חילוף חיובי: אכל כלי שווה יותר או שווה
            if (targetValue >= attackerValue) {
                // הקרבה למען מט: אם אחרי הביצוע יש שח-מט – בצע!
                if (leadToCheckmate(state, move)) return move;
                profitableMoves.add(move);
            }
            // חילוף סוס מול רץ – בחר לפי סוג הלוח
            else if (isKnightVsBishop(move, target)) {
                Move preferred = chooseKnightOrBishop(state, move, target);
                if (preferred != null) return preferred;
            }
        }

        // אל תקריב – רק חילוף שמוכח כרווחי
        return getBestMoveFromList(state, profitableMoves);
    }

    // בדיקה אם מהלך מוביל ישירות לשח-מט (מסלול הקרבה)
    private boolean leadToCheckmate(GameState state, Move move) {
        int oldRow = move.get_piece().getRow();
        int oldCol = move.get_piece().getCol();
        Piece captured = simulateMoveLocally(state, move);

        // בדוק: האם הלבן בשח-מט אחרי המהלך
        boolean isCheckmate = state.isKingInCheck(Color.white)
                           && !hasAnyLegalMove(state, Color.white);

        undoMoveSimulationLocally(state, move, captured, oldRow, oldCol);
        return isCheckmate;
    }

    // בדיקה אם מדובר בחילוף סוס מול רץ
    private boolean isKnightVsBishop(Move move, Piece target) {
        byte mover  = move.get_piece().getPieceType();
        byte eaten  = target.getPieceType();
        return (mover == Piece.KNIGHT && eaten == Piece.BISHOP)
            || (mover == Piece.BISHOP && eaten == Piece.KNIGHT);
    }

    // לוח פתוח = מעט רגלים → עדיף רץ; לוח סגור = הרבה רגלים → עדיף סוס
    private Move chooseKnightOrBishop(GameState state, Move move, Piece target) {
        int totalPawns = state.getWhitePawns().size() + state.getBlackPawns().size();
        boolean openBoard = (totalPawns <= 8); // מחצית מהרגלים נאכלה → לוח פתוח

        // בלוח פתוח: שמור רץ (אל תאכל אותו), בלוח סגור: שמור סוס
        if (openBoard && move.get_piece().getPieceType() == Piece.BISHOP) return move;
        if (!openBoard && move.get_piece().getPieceType() == Piece.KNIGHT) return move;
        return null; // לא כדאי
    }

    // בדיקת עזר: האם לצבע יש בכלל מהלך חוקי?
    private boolean hasAnyLegalMove(GameState state, Color color) {
        state.getAllPossibleMoves(color);
        byte[] types = {Piece.PAWN, Piece.ROOK, Piece.KNIGHT, Piece.BISHOP, Piece.QUEEN, Piece.KING};
        for (byte t : types) {
            for (Byte pos : state.getPiecePositions(t, color)) {
                Piece p = state.get_board()[GameState.getRowFromByte(pos)][GameState.getColFromByte(pos)];
                if (p != null && !p._allPossibleMoves.isEmpty()) return true;
            }
        }
        return false;
    }

    // ---- שלב אמצע משחק – ענף ג': שיפור פוזיציה ----
    private Move actionPositional(GameState state) {
        ArrayList<Move> allMoves = getAllLegalMoves(state);

        // מסלול הצרחה: אם עדיין לא הצרחנו – נעדיף זאת
        Move castlingMove = findCastlingMove(state, allMoves);
        if (castlingMove != null) return castlingMove;

        // מסלול סידור רגלים: בנה חומת הגנה לפני המלך
        Move pawnShieldMove = findPawnShieldMove(state, allMoves);
        if (pawnShieldMove != null) return pawnShieldMove;

        // מסלול מיקום סוסים: סוס בשוליים → הוצא למרכז
        ArrayList<Move> knightMoves = filterMoves(allMoves, Piece.KNIGHT);
        Move bestKnight = getBestMoveFromList(state, knightMoves);
        if (bestKnight != null && isMoveMakingProgress(state, bestKnight)) return bestKnight;

        // מסלול מיקום צריחים: הצב על עמודה פתוחה (d/e) או שורה 7
        Move rookMove = findActiveRookMove(state, allMoves);
        if (rookMove != null) return rookMove;

        // מסלול En Passant: אם קיימת הזדמנות – נצלה
        Move enPassant = findEnPassantMove(state, allMoves);
        if (enPassant != null) return enPassant;

        // מסלול יישור כוונות: ישר מלכה + רץ (הכי טוב מכלל הכלים)
        ArrayList<Move> bishopMoves = filterMoves(allMoves, Piece.BISHOP);
        Move bestBishop = getBestMoveFromList(state, bishopMoves);
        if (bestBishop != null && isMoveMakingProgress(state, bestBishop)) return bestBishop;

        return null; // fallback לברירת מחדל
    }

    // מצא מהלך הצרחה (המלך זז 2 עמודות)
    private Move findCastlingMove(GameState state, ArrayList<Move> allMoves) {
        for (Move move : allMoves) {
            if (move.get_piece().getPieceType() == Piece.KING
                    && Math.abs(move.get_toCol() - move.get_fromCol()) == 2) {
                return move; // מהלך הצרחה
            }
        }
        return null;
    }

    // מצא מהלך רגלי שמחזק את ההגנה לפני המלך
    private Move findPawnShieldMove(GameState state, ArrayList<Move> allMoves) {
        if (state.getBlackKings().isEmpty()) return null;
        byte kingPos  = state.getBlackKings().get(0);
        int  kingRow  = GameState.getRowFromByte(kingPos);
        int  kingCol  = GameState.getColFromByte(kingPos);

        for (Move move : allMoves) {
            if (move.get_piece().getPieceType() != Piece.PAWN) continue;
            int toRow = move.get_toRow();
            int toCol = move.get_toCol();
            // רגלי שנמצא שורה לפני המלך ובאזור המלך – מהלך הגנה
            if (toRow == kingRow + 1 && Math.abs(toCol - kingCol) <= 1) {
                return move;
            }
        }
        return null;
    }

    // מצא צריח שעובר לעמודה d(3)/e(4) או לשורה 1 (שורה 7 מבחינת לבן)
    private Move findActiveRookMove(GameState state, ArrayList<Move> allMoves) {
        for (Move move : allMoves) {
            if (move.get_piece().getPieceType() != Piece.ROOK) continue;
            int toCol = move.get_toCol();
            int toRow = move.get_toRow();
            // עמודה מרכזית d או e
            if (toCol == 3 || toCol == 4) return move;
            // שורה 1 (לחץ על הצד של הלבן)
            if (toRow == 1) return move;
        }
        return null;
    }

    // מצא מהלך דרך אגב (En Passant)
    private Move findEnPassantMove(GameState state, ArrayList<Move> allMoves) {
        for (Move move : allMoves) {
            if (move.is_isEnPassant()) return move;
        }
        return null;
    }

    // בדיקה אם משבצת מותקפת ע"י צבע האויב
    private boolean isSquareUnderAttack(GameState state, int row, int col, Color friendlyColor) {
        Color enemyColor = (friendlyColor == Color.black) ? Color.white : Color.black;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece enemy = state.get_board()[r][c];
                if (enemy == null || enemy.getColor() != enemyColor) continue;
                if (enemy.isValidMovement(r, c, row, col, state.getPiecePlace(row, col))
                        && !enemy.isMoveOverAnotherPiece(state, r, c, row, col)) {
                    return true; // המשבצת מותקפת
                }
            }
        }
        return false;
    }

    // ---- שלב סיום משחק ----
    private Move actionEndgame(GameState state) {
        ArrayList<Move> allMoves = getAllLegalMoves(state);

        // מסלול הכתרה: יש רגלי חופשי – קדם אותו לשורה 7 (מלכה)
        Move promotionMove = findPawnPromotionMove(state, allMoves);
        if (promotionMove != null) return promotionMove;

        // מסלול דחיקת מלך: יש 2 כלים כבדים – בחר מהלך שמקטין את שטח המלך הלבן
        if (hasSufficientMatingMaterial(state)) {
            Move matingMove = findKingCorneringMove(state, allMoves);
            if (matingMove != null) return matingMove;
        }

        // מסלול מרכוז מלך: אין יתרון – הוצא מלך למרכז
        Move kingCenterMove = findKingCenterMove(state, allMoves);
        if (kingCenterMove != null) return kingCenterMove;

        // ברירת מחדל: הכי טוב שנמצא
        return getBestMoveFromList(state, allMoves);
    }

    // מצא מהלך שמקדם רגלי קרוב ביותר להכתרה
    private Move findPawnPromotionMove(GameState state, ArrayList<Move> allMoves) {
        Move bestPawnMove = null;
        int  maxRow = -1; // ככל שהשורה גבוהה יותר, הרגלי קרוב יותר לשורה 7

        for (Move move : allMoves) {
            if (move.get_piece().getPieceType() != Piece.PAWN) continue;
            int toRow = move.get_toRow();
            if (toRow > maxRow) {
                maxRow = toRow;
                bestPawnMove = move; // הרגלי הכי מתקדם
            }
        }
        return bestPawnMove;
    }

    // בדיקה אם יש מספיק חומר להכתרה (צריח + מלך, או שתי מלכות)
    private boolean hasSufficientMatingMaterial(GameState state) {
        return !state.getBlackQueens().isEmpty() || !state.getBlackRooks().isEmpty();
    }

    // מצא מהלך שדוחק את המלך הלבן לפינה (ממקסם התקרבות מלכים)
    private Move findKingCorneringMove(GameState state, ArrayList<Move> allMoves) {
        if (state.getWhiteKings().isEmpty()) return null;
        byte whiteKingPos = state.getWhiteKings().get(0);
        int  wRow = GameState.getRowFromByte(whiteKingPos);
        int  wCol = GameState.getColFromByte(whiteKingPos);

        Move bestMove   = null;
        int  maxCorner  = -1; // ניקוד קירנות – ככל שגדול יותר המלך קרוב לפינה

        for (Move move : allMoves) {
            int oldRow = move.get_piece().getRow();
            int oldCol = move.get_piece().getCol();
            Piece captured = simulateMoveLocally(state, move);

            // חשב עד כמה המלך הלבן "כלוא" – מרחק מהמרכז = שליטה על פינה
            int distFromCenter = Math.abs(wRow - 3) + Math.abs(wCol - 3);
            int score = distFromCenter + evaluate(state);

            undoMoveSimulationLocally(state, move, captured, oldRow, oldCol);

            if (score > maxCorner) {
                maxCorner = score;
                bestMove  = move;
            }
        }
        return bestMove;
    }

    // מצא מהלך שמוציא את המלך השחור למרכז (שורות 3-4, עמודות 3-4)
    private Move findKingCenterMove(GameState state, ArrayList<Move> allMoves) {
        for (Move move : allMoves) {
            if (move.get_piece().getPieceType() != Piece.KING) continue;
            int toRow = move.get_toRow();
            int toCol = move.get_toCol();
            // המרכז הוא [3-4][3-4]
            if (toRow >= 3 && toRow <= 4 && toCol >= 3 && toCol <= 4) {
                return move;
            }
        }
        return null;
    }

    // ---- ברירת מחדל ----
    private Move actionDefaultBestMove(GameState state) {
        return getBestMoveFromList(state, getAllLegalMoves(state));
    }

    // =========================================================
    // 8. פונקציות עזר
    // =========================================================

    // אסוף את כל המהלכים החוקיים של שחור מרשימות _allPossibleMoves
    private ArrayList<Move> getAllLegalMoves(GameState state) {
        ArrayList<Move> allMoves = new ArrayList<>();
        byte[] pieceTypes = {Piece.PAWN, Piece.ROOK, Piece.KNIGHT, Piece.BISHOP, Piece.QUEEN, Piece.KING};

        for (byte pieceType : pieceTypes) {
            for (Byte position : state.getPiecePositions(pieceType, Color.black)) {
                int row = GameState.getRowFromByte(position);
                int col = GameState.getColFromByte(position);
                Piece p = state.get_board()[row][col];
                if (p != null) {
                    allMoves.addAll(p._allPossibleMoves);
                }
            }
        }
        return allMoves;
    }

    // סנן מהלכים לפי סוג כלי
    private ArrayList<Move> filterMoves(ArrayList<Move> allMoves, byte pieceType) {
        ArrayList<Move> filtered = new ArrayList<>();
        for (Move move : allMoves) {
            if (move.get_piece().getPieceType() == pieceType) filtered.add(move);
        }
        return filtered;
    }

    // ערך מספרי של כלי לצורך השוואות
    private int getPieceValue(byte type) {
        switch(type) {
            case Piece.PAWN:   return 100;
            case Piece.KNIGHT: return 300;
            case Piece.BISHOP: return 300;
            case Piece.ROOK:   return 500;
            case Piece.QUEEN:  return 900;
            default:           return 0;
        }
    }

    private Move getBestMoveFromList(GameState state, ArrayList<Move> moves) {
        if (moves.isEmpty()) return null;

        Move bestMove  = null;
        int  bestScore = Integer.MIN_VALUE;

        for (Move move : moves) {
            int oldRow = move.get_piece().getRow();
            int oldCol = move.get_piece().getCol();
            Piece captured = simulateMoveLocally(state, move);
            
            // 1. קבלת הציון הרגיל על בסיס מצב הלוח (הרווח המיידי מהמהלך)
            int score = evaluate(state);
            
            // ---> התיקון החכם לחילופי כלים (מי אוכל מיד אחרי?) <---
            Piece whiteAttacker = getLowestValueAttacker(state, move.get_toRow(), move.get_toCol(), Color.white);
            
            if (whiteAttacker != null) {
                // הלבן אכן מאיים על המשבצת שניכנס אליה!
                int myPieceValue = getPieceValue(move.get_piece().getPieceType());
                int attackerValue = getPieceValue(whiteAttacker.getPieceType());
                
                // האם גם אנחנו (השחור) מאיימים על המשבצת הזו? (כלומר, נוכל לאכול חזרה)
                Piece blackDefender = getLowestValueAttacker(state, move.get_toRow(), move.get_toCol(), Color.black);
                
                if (blackDefender != null) {
                    // המשבצת מוגנת! יהיה כאן חילוף כלים.
                    if (myPieceValue > attackerValue) {
                        // חילוף גרוע: מציעים כלי יקר שלנו (למשל צריח) תמורת כלי זול של הלבן (סוס).
                        // הלבן יאכל, אנחנו נאכל, והפסדנו את ההפרש.
                        score -= (myPieceValue - attackerValue);
                    } else {
                        // חילוף טוב/שוויוני: סוס בסוס, או רגלי שלנו מול מלכה של הלבן.
                        // הלבן לא "פראייר" ולכן לא יאכל אותנו כדי לא לאבד את המלכה שלו. 
                        // המשבצת בטוחה! לא מורידים אף נקודה.
                    }
                } else {
                    // המשבצת לא מוגנת בכלל. הלבן יאכל את הכלי שלנו בחינם.
                    score -= myPieceValue;
                }
            }

            // הוספת 'רעש' אקראי קטן (גיוון)
            score += (int)(Math.random() * 6);

            undoMoveSimulationLocally(state, move, captured, oldRow, oldCol);

            if (score > bestScore) {
                bestScore = score;
                bestMove  = move;
            }
        }
        return bestMove;
    }
 // פונקציית עזר: מוצאת את הכלי הכי זול שמאוים/מגן על משבצת מסוימת
    private Piece getLowestValueAttacker(GameState state, int targetRow, int targetCol, Color attackerColor) {
        Piece lowestAttacker = null;
        int lowestValue = Integer.MAX_VALUE;

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = state.get_board()[r][c];
                if (p != null && p.getColor() == attackerColor) {
                    // בדיקה האם הכלי מסוגל לאכול את המשבצת הזו
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

    // בדוק אם מהלך משפר את הניקוד (ביחס למצב הנוכחי)
    private boolean isMoveMakingProgress(GameState state, Move move) {
        int currentScore = evaluate(state);
        int oldRow = move.get_piece().getRow();
        int oldCol = move.get_piece().getCol();
        Piece captured = simulateMoveLocally(state, move);
        int futureScore = evaluate(state);
        undoMoveSimulationLocally(state, move, captured, oldRow, oldCol);
        return futureScore >= currentScore;
    }

    // סמלץ מהלך על הלוח ללא מחויבות
    private Piece simulateMoveLocally(GameState state, Move move) {
        Piece captured = state.get_board()[move.get_toRow()][move.get_toCol()];
        int oldRow = move.get_piece().getRow();
        int oldCol = move.get_piece().getCol();

        if (captured != null) {
            state.removePieceFromList(captured, move.get_toRow(), move.get_toCol());
        }

        state.get_board()[move.get_toRow()][move.get_toCol()] = move.get_piece();
        state.get_board()[oldRow][oldCol] = null;
        move.get_piece().setRow(move.get_toRow());
        move.get_piece().setCol(move.get_toCol());
        state.updatePiecePosition(move.get_piece(), oldRow, oldCol, move.get_toRow(), move.get_toCol());

        return captured;
    }

    // בטל את הסימולציה והחזר את הלוח למצבו הקודם
    private void undoMoveSimulationLocally(GameState state, Move move, Piece captured, int oldRow, int oldCol) {
        state.get_board()[oldRow][oldCol] = move.get_piece();
        state.get_board()[move.get_toRow()][move.get_toCol()] = captured;
        move.get_piece().setRow(oldRow);
        move.get_piece().setCol(oldCol);
        state.updatePiecePosition(move.get_piece(), move.get_toRow(), move.get_toCol(), oldRow, oldCol);

        if (captured != null) {
            state.addPieceToList(captured, move.get_toRow(), move.get_toCol());
        }
    }

    // =========================================================
    // 9. פונקציית ההערכה הכוללת (evaluate)
    // =========================================================

    public int evaluate(GameState state) {
        int score = 0;
        GamePhase phase = isMidgamePhase(state) ? GamePhase.MIDDLE : GamePhase.ENDGAME;

        score += materialScore(state);    // כמות החומר
        score += positionalScore(state, phase); // מיקום הכלים
        score += pawnStructureScore(state);     // מבנה רגלים
        
        // ---> התיקונים החדשים: בונוס מרחבי ובונוס חיילים חופשיים <---
        score += calculateThreatScore(state); 
        score += passedPawnScore(state);      

        if (phase == GamePhase.MIDDLE) {
            score += kingSafetyScore(state); // בטיחות המלך באמצע
        }

        if (phase == GamePhase.ENDGAME) {
            score += pawnAdvancementScore(state); // רגלים מתקדמים
            score += kingOppositionScore(state);  // התנגדות מלכים
        }

        return score;
    }

    // ניקוד חומר: כל כלי שחור = +, כל כלי לבן = -
    private int materialScore(GameState state) {
        int score = 0;
        score += state.getBlackPawns().size()   * 100;
        score += state.getBlackKnights().size() * 300;
        score += state.getBlackBishops().size() * 300;
        score += state.getBlackRooks().size()   * 500;
        score += state.getBlackQueens().size()  * 900;

        score -= state.getWhitePawns().size()   * 100;
        score -= state.getWhiteKnights().size() * 300;
        score -= state.getWhiteBishops().size() * 300;
        score -= state.getWhiteRooks().size()   * 500;
        score -= state.getWhiteQueens().size()  * 900;
        return score;
    }

    // ניקוד מיקום: כל כלי מקבל בונוס/קנס לפי טבלה
    private int positionalScore(GameState state, GamePhase phase) {
        int score = 0;

        for (Byte pos : state.getBlackKnights())  score += KNIGHT_TABLE[GameState.getRowFromByte(pos)][GameState.getColFromByte(pos)];
        for (Byte pos : state.getWhiteKnights())  score -= KNIGHT_TABLE[7 - GameState.getRowFromByte(pos)][GameState.getColFromByte(pos)];
        for (Byte pos : state.getBlackBishops())  score += BISHOP_TABLE[GameState.getRowFromByte(pos)][GameState.getColFromByte(pos)];
        for (Byte pos : state.getWhiteBishops())  score -= BISHOP_TABLE[7 - GameState.getRowFromByte(pos)][GameState.getColFromByte(pos)];

        for (Byte pos : state.getBlackRooks()) {
            score += ROOK_TABLE[GameState.getRowFromByte(pos)][GameState.getColFromByte(pos)];
            score += rookOpenFileBonus(state, GameState.getColFromByte(pos), Color.black);
        }
        for (Byte pos : state.getWhiteRooks()) {
            score -= ROOK_TABLE[7 - GameState.getRowFromByte(pos)][GameState.getColFromByte(pos)];
            score -= rookOpenFileBonus(state, GameState.getColFromByte(pos), Color.white);
        }

        for (Byte pos : state.getBlackPawns())  score += PAWN_TABLE_BLACK[GameState.getRowFromByte(pos)][GameState.getColFromByte(pos)];
        for (Byte pos : state.getWhitePawns())  score -= PAWN_TABLE_WHITE[GameState.getRowFromByte(pos)][GameState.getColFromByte(pos)];

        int[][] kingTable = (phase == GamePhase.ENDGAME) ? KING_ENDGAME_TABLE : KING_MIDDLE_TABLE;
        for (Byte pos : state.getBlackKings())  score += kingTable[GameState.getRowFromByte(pos)][GameState.getColFromByte(pos)];
        for (Byte pos : state.getWhiteKings())  score -= kingTable[7 - GameState.getRowFromByte(pos)][GameState.getColFromByte(pos)];

        return score;
    }

    // בונוס לצריח שעומד על עמודה פתוחה (ללא רגלים שלו)
    private int rookOpenFileBonus(GameState state, int col, Color color) {
        ArrayList<Byte> ownPawns = (color == Color.black) ? state.getBlackPawns() : state.getWhitePawns();
        for (Byte pos : ownPawns) {
            if (GameState.getColFromByte(pos) == col) return 0; // עמודה חסומה
        }
        return 15; // עמודה פתוחה = בונוס
    }

    // קנס על רגלים כפולים באותה עמודה
    private int pawnStructureScore(GameState state) {
        int score = 0;
        int[] blackPerCol = new int[8];
        int[] whitePerCol = new int[8];

        for (Byte pos : state.getBlackPawns()) blackPerCol[GameState.getColFromByte(pos)]++;
        for (Byte pos : state.getWhitePawns()) whitePerCol[GameState.getColFromByte(pos)]++;

        for (int col = 0; col < 8; col++) {
            if (blackPerCol[col] > 1) score -= 20 * (blackPerCol[col] - 1); // קנס לשחור
            if (whitePerCol[col] > 1) score += 20 * (whitePerCol[col] - 1); // יתרון ללבן
        }
        return score;
    }

    // ניקוד בטיחות מלך: בונוס על רגלים שמגנים לפני המלך
    private int kingSafetyScore(GameState state) {
        return kingShelterBonus(state, Color.black) - kingShelterBonus(state, Color.white);
    }

    private int kingShelterBonus(GameState state, Color color) {
        ArrayList<Byte> kings = (color == Color.black) ? state.getBlackKings() : state.getWhiteKings();
        ArrayList<Byte> pawns = (color == Color.black) ? state.getBlackPawns() : state.getWhitePawns();
        if (kings.isEmpty()) return 0;

        int kingRow = GameState.getRowFromByte(kings.get(0));
        int kingCol = GameState.getColFromByte(kings.get(0));
        int shelterRow = (color == Color.black) ? kingRow + 1 : kingRow - 1; // שורת ההגנה

        int bonus = 0;
        for (Byte pos : pawns) {
            int pRow = GameState.getRowFromByte(pos);
            int pCol = GameState.getColFromByte(pos);
            if (pRow == shelterRow && Math.abs(pCol - kingCol) <= 1) {
                bonus += 15; // רגלי מגן על המלך
            }
        }
        return bonus;
    }

    // בונוס להתקדמות רגלים לעבר קידום
    private int pawnAdvancementScore(GameState state) {
        int score = 0;
        for (Byte pos : state.getBlackPawns())
            score += GameState.getRowFromByte(pos) * 5; // שחור מתקדם לכיוון שורה 7
        for (Byte pos : state.getWhitePawns())
            score -= (7 - GameState.getRowFromByte(pos)) * 5; // לבן מתקדם לכיוון שורה 0
        return score;
    }

    // בונוס על "התנגדות" – מלכים מסתכלים זה על זה
    private int kingOppositionScore(GameState state) {
        if (state.getBlackKings().isEmpty() || state.getWhiteKings().isEmpty()) return 0;
        byte bPos = state.getBlackKings().get(0);
        byte wPos = state.getWhiteKings().get(0);

        int rowDiff = Math.abs(GameState.getRowFromByte(bPos) - GameState.getRowFromByte(wPos));
        int colDiff = Math.abs(GameState.getColFromByte(bPos) - GameState.getColFromByte(wPos));

        if ((rowDiff == 2 && colDiff == 0) || (rowDiff == 0 && colDiff == 2)) return 30; // התנגדות ישירה
        if (rowDiff == 2 && colDiff == 2) return 15; // התנגדות אלכסונית
        return 0;
    }
    
 // ----------------------------------------------------
    // פונקציות ההערכה החדשות - איום מרחבי ורגלי חופשי
    // ----------------------------------------------------

    // 1. חישוב "איום מרחבי" - מוריד ניקוד אם הכלים שלנו מאוימים, ומוסיף בונוס אם של הלבן
    private int calculateThreatScore(GameState state) {
        int threatScore = 0;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = state.get_board()[r][c];
                if (p != null) {
                    int pieceValue = getPieceValue(p.getPieceType());
                    
                    if (p.getColor() == Color.black) { // כלי שלנו
                        if (isSquareUnderAttack(state, r, c, Color.black)) { // האם הלבן מאיים עליו?
                            if (!isSquareDefended(state, r, c, Color.black)) {
                                threatScore -= pieceValue; // כלי שלנו בסכנה וללא הגנה - קנס ענק!
                            } else {
                                threatScore -= (pieceValue / 10); // מוגן, אבל נתון ללחץ הלבן
                            }
                        }
                    } else { // כלי של הלבן
                        if (isSquareUnderAttack(state, r, c, Color.white)) { // האם אנחנו (שחור) מאיימים עליו?
                            if (!isSquareDefended(state, r, c, Color.white)) {
                                threatScore += pieceValue; // בונוס! נעלנו אותו על כוונת והוא לא מוגן
                            } else {
                                threatScore += (pieceValue / 10); // בונוס קטן על עצם הלחץ
                            }
                        }
                    }
                }
            }
        }
        return threatScore;
    }

    // פונקציית עזר לבדיקה אם כלי שלנו שומר על המשבצת (Defended)
    private boolean isSquareDefended(GameState state, int row, int col, Color friendlyColor) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece ally = state.get_board()[r][c];
                if (ally == null || ally.getColor() != friendlyColor) continue;
                if (r == row && c == col) continue; // לא בודק את הכלי עצמו כמי שמגן על עצמו
                if (ally.isValidMovement(r, c, row, col, state.getPiecePlace(row, col))
                        && !ally.isMoveOverAnotherPiece(state, r, c, row, col)) {
                    return true;
                }
            }
        }
        return false;
    }

    // 2. חישוב בונוס ענק של חייל חופשי (Passed Pawn)
    private int passedPawnScore(GameState state) {
        int score = 0;
        
        // חיילים שחורים (רצים למטה - שורות עולות 0->7)
        for (Byte pos : state.getBlackPawns()) {
            int r = GameState.getRowFromByte(pos);
            int c = GameState.getColFromByte(pos);
            if (isPassedPawn(state, r, c, Color.black)) {
                score += 200 + (r * 30); // בונוס של 200 + עוד 30 נקודות על כל צעד קדימה!
            }
        }
        
        // חיילים לבנים (רצים למעלה - שורות יורדות 7->0)
        for (Byte pos : state.getWhitePawns()) {
            int r = GameState.getRowFromByte(pos);
            int c = GameState.getColFromByte(pos);
            if (isPassedPawn(state, r, c, Color.white)) {
                score -= 200 + ((7 - r) * 30); // עונש מראה לשחור אם ללבן יש חייל חופשי
            }
        }
        
        return score;
    }

    // בדיקה האם הדרך פנויה עד להכתרה (אין רגלי יריב בעמודה הזו או בסמוכות)
    private boolean isPassedPawn(GameState state, int row, int col, Color color) {
        int direction = (color == Color.black) ? 1 : -1;
        int r = row + direction;
        
        while (r >= 0 && r <= 7) {
            // בדיקה באותה עמודה חזיתית
            Piece p = state.get_board()[r][col];
            if (p != null && p.getPieceType() == Piece.PAWN && p.getColor() != color) return false;
            
            // בדיקה באלכסון שמאלי (כדי לדעת שרגלי יריב לא יכול פשוט לאכול אותנו כשנתקדם)
            if (col > 0) {
                p = state.get_board()[r][col - 1];
                if (p != null && p.getPieceType() == Piece.PAWN && p.getColor() != color) return false;
            }
            
            // בדיקה באלכסון ימני
            if (col < 7) {
                p = state.get_board()[r][col + 1];
                if (p != null && p.getPieceType() == Piece.PAWN && p.getColor() != color) return false;
            }
            r += direction;
        }
        return true; // הדרך פנויה להכתרה!
    }
}