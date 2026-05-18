package logic;

import java.awt.Color;
import piece.Piece;

public class Move implements java.io.Serializable {//שומרת בתוכה את כל הנתונים על צעד שמישהו שיחק
    private static final long serialVersionUID = 1L;
	public Piece _piece;
	private int _fromCol, _fromRow;
	private int _toCol, _toRow;
	private Piece _eatenPiece;
	private boolean _isHazraha;
	private boolean _isPromotion;
	private Piece _promoteTo;
	private boolean _isEnPassant; 
	private int _enPassantCaptureRow;
	private int _enPassantCaptureCol;

	public Move(Piece p, int fromC, int fromR, int toC, int toR) {//מאתחלת את הצעד עם כל המיקומים של מי זז ומי נאכל
		_piece = p;
		_fromCol = fromC;
		_fromRow = fromR;
		_toCol = toC;
		_toRow = toR;
		_eatenPiece = null;
	}

	public boolean isValidMove(GameState state) {//אם אפשר לסיים במשבצת הזאת
	    if (_toCol < 0 || _toCol > 7 || _toRow < 0 || _toRow > 7) {
	        return false;
	    }
	    
	    if(_piece.isHazraha(state, _fromRow, _fromCol, _toRow, _toCol)) {
	        if(_piece.isHazrahaLegal(state, _fromRow, _fromCol, _toRow, _toCol)) {
	            _isHazraha = true;
	            return true;
	        }
	        return false;
	    }
	    
	    if (_piece.isEnPassant(state, _fromRow, _fromCol, _toRow, _toCol, this)) {
	        _isEnPassant = true;
	        return true;
	    }
	    
	    _eatenPiece = state.getPiecePlace(_toRow, _toCol);
	    
	    if (_eatenPiece != null && _piece.getColor() == _eatenPiece.getColor()) {//בודק אם החייל הוא נאכל יעל ידי החבר שלו
	        return false;
	    }
	    
	    if(!_piece.isValidMovement(_fromRow, _fromCol, _toRow, _toCol, _eatenPiece))//בודק לוגיקה של החיילים
	        return false;
	    
	    if (_piece.isMoveOverAnotherPiece(state, _fromRow, _fromCol, _toRow, _toCol))
	        return false;

	    if(_piece.isPromotion(_toRow)) {
	        _isPromotion = true;
	    }
	    
	    return true;
	}
	
	public boolean CanMove(GameState state) {//בודקת אם הלוח משאיר את המלך שלי במצב שח , מה שאי אפשר לקרות
	    if (!isValidMove(state))
	        return false;
	    
	    if (_isHazraha) {
	        return true; 
	    }
	    
	    Piece capturedPiece = state.get_board()[_toRow][_toCol];
	    int oldRow = _piece.getRow();
	    int oldCol = _piece.getCol();
	    
	    if (capturedPiece != null) {
	        state.removePieceFromList(capturedPiece, _toRow, _toCol);
	    }
	    
	    state.get_board()[_toRow][_toCol] = _piece;
	    state.get_board()[_fromRow][_fromCol] = null;
	    _piece.setRow(_toRow);
	    _piece.setCol(_toCol);
	    
	    state.updatePiecePosition(_piece, oldRow, oldCol, _toRow, _toCol);
	    
	    boolean stillInCheck = state.isKingInCheck(_piece.getColor());
	    
	    state.updatePiecePosition(_piece, _toRow, _toCol, oldRow, oldCol);
	    
	    // שחזור הלוח והכלים
	    _piece.setRow(oldRow);
	    _piece.setCol(oldCol);
	    state.get_board()[_fromRow][_fromCol] = _piece;
	    state.get_board()[_toRow][_toCol] = capturedPiece;
	    
	    // החזר את הכלי האכול לרשימה
	    if (capturedPiece != null) {
	        state.addPieceToList(capturedPiece, _toRow, _toCol);
	    }
	    
	    return !stillInCheck;
	}

	public void DoThisMove(GameState state) {//מעדכנת טכנית את מערך הלוח ומזיזה את הכלים בזיכרון של המהלך הזה
	    if (_isEnPassant) {
	        state.get_board()[_enPassantCaptureRow][_enPassantCaptureCol] = null;
	    }
	    
	    if(_isHazraha) {
	        boolean isKingSide = (_toCol > _fromCol);
	        state.makeHazraha(this, isKingSide);
	    }
	    
	    state.makeMove(this);

	    boolean isPawnAtEnd = _piece.getPieceType() == piece.Piece.PAWN//מנהלת את ההכתרות
	            && ((_piece.getColor() == java.awt.Color.black && _toRow == 7)
	             || (_piece.getColor() == java.awt.Color.white && _toRow == 0));

	    if (_isPromotion || isPawnAtEnd) {
	        if (_promoteTo == null) {
	            _promoteTo = new piece.Queen(_toCol, _toRow, _piece.getColor());
	        }

	        state.get_board()[_toRow][_toCol] = _promoteTo;
	        _promoteTo.setRow(_toRow);
	        _promoteTo.setCol(_toCol);

	        state.removePieceFromList(_piece, _toRow, _toCol);
	        state.addPieceToList(_promoteTo, _toRow, _toCol);

	        state.set_isCheck(state.isKingInCheck(state.get_currentTurn()));
	        state.set_isCheckmate(state.isThereCheckmate(state.get_currentTurn()));
	    }
	}
	public Piece get_piece() {
		return _piece;
	}


	public int get_fromCol() {
		return _fromCol;
	}


	public int get_fromRow() {
		return _fromRow;
	}


	public int get_toCol() {
		return _toCol;
	}


	public int get_toRow() {
		return _toRow;
	}


	public Piece get_eatenPiece() {
		return _eatenPiece;
	}


	public boolean is_isHazraha() {
		return _isHazraha;
	}


	public boolean is_isPromotion() {
		return _isPromotion;
	}

	public void set_isPromotion(boolean _isPromotion) {
		this._isPromotion = _isPromotion;
	}

	public Piece get_promoteTo() {
		return _promoteTo;
	}

	public void set_promoteTo(Piece _promoteTo) {
		this._promoteTo = _promoteTo;
	}
	

	public void set_isEnPassant(boolean _isEnPassant) {
	    this._isEnPassant = _isEnPassant;
	}
	
	public void setEnPassantCapture(int row, int col, Piece piece) {
	    this._enPassantCaptureRow = row;
	    this._enPassantCaptureCol = col;
	    this._eatenPiece = piece;
	}
}