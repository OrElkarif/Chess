package logic;

import java.awt.Color;
import java.util.ArrayList;
import piece.Piece;

public class AIPlayer {//בוט שמנהל עץ התנהגות ומחליט איזה תהליך חשיבה כדאי להפעיל בהתאם למצב

    private Node<TreeTask> rootNode;
    private OpeningBook openingBook;
    private Evaluator evaluator;
    private ArrayList<String> _pastBlackFormations = new ArrayList<>();
    private ArrayList<Move> _cachedLegalMoves = null;//cache שמונע חישוב כפול של המהלכים החוקיים באותו תור
    private ArrayList<Move> _cachedTacticalMoves = null;//isTactical שומר כאן את המהלכים שמצא — actionDefense משתמש בהם ישירות


    public AIPlayer() {//בונה את העץ ומאתחלת את הבוט
        openingBook = new OpeningBook();
        evaluator = new Evaluator();
        buildStrategyTree();
    }

    private abstract class ConditionTask implements TreeTask {
        @Override public boolean isAction() { return false; }
        @Override public Move calculateMove(GameState state) { return null; }
    }

    private abstract class ActionTask implements TreeTask {
        @Override public boolean isAction() { return true; }
        @Override public boolean checkCondition(GameState state) { return false; }
    }

    class IsOpeningTask extends ConditionTask {
        @Override public boolean checkCondition(GameState state)
        { return !openingBook.isOpeningDone(); }
    }

    class IsMidgameTask extends ConditionTask {
        @Override public boolean checkCondition(GameState state)
        { return isMidgamePhase(state); }
    }

    class OpeningActionTask extends ActionTask {//פעולה שבוחרת מהלכי פתיחה מוכנים מהספר
        @Override public Move calculateMove(GameState state)
        { return openingBook.getNextMove(state, evaluator); }
    }

    class DefenseActionTask extends ActionTask {//פעולה שמחשבת איך להתגונן בצורה הכי טובה כשיש סכנה
        @Override public Move calculateMove(GameState state)
        { return actionDefense(state); }
    }

    class CaptureActionTask extends ActionTask {//פעולה שתפקידה למצוא מה מותר וכדאי לאכול ליריב
        @Override public Move calculateMove(GameState state)
        { return actionCapture(state); }
    }

    class PositionalActionTask extends ActionTask {//פעולה שמחפשת מהלכים שרק מחזקים
        @Override public Move calculateMove(GameState state)
        { return actionPositional(state); }
    }

    class DefaultActionTask extends ActionTask {//שולפת מהלך חוקי פשוט
        @Override public Move calculateMove(GameState state)
        { return getBestMoveFromList(state, getAllLegalMoves(state)); }
    }

    class EndgameActionTask extends ActionTask {//מנהלת את מהלכי הסיום
        @Override public Move calculateMove(GameState state)
        { return actionEndgame(state); }
    }

    class IsTacticalTask extends ConditionTask {//בודקת אם מצב הלוח טקטי: שח, איום על כלי, או אכילה משתלמת
        @Override public boolean checkCondition(GameState state)
        { return isTacticalSituation(state); }
    }

    private boolean isTacticalSituation(GameState state) {//מוצא את האיום, שומר את המהלכים הרלוונטיים ב-cache — actionDefense ישתמש בהם ישירות
        _cachedTacticalMoves = null;

        // 1. המלך בשח — שומר מהלכים שמצילים את המלך
        if (state.isKingInCheck(Color.black)) {
            ArrayList<Move> kingMoves = new ArrayList<>();
            for (Move mv : getAllLegalMoves(state)) {
                if (mv.get_piece().getPieceType() == Piece.KING) kingMoves.add(mv);
            }
            _cachedTacticalMoves = kingMoves.isEmpty() ? getAllLegalMoves(state) : kingMoves;
            return true;
        }

        // 2. כלי יקר מאויים — שומר מהלכי בריחה של הכלי הספציפי
        byte[] valuablePieces = {Piece.QUEEN, Piece.ROOK, Piece.BISHOP, Piece.KNIGHT};
        for (byte type : valuablePieces) {
            for (Byte pos : state.getPiecePositions(type, Color.black)) {
                int r = GameState.getRowFromByte(pos);
                int c = GameState.getColFromByte(pos);
                if (evaluator.isSquareUnderAttack(state, r, c, Color.black)) {
                    Piece threatened = state.get_board()[r][c];
                    ArrayList<Move> escapeMoves = new ArrayList<>();
                    for (Move mv : getAllLegalMoves(state)) {
                        if (mv.get_piece() == threatened) escapeMoves.add(mv);
                    }
                    _cachedTacticalMoves = escapeMoves.isEmpty() ? getAllLegalMoves(state) : escapeMoves;
                    return true;
                }
            }
        }

        // 3. אכילה משתלמת — שומר את כל האכילות הכדאיות
        ArrayList<Move> profitableCaptures = new ArrayList<>();
        for (Move move : getAllLegalMoves(state)) {
            Piece target = state.get_board()[move.get_toRow()][move.get_toCol()];
            if (target != null && target.getColor() == Color.white) {
                int targetValue   = evaluator.getPieceValue(target.getPieceType());
                int attackerValue = evaluator.getPieceValue(move.get_piece().getPieceType());
                if (targetValue >= attackerValue) profitableCaptures.add(move);
            }
        }
        if (!profitableCaptures.isEmpty()) {
            _cachedTacticalMoves = profitableCaptures;
            return true;
        }

        return false;
    }

    private void buildStrategyTree() {//בונה עץ מאוזן: כל ענף בעומק זהה, תנאי IsTactical מפצל טקטיקה מאסטרטגיה
        // --- עלים ---
        Node<TreeTask> defaultAction   = new Node<TreeTask>(new DefaultActionTask());
        Node<TreeTask> endgameAction   = new Node<TreeTask>(new EndgameActionTask());

        // --- ענף אסטרטגי (הלוח רגוע): מיקום → ברירת מחדל ---
        Node<TreeTask> positionalAction = new Node<TreeTask>(new PositionalActionTask(), null, defaultAction);

        // --- ענף טקטי (יש איום): הגנה → אכילה → ברירת מחדל ---
        Node<TreeTask> captureAction   = new Node<TreeTask>(new CaptureActionTask(), null, defaultAction);
        Node<TreeTask> defenseAction   = new Node<TreeTask>(new DefenseActionTask(), null, captureAction);

        // --- תנאי אמצע משחק: טקטי? שמאל=הגנה/אכילה | ימין=מיקום/ברירת מחדל ---
        Node<TreeTask> isTacticalNode  = new Node<TreeTask>(new IsTacticalTask(), defenseAction, positionalAction);

        // --- ענף פתיחה: הגנה → ספר פתיחות → (אם הספר נגמר) isTactical ---
        Node<TreeTask> openingAction   = new Node<TreeTask>(new OpeningActionTask(), null, isTacticalNode);
        Node<TreeTask> openingDefense  = new Node<TreeTask>(new DefenseActionTask(), null, openingAction);

        // --- שורש: פתיחה? שמאל=openingDefense | ימין=isMidgame ---
        Node<TreeTask> isMidgameNode   = new Node<TreeTask>(new IsMidgameTask(), isTacticalNode, endgameAction);
        this.rootNode = new Node<TreeTask>(new IsOpeningTask(), openingDefense, isMidgameNode);
    }

    public Move chooseBestMove(GameState state) {//מתניעה את החשיבה מעבירה את הלוח בעץ ומקבלת בסוף את המהלך המנצח שישוחק
   //הפעולה נקראת מboardpanel
    	_pastBlackFormations.add(getBlackFormation(state));
        if (_pastBlackFormations.size() > 6) {
            _pastBlackFormations.remove(0);
        }
        state.getAllPossibleMoves(Color.black);
        _cachedLegalMoves = computeLegalMoves(state);//חישוב אחד לכל התור

        Node<TreeTask> current = rootNode;
        while (current != null) {
            TreeTask task = current.getData();
            if (task.isAction()) {
                Move move = task.calculateMove(state);
                if (move != null) {
                    _cachedLegalMoves = null;
                    _cachedTacticalMoves = null;
                    return move;
                } else {
                    current = current.getRight();
                }
            } else {
                current = task.checkCondition(state) ? current.getLeft() : current.getRight();
            }
        }
        _cachedLegalMoves = null;
        _cachedTacticalMoves = null;
        return null;
    }

    private boolean isMidgamePhase(GameState state) {//אמצע משחק = יש לפחות מלכה אחת בלוח ולפחות 2 צריחים — ברגע שאין מלכות זה סיום
        boolean hasQueens = !state.getBlackQueens().isEmpty() || !state.getWhiteQueens().isEmpty();
        int rooks = state.getBlackRooks().size() + state.getWhiteRooks().size();
        return hasQueens && rooks >= 2;
    }

    private Move actionDefense(GameState state) {//קורא ישירות מ-_cachedTacticalMoves שisTactical כבר מילא — אפס חישוב כפול
        if (_cachedTacticalMoves != null && !_cachedTacticalMoves.isEmpty()) {
            if (state.isKingInCheck(Color.black)) {
                // המלך בשח — ננסה בריחה, חסימה, אכילת תוקף, אחרת הכי טוב מהcache
                Move kingEscape = findKingEscapeMove(state, _cachedTacticalMoves);
                if (kingEscape != null) return kingEscape;
                Move blockMove = findBlockCheckMove(state, _cachedTacticalMoves);
                if (blockMove != null) return blockMove;
                Move captureAttacker = findCaptureAttackerMove(state, _cachedTacticalMoves);
                if (captureAttacker != null) return captureAttacker;
            }
            return getBestMoveFromList(state, _cachedTacticalMoves);//כלי מאויים או אכילה — הcache כבר מסונן
        }
        return null;
    }

    private Move findKingEscapeMove(GameState state, ArrayList<Move> allMoves) {//פתח מילוט למלך שכרגע קיבל שח
        for (Move move : allMoves) {
            if (move.get_piece().getPieceType() == Piece.KING) return move;//ברגע שאפשר לברוח תברח
        }
        return null;
    }

    private Move findBlockCheckMove(GameState state, ArrayList<Move> allMoves) {//מוצאת כלים שיכולים לקפוץ לאמצע ולחסום
        for (Move move : allMoves) {
            if (move.get_piece().getPieceType() == Piece.KING) continue;//כבר הבנו שאין בריחה עם המלך
            int oldRow = move.get_piece().getRow();
            int oldCol = move.get_piece().getCol();
            Piece captured = simulateMoveLocally(state, move);
            boolean stillInCheck = state.isKingInCheck(Color.black);//תבדוק לי אם המהלך הקיים הזה שינה לי את מצב השח
            undoMoveSimulationLocally(state, move, captured, oldRow, oldCol);
            if (!stillInCheck) return move;
        }
        return null;
    }

    private Move findCaptureAttackerMove(GameState state, ArrayList<Move> allMoves) {//מנסה למצוא דרך פשוטה לאכול את הכלי שעושה שח
        for (Move move : allMoves) {
            Piece target = state.get_board()[move.get_toRow()][move.get_toCol()];
            if (target != null && target.getColor().equals(Color.white))  {//אם אני מוצא מישהו לאכול והוא יעצור לי את השח
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

    private Move findSavePieceMove(GameState state) {//	בודקת אם יש כלי חסר הגנה ומזיזה אותו למקום בטוח
        byte[] types  = {Piece.QUEEN, Piece.ROOK, Piece.BISHOP, Piece.KNIGHT};
        int[]  values = {900,         500,         300,          300};

        for (int i = 0; i < types.length; i++) {
            for (Byte pos : state.getPiecePositions(types[i], Color.black)) {
                int r = GameState.getRowFromByte(pos);
                int c = GameState.getColFromByte(pos);
                if (evaluator.isSquareUnderAttack(state, r, c, Color.black)) {//אם מאיימים על הכלי
                    Piece attacker = evaluator.getLowestValueAttacker(state, r, c, Color.white);
                    int attackerVal = (attacker != null) ? evaluator.getPieceValue(attacker.getPieceType()) : 0;
                    if (!evaluator.isSquareDefended(state, r, c, Color.black)
                            || (attackerVal > 0 && attackerVal < values[i])) {//אם הכלי חסר הגנה או שלא שווה לי ההחלפות האלו כיביכול
                        Piece threatened = state.get_board()[r][c];
                        ArrayList<Move> escapeMoves = new ArrayList<>();
                        for (Move mv : getAllLegalMoves(state)) {//סינון: רק מהלכים שמזיזים את הכלי המאויים עצמו
                            if (mv.get_piece() == threatened) escapeMoves.add(mv);
                        }
                        return getBestMoveFromList(state, escapeMoves.isEmpty() ? getAllLegalMoves(state) : escapeMoves);
                    }
                }
            }
        }
        return null;
    }	

    private Move actionCapture(GameState state) {//מתמקדת באכילת כלים משתלמת כשאנחנו לא תחת איום
        ArrayList<Move> allMoves = getAllLegalMoves(state);
        ArrayList<Move> profitableMoves = new ArrayList<>();

        for (Move move : allMoves) {
            Piece target = state.get_board()[move.get_toRow()][move.get_toCol()];
            if (target == null) continue;

            int targetValue   = evaluator.getPieceValue(target.getPieceType());
            int attackerValue = evaluator.getPieceValue(move.get_piece().getPieceType());

            if (targetValue >= attackerValue) {
                if (leadToCheckmate(state, move)) return move;
                profitableMoves.add(move);//רשימה של מהלכים שהרווח של האכילה הוא טוב
            }
        }
        return getBestMoveFromList(state, profitableMoves);
    }

    private boolean leadToCheckmate(GameState state, Move move) {//short-circuit: קודם בודק שח, רק אז בודק מט — חוסך hasAnyLegalMove על כל מהלך
        int oldRow = move.get_piece().getRow();
        int oldCol = move.get_piece().getCol();
        Piece captured = simulateMoveLocally(state, move);
        boolean isCheckmate = false;
        if (state.isKingInCheck(Color.white)) {//בלי שח אין מט — קצר-מעגל לפני הקריאה היקרה
            isCheckmate = !hasAnyLegalMove(state, Color.white);
        }
        undoMoveSimulationLocally(state, move, captured, oldRow, oldCol);
        return isCheckmate;
    }

    private boolean hasAnyLegalMove(GameState state, Color color) {//בודקת במהירות אם בכלל אפשר לזוז כי אם לא אז המשחק נגמר
    	//נקרא מהפעולה למעלה
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

  
    private Move actionPositional(GameState state) {//שיפור עמדות כשהלוח רגוע
        ArrayList<Move> allMoves = getAllLegalMoves(state);

        Move castlingMove = findCastlingMove(state, allMoves);//האם אפשר הצרחה
        if (castlingMove != null) return castlingMove;

        ArrayList<Move> knightMoves = filterMoves(allMoves, Piece.KNIGHT);
        Move bestKnight = getBestMoveFromList(state, knightMoves);
        if (bestKnight != null && isMoveMakingProgress(state, bestKnight)) return bestKnight;

        ArrayList<Move> bishopMoves = filterMoves(allMoves, Piece.BISHOP);
        Move bestBishop = getBestMoveFromList(state, bishopMoves);
        if (bestBishop != null && isMoveMakingProgress(state, bestBishop)) return bestBishop;

        return null;
    }

    private Move findCastlingMove(GameState state, ArrayList<Move> allMoves) {//מחפשת הזדמנות לעשות הצרחה כדי לנעול את המלך במקום בטוח
        for (Move move : allMoves) {
            if (move.get_piece().getPieceType() == Piece.KING
                    && Math.abs(move.get_toCol() - move.get_fromCol()) == 2) {
                return move;
            }
        }
        return null;
    }

   
    private Move actionEndgame(GameState state) {//משנה את האסטרטגיה
        ArrayList<Move> allMoves = getAllLegalMoves(state);

        // 1. מט מיידי
        for (Move m : allMoves) {
            if (leadToCheckmate(state, m)) return m;
        }

        // 2. קידום רגלי
        Move promotionMove = findPawnPromotionMove(state, allMoves);
        if (promotionMove != null) return promotionMove;

        // 3. אם יש לנו עדיפות חומר   נסה לבצע מט
        if (!state.getBlackQueens().isEmpty() || !state.getBlackRooks().isEmpty()) {
            Move safeCheck = findSafeCheckingMove(state, allMoves);
            if (safeCheck != null) return safeCheck;

            Move cornerMove = findKingCorneringMove(state, allMoves);
            if (cornerMove != null) return cornerMove;
        }

        // 4. הקרב מלכים בסיום
        Move kingCenterMove = findKingCenterMove(state, allMoves);
        if (kingCenterMove != null) return kingCenterMove;

        return getBestMoveFromList(state, allMoves);
    }

    private Move findSafeCheckingMove(GameState state, ArrayList<Move> allMoves) {//נותנת שח ליריב אבל רק בתנאי שלא יאכלו לנו את הכלי שעושה את זה או שאני יוצא ברווח אם אכלו לי כביכול
        for (Move move : allMoves) {
            if (move.get_piece().getPieceType() == Piece.KING) continue;

            int oldRow = move.get_piece().getRow();
            int oldCol = move.get_piece().getCol();
            Piece captured = simulateMoveLocally(state, move);

            boolean givesCheck = state.isKingInCheck(Color.white);

            if (givesCheck) {
                Piece attacker = evaluator.getLowestValueAttacker(
                        state, move.get_toRow(), move.get_toCol(), Color.white);
                int myVal  = evaluator.getPieceValue(move.get_piece().getPieceType());
                int attVal = (attacker != null) ? evaluator.getPieceValue(attacker.getPieceType()) : 0;

                boolean isSafe = (attVal == 0) || (attVal >= myVal);

                undoMoveSimulationLocally(state, move, captured, oldRow, oldCol);
                if (isSafe) return move;
            } else {
                undoMoveSimulationLocally(state, move, captured, oldRow, oldCol);
            }
        }
        return null;
    }

    private Move findKingCorneringMove(GameState state, ArrayList<Move> allMoves) {//לדחוק את המלך של היריב לקצוות הלוח
        if (state.getWhiteKings().isEmpty()) return null;
        byte whiteKingPos = state.getWhiteKings().get(0);
        int  wRow = GameState.getRowFromByte(whiteKingPos);
        int  wCol = GameState.getColFromByte(whiteKingPos);

        Move bestMove  = null;
        int  bestScore = Integer.MIN_VALUE;

        for (Move move : allMoves) {
            if (move.get_piece().getPieceType() == Piece.KING) continue;

            int oldRow = move.get_piece().getRow();
            int oldCol = move.get_piece().getCol();
            Piece captured = simulateMoveLocally(state, move);

            int kingMobility = countKingMobility(state, wRow, wCol, Color.white);
            int cornerDist = Math.max(3 - wCol, wCol - 4) + Math.max(3 - wRow, wRow - 4);

            int score = cornerDist * 30 - kingMobility * 20 + evaluator.evaluate(state, false);

            undoMoveSimulationLocally(state, move, captured, oldRow, oldCol);

            if (score > bestScore) {
                bestScore = score;
                bestMove  = move;
            }
        }
        return bestMove;
    }

    private int countKingMobility(GameState state, int kingRow, int kingCol, Color kingColor) {//סופרת כמה מקומות פנויים נשארו למלך היריב
        int mobility = 0;
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                int nr = kingRow + dr, nc = kingCol + dc;
                if (nr < 0 || nr > 7 || nc < 0 || nc > 7) continue;
                Piece atTarget = state.get_board()[nr][nc];
                if (atTarget != null && atTarget.getColor() == kingColor) continue;
                if (!evaluator.isSquareUnderAttack(state, nr, nc, kingColor)) mobility++;
            }
        }
        return mobility;
    }

    private Move findPawnPromotionMove(GameState state, ArrayList<Move> allMoves) {//מנסה לנצל הזדמנויות לדחוף רגלים עד הסוף להכתרה למלכה
        Move bestPawnMove = null;
        int  maxRow = -1;
        for (Move move : allMoves) {
            if (move.get_piece().getPieceType() != Piece.PAWN) continue;
            if (move.get_toRow() > maxRow) {
                maxRow = move.get_toRow();
                bestPawnMove = move;
            }
        }
        return bestPawnMove;
    }

    private Move findKingCenterMove(GameState state, ArrayList<Move> allMoves) {//מביא את המלך השחור למרכז כדי שהוא ישתתף בהתקפה
        for (Move move : allMoves) {
            if (move.get_piece().getPieceType() != Piece.KING) continue;
            int toRow = move.get_toRow(), toCol = move.get_toCol();
            if (toRow >= 3 && toRow <= 4 && toCol >= 3 && toCol <= 4) return move;
        }
        return null;
    }

   
    private Move getBestMoveFromList(GameState state, ArrayList<Move> moves) {//בוחרת מתוך רשימה של אופציות את זו שמקבלת הכי הרבה נקודות
        //נקרא מכל הפעולות למעלה
    	if (moves.isEmpty()) return null;

        Move bestMove  = null;
        int  bestScore = Integer.MIN_VALUE;

        for (Move move : moves) {
            if (leadToCheckmate(state, move)) return move;

            int oldRow = move.get_piece().getRow();
            int oldCol = move.get_piece().getCol();
            Piece captured = simulateMoveLocally(state, move);

            int score = evaluator.evaluate(state, isMidgamePhase(state));

            // עונש על תלית מלכה
            for (Byte pos : state.getBlackQueens()) {
                int r = GameState.getRowFromByte(pos);
                int c = GameState.getColFromByte(pos);
                if (evaluator.isSquareUnderAttack(state, r, c, Color.black)) {
                    Piece attacker = evaluator.getLowestValueAttacker(state, r, c, Color.white);
                    if (attacker != null) {
                        if (!evaluator.isSquareDefended(state, r, c, Color.black) ||
                            evaluator.getPieceValue(attacker.getPieceType()) < Piece.QUEEN) {
                            score -= 9000;
                            break;
                        }
                    }
                }
            }

            // בונוס על מתן שח
            if (state.isKingInCheck(Color.white)) score += 60;

            // עונש על תלית כלי
            Piece whiteAttacker = evaluator.getLowestValueAttacker(
                    state, move.get_toRow(), move.get_toCol(), Color.white);
            if (whiteAttacker != null) {
                int myValue       = evaluator.getPieceValue(move.get_piece().getPieceType());
                int attackerValue = evaluator.getPieceValue(whiteAttacker.getPieceType());
                Piece blackDefender = evaluator.getLowestValueAttacker(
                        state, move.get_toRow(), move.get_toCol(), Color.black);
                if (blackDefender == null || myValue > attackerValue) {
                    score -= 1000;
                } else {
                    score -= (myValue / 10);
                }
            }

            // מניעת חזרות
            String futureBlackFormation = getBlackFormation(state);
            if (_pastBlackFormations.contains(futureBlackFormation)) {
                score -= 8000;
            }

            score += (int)(Math.random() * 6);

            undoMoveSimulationLocally(state, move, captured, oldRow, oldCol);

            if (score > bestScore) {
                bestScore = score;
                bestMove  = move;
            }
        }
        return bestMove;
    }

    private boolean isMoveMakingProgress(GameState state, Move move) {//בודקת שאנחנו לא סתם מזיזים הלוך חזור
        boolean midgame = isMidgamePhase(state);
        int currentScore = evaluator.evaluate(state, midgame);
        int oldRow = move.get_piece().getRow();
        int oldCol = move.get_piece().getCol();
        Piece captured = simulateMoveLocally(state, move);
        int futureScore = evaluator.evaluate(state, midgame);
        undoMoveSimulationLocally(state, move, captured, oldRow, oldCol);
        return futureScore >= currentScore;
    }

  
    private ArrayList<Move> getAllLegalMoves(GameState state) {//מחזיר cache אם קיים, אחרת מחשב — מונע כפילות בתוך אותו תור
        if (_cachedLegalMoves != null) return _cachedLegalMoves;
        return computeLegalMoves(state);
    }

    private ArrayList<Move> computeLegalMoves(GameState state) {//החישוב האמיתי — מסנן גם חזרות וגם מהלכים שמשאירים את המלך בשח
        ArrayList<Move> allMoves = new ArrayList<>();
        byte[] pieceTypes = {Piece.PAWN, Piece.ROOK, Piece.KNIGHT, Piece.BISHOP, Piece.QUEEN, Piece.KING};

        for (byte pieceType : pieceTypes) {
            for (Byte position : state.getPiecePositions(pieceType, Color.black)) {
                int row = GameState.getRowFromByte(position);
                int col = GameState.getColFromByte(position);
                Piece p = state.get_board()[row][col];
                if (p != null) allMoves.addAll(p._allPossibleMoves);
            }
        }

        ArrayList<Move> legalMoves = new ArrayList<>();
        ArrayList<Move> legalRepeatingMoves = new ArrayList<>();

        for (Move move : allMoves) {
            int oldRow = move.get_piece().getRow();
            int oldCol = move.get_piece().getCol();
            Piece captured = simulateMoveLocally(state, move);

            boolean leavesKingInCheck = state.isKingInCheck(Color.black);//הסימולציה כבר רצה — בדיקה ללא עלות נוספת
            String futureFormation = getBlackFormation(state);
            undoMoveSimulationLocally(state, move, captured, oldRow, oldCol);

            if (leavesKingInCheck) continue;//מהלך לא חוקי — המלך נשאר בשח, מסנן החוצה

            if (!_pastBlackFormations.contains(futureFormation)) {
                legalMoves.add(move);
            } else {
                legalRepeatingMoves.add(move);//חוקי אבל חזרה — fallback בלבד
            }
        }

        if (!legalMoves.isEmpty())         return legalMoves;
        if (!legalRepeatingMoves.isEmpty()) return legalRepeatingMoves;
        return new ArrayList<>();//מט/פט — אין מהלכים חוקיים
    }

    private ArrayList<Move> filterMoves(ArrayList<Move> allMoves, byte pieceType) {//מסננת מהלכים  
        ArrayList<Move> filtered = new ArrayList<>();
        for (Move move : allMoves) {
            if (move.get_piece().getPieceType() == pieceType) filtered.add(move);//מקבל כבר חייל מוגדר מראש סוס או רץ ופשוט נותנת רק להם את המהלכים שכדאי
        }
        return filtered;
    }

    private Piece simulateMoveLocally(GameState state, Move move) {//תעשה סימולציה של הזזה
        Piece captured = state.get_board()[move.get_toRow()][move.get_toCol()];
        int oldRow = move.get_piece().getRow();
        int oldCol = move.get_piece().getCol();

        if (captured != null) state.removePieceFromList(captured, move.get_toRow(), move.get_toCol());//תוצציא את הכלי מהרשימות

        state.get_board()[move.get_toRow()][move.get_toCol()] = move.get_piece();
        state.get_board()[oldRow][oldCol] = null;
        move.get_piece().setRow(move.get_toRow());
        move.get_piece().setCol(move.get_toCol());
        state.updatePiecePosition(move.get_piece(), oldRow, oldCol, move.get_toRow(), move.get_toCol());

        return captured;
    }

    private void undoMoveSimulationLocally(GameState state, Move move, Piece captured, int oldRow, int oldCol) {//תחזיר את המהלך לקדמותו
        state.get_board()[oldRow][oldCol] = move.get_piece();
        state.get_board()[move.get_toRow()][move.get_toCol()] = captured;
        move.get_piece().setRow(oldRow);
        move.get_piece().setCol(oldCol);
        state.updatePiecePosition(move.get_piece(), move.get_toRow(), move.get_toCol(), oldRow, oldCol);

        if (captured != null) state.addPieceToList(captured, move.get_toRow(), move.get_toCol());
    }

    private String getBlackFormation(GameState state) {//שולפת מערך פתיחה
        StringBuilder sb = new StringBuilder();//משתנה שאפשר לשנות אותו ולא כל פעם להקצות מקום מחדש בזיכרון בניגוד למחרוזת רגילה
        byte[] types = {Piece.PAWN, Piece.ROOK, Piece.KNIGHT, Piece.BISHOP, Piece.QUEEN, Piece.KING};
        for (byte t : types) {
            for (Byte pos : state.getPiecePositions(t, Color.black)) {
                sb.append(t).append(pos);
            }
        }
        return sb.toString();
    }
}