package piece;

import java.awt.Color;

import logic.GameState;
import logic.Move;

public class Rook extends Piece implements java.io.Serializable {//מייצגת צריח
    private static final long serialVersionUID = 1L;
	public boolean _haveRookMoved= false;

	public Rook( int col, int row, Color color) {
		super(col,row,color);
		
		if(color == Color.white) {
			image = getImage("/piece/w-rook");
			
		}
		else
			image = getImage("/piece/b-rook");
	}
	
	public boolean isValidMovement(int fromRow, int fromCol, int toRow, int toCol, Piece eaten) {
		int colDiff = Math.abs(toCol - fromCol);
		int rowDiff = Math.abs(toRow - fromRow);
		if (rowDiff > 0 && colDiff > 0) {
			return false;
		}
		return true;
	}
	public boolean isMoveOverAnotherPiece(GameState state,int fromRow, int fromCol, int toRow, int toCol) {
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
	public void haveMoved(GameState state) {
		_haveRookMoved=true;
	}
	public byte getPieceType() { return ROOK; }
	
	
	
	
	public void PossibleMoves(GameState game) {
	    int[][] directions = {
	        {-1, 0},  
	        {1, 0},   
	        {0, -1},  
	        {0, 1}    
	    };
	    
	    for (int[] dir : directions) {
	        int toRow = row + dir[0];
	        int toCol = col + dir[1];
	        
	        while (toRow >= 0 && toRow <= 7 && toCol >= 0 && toCol <= 7) {
	            Move move = new Move(this, col, row, toCol, toRow);
	            if (move.CanMove(game)) {
	                _allPossibleMoves.add(move);
	                
	                if (game.getPiecePlace(toRow, toCol) != null) {
	                    break;
	                }
	            } else {
	                break;
	            }
	            
	            toRow += dir[0];
	            toCol += dir[1];
	        }
	    }
	}



}