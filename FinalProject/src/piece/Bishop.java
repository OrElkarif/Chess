package piece;

import java.awt.Color;

import logic.GameState;
import logic.Move;

public class Bishop extends Piece implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
	
	public Bishop( int col, int row, Color color) {
		super(col,row,color);
		
		if(color == Color.white) {
			image = getImage("/piece/w-bishop");
			
		}
		else
			image = getImage("/piece/b-bishop");
	}
	public boolean isValidMovement(int fromRow, int fromCol, int toRow, int toCol, Piece eaten) {
		int colDiff = Math.abs(toCol - fromCol);
		int rowDiff = Math.abs(toRow - fromRow);
		if (colDiff != rowDiff) {
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
	public byte getPieceType() { return BISHOP; }
	
	
	
	public void PossibleMoves(GameState game) {
	    int[][] directions = {
	        {-1, -1}, 
	        {-1, 1},   
	        {1, -1},   
	        {1, 1}     
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