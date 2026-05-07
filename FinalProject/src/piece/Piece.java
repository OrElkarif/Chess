package piece;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import logic.Move;
import logic.GameState;


public abstract class Piece implements java.io.Serializable {//מחלקת האב הכללית שכל כלי שחמט יורש ממנה את הדברים שעושים אותו כלי
    private static final long serialVersionUID = 1L;
    public transient BufferedImage image;
    public int x,y, col ,row;
	public Color color;
	public ArrayList<Move> _allPossibleMoves = new ArrayList<Move>();

	public static final byte PAWN   = 1;
	public static final byte ROOK   = 2;
	public static final byte KNIGHT = 3;
	public static final byte BISHOP = 4;
	public static final byte QUEEN  = 5;
	public static final byte KING   = 6;
	
	public Piece(int col, int row, Color color) {//בונה את האובייקט של הכלי עם צבע ומיקום
		this.col = col;
		this.row = row;
		this.color = color;
	}
	
	public abstract boolean isValidMovement(int fromRow, int fromCol, int toRow, int toCol, Piece eaten);
	public abstract boolean isMoveOverAnotherPiece(GameState state,int fromRow, int fromCol, int toRow, int toCol); 
	
	
	
	
    public void MakeMovement(GameState state, Move move) {//מעדכנת נטו את המיקום הפנימי של הכלי לאן שהוא הגיע
    	Piece[][] board = state.get_board();
    	board[move.get_toRow()][move.get_toCol()] = move.get_piece();
    	board[move.get_fromRow()][move.get_fromCol()] = null;
	    move.get_piece().setCol(move.get_toCol());
	    move.get_piece().setRow(move.get_toRow());
	    state.get_moveHistory().add(move);
	}
    	
    
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

	public abstract void PossibleMoves(GameState game);
	
	


public abstract byte getPieceType();
	
	public BufferedImage getImage(String imagePath) {//נותנת את האובייקט תמונה של הכלי שיהיה אפשר לצייר
		BufferedImage image = null;
		
		try {
			image = ImageIO.read(getClass().getResourceAsStream(imagePath + ".png"));
		}
		catch(IOException e){
			e.printStackTrace();
			
		}
		return image;
	}
	public void loadPieceImage() {//טוענת מהתיקיות במחשב את הפיקסלים של תמונת הכלי
	    String path = "/piece/";
	    path += (Color.white.equals(color)) ? "w-" : "b-";	    
	    switch(getPieceType()) {
	        case PAWN:   path += "pawn"; break;
	        case ROOK:   path += "rook"; break;
	        case KNIGHT: path += "knight"; break;
	        case BISHOP: path += "bishop"; break;
	        case QUEEN:  path += "queen"; break;
	        case KING:   path += "king"; break;
	    }
	    this.image = getImage(path);
	}
	
	public void draw(Graphics2D g) {//לוקחת את התמונה ושמה אותה על המסך במשבצת המתאימה
	    if (image == null) {
	        loadPieceImage();
	    }
	    g.drawImage(image, x, y, 100, 100, null);
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

	

	public Color getColor() {
		return color;
	}

	public void setColor(Color color) {
		this.color = color;
	}
	
}