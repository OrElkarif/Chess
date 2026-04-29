package logic;

import java.awt.Color;

import java.util.ArrayDeque;
import java.util.Queue;

import piece.Piece;

public class GameLogic {
	public GameState _state;
	private boolean _isWhiteHuman;
	private boolean _isBlackHuman;
	public boolean _gameInProcess;

	public GameLogic(boolean isWhiteHuman, boolean isBlackHuman) {
		_isWhiteHuman = isWhiteHuman;
		_isBlackHuman = isBlackHuman;
		_gameInProcess = true;
		_state = new GameState();
	}

	public GameLogic(boolean isWhiteHuman, boolean isBlackHuman, GameState savedState) {
		_isWhiteHuman = isWhiteHuman;
		_isBlackHuman = isBlackHuman;
		_gameInProcess = true;
		_state = savedState;
	}

	public boolean PlayerMove(int fromRow, int fromCol, int toRow, int toCol) throws IllegalStateException {
		boolean currentIsHuman = (_state.get_currentTurn() == Color.white) ? _isWhiteHuman : _isBlackHuman;
		if (!currentIsHuman) {
			return false;
		}

		Piece piece = _state.getPiecePlace(fromRow, fromCol);

		if (piece == null) {
			return false;
		}

		if (piece.getColor() != _state.get_currentTurn()) {
			throw new IllegalStateException("You may only move your own tools");
		}

		Move move = new Move(piece, fromCol, fromRow, toCol, toRow);

		if (move.CanMove(_state)) {
			move.DoThisMove(_state);
			checkGameOver();
			return true;
		} else {
			return false;
		}
	}

	public Queue<int[]> highlightPossibleMoves(int row, int col) {
		Queue<int[]> possibleMoves = new ArrayDeque<>();

		Piece piece = _state.getPiecePlace(row, col);

		if (piece == null || piece.getColor() != _state.get_currentTurn()) {
			return possibleMoves;
		}

		piece._allPossibleMoves.clear();

		for (int toRow = 0; toRow < 8; toRow++) {
			for (int toCol = 0; toCol < 8; toCol++) {
				Move move = new Move(piece, col, row, toCol, toRow);

				if (move.CanMove(_state)) {
					piece._allPossibleMoves.add(move);
					possibleMoves.add(new int[]{toRow, toCol});
				}
			}
		}

		return possibleMoves;
	}

	public boolean checkGameOver() {
		if (_state.is_isCheckmate() || _state.is_isStalemate())
			_gameInProcess = false;
		return _gameInProcess;
	}

	public Color getWinner() {
		Color winner = null;
		if (_state.is_isCheckmate()) {
			winner = (_state.get_currentTurn() == Color.white) ? Color.black : Color.white;
		}
		return winner;
	}
}