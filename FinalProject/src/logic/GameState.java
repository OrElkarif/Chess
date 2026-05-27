package logic;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;

import piece.Bishop;
import piece.King;
import piece.Knight;
import piece.Pawn;
import piece.Piece;
import piece.Queen;
import piece.Rook;

public class GameState implements java.io.Serializable {//שומר את כל מצב הלוח הכלים וסטטוס השח
    private static final long serialVersionUID = 1L;//כדי שגאווה לא תיתן מספר אחר בכל פעם וככה אני אוכל לשנות את הקוד וגם שיישמר לי הקובץ גם אם השתנה הקוד, ליתר ביטחון
	private Color _currentTurn;//של מי התור הנוכחי
	private Piece _board[][]= new Piece [8][8];//עוקב אחרי הלוח
	private ArrayList<Move> _moveHistory;// עוקב אחרי המהלכים
	private ArrayList<Piece> _whiteWereEaten, _blackWereEaten; //איזה שחקנים נאכלו מכל צבע
	private boolean _isCheck; //לבדוק שח
	private boolean _isCheckmate;//לבדוק שחמט(ניצחון)
	private boolean _isStalemate; //תיקו
	private final HashMap<String, Integer> _boardPositionCount = new HashMap<>();
	private int _movesCount=0;
	public boolean _resetCounter=false;
	
//רשימת מיקומים של החיילים - 4 ספרות ימניות זה עמודה  , 4 שמאליות זה שורה
	private ArrayList<Byte> _whitePawns;
	private ArrayList<Byte> _blackPawns;
	private ArrayList<Byte> _whiteRooks;
	private ArrayList<Byte> _blackRooks;
	private ArrayList<Byte> _whiteKnights;
	private ArrayList<Byte> _blackKnights;
	private ArrayList<Byte> _whiteBishops;
	private ArrayList<Byte> _blackBishops;
	private ArrayList<Byte> _whiteQueens;
	private ArrayList<Byte> _blackQueens;
	private ArrayList<Byte> _whiteKings;
	private ArrayList<Byte> _blackKings;

	
	public GameState() {//מייצר לוח ריק ומסדר את הכלים בהתחלה או טוען משמירה קיימת
		set_currentTurn(Color.white);
		_moveHistory = new ArrayList<>();
	    set_whiteWereEaten(new ArrayList<>());
	    set_blackWereEaten(new ArrayList<>());
	    initPieceLists();
	    
		init_board();
		_boardPositionCount.merge(getBoardAsString(), 1, Integer::sum);
		
	}

	GameState(boolean initBoard) {//בנאי פנימי: initBoard=false ליצירת עמדות מותאמות ללא כלים
		set_currentTurn(Color.white);
		_moveHistory = new ArrayList<>();
		set_whiteWereEaten(new ArrayList<>());
		set_blackWereEaten(new ArrayList<>());
		initPieceLists();
		if (initBoard) {
			init_board();
			_boardPositionCount.merge(getBoardAsString(), 1, Integer::sum);
		}
	}

	public static GameState createMidgamePosition() {//בונה עמדת אמצע משחק: Italian Game עם איום טקטי Ng5 מאיים Nxf7
		// לבן: רץ c4 + פרש f3 — מהלך מנצח: 1.Ng5! מאיים Nxf7 (מזלג מלכה+צריח) ואחריו מט ב-3-4
		GameState state = new GameState(false);
		Piece[][] b = state._board;

		// --- כלים שחורים ---
		b[0][6] = new King(6, 0, Color.black);    state.addPieceToList(b[0][6], 0, 6);
		b[0][0] = new Rook(0, 0, Color.black);    state.addPieceToList(b[0][0], 0, 0);
		b[0][4] = new Rook(4, 0, Color.black);    state.addPieceToList(b[0][4], 0, 4);
		b[1][3] = new Queen(3, 1, Color.black);   state.addPieceToList(b[1][3], 1, 3);
		b[2][2] = new Knight(2, 2, Color.black);  state.addPieceToList(b[2][2], 2, 2);
		b[2][5] = new Knight(5, 2, Color.black);  state.addPieceToList(b[2][5], 2, 5);
		b[1][0] = new Pawn(0, 1, Color.black);    state.addPieceToList(b[1][0], 1, 0);
		b[1][1] = new Pawn(1, 1, Color.black);    state.addPieceToList(b[1][1], 1, 1);
		b[1][5] = new Pawn(5, 1, Color.black);    state.addPieceToList(b[1][5], 1, 5);
		b[1][6] = new Pawn(6, 1, Color.black);    state.addPieceToList(b[1][6], 1, 6);
		b[1][7] = new Pawn(7, 1, Color.black);    state.addPieceToList(b[1][7], 1, 7);
		b[3][3] = new Pawn(3, 3, Color.black);    state.addPieceToList(b[3][3], 3, 3);
		b[3][4] = new Pawn(4, 3, Color.black);    state.addPieceToList(b[3][4], 3, 4);

		// --- כלים לבנים ---
		b[7][6] = new King(6, 7, Color.white);    state.addPieceToList(b[7][6], 7, 6);
		b[7][3] = new Queen(3, 7, Color.white);   state.addPieceToList(b[7][3], 7, 3);
		b[7][0] = new Rook(0, 7, Color.white);    state.addPieceToList(b[7][0], 7, 0);
		b[7][5] = new Rook(5, 7, Color.white);    state.addPieceToList(b[7][5], 7, 5);
		b[4][2] = new Bishop(2, 4, Color.white);  state.addPieceToList(b[4][2], 4, 2); // c4 — מכוון ל-f7
		b[5][5] = new Knight(5, 5, Color.white);  state.addPieceToList(b[5][5], 5, 5); // f3 — Ng5 תקיפה מרכזית
		b[6][0] = new Pawn(0, 6, Color.white);    state.addPieceToList(b[6][0], 6, 0);
		b[6][1] = new Pawn(1, 6, Color.white);    state.addPieceToList(b[6][1], 6, 1);
		b[5][2] = new Pawn(2, 5, Color.white);    state.addPieceToList(b[5][2], 5, 2);
		b[4][4] = new Pawn(4, 4, Color.white);    state.addPieceToList(b[4][4], 4, 4);
		b[6][5] = new Pawn(5, 6, Color.white);    state.addPieceToList(b[6][5], 6, 5);
		b[6][6] = new Pawn(6, 6, Color.white);    state.addPieceToList(b[6][6], 6, 6);
		b[6][7] = new Pawn(7, 6, Color.white);    state.addPieceToList(b[6][7], 6, 7);

		state._boardPositionCount.merge(state.getBoardAsString(), 1, Integer::sum);
		return state;
	}
	

	private void initPieceLists() {//מאתחלת את הרשימות המהירות ששומרות את המיקומים של כל הכלים
		_whitePawns = new ArrayList<>();
		_blackPawns = new ArrayList<>();
		_whiteRooks = new ArrayList<>();
		_blackRooks = new ArrayList<>();
		_whiteKnights = new ArrayList<>();
		_blackKnights = new ArrayList<>();
		_whiteBishops = new ArrayList<>();
		_blackBishops = new ArrayList<>();
		_whiteQueens = new ArrayList<>();
		_blackQueens = new ArrayList<>();
		_whiteKings = new ArrayList<>();
		_blackKings = new ArrayList<>();
	}
	

	public static byte createPositionByte(int row, int col) {//הופכת שורה ועמודה למשתנה אחד קטן של בייט
		return (byte)((row << 4) | col);
	}
	

	public static int getRowFromByte(byte position) {//מחלצת את השורה מתוך המשתנה
		return (position >> 4) & 0x0F;
	}
	

	public static int getColFromByte(byte position) {//מחלצת את העמודה מתוך המשתנה
		return position & 0x0F;
	}
	

	public void addPieceToList(Piece piece, int row, int col) {//מוסיפה כלי חדש לרשימת המעקב הספציפית של הסוג והצבע שלו
		byte position = createPositionByte(row, col);
		byte type = piece.getPieceType();
		Color color = piece.getColor();
		
		if (color == Color.white) {
			switch(type) {
				case Piece.PAWN:   _whitePawns.add(position);   break;
				case Piece.ROOK:   _whiteRooks.add(position);   break;
				case Piece.KNIGHT: _whiteKnights.add(position); break;
				case Piece.BISHOP: _whiteBishops.add(position); break;
				case Piece.QUEEN:  _whiteQueens.add(position);  break;
				case Piece.KING:   _whiteKings.add(position);   break;
			}
		} else {
			switch(type) {
				case Piece.PAWN:   _blackPawns.add(position);   break;
				case Piece.ROOK:   _blackRooks.add(position);   break;
				case Piece.KNIGHT: _blackKnights.add(position); break;
				case Piece.BISHOP: _blackBishops.add(position); break;
				case Piece.QUEEN:  _blackQueens.add(position);  break;
				case Piece.KING:   _blackKings.add(position);   break;
			}
		}
	}
	
	
	public void removePieceFromList(Piece piece, int row, int col) {//מוחקת כלי מהרשימה כשהוא נאכל
		byte position = createPositionByte(row, col);
		byte type = piece.getPieceType();
		Color color = piece.getColor();
		
		if (color == Color.white) {
			switch(type) {
				case Piece.PAWN:   _whitePawns.remove((Object)position);   break;
				case Piece.ROOK:   _whiteRooks.remove((Object)position);   break;
				case Piece.KNIGHT: _whiteKnights.remove((Object)position); break;
				case Piece.BISHOP: _whiteBishops.remove((Object)position); break;
				case Piece.QUEEN:  _whiteQueens.remove((Object)position);  break;
				case Piece.KING:   _whiteKings.remove((Object)position);   break;
			}
		} else {
			switch(type) {
				case Piece.PAWN:   _blackPawns.remove((Object)position);   break;
				case Piece.ROOK:   _blackRooks.remove((Object)position);   break;
				case Piece.KNIGHT: _blackKnights.remove((Object)position); break;
				case Piece.BISHOP: _blackBishops.remove((Object)position); break;
				case Piece.QUEEN:  _blackQueens.remove((Object)position);  break;
				case Piece.KING:   _blackKings.remove((Object)position);   break;
			}
		}
	}
	

	public void updatePiecePosition(Piece piece, int oldRow, int oldCol, int newRow, int newCol) {//מעדכנת את המיקום החדש של הכלי ברשימות המעקב
		removePieceFromList(piece, oldRow, oldCol);
		addPieceToList(piece, newRow, newCol);
	}
	

	

	public ArrayList<Byte> getPiecePositions(byte pieceType, Color color) {//מחזירה את רשימת המיקומים המעודכנת של סוג כלי מסוים לפי הצבע שלו
		if (color == Color.white) {
			switch(pieceType) {
				case Piece.PAWN:   return _whitePawns;
				case Piece.ROOK:   return _whiteRooks;
				case Piece.KNIGHT: return _whiteKnights;
				case Piece.BISHOP: return _whiteBishops;
				case Piece.QUEEN:  return _whiteQueens;
				case Piece.KING:   return _whiteKings;
			}
		} else {
			switch(pieceType) {
				case Piece.PAWN:   return _blackPawns;
				case Piece.ROOK:   return _blackRooks;
				case Piece.KNIGHT: return _blackKnights;
				case Piece.BISHOP: return _blackBishops;
				case Piece.QUEEN:  return _blackQueens;
				case Piece.KING:   return _blackKings;
			}
		}
		return new ArrayList<>();
	}
	
	public void init_board() {//מסדרת פיזית את כל הכלים בעמדות הפתיחה שלהם
		for(int i =0;i<_board.length;i++) {
			for(int j=0;j<_board.length;j++) {
				if(i==1) {
					_board[i][j]= new Pawn(j, i, Color.black);
					addPieceToList(_board[i][j], i, j);
				}
				if(i==6) {
					_board[i][j] = new Pawn(j, i, Color.white);
					addPieceToList(_board[i][j], i, j);
				}
			}
					
		}
		//black
	    _board[0][0] = new Rook(0, 0, Color.black);
	    addPieceToList(_board[0][0], 0, 0);
	    
	    _board[0][1] = new Knight(1, 0, Color.black);
	    addPieceToList(_board[0][1], 0, 1);
	    
	    _board[0][2] = new Bishop(2, 0, Color.black);
	    addPieceToList(_board[0][2], 0, 2);
	    
	    _board[0][3] = new Queen(3, 0, Color.black);
	    addPieceToList(_board[0][3], 0, 3);
	    
	    _board[0][4] = new King(4, 0, Color.black);
	    addPieceToList(_board[0][4], 0, 4);
	    
	    _board[0][5] = new Bishop(5, 0, Color.black);
	    addPieceToList(_board[0][5], 0, 5);
	    
	    _board[0][6] = new Knight(6, 0, Color.black);
	    addPieceToList(_board[0][6], 0, 6);
	    
	    _board[0][7] = new Rook(7, 0, Color.black);
	    addPieceToList(_board[0][7], 0, 7);
	    
	    //white
	    _board[7][0] = new Rook(0, 7, Color.white);
	    addPieceToList(_board[7][0], 7, 0);
	    
	    _board[7][1] = new Knight(1, 7, Color.white);
	    addPieceToList(_board[7][1], 7, 1);
	    
	    _board[7][2] = new Bishop(2, 7, Color.white);
	    addPieceToList(_board[7][2], 7, 2);
	    
	    _board[7][3] = new Queen(3, 7, Color.white);
	    addPieceToList(_board[7][3], 7, 3);
	    
	    _board[7][4] = new King(4, 7, Color.white);
	    addPieceToList(_board[7][4], 7, 4);
	    
	    _board[7][5] = new Bishop(5, 7, Color.white);
	    addPieceToList(_board[7][5], 7, 5);
	    
	    _board[7][6] = new Knight(6, 7, Color.white);
	    addPieceToList(_board[7][6], 7, 6);
	    
	    _board[7][7] = new Rook(7, 7, Color.white);
	    addPieceToList(_board[7][7], 7, 7);
	}
	
	
	public void switchTurn() {//מעבירה את התור משחקן אחד לשחקן השני אחרי כל מהלך
		set_currentTurn((get_currentTurn() == Color.white) ? Color.black : Color.white);
	}
	
	public Piece getPiecePlace(int row, int col) {//מביאה את הכלי שנמצא במשבצת ספציפית שביקשנו או מחזירה כלום אם ריק
		if(_board[row][col]==null) {
			return null;
		}
		return _board[row][col];
	}

	

	
	public void makeHazraha(Move move,boolean kind) {//הזיז כבר את המלך נשאר רק להזיז את הצריח
		    if(kind) {//אם לבן
		        Piece rook = _board[move.get_fromRow()][7];
		        
		        updatePiecePosition(rook, move.get_fromRow(), 7, move.get_fromRow(), 5);
		        
		        _board[move.get_fromRow()][5] = rook;
		        _board[move.get_fromRow()][7] = null;
		        rook.setCol(5); 
		        rook.setRow(move.get_fromRow());
		    }
		    else {//אם שחור
		        Piece rook = _board[move.get_fromRow()][0];
		        
		        updatePiecePosition(rook, move.get_fromRow(), 0, move.get_fromRow(), 3);
		        
		        _board[move.get_fromRow()][3] = rook;
		        _board[move.get_fromRow()][0] = null;
		        rook.setCol(3);
		        rook.setRow(move.get_fromRow());
		    }
		}
	
	public void makeMove(Move move) {//עושה את המהלך המלא על הלוח אוכלת כלים אם צריך ומעדכנת רשימות
	    Piece eaten = move.get_eatenPiece();
	    if (eaten != null) {
	        removePieceFromList(eaten, eaten.getRow(), eaten.getCol());//מוחק את החתיכה מהרשימות של הלוח
	        
	        if (eaten.color == Color.white) {
	            get_whiteWereEaten().add(eaten);
	        } else {
	            get_blackWereEaten().add(eaten);
	        }
	        _resetCounter = true;//בשביל החמישים חזרות
	    }
	    move.get_piece().haveMoved(this);
	    if (_resetCounter) {
	        set_movesCount(0);
	        _resetCounter= false;
	    } else {
	        set_movesCount(get_movesCount() + 1);
	    }
	    
	    updatePiecePosition(move.get_piece(), move.get_fromRow(), move.get_fromCol(), 
	                       move.get_toRow(), move.get_toCol());//מעדכן את המיקום של החייל 
	    
	    move.get_piece().MakeMovement(this, move);//מזיז את הכלי בלוח של הממשק הגרפי ושומר את המיקום שלו באובייקט הספציפי של החתיכה
	    switchTurn();
	    _boardPositionCount.merge(getBoardAsString(), 1, Integer::sum);//מוסיף לטבלת גיבוב את התור הנוכחי בשביל בדיקת 3 חזרות
	    _isCheck = isKingInCheck(_currentTurn);

	    if (checkNotEnoughPieces()) {
			 System.out.println("not enough pieces on the board - STALEMATE");
		     _isStalemate = true;
		     return;
		 }

	 _isCheckmate = isThereCheckmate(_currentTurn);

	 if (!_isCheck) {
	     _isStalemate = isGameDraw(_currentTurn);
	 }

	    
	}
	
	

	

	public ArrayList<Move> get_moveHistory() {//גישה להיסטוריה המלאה של כל המהלכים ששוחקו במשחק
		return _moveHistory;
	}

	public boolean isKingInCheck(Color kingColor) {//סורקת  את הלוח דרך הרשימות כדי להבין אם המלך כרגע מאוים
	    ArrayList<Byte> kingList = (kingColor == Color.white) ? _whiteKings : _blackKings;
	    
	    if (kingList.isEmpty()) return false;
	    
	    byte kingPosition = kingList.get(0);
	    int kingRow = getRowFromByte(kingPosition);
	    int kingCol = getColFromByte(kingPosition);

	    Color enemyColor = (kingColor == Color.white) ? Color.black : Color.white;
	    byte[] pieceTypes = {Piece.PAWN, Piece.ROOK, Piece.KNIGHT, Piece.BISHOP, Piece.QUEEN, Piece.KING};
	    
	    for (byte pieceType : pieceTypes) {
	        ArrayList<Byte> enemyPositions = getPiecePositions(pieceType, enemyColor);
	        
	        for (Byte position : enemyPositions) {
	            int r = getRowFromByte(position);
	            int c = getColFromByte(position);
	            Piece enemyPiece = _board[r][c];
	            
	            if (enemyPiece != null) {
	                Move attackMove = new Move(enemyPiece, c, r, kingCol, kingRow);
	                
	                if (attackMove.isValidMove(this)) {
	                    return true; 
	                }
	            }
	        }
	    }
	    return false;
	}
	public boolean isThereCheckmate(Color color) {//מדמה כל מהלך אפשרי — רק אם מהלך כלשהו מסיר את השח זה לא שח-מט
		if (!isKingInCheck(color)) return false;

		ArrayList<Byte> kingList = (color == Color.white) ? _whiteKings : _blackKings;
		byte[] pieceTypes = {Piece.PAWN, Piece.ROOK, Piece.KNIGHT, Piece.BISHOP, Piece.QUEEN, Piece.KING};

		for (byte pieceType : pieceTypes) {
		    for (Byte position : new ArrayList<>(getPiecePositions(pieceType, color))) {
		        int row = getRowFromByte(position);
		        int col = getColFromByte(position);
		        Piece p = _board[row][col];
		        if (p == null) continue;

		        p._allPossibleMoves.clear();
		        p.PossibleMoves(this);

		        for (Move move : new ArrayList<>(p._allPossibleMoves)) {
		            int toRow = move.get_toRow();
		            int toCol = move.get_toCol();

		            // סימולציה קלה: עדכון לוח ומיקום הכלי בלבד
		            Piece captured = _board[toRow][toCol];
		            _board[toRow][toCol] = p;
		            _board[row][col] = null;
		            byte savedRow = p.row, savedCol = p.col;
		            p.row = (byte) toRow;
		            p.col = (byte) toCol;

		            // אם המלך עצמו זז — מעדכן את הרשימה כדי שisKingInCheck ימצא אותו נכון
		            Byte newKingPos = null;
		            if (pieceType == Piece.KING) {
		                kingList.remove(position);
		                newKingPos = createPositionByte(toRow, toCol);
		                kingList.add(newKingPos);
		            }

		            boolean stillInCheck = isKingInCheck(color);

		            // undo: מחזיר הכל לקדמותו
		            if (pieceType == Piece.KING) {
		                kingList.remove(newKingPos);
		                kingList.add(position);
		            }
		            _board[row][col] = p;
		            _board[toRow][toCol] = captured;
		            p.row = savedRow;
		            p.col = savedCol;

		            if (!stillInCheck) return false;//נמצא מהלך שמסיר שח — לא שח-מט
		        }
		    }
		}
		return true;//אין מהלך חוקי שמסיר את השח — שח-מט אמיתי
	}
	public boolean isGameDraw(Color color) {//בודקת את כל סוגי התיקו האפשריים כדי לעצור את המשחק
	    if (checkStalemate(color)) {
	        String turn;
	        turn = (get_currentTurn() == Color.white) ? "white" : "black"; 
	        System.out.println(turn + " cant move anywhere - STALEMATE");
	        return true;
	    }

	    if (checkNotEnoughPieces()) {
	        System.out.println("Not enough pieces to checkmate - STALEMATE");
	        return true;
	    }

	    if (check50Moves()) {
	        System.out.println("50 MOVES - STALEMATE");
	        return true;
	    }

	    if (check3Repetitions()) {
	        System.out.println("we've been here before, 3 times already - STALEMATE");
	        return true;
	    }

	    return false;
	}
	public boolean checkStalemate(Color color) {//בודקת מצב פט שבו שחקן לא יכול לזוז אבל המלך שלו  לא בשח
	    if (isKingInCheck(color)) return false;
	    byte[] pieceTypes = {Piece.PAWN, Piece.ROOK, Piece.KNIGHT, Piece.BISHOP, Piece.QUEEN, Piece.KING};
	    for (byte pieceType : pieceTypes) {
	        for (Byte position : new ArrayList<>(getPiecePositions(pieceType, color))) {
	            int row = getRowFromByte(position);
	            int col = getColFromByte(position);
	            Piece p = _board[row][col];
	            if (p == null) continue;
	            p._allPossibleMoves.clear();
	            p.PossibleMoves(this);
	            if (!p._allPossibleMoves.isEmpty()) return false;
	        }
	    }
	    return true;
	}
	
	public boolean checkNotEnoughPieces() {//בודקת אם נשארו רק שני מלכים או מעט מדי כלים כדי לעשות מט
        if (getWhitePawns().size() > 0 || getBlackPawns().size() > 0 ||
            getWhiteRooks().size() > 0 || getBlackRooks().size() > 0 ||
            getWhiteQueens().size() > 0 || getBlackQueens().size() > 0) {
            return false;
        }

        int minorPiecesCount = getWhiteKnights().size() + getWhiteBishops().size() +
                               getBlackKnights().size() + getBlackBishops().size();

        if (minorPiecesCount <= 1) {
            return true;
        }

        return false;
    }
	
	private boolean check50Moves() {//בודקת חוק חמישים המהלכים רצף ארוך בלי לאכול כלי או להזיז רגלי
	    return _movesCount >= 50;
	}
		
	private boolean check3Repetitions() {//מוודאת שאותה עמדה בדיוק לא חזרה על עצמה שלוש פעמים שזה גם תיקו
	    return _boardPositionCount.getOrDefault(getBoardAsString(), 0) >= 3;
	}

	
	
	

	public String getBoardAsString() {//ממירה את כל מצב הלוח למחרוזת כדי שיהיה אפשר להשוות עמדות מהעבר
	    StringBuilder sb = new StringBuilder();
	    
	    for (int row = 0; row < _board.length; row++) {
	        for (int col = 0; col < _board.length; col++) {
	            Piece p = _board[row][col];
	            if (p == null) {
	                sb.append("-");
	            } else {
	                sb.append(p.getColor() == Color.white ? 'w' : 'b');
	                sb.append(p.getPieceType()); 
	            }
	        }
	    }
	    
	    

	    
	    return sb.toString();
	}
	
	public void getAllPossibleMoves(Color color) {//מחזירה רשימה ענקית של כל המהלכים ששחקן מסוים רשאי לעשות עכשיו
	    byte[] pieceTypes = {Piece.PAWN, Piece.ROOK, Piece.KNIGHT, Piece.BISHOP, Piece.QUEEN, Piece.KING};
	    
	    for (byte pieceType : pieceTypes) {
	        ArrayList<Byte> positions = getPiecePositions(pieceType, color);
	        
	        ArrayList<Byte> positionsCopy = new ArrayList<>(positions);
	        
	        for (Byte position : positionsCopy) {
	            int row = getRowFromByte(position);
	            int col = getColFromByte(position);
	            Piece p = _board[row][col];
	            
	            if (p != null) {
	               p._allPossibleMoves.clear();
	               p.PossibleMoves(this);
	            }
	        }
	     }
	}
	

	public void fixColorReferences() {//מתקנת בעיות של כתובות בזיכרון של הצבעים כשקוראים מהיסטוריה או משמירה
	    if (Color.white.equals(this._currentTurn)) {
	        this._currentTurn = Color.white;
	    } else if (Color.black.equals(this._currentTurn)) {
	        this._currentTurn = Color.black;
	    }

	    for (int r = 0; r < 8; r++) {
	        for (int c = 0; c < 8; c++) {
	            Piece p = _board[r][c];
	            if (p != null) {
	                if (Color.white.equals(p.getColor())) {
	                    p.setColor(Color.white);
	                } else if (Color.black.equals(p.getColor())) {
	                    p.setColor(Color.black);
	                }
	            }
	        }
	    }
	    
	    for (Piece p : _whiteWereEaten) p.setColor(Color.white);
	    for (Piece p : _blackWereEaten) p.setColor(Color.black);
	}
	
	
	
	
	public Move getLastMove() {//מחזירה את המהלך האחרון בדיוק ששוחק על הלוח
	    if (_moveHistory == null || _moveHistory.isEmpty()) {
	        return null;
	    }
	    return _moveHistory.get(_moveHistory.size() - 1);
	}
	public boolean is_isCheck() {
		return _isCheck;
	}
	public void set_isCheck(boolean _isCheck) {
		this._isCheck = _isCheck;
	}
	public boolean is_isCheckmate() {
		return _isCheckmate;
	}
	public void set_isCheckmate(boolean _isCheckmate) {
		this._isCheckmate = _isCheckmate;
	}
	public boolean is_isStalemate() {
		return _isStalemate;
	}
	public void set_isStalemate(boolean _isStalemate) {
		this._isStalemate = _isStalemate;
	}
	public ArrayList<Piece> get_whiteWereEaten() {
		return _whiteWereEaten;
	}
	public void set_whiteWereEaten(ArrayList<Piece> _whiteWereEaten) {
		this._whiteWereEaten = _whiteWereEaten;
	}
	public ArrayList<Piece> get_blackWereEaten() {
		return _blackWereEaten;
	}
	public void set_blackWereEaten(ArrayList<Piece> _blackWereEaten) {
		this._blackWereEaten = _blackWereEaten;
	}
	public int get_movesCount() {
		return _movesCount;
	}
	public void set_movesCount(int _movesCount) {
		this._movesCount = _movesCount;
	}
	public Color get_currentTurn() {
		return _currentTurn;
	}
	public void set_currentTurn(Color _currentTurn) {
		this._currentTurn = _currentTurn;
	}
	public Piece[][] get_board() {
		return _board;
	}
	
	// Getters לרשימות הכלים
	public ArrayList<Byte> getWhitePawns() {
		return _whitePawns;
	}
	
	public ArrayList<Byte> getBlackPawns() {
		return _blackPawns;
	}
	
	public ArrayList<Byte> getWhiteRooks() {
		return _whiteRooks;
	}
	
	public ArrayList<Byte> getBlackRooks() {
		return _blackRooks;
	}
	
	public ArrayList<Byte> getWhiteKnights() {
		return _whiteKnights;
	}
	
	public ArrayList<Byte> getBlackKnights() {
		return _blackKnights;
	}
	
	public ArrayList<Byte> getWhiteBishops() {
		return _whiteBishops;
	}
	
	public ArrayList<Byte> getBlackBishops() {
		return _blackBishops;
	}
	
	public ArrayList<Byte> getWhiteQueens() {
		return _whiteQueens;
	}
	
	public ArrayList<Byte> getBlackQueens() {
		return _blackQueens;
	}
	
	public ArrayList<Byte> getWhiteKings() {
		return _whiteKings;
	}
	
	public ArrayList<Byte> getBlackKings() {
		return _blackKings;
	}
	
	public String getEndGameReason() {
	    if (_isCheckmate) {
	        return "BY CHECKMATE";
	    } else if (_isStalemate) {
	        if (checkNotEnoughPieces()) return "Draw: Not Enough Material";
	        if (check50Moves()) return "Draw: 50 Moves Rule";
	        if (check3Repetitions()) return "Draw: 3 Repetitions";
	        return "Draw: Stalemate";
	    }
	    return "";
	}
	
}