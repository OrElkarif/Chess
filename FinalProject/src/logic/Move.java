package logic;

import java.awt.Color;
import piece.Piece;

public class Move implements java.io.Serializable {
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

	public Move(Piece p, int fromC, int fromR, int toC, int toR) {
		_piece = p;
		_fromCol = fromC;
		_fromRow = fromR;
		_toCol = toC;
		_toRow = toR;
		_eatenPiece = null;
	}

	public boolean isValidMove(GameState state) {
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
	    
	    if (_eatenPiece != null && _piece.getColor() == _eatenPiece.getColor()) {
	        return false;
	    }
	    
	    if(!_piece.isValidMovement(_fromRow, _fromCol, _toRow, _toCol, _eatenPiece))
	        return false;
	    
	    if (_piece.isMoveOverAnotherPiece(state, _fromRow, _fromCol, _toRow, _toCol))
	        return false;

	    if(_piece.isPromotion(_toRow)) {
	        _isPromotion = true;
	    }
	    
	    return true;
	}
	
	public boolean CanMove(GameState state) {
	    if (!isValidMove(state))
	        return false;
	    
	    if (_isHazraha) {
	        return true; 
	    }
	    
	    Piece capturedPiece = state.get_board()[_toRow][_toCol];
	    int oldRow = _piece.getRow();
	    int oldCol = _piece.getCol();
	    
	    // הסר זמנית את הכלי האכול גם מהרשימה
	    if (capturedPiece != null) {
	        state.removePieceFromList(capturedPiece, _toRow, _toCol);
	    }
	    
	    // סימולציה: הזזת הכלי על הלוח
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

	public void DoThisMove(GameState state) {
	    if (_isEnPassant) {
	        state.get_board()[_enPassantCaptureRow][_enPassantCaptureCol] = null;
	    }
	    
	    if(_isHazraha) {
	        boolean isKingSide = (_toCol > _fromCol);
	        state.makeHazraha(this, isKingSide);
	    }
	    
	    // שלב 1: ביצוע התנועה הרגילה של הכלי (עדכון מיקומים ראשוני)
	    state.makeMove(this);

	    // שלב 2: הכתרה - טיפול מקיף
	    // תנאי: רגלי הגיע לקצה (דגל isPromotion) או רגלי שחור בשורה 7 / לבן בשורה 0
	    boolean isPawnAtEnd = _piece.getPieceType() == piece.Piece.PAWN
	            && ((_piece.getColor() == java.awt.Color.black && _toRow == 7)
	             || (_piece.getColor() == java.awt.Color.white && _toRow == 0));

	    if (_isPromotion || isPawnAtEnd) {
	        // אם לא נקבע כלי להכתרה - בחר מלכה אוטומטית לפי צבע הרגלי
	        if (_promoteTo == null) {
	            _promoteTo = new piece.Queen(_toCol, _toRow, _piece.getColor());
	        }

	        // א. הצבת הכלי המוכתר על הלוח
	        state.get_board()[_toRow][_toCol] = _promoteTo;
	        _promoteTo.setRow(_toRow);
	        _promoteTo.setCol(_toCol);

	        // ב. עדכון הרשימות (קריטי לבדיקות שח/שחמט!)
	        state.removePieceFromList(_piece, _toRow, _toCol);
	        state.addPieceToList(_promoteTo, _toRow, _toCol);

	        // ג. רענון מצב המשחק
	        state.set_isCheck(state.isKingInCheck(state.get_currentTurn()));
	        state.set_isCheckmate(state.isThereCheckmate(state.get_currentTurn()));
	    }
	}
	public Piece get_piece() {
		return _piece;
	}

	public void set_piece(Piece _piece) {
		this._piece = _piece;
	}

	public int get_fromCol() {
		return _fromCol;
	}

	public void set_fromCol(int _fromCol) {
		this._fromCol = _fromCol;
	}

	public int get_fromRow() {
		return _fromRow;
	}

	public void set_fromRow(int _fromRow) {
		this._fromRow = _fromRow;
	}

	public int get_toCol() {
		return _toCol;
	}

	public void set_toCol(int _toCol) {
		this._toCol = _toCol;
	}

	public int get_toRow() {
		return _toRow;
	}

	public void set_toRow(int _toRow) {
		this._toRow = _toRow;
	}

	public Piece get_eatenPiece() {
		return _eatenPiece;
	}

	public void set_eatenPiece(Piece _eatenPiece) {
		this._eatenPiece = _eatenPiece;
	}

	public boolean is_isHazraha() {
		return _isHazraha;
	}

	public void set_isHazraha(boolean _isHazraha) {
		this._isHazraha = _isHazraha;
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
	
	public boolean is_isEnPassant() {
	    return _isEnPassant;
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