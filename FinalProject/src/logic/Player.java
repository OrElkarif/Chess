package logic;

import java.awt.Color;

public class Player {
    private Color _playerColor;
    private boolean _isHuman;
    private Move _pendingMove;
    private AIPlayer _aiPlayer; // הוספת אובייקט הבוט

    public Player(Color color, boolean ishuman) {
        _playerColor = color;
        _isHuman = ishuman;
        if (!ishuman) {
            _aiPlayer = new AIPlayer(); // אתחול פעם אחת בלבד כדי לשמור על הזיכרון שלו!
        }
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
            return _aiPlayer.chooseBestMove(state); // הפעלת אלגוריתם העץ!
        }
    }

    public void setHumanMove(Move move) {
        if (_isHuman) {
            this._pendingMove = move;
        }
    }
    
    public boolean isHuman() {
        return _isHuman;
    }
}