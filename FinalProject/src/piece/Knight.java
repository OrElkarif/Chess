package piece;

import java.awt.Color;

import logic.GameState;
import logic.Move;

public class Knight extends Piece implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
	
	public Knight( int col, int row, Color color) {
		super(col,row,color);
		
		if(color == Color.white) {
			image = getImage("/piece/w-knight");			
		}
		else
			image = getImage("/piece/b-knight");	}
	
	public boolean isValidMovement(int fromRow, int fromCol, int toRow, int toCol, Piece eaten) {
		int colDiff = Math.abs(toCol - fromCol);
		int rowDiff = Math.abs(toRow - fromRow);
		boolean isLShape = (colDiff == 2 && rowDiff == 1) || (colDiff == 1 && rowDiff == 2);
		if (!isLShape)
			return false;
		
		return true;
	}
	public boolean isMoveOverAnotherPiece(GameState state,int fromRow, int fromCol, int toRow, int toCol) {
		return false;
	}	
	public void drawBoard() {
		System.out.print("knight "+charColor+"  \t"); 
		
	}

	public byte getPieceType() { return KNIGHT; }
	
	public void PossibleMoves(GameState game) {
	    int[][] knightMoves = {
	        {-2, -1}, {-2, 1},  
	        {-1, -2}, {-1, 2},  
	        {1, -2}, {1, 2},    
	        {2, -1}, {2, 1}     
	    };
	    
	    for (int[] moveOffset : knightMoves) {
	        int toRow = row + moveOffset[0];
	        int toCol = col + moveOffset[1];
	        
	        if (toRow >= 0 && toRow <= 7 && toCol >= 0 && toCol <= 7) {
	            Move move = new Move(this, col, row, toCol, toRow);
	            if (move.CanMove(game)) {
	                _allPossibleMoves.add(move);
	            }
	        }
	    }
	}
}