package logic;

import java.awt.Color;
import java.util.ArrayList;
import piece.Piece;

public class AIPlayer {

    private Node<TreeTask> rootNode; // שורש העץ מהסוג החדש והתקני!
    private OpeningBook openingBook;
    private Evaluator evaluator;

    public AIPlayer() {
        openingBook = new OpeningBook();
        evaluator = new Evaluator();
        buildStrategyTree();
    }

    // ==========================================
    // תשתית עזר פנימית לקיצור קוד המשימות
    // ==========================================
    private abstract class ConditionTask implements TreeTask {
        @Override public boolean isAction() { return false; }
        @Override public Move calculateMove(GameState state) { return null; }
    }

    private abstract class ActionTask implements TreeTask {
        @Override public boolean isAction() { return true; }
        @Override public boolean checkCondition(GameState state) { return false; }
    }

    // ==========================================
    // הגדרת המשימות (התוכן של הצמתים)
    // ==========================================
    class IsOpeningTask extends ConditionTask {
        @Override public boolean checkCondition(GameState state) { return !openingBook.isOpeningDone(); }
    }

    class IsMidgameTask extends ConditionTask {
        @Override public boolean checkCondition(GameState state) { return isMidgamePhase(state); }
    }

    class OpeningActionTask extends ActionTask {
        @Override public Move calculateMove(GameState state) { return openingBook.getNextMove(state, evaluator); }
    }

    class DefenseActionTask extends ActionTask {
        @Override public Move calculateMove(GameState state) { return actionDefense(state); }
    }

    class CaptureActionTask extends ActionTask {
        @Override public Move calculateMove(GameState state) { return actionCapture(state); }
    }

    class PositionalActionTask extends ActionTask {
        @Override public Move calculateMove(GameState state) { return actionPositional(state); }
    }

    class DefaultActionTask extends ActionTask {
        @Override public Move calculateMove(GameState state) { return getBestMoveFromList(state, getAllLegalMoves(state)); }
    }

    class EndgameActionTask extends ActionTask {
        @Override public Move calculateMove(GameState state) { return actionEndgame(state); }
    }

    // ==========================================
    // בניית העץ הגנרי החדש
    // ==========================================
    private void buildStrategyTree() {
        // עלים (Actions) - מקבלים רק את ה-Task. הגיבוי הוא היד הימנית (Right) של ה-Node.
        Node<TreeTask> endgameAction  = new Node<>(new EndgameActionTask());
        Node<TreeTask> defaultAction  = new Node<>(new DefaultActionTask());

        Node<TreeTask> positionAction = new Node<>(new PositionalActionTask(), null, defaultAction);
        Node<TreeTask> captureAction  = new Node<>(new CaptureActionTask(), null, positionAction);
        Node<TreeTask> defenseAction  = new Node<>(new DefenseActionTask(), null, captureAction);

        // שאלות (Questions) - ענף שמאלי (כן) וענף ימני (לא)
        Node<TreeTask> isMidgameNode  = new Node<>(new IsMidgameTask(), defenseAction, endgameAction);
        Node<TreeTask> openingAction  = new Node<>(new OpeningActionTask(), null, isMidgameNode);
        Node<TreeTask> openingDefense = new Node<>(new DefenseActionTask(), null, openingAction); 

        // השורש
        this.rootNode = new Node<>(new IsOpeningTask(), openingDefense, isMidgameNode);
    }

    // ==========================================
    // ריצה על העץ
    // ==========================================
    public Move chooseBestMove(GameState state) {
        state.getAllPossibleMoves(Color.black);
        Node<TreeTask> current = rootNode;

        while (current != null) {
            TreeTask task = current.getData(); // שולפים את המשימה מתוך הצומת
            
            if (task.isAction()) {
                Move move = task.calculateMove(state);
                if (move != null) return move;
                else current = current.getRight(); // fallback
            } else {
                current = task.checkCondition(state) ? current.getLeft() : current.getRight();
            }
        }
        return null;
    }

    // ==========================================
    // שאר הלוגיקה והפונקציות (בדיוק כמו שהיו)
    // ==========================================
    
    private boolean isMidgamePhase(GameState state) {
        int heavyPieces = state.getBlackQueens().size() + state.getWhiteQueens().size()
                        + state.getBlackRooks().size()  + state.getWhiteRooks().size();
        return heavyPieces >= 3;
    }

    // --- הגנה ---
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
        ArrayList<Move> allMoves = getAllLegalMoves(state);
        for (Move move : allMoves) {
            Piece piece = move.get_piece();
            if (evaluator.getPieceValue(piece.getPieceType()) < 300) continue;
            if (evaluator.isSquareUnderAttack(state, piece.getRow(), piece.getCol(), Color.black)) {
                if (!evaluator.isSquareUnderAttack(state, move.get_toRow(), move.get_toCol(), Color.black)) {
                    return move;
                }
            }
        }
        return null;
    }

    // --- תקיפה ---
    private Move actionCapture(GameState state) {
        ArrayList<Move> allMoves = getAllLegalMoves(state);
        ArrayList<Move> profitableMoves = new ArrayList<>();

        for (Move move : allMoves) {
            Piece target = state.get_board()[move.get_toRow()][move.get_toCol()];
            if (target == null) continue;

            int targetValue  = evaluator.getPieceValue(target.getPieceType());
            int attackerValue = evaluator.getPieceValue(move.get_piece().getPieceType());

            if (targetValue >= attackerValue) {
                if (leadToCheckmate(state, move)) return move;
                profitableMoves.add(move);
            }
            else if (isKnightVsBishop(move, target)) {
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
        byte mover  = move.get_piece().getPieceType();
        byte eaten  = target.getPieceType();
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

    // --- שיפור פוזיציה ---
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
            if (move.get_piece().getPieceType() == Piece.KING && Math.abs(move.get_toCol() - move.get_fromCol()) == 2) {
                return move; 
            }
        }
        return null;
    }

    private Move findPawnShieldMove(GameState state, ArrayList<Move> allMoves) {
        if (state.getBlackKings().isEmpty()) return null;
        byte kingPos  = state.getBlackKings().get(0);
        int  kingRow  = GameState.getRowFromByte(kingPos);
        int  kingCol  = GameState.getColFromByte(kingPos);

        for (Move move : allMoves) {
            if (move.get_piece().getPieceType() != Piece.PAWN) continue;
            int toRow = move.get_toRow();
            int toCol = move.get_toCol();
            if (toRow == kingRow + 1 && Math.abs(toCol - kingCol) <= 1) {
                return move;
            }
        }
        return null;
    }

    private Move findActiveRookMove(GameState state, ArrayList<Move> allMoves) {
        for (Move move : allMoves) {
            if (move.get_piece().getPieceType() != Piece.ROOK) continue;
            int toCol = move.get_toCol();
            int toRow = move.get_toRow();
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

    // --- סיום משחק ---
    private Move actionEndgame(GameState state) {
        ArrayList<Move> allMoves = getAllLegalMoves(state);

        Move promotionMove = findPawnPromotionMove(state, allMoves);
        if (promotionMove != null) return promotionMove;

        if (!state.getBlackQueens().isEmpty() || !state.getBlackRooks().isEmpty()) {
            Move matingMove = findKingCorneringMove(state, allMoves);
            if (matingMove != null) return matingMove;
        }

        Move kingCenterMove = findKingCenterMove(state, allMoves);
        if (kingCenterMove != null) return kingCenterMove;

        return getBestMoveFromList(state, allMoves);
    }

    private Move findPawnPromotionMove(GameState state, ArrayList<Move> allMoves) {
        Move bestPawnMove = null;
        int  maxRow = -1; 
        for (Move move : allMoves) {
            if (move.get_piece().getPieceType() != Piece.PAWN) continue;
            int toRow = move.get_toRow();
            if (toRow > maxRow) {
                maxRow = toRow;
                bestPawnMove = move; 
            }
        }
        return bestPawnMove;
    }

    private Move findKingCorneringMove(GameState state, ArrayList<Move> allMoves) {
        if (state.getWhiteKings().isEmpty()) return null;
        byte whiteKingPos = state.getWhiteKings().get(0);
        int  wRow = GameState.getRowFromByte(whiteKingPos);
        int  wCol = GameState.getColFromByte(whiteKingPos);

        Move bestMove   = null;
        int  maxCorner  = -1; 

        for (Move move : allMoves) {
            int oldRow = move.get_piece().getRow();
            int oldCol = move.get_piece().getCol();
            Piece captured = simulateMoveLocally(state, move);

            int distFromCenter = Math.abs(wRow - 3) + Math.abs(wCol - 3);
            int score = distFromCenter + evaluator.evaluate(state, false);

            undoMoveSimulationLocally(state, move, captured, oldRow, oldCol);

            if (score > maxCorner) {
                maxCorner = score;
                bestMove  = move;
            }
        }
        return bestMove;
    }

    private Move findKingCenterMove(GameState state, ArrayList<Move> allMoves) {
        for (Move move : allMoves) {
            if (move.get_piece().getPieceType() != Piece.KING) continue;
            int toRow = move.get_toRow();
            int toCol = move.get_toCol();
            if (toRow >= 3 && toRow <= 4 && toCol >= 3 && toCol <= 4) return move;
        }
        return null;
    }

    // --- מערכת הבחירה והסימולציה ---
    private ArrayList<Move> getAllLegalMoves(GameState state) {
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
        return allMoves;
    }

    private ArrayList<Move> filterMoves(ArrayList<Move> allMoves, byte pieceType) {
        ArrayList<Move> filtered = new ArrayList<>();
        for (Move move : allMoves) {
            if (move.get_piece().getPieceType() == pieceType) filtered.add(move);
        }
        return filtered;
    }

    private Move getBestMoveFromList(GameState state, ArrayList<Move> moves) {
        if (moves.isEmpty()) return null;

        Move bestMove = null;
        int bestScore = Integer.MIN_VALUE;

        for (Move move : moves) {
            int oldRow = move.get_piece().getRow();
            int oldCol = move.get_piece().getCol();
            Piece captured = simulateMoveLocally(state, move);
            
            int score = evaluator.evaluate(state, isMidgamePhase(state));
            
            Piece whiteAttacker = evaluator.getLowestValueAttacker(state, move.get_toRow(), move.get_toCol(), Color.white);
            
            if (whiteAttacker != null) {
                int myValue = evaluator.getPieceValue(move.get_piece().getPieceType());
                int attackerValue = evaluator.getPieceValue(whiteAttacker.getPieceType());
                Piece blackDefender = evaluator.getLowestValueAttacker(state, move.get_toRow(), move.get_toCol(), Color.black);

                if (blackDefender != null) {
                    if (myValue > attackerValue) score -= myValue; 
                } else {
                    if (move.get_piece().getPieceType() == Piece.QUEEN) score -= (myValue * 2); 
                    else score -= myValue;
                }
            }

            score += (int)(Math.random() * 6);
            undoMoveSimulationLocally(state, move, captured, oldRow, oldCol);

            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
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
}