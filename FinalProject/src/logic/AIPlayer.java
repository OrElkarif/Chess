package logic;

import java.awt.Color;
import java.util.ArrayList;
import piece.Piece;

public class AIPlayer {

    private Node<TreeTask> rootNode;
    private OpeningBook openingBook;
    private Evaluator evaluator;
    private ArrayList<String> _pastBlackFormations = new ArrayList<>();

    // --- מניעת חזרות ---
    private int[] _lastAIMove = null;  // {fromRow, fromCol, toRow, toCol}
    private int[] _prevAIMove = null;

    public AIPlayer() {
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

    class OpeningActionTask extends ActionTask {
        @Override public Move calculateMove(GameState state)
        { return openingBook.getNextMove(state, evaluator); }
    }

    class DefenseActionTask extends ActionTask {
        @Override public Move calculateMove(GameState state)
        { return actionDefense(state); }
    }

    class CaptureActionTask extends ActionTask {
        @Override public Move calculateMove(GameState state)
        { return actionCapture(state); }
    }

    class PositionalActionTask extends ActionTask {
        @Override public Move calculateMove(GameState state)
        { return actionPositional(state); }
    }

    class DefaultActionTask extends ActionTask {
        @Override public Move calculateMove(GameState state)
        { return getBestMoveFromList(state, getAllLegalMoves(state)); }
    }

    class EndgameActionTask extends ActionTask {
        @Override public Move calculateMove(GameState state)
        { return actionEndgame(state); }
    }

    private void buildStrategyTree() {
        Node<TreeTask> endgameAction  = new Node<TreeTask>(new EndgameActionTask());
        Node<TreeTask> defaultAction  = new Node<TreeTask>(new DefaultActionTask());

        Node<TreeTask> positionAction = new Node<TreeTask>(new PositionalActionTask(), null, defaultAction);
        Node<TreeTask> captureAction  = new Node<TreeTask>(new CaptureActionTask(), null, positionAction);
        Node<TreeTask> defenseAction  = new Node<TreeTask>(new DefenseActionTask(), null, captureAction);

        Node<TreeTask> isMidgameNode  = new Node<TreeTask>(new IsMidgameTask(), defenseAction, endgameAction);
        Node<TreeTask> openingAction  = new Node<TreeTask>(new OpeningActionTask(), null, isMidgameNode);
        Node<TreeTask> openingDefense = new Node<TreeTask>(new DefenseActionTask(), null, openingAction);

        this.rootNode = new Node<TreeTask>(new IsOpeningTask(), openingDefense, isMidgameNode);
    }

    public Move chooseBestMove(GameState state) {
    	_pastBlackFormations.add(getBlackFormation(state));
        if (_pastBlackFormations.size() > 6) { 
            _pastBlackFormations.remove(0); // נשמור רק את 6 התורות האחרונים שלא יפוצץ את הזיכרון
        }
        state.getAllPossibleMoves(Color.black);
        Node<TreeTask> current = rootNode;

        while (current != null) {
            TreeTask task = current.getData();
            if (task.isAction()) {
                Move move = task.calculateMove(state);
                if (move != null) {
                    // עדכן היסטוריית מהלכים למניעת חזרות
                    _prevAIMove = _lastAIMove;
                    _lastAIMove = new int[]{
                        move.get_fromRow(), move.get_fromCol(),
                        move.get_toRow(),   move.get_toCol()
                    };
                    return move;
                } else {
                    current = current.getRight();
                }
            } else {
                current = task.checkCondition(state) ? current.getLeft() : current.getRight();
            }
        }
        return null;
    }

    private boolean isMidgamePhase(GameState state) {
        int heavyPieces = state.getBlackQueens().size() + state.getWhiteQueens().size()
                        + state.getBlackRooks().size()  + state.getWhiteRooks().size();
        return heavyPieces >= 3;
    }

    private Move actionDefense(GameState state) {
        if (state.isKingInCheck(Color.black)) {
            ArrayList<Move> allMoves = getAllLegalMoves(state);
            Move kingEscape = findKingEscapeMove(state, allMoves);
            if (kingEscape != null) return kingEscape;
            Move blockMove = findBlockCheckMove(state, allMoves);
            if (blockMove != null) return blockMove;
            Move captureAttacker = findCaptureAttackerMove(state, allMoves);
            if (captureAttacker != null) return captureAttacker;
            return getBestMoveFromList(state, allMoves);
        }

        Move saveMove = findSavePieceMove(state);
        if (saveMove != null) return saveMove;
        return null;
    }

    private Move findKingEscapeMove(GameState state, ArrayList<Move> allMoves) {
        for (Move move : allMoves) {
            if (move.get_piece().getPieceType() == Piece.KING) return move;
        }
        return null;
    }

    private Move findBlockCheckMove(GameState state, ArrayList<Move> allMoves) {
        for (Move move : allMoves) {
            if (move.get_piece().getPieceType() == Piece.KING) continue;
            int oldRow = move.get_piece().getRow();
            int oldCol = move.get_piece().getCol();
            Piece captured = simulateMoveLocally(state, move);
            boolean stillInCheck = state.isKingInCheck(Color.black);
            undoMoveSimulationLocally(state, move, captured, oldRow, oldCol);
            if (!stillInCheck) return move;
        }
        return null;
    }

    private Move findCaptureAttackerMove(GameState state, ArrayList<Move> allMoves) {
        for (Move move : allMoves) {
            Piece target = state.get_board()[move.get_toRow()][move.get_toCol()];
            if (target != null && target.getColor() == Color.white) {
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

    private Move findSavePieceMove(GameState state) {
        boolean highValueThreatened = false;

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = state.get_board()[r][c];
                if (p != null && p.getColor() == Color.black) {
                    int val = evaluator.getPieceValue(p.getPieceType());
                    if (val >= 300) {
                        if (evaluator.isSquareUnderAttack(state, r, c, Color.black)) {
                            Piece attacker = evaluator.getLowestValueAttacker(state, r, c, Color.white);
                            int attackerVal = (attacker != null) ? evaluator.getPieceValue(attacker.getPieceType()) : 0;
                            if (!evaluator.isSquareDefended(state, r, c, Color.black) || (attackerVal > 0 && attackerVal < val)) {
                                highValueThreatened = true;
                                break;
                            }
                        }
                    }
                }
            }
            if (highValueThreatened) break;
        }

        if (highValueThreatened) {
            return getBestMoveFromList(state, getAllLegalMoves(state));
        }
        return null;
    }

    private Move actionCapture(GameState state) {
        ArrayList<Move> allMoves = getAllLegalMoves(state);
        ArrayList<Move> profitableMoves = new ArrayList<>();

        for (Move move : allMoves) {
            Piece target = state.get_board()[move.get_toRow()][move.get_toCol()];
            if (target == null) continue;

            int targetValue   = evaluator.getPieceValue(target.getPieceType());
            int attackerValue = evaluator.getPieceValue(move.get_piece().getPieceType());

            if (targetValue >= attackerValue) {
                if (leadToCheckmate(state, move)) return move;
                profitableMoves.add(move);
            } else if (isKnightVsBishop(move, target)) {
                Move preferred = chooseKnightOrBishop(state, move, target);
                if (preferred != null) return preferred;
            }
        }
        return getBestMoveFromList(state, profitableMoves);
    }

    private boolean leadToCheckmate(GameState state, Move move) {
        int oldRow = move.get_piece().getRow();
        int oldCol = move.get_piece().getCol();
        Piece captured = simulateMoveLocally(state, move);
        boolean isCheckmate = state.isKingInCheck(Color.white) && !hasAnyLegalMove(state, Color.white);
        undoMoveSimulationLocally(state, move, captured, oldRow, oldCol);
        return isCheckmate;
    }

    private boolean isKnightVsBishop(Move move, Piece target) {
        byte mover = move.get_piece().getPieceType();
        byte eaten = target.getPieceType();
        return (mover == Piece.KNIGHT && eaten == Piece.BISHOP) || (mover == Piece.BISHOP && eaten == Piece.KNIGHT);
    }

    private Move chooseKnightOrBishop(GameState state, Move move, Piece target) {
        int totalPawns = state.getWhitePawns().size() + state.getBlackPawns().size();
        boolean openBoard = (totalPawns <= 8);
        if (openBoard && move.get_piece().getPieceType() == Piece.BISHOP) return move;
        if (!openBoard && move.get_piece().getPieceType() == Piece.KNIGHT) return move;
        return null;
    }

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

    private Move actionPositional(GameState state) {
        ArrayList<Move> allMoves = getAllLegalMoves(state);

        Move castlingMove = findCastlingMove(state, allMoves);
        if (castlingMove != null) return castlingMove;

        Move pawnShieldMove = findPawnShieldMove(state, allMoves);
        if (pawnShieldMove != null) return pawnShieldMove;

        ArrayList<Move> knightMoves = filterMoves(allMoves, Piece.KNIGHT);
        Move bestKnight = getBestMoveFromList(state, knightMoves);
        if (bestKnight != null && isMoveMakingProgress(state, bestKnight)) return bestKnight;

        Move rookMove = findActiveRookMove(state, allMoves);
        if (rookMove != null) return rookMove;

        Move enPassant = findEnPassantMove(state, allMoves);
        if (enPassant != null) return enPassant;

        ArrayList<Move> bishopMoves = filterMoves(allMoves, Piece.BISHOP);
        Move bestBishop = getBestMoveFromList(state, bishopMoves);
        if (bestBishop != null && isMoveMakingProgress(state, bestBishop)) return bestBishop;

        return null;
    }

    private Move findCastlingMove(GameState state, ArrayList<Move> allMoves) {
        for (Move move : allMoves) {
            if (move.get_piece().getPieceType() == Piece.KING
                    && Math.abs(move.get_toCol() - move.get_fromCol()) == 2) {
                return move;
            }
        }
        return null;
    }

    private Move findPawnShieldMove(GameState state, ArrayList<Move> allMoves) {
        if (state.getBlackKings().isEmpty()) return null;
        byte kingPos = state.getBlackKings().get(0);
        int  kingRow = GameState.getRowFromByte(kingPos);
        int  kingCol = GameState.getColFromByte(kingPos);

        for (Move move : allMoves) {
            if (move.get_piece().getPieceType() != Piece.PAWN) continue;
            int toRow = move.get_toRow(), toCol = move.get_toCol();
            if (toRow == kingRow + 1 && Math.abs(toCol - kingCol) <= 1) return move;
        }
        return null;
    }

    private Move findActiveRookMove(GameState state, ArrayList<Move> allMoves) {
        for (Move move : allMoves) {
            if (move.get_piece().getPieceType() != Piece.ROOK) continue;
            int toCol = move.get_toCol(), toRow = move.get_toRow();
            if (toCol == 3 || toCol == 4) return move;
            if (toRow == 1) return move;
        }
        return null;
    }

    private Move findEnPassantMove(GameState state, ArrayList<Move> allMoves) {
        for (Move move : allMoves) {
            if (move.is_isEnPassant()) return move;
        }
        return null;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  סיום משחק — עם שיפורי מט
    // ══════════════════════════════════════════════════════════════════════════
    private Move actionEndgame(GameState state) {
        ArrayList<Move> allMoves = getAllLegalMoves(state);

        // 1. מט מיידי
        for (Move m : allMoves) {
            if (leadToCheckmate(state, m)) return m;
        }

        // 2. קידום רגלי
        Move promotionMove = findPawnPromotionMove(state, allMoves);
        if (promotionMove != null) return promotionMove;

        // 3. אם יש לנו עדיפות חומר — נסה לבצע מט
        if (!state.getBlackQueens().isEmpty() || !state.getBlackRooks().isEmpty()) {
            // מהלך שנותן שח בטוח (לא תולה כלי)
            Move safeCheck = findSafeCheckingMove(state, allMoves);
            if (safeCheck != null) return safeCheck;

            // דחיקת מלך לפינה
            Move cornerMove = findKingCorneringMove(state, allMoves);
            if (cornerMove != null) return cornerMove;
        }

        // 4. הקרב מלכים בסיום
        Move kingCenterMove = findKingCenterMove(state, allMoves);
        if (kingCenterMove != null) return kingCenterMove;

        return getBestMoveFromList(state, allMoves);
    }

    /**
     * מחפש מהלך שנותן שח אבל לא תולה את הכלי שנע
     */
    private Move findSafeCheckingMove(GameState state, ArrayList<Move> allMoves) {
        for (Move move : allMoves) {
            // דלג על מהלכי מלך
            if (move.get_piece().getPieceType() == Piece.KING) continue;

            int oldRow = move.get_piece().getRow();
            int oldCol = move.get_piece().getCol();
            Piece captured = simulateMoveLocally(state, move);

            boolean givesCheck = state.isKingInCheck(Color.white);

            if (givesCheck) {
                // בדוק אם הכלי שנע בסכנה אחרי המהלך
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

    /**
     * מדרג מהלכים לפי כמה הם מגבילים את ניידות המלך הלבן
     */
    private Move findKingCorneringMove(GameState state, ArrayList<Move> allMoves) {
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

            // ניידות מלך לאחר המהלך
            int kingMobility = countKingMobility(state, wRow, wCol, Color.white);
            // מרחק מלך מהפינה
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

    /**
     * סופר כמה משבצות יש למלך לברוח אליהן
     */
    private int countKingMobility(GameState state, int kingRow, int kingCol, Color kingColor) {
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

    private Move findPawnPromotionMove(GameState state, ArrayList<Move> allMoves) {
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

    private Move findKingCenterMove(GameState state, ArrayList<Move> allMoves) {
        for (Move move : allMoves) {
            if (move.get_piece().getPieceType() != Piece.KING) continue;
            int toRow = move.get_toRow(), toCol = move.get_toCol();
            if (toRow >= 3 && toRow <= 4 && toCol >= 3 && toCol <= 4) return move;
        }
        return null;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  בחירת המהלך הטוב ביותר — עם מניעת חזרות
    // ══════════════════════════════════════════════════════════════════════════
    private Move getBestMoveFromList(GameState state, ArrayList<Move> moves) {
        if (moves.isEmpty()) return null;

        Move bestMove  = null;
        int  bestScore = Integer.MIN_VALUE;

        for (Move move : moves) {
            if (leadToCheckmate(state, move)) return move;

            int oldRow = move.get_piece().getRow();
            int oldCol = move.get_piece().getCol();
            Piece captured = simulateMoveLocally(state, move);

            int score = evaluator.evaluate(state, isMidgamePhase(state));

            boolean queenHanged = false;
            for (Byte pos : state.getBlackQueens()) {
                int r = GameState.getRowFromByte(pos);
                int c = GameState.getColFromByte(pos);
                if (evaluator.isSquareUnderAttack(state, r, c, Color.black)) {
                    Piece attacker = evaluator.getLowestValueAttacker(state, r, c, Color.white);
                    if (attacker != null) {
                        // אם המלכה מותקפת ולא מוגנת, או מותקפת על ידי כלי זול ממנה
                        if (!evaluator.isSquareDefended(state, r, c, Color.black) || 
                            evaluator.getPieceValue(attacker.getPieceType()) < Piece.QUEEN) {
                            queenHanged = true;
                            break;
                        }
                    }
                }
            }
            if (queenHanged) {
                score -= 9000; // עונש קטלני על איבוד מלכה בחינם
            }
            // בונוס על מתן שח
            if (state.isKingInCheck(Color.white)) score += 60;

            // בדיקה אם הכלי בסכנה
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

            // ── מניעת חזרות חכמה (תמונת מצב שחורה בלבד) ──
            String futureBlackFormation = getBlackFormation(state);
            if (_pastBlackFormations.contains(futureBlackFormation)) {
                score -= 8000; // עונש עצום שמונע ממנו לבחור במהלך הזה
            }

            // קצת אקראיות
            score += (int)(Math.random() * 6);

            undoMoveSimulationLocally(state, move, captured, oldRow, oldCol);

            if (score > bestScore) {
                bestScore = score;
                bestMove  = move;
            }
        }
        return bestMove;
    }

    private boolean isMoveMakingProgress(GameState state, Move move) {
        boolean midgame = isMidgamePhase(state);
        int currentScore = evaluator.evaluate(state, midgame);
        int oldRow = move.get_piece().getRow();
        int oldCol = move.get_piece().getCol();
        Piece captured = simulateMoveLocally(state, move);
        int futureScore = evaluator.evaluate(state, midgame);
        undoMoveSimulationLocally(state, move, captured, oldRow, oldCol);
        return futureScore >= currentScore;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  עזרים
    // ══════════════════════════════════════════════════════════════════════════
    private ArrayList<Move> getAllLegalMoves(GameState state) {
        ArrayList<Move> allMoves = new ArrayList<>();
        byte[] pieceTypes = {Piece.PAWN, Piece.ROOK, Piece.KNIGHT, Piece.BISHOP, Piece.QUEEN, Piece.KING};

        // 1. איסוף כל המהלכים החוקיים
        for (byte pieceType : pieceTypes) {
            for (Byte position : state.getPiecePositions(pieceType, Color.black)) {
                int row = GameState.getRowFromByte(position);
                int col = GameState.getColFromByte(position);
                Piece p = state.get_board()[row][col];
                if (p != null) allMoves.addAll(p._allPossibleMoves);
            }
        }

        // 2. סינון מהלכים שגורמים לחזרה על מצב שהיה קודם (מונע הלוך-חזור של הצריח)
        ArrayList<Move> nonRepeatingMoves = new ArrayList<>();
        for (Move move : allMoves) {
            int oldRow = move.get_piece().getRow();
            int oldCol = move.get_piece().getCol();
            Piece captured = simulateMoveLocally(state, move);
            
            String futureBlackFormation = getBlackFormation(state);
            
            undoMoveSimulationLocally(state, move, captured, oldRow, oldCol);
            
            // אם הצורה הזו של הכלים השחורים לא הופיעה לאחרונה - המהלך תקין
            if (!_pastBlackFormations.contains(futureBlackFormation)) {
                nonRepeatingMoves.add(move);
            }
        }

        // מנגנון ביטחון: אם משום מה כל המהלכים האפשריים הם רק חזרות, 
        // נחזיר את הרשימה המלאה כדי שהבוט לא יקרוס
        if (nonRepeatingMoves.isEmpty()) {
            return allMoves;
        }

        return nonRepeatingMoves;
    }
    
    private ArrayList<Move> filterMoves(ArrayList<Move> allMoves, byte pieceType) {
        ArrayList<Move> filtered = new ArrayList<>();
        for (Move move : allMoves) {
            if (move.get_piece().getPieceType() == pieceType) filtered.add(move);
        }
        return filtered;
    }

    private Piece simulateMoveLocally(GameState state, Move move) {
        Piece captured = state.get_board()[move.get_toRow()][move.get_toCol()];
        int oldRow = move.get_piece().getRow();
        int oldCol = move.get_piece().getCol();

        if (captured != null) state.removePieceFromList(captured, move.get_toRow(), move.get_toCol());

        state.get_board()[move.get_toRow()][move.get_toCol()] = move.get_piece();
        state.get_board()[oldRow][oldCol] = null;
        move.get_piece().setRow(move.get_toRow());
        move.get_piece().setCol(move.get_toCol());
        state.updatePiecePosition(move.get_piece(), oldRow, oldCol, move.get_toRow(), move.get_toCol());

        return captured;
    }

    private void undoMoveSimulationLocally(GameState state, Move move, Piece captured, int oldRow, int oldCol) {
        state.get_board()[oldRow][oldCol] = move.get_piece();
        state.get_board()[move.get_toRow()][move.get_toCol()] = captured;
        move.get_piece().setRow(oldRow);
        move.get_piece().setCol(oldCol);
        state.updatePiecePosition(move.get_piece(), move.get_toRow(), move.get_toCol(), oldRow, oldCol);

        if (captured != null) state.addPieceToList(captured, move.get_toRow(), move.get_toCol());
    }
    
    private String getBlackFormation(GameState state) {
        StringBuilder sb = new StringBuilder();
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece p = state.get_board()[row][col];
                if (p != null && p.getColor() == Color.black) {
                    sb.append(p.getPieceType()).append(row).append(col);
                }
            }
        }
        return sb.toString();
    }
}