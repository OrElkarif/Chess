package logic;
import java.awt.Color;
import java.util.ArrayList;

import piece.Bishop;
import piece.King;
import piece.Knight;
import piece.Pawn;
import piece.Piece;
import piece.Queen;
import piece.Rook;

public class GameState {
	private Color _currentTurn;//של מי התור הנוכחי
	private Piece _board[][]= new Piece [8][8];//עוקב אחרי הלוח
	private ArrayList<Move> _moveHistory;// עוקב אחרי המהלכים
	private ArrayList<Piece> _whiteWereEaten, _blackWereEaten; //איזה שחקנים נאכלו מכל צבע
	private boolean _isCheck; //לבדוק שח
	private boolean _isCheckmate;//לבדוק שחמט(ניצחון)
	private boolean _isStalemate; //תיקו
	private ArrayList<String> _boardHistory;
	private int _movesCount=0;
	public boolean resetCounter=false;
	
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

	
	public GameState() {
		set_boardHistory(new ArrayList<>());
		set_currentTurn(Color.white);
		_moveHistory = new ArrayList<>();
	    set_whiteWereEaten(new ArrayList<>());
	    set_blackWereEaten(new ArrayList<>());
	    initPieceLists();
	    
		init_board();
		get_boardHistory().add(getBoardAsString());		
		
	}
	

	private void initPieceLists() {
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
	

	public static byte createPositionByte(int row, int col) {
		return (byte)((row << 4) | col);
	}
	

	public static int getRowFromByte(byte position) {
		return (position >> 4) & 0x0F;
	}
	

	public static int getColFromByte(byte position) {
		return position & 0x0F;
	}
	

	public void addPieceToList(Piece piece, int row, int col) {
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
	
	
	public void removePieceFromList(Piece piece, int row, int col) {
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
	

	public void updatePiecePosition(Piece piece, int oldRow, int oldCol, int newRow, int newCol) {
		removePieceFromList(piece, oldRow, oldCol);
		addPieceToList(piece, newRow, newCol);
	}
	

	public boolean hasPieceAt(byte pieceType, Color color, int row, int col) {
		byte position = createPositionByte(row, col);
		
		if (color == Color.white) {
			switch(pieceType) {
				case Piece.PAWN:   return _whitePawns.contains(position);
				case Piece.ROOK:   return _whiteRooks.contains(position);
				case Piece.KNIGHT: return _whiteKnights.contains(position);
				case Piece.BISHOP: return _whiteBishops.contains(position);
				case Piece.QUEEN:  return _whiteQueens.contains(position);
				case Piece.KING:   return _whiteKings.contains(position);
			}
		} else {
			switch(pieceType) {
				case Piece.PAWN:   return _blackPawns.contains(position);
				case Piece.ROOK:   return _blackRooks.contains(position);
				case Piece.KNIGHT: return _blackKnights.contains(position);
				case Piece.BISHOP: return _blackBishops.contains(position);
				case Piece.QUEEN:  return _blackQueens.contains(position);
				case Piece.KING:   return _blackKings.contains(position);
			}
		}
		return false;
	}
	

	public ArrayList<Byte> getPiecePositions(byte pieceType, Color color) {
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
	
	public void init_board() {
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
	
	
	public void switchTurn() {
		set_currentTurn((get_currentTurn() == Color.white) ? Color.black : Color.white);
	}
	
	public Piece getPiecePlace(int row, int col) {
		if(_board[row][col]==null) {
			return null;
		}
		return _board[row][col];
	}

	
	public boolean isValidMovement(Move move) {
		return move.isValidMove(this);
	}
	
	public void makeHazraha(Move move,boolean kind) {//הזיז כבר את המלך נשאר רק להזיז את הצריח
		    if(kind) {
		        Piece rook = _board[move.get_fromRow()][7];
		        
		        updatePiecePosition(rook, move.get_fromRow(), 7, move.get_fromRow(), 5);
		        
		        _board[move.get_fromRow()][5] = rook;
		        _board[move.get_fromRow()][7] = null;
		        rook.setCol(5); 
		        rook.setRow(move.get_fromRow());
		    }
		    else {
		        Piece rook = _board[move.get_fromRow()][0];
		        
		        updatePiecePosition(rook, move.get_fromRow(), 0, move.get_fromRow(), 3);
		        
		        _board[move.get_fromRow()][3] = rook;
		        _board[move.get_fromRow()][0] = null;
		        rook.setCol(3);
		        rook.setRow(move.get_fromRow());
		    }
		}
	
	public void makeMove(Move move) {
	    Piece eaten = move.get_eatenPiece();
	    if (eaten != null) {
	        removePieceFromList(eaten, eaten.getRow(), eaten.getCol());
	        
	        if (eaten.color == Color.white) {
	            get_whiteWereEaten().add(eaten);
	        } else {
	            get_blackWereEaten().add(eaten);
	        }
	        resetCounter = true;
	    }
	    move.get_piece().haveMoved(this);
	    if (resetCounter) {
	        set_movesCount(0);
	    } else {
	        set_movesCount(get_movesCount() + 1);
	    }
	    
	    updatePiecePosition(move.get_piece(), move.get_fromRow(), move.get_fromCol(), 
	                       move.get_toRow(), move.get_toCol());
	    
	    move.get_piece().MakeMovement(this, move);
	    switchTurn();
	    get_boardHistory().add(getBoardAsString());
	    _isCheck = isKingInCheck(_currentTurn);

	    if (checkNotEnoughPieces()) {
			 System.out.println("not enough pieces on the board - STALEMATE");
		     _isStalemate = true;
		     updateGameStatus(); // <-- הוסף את זה לפני ה-return!
		     return;
		 }

	 _isCheckmate = isThereCheckmate(_currentTurn);

	 if (!_isCheck) {
	     _isStalemate = isGameDraw(_currentTurn);
	 }

	    
	    updateGameStatus();
	}
	
	

	
	public void updateGameStatus() {

		if(_isCheck ==true) {
			String col = (get_currentTurn()==Color.white)? "White" : "Black";
			System.out.println("Check on"+	col);
		}
		if(_isCheckmate == true) {
			System.out.println("Checkmate!!");
			if(get_currentTurn() == Color.white) {
				System.out.println("Black Won");
			}
			else {
				System.out.println("White Won");
			}
			
		}
		if(_isStalemate == true) {
			System.out.println("THERE IS A DRAW!!!!");
		}
		
	}
	public ArrayList<Move> get_moveHistory() {
		return _moveHistory;
	}
	public void set_moveHistory(ArrayList<Move> _moveHistory) {
		this._moveHistory = _moveHistory;
	}
	
	public boolean isKingInCheck(Color kingColor) {
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
	                
	                if (isValidMovement(attackMove)) {
	                    return true; 
	                }
	            }
	        }
	    }
	    return false;
	}
	public boolean isThereCheckmate(Color color) {
		
		if (!isKingInCheck(color)) {
	        return false;
	    }
		
		byte[] pieceTypes = {Piece.PAWN, Piece.ROOK, Piece.KNIGHT, Piece.BISHOP, Piece.QUEEN, Piece.KING};
		
		for (byte pieceType : pieceTypes) {
		    ArrayList<Byte> positions = getPiecePositions(pieceType, color);
		    
		    // עובדים על עותק של הרשימה כדי למנוע ConcurrentModificationException
		    ArrayList<Byte> positionsCopy = new ArrayList<>(positions);
		    
		    for (Byte position : positionsCopy) {
		        int row = getRowFromByte(position);
		        int col = getColFromByte(position);
		        Piece p = _board[row][col];
		        
		        if (p == null) continue;
		        
		        for (int toRow = 0; toRow < 8; toRow++) {
		            for (int toCol = 0; toCol < 8; toCol++) {
		                Move move = new Move(p, col, row, toCol, toRow);
		                // CanMove בודק גם חוקיות תנועה וגם שהמלך לא נשאר בשח
		                if (move.CanMove(this)) {
		                    return false; // נמצא לפחות מהלך אחד שמוציא מהשח – לא שחמט
		                }
		            }
		        }
		    }
		}
		return true; // אין אף מהלך שמוציא מהשח – שחמט

	}
	public boolean isGameDraw(Color color) {
	    if (checkStalemate(color)) {
	    	String turn;
	    	turn = (get_currentTurn()==Color.white)? "white": "black"; 
	    	System.out.println(turn + " cant move anywhere - STALEMATE");
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
	public boolean checkStalemate(Color color) {
	    if (isKingInCheck(color)) {
	        return false;
	    }
	    
	    byte[] pieceTypes = {Piece.PAWN, Piece.ROOK, Piece.KNIGHT, Piece.BISHOP, Piece.QUEEN, Piece.KING};
	    
	    for (byte pieceType : pieceTypes) {
	        ArrayList<Byte> positions = getPiecePositions(pieceType, color);
	        
	        for (Byte position : positions) {
	            int row = getRowFromByte(position);
	            int col = getColFromByte(position);
	            Piece p = _board[row][col];
	            
	            if (p != null) {
	                p._allPossibleMoves.clear();
	            }
	        }
	    }
	    
	    getAllPossibleMoves(color);
	    
	    for (byte pieceType : pieceTypes) {
	        ArrayList<Byte> positions = getPiecePositions(pieceType, color);
	        
	        for (Byte position : positions) {
	            int row = getRowFromByte(position);
	            int col = getColFromByte(position);
	            Piece p = _board[row][col];
	            
	            if (p != null && p._allPossibleMoves.size() > 0) {
	                return false; 
	            }
	        }
	    }
	    
	    return true;
	}
	
	private boolean checkNotEnoughPieces() {
	    if (!_whitePawns.isEmpty() || !_blackPawns.isEmpty() ||
	        !_whiteRooks.isEmpty() || !_blackRooks.isEmpty() ||
	        !_whiteQueens.isEmpty() || !_blackQueens.isEmpty()) {
	        return false;
	    }
	    int totalLightPieces = _whiteBishops.size() + _blackBishops.size() + 
	                           _whiteKnights.size() + _blackKnights.size();
	    
	    if (totalLightPieces == 0) {
	        return true;
	    }
	    
	    if (totalLightPieces == 1) {
	        return true;
	    }
	    
	    if (_whiteKnights.size() == 2 && totalLightPieces == 2) {
	        return true;
	    }
	    if (_blackKnights.size() == 2 && totalLightPieces == 2) {
	        return true;
	    }
	    
	    if (_whiteBishops.size() == 1 && _blackBishops.size() == 1 && totalLightPieces == 2) {
	        byte whitePos = _whiteBishops.get(0);
	        byte blackPos = _blackBishops.get(0);
	        
	        int whiteRow = getRowFromByte(whitePos);
	        int whiteCol = getColFromByte(whitePos);
	        int blackRow = getRowFromByte(blackPos);
	        int blackCol = getColFromByte(blackPos);
	        
	        boolean whiteBishopOnLightSquare = (whiteRow + whiteCol) % 2 == 0;
	        boolean blackBishopOnLightSquare = (blackRow + blackCol) % 2 == 0;
	        
	        if (whiteBishopOnLightSquare == blackBishopOnLightSquare) {
	            return true;
	        }
	    }
	    
	    return false;
	}
	private boolean check50Moves() {
	    if (get_movesCount() >= 50) {
	        return true;
	    }
	    return false;
	}
		
	private boolean check3Repetitions() {
	    String currentBoardState = getBoardAsString();
	    int counter = 0;

	    for (String state : get_boardHistory()) {
	        if (state.equals(currentBoardState)) {
	            counter++;
	        }
	    }

	    return counter >= 3;
	}

	
	
	

	public String getBoardAsString() {
	    StringBuilder sb = new StringBuilder();
	    
	    for (int row = 0; row < _board.length; row++) {
	        for (int col = 0; col < _board.length; col++) {
	            Piece p = _board[row][col];
	            if (p == null) {
	                sb.append("-");
	            } else {
	                sb.append(p.getColor() == Color.white ? 'w' : 'b');
	                sb.append(p.getPieceType()); // byte 1-6
	            }
	        }
	    }
	    
	    

	    
	    return sb.toString();
	}
	
	public void getAllPossibleMoves(Color color) {
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
	
	public void drawBoard() {
	    System.out.print("\t");
	    int i, j;
	    for(i = 0 ; i< _board.length ; i++) {
	        System.out.print("  " +i+"\t\t"); 
	    }
	    System.out.println();
	    System.out.println();
	    
	    for(i = 0 ; i< _board.length ; i++) {
	        System.out.print(i +"\t");
	        for(j=0;j<_board.length;j++) {
	            
	            if(_board[i][j]==null) {
	                System.out.print("empty   \t"); 
	            }
	            else {
	            	_board[i][j].drawBoard();
	            	
	            }
	            if(j==_board.length-1) {
	            	System.out.println();
	            	System.out.println();
	            }
	            
	            
	    
	    } 
	    }
	}
	
	
	
	
	
	public Move getLastMove() {
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
	public ArrayList<String> get_boardHistory() {
		return _boardHistory;
	}
	public void set_boardHistory(ArrayList<String> _boardHistory) {
		this._boardHistory = _boardHistory;
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
	public void set_board(Piece _board[][]) {
		this._board = _board;
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