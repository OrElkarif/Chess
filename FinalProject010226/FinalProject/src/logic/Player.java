package logic;

import java.awt.Color;
import java.util.Scanner;

public class Player {
	private Color _playerColor;
	private boolean _isHuman;
	private Move _pendingMove;
	
	
	public Player(Color color, boolean ishuman) {
		_playerColor= color;
		_isHuman = ishuman;
	}
	
	public void makeMove(GameState state) {
        Move moveToDo = getMove(state);

        if (moveToDo != null) {
            moveToDo.DoThisMove(state);
            
            _pendingMove = null;
        }
    }
	public Move getMove(GameState state) {
        if (_isHuman) {
        	
        	return _pendingMove; 
        } else {
        		//בינתיים הפעולה ריקה מכיוון שפה ייכנס האלגוריתם	
            return null; // זמני
        }
    }
	
	
    public void setHumanMove(Move move) {//זאת הפעולה שאני אקרא שהמשתמש יזיז את העכבר
        if (_isHuman) {
            this._pendingMove = move;
        }
    }

}
