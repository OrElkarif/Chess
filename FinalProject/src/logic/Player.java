package logic;

import java.awt.Color;

public class Player {
    private Color _playerColor;
    private boolean _isHuman;
    private AIPlayer _aiPlayer; 

    public Player(Color color, boolean ishuman) {
        _playerColor = color;
        _isHuman = ishuman;
        if (!ishuman) {
            _aiPlayer = new AIPlayer(); 
        }
    }

 

    public Move getMove(GameState state) {
        if (!_isHuman) {
            return _aiPlayer.chooseBestMove(state);
        }
        return null;
    }
    
    public boolean isHuman() {
        return _isHuman;
    }
}