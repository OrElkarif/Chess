package piece;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import logic.Move;
import logic.GameState;


public abstract class Piece {
	public BufferedImage image;
	public int x,y, col ,row, preCol,preRow;
	public Color color;
	public ArrayList<Move> _allPossibleMoves = new ArrayList<Move>();
	public Piece[][] _boardToChange;
	public char charColor;

	
	public Piece(int col, int row, Color color) {
		this.col = col;
		this.row = row;
		this.color = color;
		this.x= getX(col);
		this.y = getY(row);
		this.charColor = (color==Color.white)? 'b':'w';
	}
	
	public abstract boolean isValidMovement(int fromRow, int fromCol, int toRow, int toCol, Piece eaten);
	public abstract boolean isMoveOverAnotherPiece(GameState state,int fromRow, int fromCol, int toRow, int toCol); 
	
	
	
	
    public void MakeMovement(GameState state,Move move) {
    	_boardToChange = state.get_board();
    	_boardToChange[move.get_toRow()][move.get_toCol()] = move.get_piece();
    	_boardToChange[move.get_fromRow()][move.get_fromCol()] = null;
    	
		  state.set_board(_boardToChange);
		    
		    move.get_piece().setCol(move.get_toCol());
		    move.get_piece().setRow(move.get_toRow());
		    
		   
		    state.get_moveHistory().add(move);
		
		}
    public abstract void drawBoard(); 
    	
    
	//פעולות שיידרסו במחלקות שרלוונטיות אליהן
	public boolean isHazraha(GameState state, int fromRow, int fromCol, int toRow, int toCol) {
		return false;
	}
	
	public boolean isHazrahaLegal(GameState state, int fromRow, int fromCol, int toRow, int toCol) {
		return false;
	}
	
	public boolean isEnPassant(GameState state, int fromRow, int fromCol, int toRow, int toCol, Move move) {
		return false;
	}
	
	public boolean isPromotion(int toRow) {
		return false;
	}
	
	public void haveMoved(GameState state) {
	}
	public boolean heavyTool() {
		return false;
	}
	public abstract void PossibleMoves(GameState game);
	
	
public static final byte PAWN   = 1;
public static final byte ROOK   = 2;
public static final byte KNIGHT = 3;
public static final byte BISHOP = 4;
public static final byte QUEEN  = 5;
public static final byte KING   = 6;

public abstract byte getPieceType();
	
	public BufferedImage getImage(String imagePath) {
		BufferedImage image = null;
		
		try {
			image = ImageIO.read(getClass().getResourceAsStream(imagePath + ".png"));
		}
		catch(IOException e){
			e.printStackTrace();
			
		}
		return image;
	}
	
	public int getCol() {
		return col;
	}

	public void setCol(int col) {
		this.col = col;
	}

	public int getRow() {
		return row;
	}

	public void setRow(int row) {
		this.row = row;
	}

	public int getX(int col) {
		return col* 100;
	}
	public int getY(int row) {
		return row* 100;
	}
	
	public void draw(Graphics2D g) {
		g.drawImage(image, x,y,100,100,null);
		
	}

	public Color getColor() {
		return color;
	}

	public void setColor(Color color) {
		this.color = color;
	}
	
}