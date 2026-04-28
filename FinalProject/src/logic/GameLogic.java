package logic;

import java.awt.Color;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;

import piece.Bishop;
import piece.Knight;
import piece.Piece;
import piece.Queen;
import piece.Rook;

public class GameLogic {
	public GameState _state;
	private Player _whitePlayer;
	private Player _blackPlayer;
	public boolean _gameInProcess;
	
	
	public GameLogic(Player white, Player black) {
		_whitePlayer = white;
		_blackPlayer = black;
		_gameInProcess= true;
		_state = new GameState();
	}
	public GameLogic(Player white, Player black, GameState savedState) {
	    _whitePlayer = white;
	    _blackPlayer = black;
	    _gameInProcess = true;
	    _state = savedState;
	}
	
	public void startGame() {
		_state = new GameState();
		_gameInProcess= true;
		
	}
	
	public boolean PlayerMove(int fromRow, int fromCol, int toRow, int toCol) throws IllegalStateException {
	    Player currentPlayer = (_state.get_currentTurn() == Color.white) ? _whitePlayer : _blackPlayer;
	    if (!currentPlayer.isHuman()) {
	        return false; 
	    }
	    
	    Piece piece = _state.getPiecePlace(fromRow, fromCol);
	    
	    if(piece == null) {
	        System.out.println("No piece at this location");
	        return false;
	    }
	    
	    if(piece.getColor() != _state.get_currentTurn()) {
	        throw new IllegalStateException("You may only move your own tools");
	    }
	    
	    Move move = new Move(piece, fromCol, fromRow, toCol, toRow);
	    boolean isvalid = move.CanMove(_state);
	    
	    if(isvalid) {
	        // לוגיקת קידום חייל (הצרחה וכו')
	        if(move.is_isPromotion()) {
	            System.out.println("Your pawn reached the end! Choose promotion:");
	            System.out.println("0 - Queen");
	            System.out.println("1 - Rook");
	            System.out.println("2 - Bishop");
	            System.out.println("3 - Knight");
	            
	            java.util.Scanner scanner = new java.util.Scanner(System.in);
	            int choice = scanner.nextInt();
	            
	            Piece promotedPiece = createPromotionPiece(choice, toCol, toRow, piece.getColor());
	            move.set_promoteTo(promotedPiece);
	        }
	        
	        move.DoThisMove(_state); 
	        
	        checkGameOver();
	        if (_gameInProcess) {
	            Player nextPlayer = (_state.get_currentTurn() == Color.white) ? _whitePlayer : _blackPlayer;
	            if (!nextPlayer.isHuman()) {
	                Move aiMove = nextPlayer.getMove(_state); 
	                if (aiMove != null) {
	                    aiMove.DoThisMove(_state);
	                }
	            }
	        }
	        
	        return true;
	    } else {
	        return false;
	    }
	}
	private Piece createPromotionPiece(int choice, int col, int row, Color color) {
	    switch(choice) {
	        case 0: return new Queen(col, row, color);
	        case 1: return new Rook(col, row, color);
	        case 2: return new Bishop(col, row, color);
	        case 3: return new Knight(col, row, color);
	        default: return new Queen(col, row, color);
	    }
	}
	


	public Queue<int[]> highlightPossibleMoves(int row, int col) {
	    Queue<int[]> possibleMoves = new ArrayDeque<>();
	    
	    Piece piece = _state.getPiecePlace(row, col);
	    
	    if(piece == null || piece.getColor() != _state.get_currentTurn()) {
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
		Color winner= null;
		if(_state.is_isCheckmate()) {
			winner = (_state.get_currentTurn() == Color.white)? Color.black: Color.white;
		}
		return winner;
	}

	public void resetGame() {
		startGame();
	}
}
