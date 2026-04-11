package main;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Queue;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import logic.GameLogic;
import logic.Move;
import logic.Player;
import piece.Bishop;
import piece.Knight;
import piece.Piece;
import piece.Queen;
import piece.Rook;

public class BoardPanel extends JPanel {

    private static final int SQUARE_SIZE = 100;
    public static final int BOARD_SIZE = 8; // <-- תיקון: הגדרת גודל הלוח כקבוע

    private static final Color LIGHT_SQUARE       = new Color(230, 192, 141);
    private static final Color DARK_SQUARE        = new Color(121,  67,  43);
    private static final Color SELECTED_COLOR     = new Color(186, 202,  68, 210);
    private static final Color POSSIBLE_MOVE_COLOR = new Color( 50, 200,  50, 160);
    private static final Color CAPTURE_COLOR      = new Color(220,  50,  50, 160);
    private static final Color CHECK_COLOR        = new Color(220,   0,   0, 180);

    private logic.AIPlayer _aiPlayer = new logic.AIPlayer();
    private GameLogic _gameLogic;

    private int _selectedRow = -1;
    private int _selectedCol = -1;

    private ArrayList<int[]> _possibleMoves = new ArrayList<>();
    private ArrayList<int[]> _captureMoves  = new ArrayList<>();

    private JButton _viewResultButton;

    public BoardPanel() {
        setLayout(null); 
        Player white = new Player(Color.white, true);
        Player black = new Player(Color.black, false); // הבוט הוא שחור
        _gameLogic = new GameLogic(white, black);
        setPreferredSize(new java.awt.Dimension(800, 800));

        _viewResultButton = new JButton("VIEW RESULTS");
        _viewResultButton.setFont(new Font("Arial", Font.BOLD, 22));
        _viewResultButton.setBackground(new Color(230, 192, 141));
        _viewResultButton.setForeground(new Color(49, 46, 43));
        _viewResultButton.setFocusPainted(false);
        _viewResultButton.setBorderPainted(false);
        _viewResultButton.setVisible(false); 
        _viewResultButton.setBounds(275, 370, 250, 60); 

        _viewResultButton.addActionListener(e -> {
            JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(BoardPanel.this);
            if (frame != null) {
                Color winner = _gameLogic.getWinner();
                String reason = _gameLogic._state.getEndGameReason(); // מניח שהוספת את מה שדיברנו מקודם
                frame.getContentPane().removeAll();
                frame.add(new WinScreen(frame, winner, reason));
                frame.revalidate();
                frame.repaint();
            }
        });

        add(_viewResultButton);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleClick(e.getX(), e.getY());
            }
        });
    }

    private void handleClick(int pixelX, int pixelY) {
        if (!_gameLogic._gameInProcess) return;

        int col = pixelX / SQUARE_SIZE;
        int row = pixelY / SQUARE_SIZE;

        if (col < 0 || col >= BOARD_SIZE || row < 0 || row >= BOARD_SIZE) return;

        if (_selectedRow == -1) {
            Piece piece = _gameLogic._state.getPiecePlace(row, col);
            if (piece != null && piece.getColor() == _gameLogic._state.get_currentTurn()) {
                _selectedRow = row;
                _selectedCol = col;
                loadPossibleMoves(row, col);
            }
        } else {
            if (isInPossibleMoves(row, col) || isInCaptureMoves(row, col)) {
                executeMove(_selectedRow, _selectedCol, row, col);
            } else {
                Piece piece = _gameLogic._state.getPiecePlace(row, col);
                if (piece != null && piece.getColor() == _gameLogic._state.get_currentTurn()) {
                    _selectedRow = row;
                    _selectedCol = col;
                    loadPossibleMoves(row, col);
                } else {
                    clearSelection();
                }
            }
        }
        repaint();
    }

    private boolean isInPossibleMoves(int row, int col) {
        for (int[] move : _possibleMoves) {
            if (move[0] == row && move[1] == col) return true;
        }
        return false;
    }

    private boolean isInCaptureMoves(int row, int col) {
        for (int[] move : _captureMoves) {
            if (move[0] == row && move[1] == col) return true;
        }
        return false;
    }

    private void executeMove(int fromRow, int fromCol, int toRow, int toCol) {
        Piece piece = _gameLogic._state.getPiecePlace(fromRow, fromCol);

        // בדיקה רק עבור השחקן האנושי (לבן מגיע לשורה 0)
        boolean isWhitePromotion = (piece != null && piece.getPieceType() == Piece.PAWN && piece.getColor() == Color.white && toRow == 0);

        if (isWhitePromotion) {
            // השחקן הלבן הגיע לשורה 0 - פתח דיאלוג בחירה
            handlePromotion(fromRow, fromCol, toRow, toCol, piece.getColor());
        } else {
            // מהלך רגיל של שחקן
            try {
                _gameLogic.PlayerMove(fromRow, fromCol, toRow, toCol);
            } catch (IllegalStateException ex) {
                System.out.println(ex.getMessage());
            }
        }
        
        clearSelection();
        repaint();
        // ... (מכאן ממשיך הקוד של ה-Thread שהחלפנו בשלב 2)

        // תור הבוט - רץ ברקע
        if (_gameLogic._gameInProcess && _gameLogic._state.get_currentTurn() == Color.black) {
            new Thread(() -> {
                Move aiMove = _aiPlayer.chooseBestMove(_gameLogic._state);
                SwingUtilities.invokeLater(() -> {
                    if (aiMove != null) {
                        // DoThisMove מטפל בהכתרה אוטומטית (ראה Move.java שלב 2)
                        aiMove.DoThisMove(_gameLogic._state);
                    }
                    checkGameOver();
                    repaint();
                });
            }).start();
        } else {
            checkGameOver();
            repaint();
        }
    }

    private void handlePromotion(int fromRow, int fromCol, int toRow, int toCol, Color color) {
        String[] options = {"מלכה", "צריח", "רץ", "פרש"};
        int choice = JOptionPane.showOptionDialog(
                this,
                "הרגלי הגיע לקצה! בחר לאיזה כלי לשדרג:",
                "קידום רגלי",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );
        if (choice < 0) choice = 0;

        Piece promotedPiece = createPromotion(choice, toCol, toRow, color);
        performPromotionMove(fromRow, fromCol, toRow, toCol, promotedPiece);
    }

    private Piece createPromotion(int choice, int col, int row, Color color) {
        switch (choice) {
            case 1:  return new Rook(col, row, color);
            case 2:  return new Bishop(col, row, color);
            case 3:  return new Knight(col, row, color);
            default: return new Queen(col, row, color);
        }
    }

    private void performPromotionMove(int fromRow, int fromCol, int toRow, int toCol, Piece promotedPiece) {
        Move move = new Move(
                _gameLogic._state.getPiecePlace(fromRow, fromCol),
                fromCol, fromRow, toCol, toRow);

        if (move.CanMove(_gameLogic._state)) {
            move.set_isPromotion(true);
            move.set_promoteTo(promotedPiece);
            move.DoThisMove(_gameLogic._state);
        }
    }

    private void loadPossibleMoves(int row, int col) {
        _possibleMoves.clear();
        _captureMoves.clear();

        Queue<int[]> moves = _gameLogic.highlightPossibleMoves(row, col);

        for (int[] m : moves) {
            int toRow = m[0];
            int toCol = m[1];
            Piece target = _gameLogic._state.getPiecePlace(toRow, toCol);
            if (target != null) {
                _captureMoves.add(new int[]{toRow, toCol});
            } else {
                _possibleMoves.add(new int[]{toRow, toCol});
            }
        }
    }

    private void clearSelection() {
        _selectedRow = -1;
        _selectedCol = -1;
        _possibleMoves.clear();
        _captureMoves.clear();
    }

    private void checkGameOver() {
        if (_gameLogic._state.is_isCheckmate() || _gameLogic._state.is_isStalemate()) {
            _gameLogic._gameInProcess = false;

            SwingUtilities.invokeLater(() -> {
                _viewResultButton.setVisible(true);
                repaint();
            });
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawSquares(g2);
        drawHighlights(g2);
        drawPieces(g2);
        drawTurnIndicator(g2);

        if (!_gameLogic._gameInProcess) {
            g2.setColor(new Color(0, 0, 0, 120));
            g2.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    private void drawSquares(Graphics2D g2) {
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                Color bg = ((row + col) % 2 == 0) ? LIGHT_SQUARE : DARK_SQUARE;
                g2.setColor(bg);
                g2.fillRect(col * SQUARE_SIZE, row * SQUARE_SIZE, SQUARE_SIZE, SQUARE_SIZE);
            }
        }
    }

    private void drawHighlights(Graphics2D g2) {
        if (_selectedRow >= 0) {
            g2.setColor(SELECTED_COLOR);
            g2.fillRect(_selectedCol * SQUARE_SIZE, _selectedRow * SQUARE_SIZE,
                        SQUARE_SIZE, SQUARE_SIZE);
        }

        highlightKingInCheck(g2);

        for (int[] m : _possibleMoves) {
            g2.setColor(POSSIBLE_MOVE_COLOR);
            int dotSize = 30;
            g2.fillOval(m[1] * SQUARE_SIZE + (SQUARE_SIZE - dotSize) / 2,
                        m[0] * SQUARE_SIZE + (SQUARE_SIZE - dotSize) / 2,
                        dotSize, dotSize);
        }

        for (int[] m : _captureMoves) {
            g2.setColor(CAPTURE_COLOR);
            int border = 5;
            g2.fillRect(m[1] * SQUARE_SIZE, m[0] * SQUARE_SIZE,  SQUARE_SIZE, border);
            g2.fillRect(m[1] * SQUARE_SIZE, m[0] * SQUARE_SIZE + SQUARE_SIZE - border, SQUARE_SIZE, border);
            g2.fillRect(m[1] * SQUARE_SIZE, m[0] * SQUARE_SIZE,  border, SQUARE_SIZE);
            g2.fillRect(m[1] * SQUARE_SIZE + SQUARE_SIZE - border, m[0] * SQUARE_SIZE, border, SQUARE_SIZE);
        }
    }

    private void highlightKingInCheck(Graphics2D g2) {
        Color currentTurn = _gameLogic._state.get_currentTurn();
        if (!_gameLogic._state.isKingInCheck(currentTurn)) return;

        java.util.ArrayList<Byte> kings = (currentTurn == Color.white)
                ? _gameLogic._state.getWhiteKings()
                : _gameLogic._state.getBlackKings();

        if (kings.isEmpty()) return;

        byte pos = kings.get(0);
        int kingRow = logic.GameState.getRowFromByte(pos);
        int kingCol = logic.GameState.getColFromByte(pos);

        g2.setColor(CHECK_COLOR);
        g2.fillRect(kingCol * SQUARE_SIZE, kingRow * SQUARE_SIZE, SQUARE_SIZE, SQUARE_SIZE);
    }

    private void drawPieces(Graphics2D g2) {
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                Piece p = _gameLogic._state.getPiecePlace(row, col);
                if (p != null) {
                    p.x = col * SQUARE_SIZE;
                    p.y = row * SQUARE_SIZE;
                    p.draw(g2);
                }
            }
        }
    }

    private void drawTurnIndicator(Graphics2D g2) {
        String turnText = (_gameLogic._state.get_currentTurn() == Color.white)
                          ? "תור: לבן" : "תור: שחור (חושב...)";
        g2.setFont(new Font("Arial", Font.BOLD, 16));
        g2.setColor(Color.BLACK);
        g2.drawString(turnText, 10, getHeight() - 10);
        g2.setColor(Color.WHITE);
        g2.drawString(turnText, 11, getHeight() - 9);
    }
}