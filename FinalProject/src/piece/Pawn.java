package piece;

import java.awt.Color;

import logic.GameState;
import logic.Move;


public class Pawn extends Piece implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
	public boolean _havePawnMoved= false;
	
	public Pawn( int col, int row, Color color) {
		super(col,row,color);
		
		if(color == Color.white) {
			image = getImage("/piece/w-pawn");			
		}
		else
			image = getImage("/piece/b-pawn");	
	}
	
	@Override
	public boolean isValidMovement(int fromRow, int fromCol, int toRow, int toCol, Piece eaten) {
		int colDiff = Math.abs(toCol - fromCol);
		int rowDiff = Math.abs(toRow - fromRow);
		
		if (color == Color.white) {
			if (toRow > fromRow) return false; 
		} else {
			if (toRow < fromRow) return false; 
		}

		if (rowDiff > 2 || colDiff > 1)
			return false;
		
		if (colDiff == 1 && rowDiff != 1)
			return false;

		if (colDiff == 0) { 
			if (eaten != null) return false; 

			if (rowDiff == 2) {
				if (_havePawnMoved) return false; 
			}
		}
		else { 
			if (eaten == null) return false; 
		}
		
		return true;
	}
	
	@Override
	public boolean isMoveOverAnotherPiece(GameState state, int fromRow, int fromCol, int toRow, int toCol) {
		int dCol = Integer.signum(toCol - fromCol);
		int dRow = Integer.signum(toRow - fromRow);

		int curCol = fromCol + dCol;
		int curRow = fromRow + dRow;

		while (curCol != toCol || curRow != toRow) {
			
			if (curCol < 0 || curCol > 7 || curRow < 0 || curRow > 7) return true;

			if (state.getPiecePlace(curRow, curCol) != null) {
				return true;
			}

			curCol += dCol;
			curRow += dRow;
		}

		return false;
	}
	
	@Override
	public boolean isEnPassant(GameState state, int fromRow, int fromCol, int toRow, int toCol, Move move) {
	    Move lastMove = state.getLastMove();
	    if (lastMove == null) {
	        return false;
	    }
	    
	    if (lastMove.get_piece().getPieceType() != PAWN) {
	        return false;
	    }
	    
	    int lastMoveDistance = Math.abs(lastMove.get_toRow() - lastMove.get_fromRow());
	    if (lastMoveDistance != 2) {
	        return false;
	    }
	    
	    if (fromRow != lastMove.get_toRow()) {
	        return false;
	    }
	    
	    if (Math.abs(fromCol - lastMove.get_toCol()) != 1) {
	        return false;
	    }
	    
	    int direction = (this.getColor() == Color.white) ? -1 : 1;
	    
	    if (toRow != fromRow + direction) {
	        return false;
	    }
	    
	    if (toCol != lastMove.get_toCol()) {
	        return false;
	    }
	    
	    int enPassantCaptureRow = lastMove.get_toRow();
	    int enPassantCaptureCol = lastMove.get_toCol();
	    Piece capturedPiece = state.getPiecePlace(enPassantCaptureRow, enPassantCaptureCol);
	    
	    move.setEnPassantCapture(enPassantCaptureRow, enPassantCaptureCol, capturedPiece);
	    
	    return true;
	}
	
	@Override
	public boolean isPromotion(int toRow) {
		if(color == Color.white) {
			return (toRow == 0);
		} else {
			return (toRow == 7);
		}
	}
	@Override 
	public void haveMoved(GameState state) {
		_havePawnMoved=true;
		state.resetCounter=true;
	}
	
	public byte getPieceType() { return PAWN; }
	
	
	public void PossibleMoves(GameState game) {
	    Move PotentialMove = null;
	    int direction = (color == Color.white) ? -1 : 1;
	    
	    int toRow = row + direction;
	    if (toRow >= 0 && toRow <= 7) {
	        PotentialMove = new Move(this, col, row, col, toRow);
	        if (PotentialMove.CanMove(game)) {
	            _allPossibleMoves.add(PotentialMove);
	        }
	        
	        if (!_havePawnMoved) {
	            toRow = row + (2 * direction);
	            if (toRow >= 0 && toRow <= 7) {
	                PotentialMove = new Move(this, col, row, col, toRow);
	                if (PotentialMove.CanMove(game)) {
	                    _allPossibleMoves.add(PotentialMove);
	                }
	            }
	        }
	    }
	    
	    toRow = row + direction;
	    if (toRow >= 0 && toRow <= 7) {
	        // תקיפה שמאלה
	        if (col - 1 >= 0) {
	            PotentialMove = new Move(this, col, row, col - 1, toRow);
	            if (PotentialMove.CanMove(game)) {
	                _allPossibleMoves.add(PotentialMove);
	            }
	        }
	        
	        // תקיפה ימינה
	        if (col + 1 <= 7) {
	            PotentialMove = new Move(this, col, row, col + 1, toRow);
	            if (PotentialMove.CanMove(game)) {
	                _allPossibleMoves.add(PotentialMove);
	            }
	        }
	    }
	}
}