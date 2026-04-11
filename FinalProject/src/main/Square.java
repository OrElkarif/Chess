package main;
import java.awt.Color;
import piece.Piece;

public class Square {
    public enum SoldierColor {
        EMPTY,  // 0 
        BLACK,  // 1
        WHITE   // 2
    }
    
    private int _row;
    private int _col;
    private Color _bgColor;
    private SoldierColor _soldierColor;
    private Piece _piece;  
    private boolean _isHighlighted;  
    
    public Square(int row, int col, Color bgColor, int soldier) {
        this._row = row;
        this._col = col;
        this._bgColor = bgColor;
        this._piece = null;
        this._isHighlighted = false;
        
        if (soldier == 0) {
            this._soldierColor = SoldierColor.EMPTY;
        } else if (soldier == 1) {
            this._soldierColor = SoldierColor.BLACK;
        } else if (soldier == 2) {
            this._soldierColor = SoldierColor.WHITE;
        } else {
            this._soldierColor = SoldierColor.EMPTY; 
        }
    }
    
    public Color getBgColor() {
        return _bgColor;
    }
    
    public int getRow() {
        return _row;
    }
    
    public int getCol() {
        return _col;
    }
    
    public Piece getPiece() {
        return _piece;
    }
    
    public SoldierColor getSoldierColor() {
        return _soldierColor;
    }
    
    public boolean isHighlighted() {
        return _isHighlighted;
    }
    
    public void setPiece(Piece piece) {
        this._piece = piece;
        if (piece != null) {
            if (piece.color == Color.white) {
                this._soldierColor = SoldierColor.WHITE;
            } else {
                this._soldierColor = SoldierColor.BLACK;
            }
        } else {
            this._soldierColor = SoldierColor.EMPTY;
        }
    }
    
    public void setHighlighted(boolean highlighted) {
        this._isHighlighted = highlighted;
    }
    
    public void setBgColor(Color color) {
        this._bgColor = color;
    }
    
    public boolean isEmpty() {
        return _piece == null;
    }
    
    public boolean hasPiece() {
        return _piece != null;
    }
    
    public boolean hasWhitePiece() {
        return _piece != null && _piece.color == Color.white;
    }
    
    public boolean hasBlackPiece() {
        return _piece != null && _piece.color == Color.black;
    }
    
    public void removePiece() {
        this._piece = null;
        this._soldierColor = SoldierColor.EMPTY;
    }
    
    public Color getHighlightColor() {
        if (_isHighlighted) {
            return new Color(186, 202, 68, 180);  
        }
        return _bgColor;
    }
}