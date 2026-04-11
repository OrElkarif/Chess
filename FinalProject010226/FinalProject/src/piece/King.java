package piece;

import java.awt.Color;

import logic.GameState;
import logic.Move;

public class King extends Piece{
	public boolean _haveKingMoved= false;

	public King( int col, int row, Color color) {
		super(col,row,color);
		
		if(color == Color.white) {
			image = getImage("/piece/w-king");
			
		}
		else
			image = getImage("/piece/b-king");
	}
	
	@Override
	public boolean isValidMovement(int fromRow, int fromCol, int toRow, int toCol, Piece eaten) {
		int colDiff = Math.abs(toCol - fromCol);
		int rowDiff = Math.abs(toRow - fromRow);
		if (rowDiff > 1 || colDiff > 1) {
			return false;
		}
		return true;
	}
	
	@Override
	public boolean isMoveOverAnotherPiece(GameState state, int fromRow, int fromCol, int toRow, int toCol) {
	    int colDiff = Math.abs(toCol - fromCol);
	    if (colDiff <= 1) {
	        return false;
	    }
	    
	    int direction = Integer.signum(toCol - fromCol);
	    int curCol = fromCol + direction;
	    
	    while (curCol != toCol) {
	        if (state.getPiecePlace(fromRow, curCol) != null) {
	            return true; 
	        }
	        curCol += direction;
	    }
	    
	    return false;
	}	
	
	@Override
	public boolean isHazraha(GameState state, int fromRow, int fromCol, int toRow, int toCol) {
		if(_haveKingMoved==true) {
			return false;
		}
		int colDiff = Math.abs(toCol - fromCol);
		int rowDiff = Math.abs(toRow - fromRow);
		if(colDiff==2 && rowDiff==0)
			return true;
		return false;
	}
	
	@Override
	public boolean isHazrahaLegal(GameState state, int fromRow, int fromCol, int toRow, int toCol) {
	    if (!isHazraha(state, fromRow, fromCol, toRow, toCol)) {
	        return false;
	    }
	    
	    Color kingColor = this.getColor();
	    
	    boolean isKingSide = (toCol > fromCol);
	    int rookCol = isKingSide ? 7 : 0;
	    
	    Piece rookPiece = state.getPiecePlace(fromRow, rookCol);
	    if (!(rookPiece instanceof Rook) || rookPiece.getColor() != kingColor) {
	        return false;
	    }
	    
	    Rook rook = (Rook) rookPiece;
	    if (rook._haveRookMoved) {
	        return false;
	    }
	    
	    if (state.isKingInCheck(kingColor)) {
	        return false;
	    }
	    
	    if(this.isMoveOverAnotherPiece(state, fromRow, fromCol, toRow, toCol)) {
	        return false;
	    }
	    
	    if(!isKingSide) {
	        if(rookPiece.isMoveOverAnotherPiece(state, fromRow, rookCol, fromRow, 3)) {
	            return false;
	        }
	    }
	    
	    int direction = isKingSide ? 1 : -1;
	    
	    for (int col = fromCol + direction; col != toCol + direction; col += direction) {
	        if (isSquareUnderAttack(state, fromRow, col, kingColor)) {
	            return false;
	        }
	    }
	    
	    return true;
	}
	
	private boolean isSquareUnderAttack(GameState state, int row, int col, Color kingColor) {
	    Color enemyColor = (kingColor == Color.white) ? Color.black : Color.white;
	    
	    for (int r = 0; r < 8; r++) {
	        for (int c = 0; c < 8; c++) {
	            Piece enemyPiece = state.get_board()[r][c];
	            
	            if (enemyPiece != null && enemyPiece.getColor() == enemyColor) {
	                if (enemyPiece.isValidMovement(r, c, row, col, state.getPiecePlace(row, col)) &&
	                    !enemyPiece.isMoveOverAnotherPiece(state, r, c, row, col)) {
	                    return true;
	                }
	            }
	        }
	    }
	    
	    return false;
	}
	@Override 
	public void haveMoved(GameState state) {
		_haveKingMoved=true;
	}
	public byte getPieceType() { return KING; }
	
	public void drawBoard() {
		System.out.print("King "+charColor+"  \t"); 
		
	}
	
	
	public void PossibleMoves(GameState game) {
	    int[][] kingMoves = {
	        {-1, -1}, {-1, 0}, {-1, 1},  
	        {0, -1},           {0, 1}, 
	        {1, -1},  {1, 0},  {1, 1} 
	    };
	    
	    for (int[] moveOffset : kingMoves) {
	        int toRow = row + moveOffset[0];
	        int toCol = col + moveOffset[1];
	        
	        if (toRow >= 0 && toRow <= 7 && toCol >= 0 && toCol <= 7) {
	            Move move = new Move(this, col, row, toCol, toRow);
	            if (move.CanMove(game)) {
	                _allPossibleMoves.add(move);
	            }
	        }
	    }
	    
	    if (!_haveKingMoved) {
	        Move kingsideCastle = new Move(this, col, row, col + 2, row);
	        if (kingsideCastle.CanMove(game)) {
	            _allPossibleMoves.add(kingsideCastle);
	        }
	        
	        Move queensideCastle = new Move(this, col, row, col - 2, row);
	        if (queensideCastle.CanMove(game)) {
	            _allPossibleMoves.add(queensideCastle);
	        }
	    }
	}

}