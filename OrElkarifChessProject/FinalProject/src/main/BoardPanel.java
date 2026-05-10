package main;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

import javax.swing.*;

import logic.GameLogic;
import logic.GameState;
import logic.Move;
import logic.SaveManager;
import piece.*;

public class BoardPanel extends JPanel {//המחלקה שמציירת את לוח השחמט ומטפלת בכל הלחיצות של העכבר של השחקן

    private static final int SQUARE_SIZE  = 100;
    private static final int BOARD_SIZE   = 8;

    private static final Color LIGHT_SQUARE        = new Color(230, 192, 141);
    private static final Color DARK_SQUARE         = new Color(121,  67,  43);
    private static final Color SELECTED_COLOR      = new Color(186, 202,  68, 210);
    private static final Color POSSIBLE_MOVE_COLOR = new Color( 50, 200,  50, 160);
    private static final Color CAPTURE_COLOR       = new Color(220,  50,  50, 160);
    private static final Color CHECK_COLOR         = new Color(220,   0,   0, 180);
    private static final Color SIDEBAR_BG          = new Color(49,  46,  43);
    private static final Color ACCENT              = new Color(230, 192, 141);

    private final GameLogic      _gameLogic;
    private final logic.AIPlayer _aiPlayer = new logic.AIPlayer();
    private final boolean        _twoPlayerMode;

    private int _selectedRow = -1, _selectedCol = -1;
    private final ArrayList<int[]> _possibleMoves = new ArrayList<>();
    private final ArrayList<int[]> _captureMoves  = new ArrayList<>();

    private JPanel  _boardCanvas;
    private JButton _viewResultButton;
    private JLabel  _turnLabel;
    private JLabel  _checkStatusLabel;

    public BoardPanel(boolean twoPlayers) {//מאתחל את הלוח את השחקנים ואת מצב המשחק
        _twoPlayerMode = twoPlayers;
        setLayout(new BorderLayout());
        setBackground(SIDEBAR_BG);

        _gameLogic = new GameLogic(true, twoPlayers);

        add(buildBoardCanvas(), BorderLayout.CENTER);
        add(buildSidebar(),     BorderLayout.EAST);
    }

    public BoardPanel(boolean twoPlayers, GameState savedState) {//בנאי למקרה ונשמר משחק בקובץ
        _twoPlayerMode = twoPlayers;
        setLayout(new BorderLayout());
        setBackground(SIDEBAR_BG);

        _gameLogic = new GameLogic(true, twoPlayers, savedState);

        add(buildBoardCanvas(), BorderLayout.CENTER);
        add(buildSidebar(),     BorderLayout.EAST);

        refreshTurn();
    }

    private JPanel buildBoardCanvas() {//בונה את אזור הלוח עצמו ומגדיר את ההאזנה ללחיצות עכבר
        _boardCanvas = new JPanel() {
            @Override protected void paintComponent(Graphics g) {//מציירת
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                drawSquares(g2);
                drawHighlights(g2);
                drawPieces(g2);
                if (!_gameLogic._gameInProcess) {
                    g2.setColor(new Color(0, 0, 0, 110));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        };
        _boardCanvas.setLayout(null);
        _boardCanvas.setPreferredSize(new Dimension(BOARD_SIZE * SQUARE_SIZE,
                                                    BOARD_SIZE * SQUARE_SIZE));

        _viewResultButton = new JButton("VIEW RESULTS");
        _viewResultButton.setFont(new Font("Arial", Font.BOLD, 20));
        _viewResultButton.setBackground(ACCENT);
        _viewResultButton.setForeground(new Color(49, 46, 43));
        _viewResultButton.setFocusPainted(false);
        _viewResultButton.setBorderPainted(false);
        _viewResultButton.setBounds(275, 370, 250, 55);
        _viewResultButton.setVisible(false);
        _viewResultButton.addActionListener(e -> goToWinScreen());
        _boardCanvas.add(_viewResultButton);

        _boardCanvas.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                handleClick(e.getX(), e.getY());
            }
        });
        return _boardCanvas;
    }

    private JPanel buildSidebar() {//בונה את התפריט בצד שמציג את התור מצב השח וכפתור חזרה
        JPanel bar = new JPanel();
        bar.setPreferredSize(new Dimension(200, BOARD_SIZE * SQUARE_SIZE));
        bar.setBackground(SIDEBAR_BG);
        bar.setLayout(new BoxLayout(bar, BoxLayout.Y_AXIS));
        bar.setBorder(BorderFactory.createEmptyBorder(40, 20, 40, 20));

        JLabel turnHeader = smallHeader("TURN");

        _turnLabel = new JLabel("WHITE");
        _turnLabel.setFont(new Font("Arial", Font.BOLD, 32));
        _turnLabel.setForeground(Color.WHITE);
        _turnLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        _checkStatusLabel = new JLabel("Check on: None");
        _checkStatusLabel.setFont(new Font("Arial", Font.BOLD, 16));
        _checkStatusLabel.setForeground(new Color(200, 200, 200));
        _checkStatusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton backBtn = new JButton("BACK");
        backBtn.setFont(new Font("Arial", Font.BOLD, 16));
        backBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        backBtn.setMaximumSize(new Dimension(160, 42));
        backBtn.setBackground(new Color(70, 65, 60));
        backBtn.setForeground(ACCENT);
        backBtn.setFocusPainted(false);
        backBtn.setBorderPainted(false);
        backBtn.addActionListener(e -> {
            JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(BoardPanel.this);
            if (frame != null) {
                frame.getContentPane().removeAll();
                frame.add(new ModeSelectScreen(frame));
                frame.revalidate();
                frame.repaint();
            }
        });

        bar.add(turnHeader);
        bar.add(Box.createRigidArea(new Dimension(0, 8)));
        bar.add(_turnLabel);
        bar.add(Box.createRigidArea(new Dimension(0, 30))); 
        bar.add(_checkStatusLabel);
        bar.add(Box.createVerticalGlue());
        bar.add(backBtn);

        return bar;
    }

    private JLabel smallHeader(String text) {//מייצר כותרת קטנה ומעוצבת לתפריט בצד
        JLabel l = new JLabel(text);
        l.setFont(new Font("Arial", Font.BOLD, 11));
        l.setForeground(new Color(130, 118, 106));
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        return l;
    }

    private void refreshTurn() {//מעדכן את התצוגה של התור הנוכחי ומצב השח אחרי כל מהלך שקורה
        boolean white = (_gameLogic._state.get_currentTurn() == Color.white);
        _turnLabel.setText(white ? "WHITE" : "BLACK");
        _turnLabel.setForeground(white ? Color.WHITE : new Color(200, 200, 200));

        if (_gameLogic._state.isKingInCheck(Color.white)) {
            _checkStatusLabel.setText("Check on: White");
            _checkStatusLabel.setForeground(new Color(255, 80, 80)); 
        } else if (_gameLogic._state.isKingInCheck(Color.black)) {
            _checkStatusLabel.setText("Check on: Black");
            _checkStatusLabel.setForeground(new Color(255, 80, 80)); 
        } else {
            _checkStatusLabel.setText("Check on: None");
            _checkStatusLabel.setForeground(new Color(200, 200, 200)); 
        }
    }

    private void handleClick(int px, int py) {//מטפל בלחיצת עכבר על הלוח ומחליט אם לבחור כלי או לבצע מהלך
        if (!_gameLogic._gameInProcess) return;
        if (!_twoPlayerMode && _gameLogic._state.get_currentTurn() == Color.black) {//חסימת כל הלחיצות אם עכשיו התור של הבוט
            return; 
        }
        int col = px / SQUARE_SIZE, row = py / SQUARE_SIZE;
        if (col < 0 || col >= BOARD_SIZE || row < 0 || row >= BOARD_SIZE) return;

        if (_selectedRow == -1) {
            Piece p = _gameLogic._state.getPiecePlace(row, col);
            if (p != null && p.getColor() == _gameLogic._state.get_currentTurn()) {
                _selectedRow = row; _selectedCol = col;
                loadPossibleMoves(row, col);
            }
        } else {
            if (isIn(_possibleMoves, row, col) || isIn(_captureMoves, row, col)) {//אם הבחירה היא ביריב או במשבצת חוקית
                executeMove(_selectedRow, _selectedCol, row, col);
                return;
            }
            Piece p = _gameLogic._state.getPiecePlace(row, col);
            if (p != null && p.getColor() == _gameLogic._state.get_currentTurn()) {//אם הבחירה היא באותו צבע של התור הנוכחי
                _selectedRow = row; _selectedCol = col;
                loadPossibleMoves(row, col);
            } else {
                clearSelection();
            }
        }
        _boardCanvas.repaint();
    }

    private boolean isIn(ArrayList<int[]> list, int r, int c) {//בודק אם המהלך בתור רשימת המהלכים שהגדרנו
        for (int[] m : list) if (m[0]==r && m[1]==c) return true;
        return false;
    }

    private void executeMove(int fR, int fC, int tR, int tC) {//מבצעת את המהלך בפועל על הלוח הגרפי וקוראת לתור של הבוט אם צריך
        Piece p = _gameLogic._state.getPiecePlace(fR, fC);

        boolean wPromo = p != null && p.getPieceType()==Piece.PAWN//הכתרות
                && p.getColor()==Color.white && tR==0;
        boolean bPromo = _twoPlayerMode && p != null && p.getPieceType()==Piece.PAWN
                && p.getColor()==Color.black && tR==7;

        if (wPromo || bPromo) {
            handlePromotion(fR, fC, tR, tC, p.getColor());
        } else {
            try { _gameLogic.PlayerMove(fR, fC, tR, tC); }
            catch (IllegalStateException ex) { System.out.println(ex.getMessage()); }
        }

        clearSelection();
        refreshTurn();
        saveGame();
        _boardCanvas.repaint();

        if (!_twoPlayerMode && _gameLogic._gameInProcess
                && _gameLogic._state.get_currentTurn() == Color.black) {
            javax.swing.Timer timer = new javax.swing.Timer(1500, e -> {
                Move ai = _aiPlayer.chooseBestMove(_gameLogic._state);
                if (ai != null) ai.DoThisMove(_gameLogic._state);
                refreshTurn();
                saveGame();
                checkGameOver();
                _boardCanvas.repaint();
            });
            timer.setRepeats(false);
            timer.start();
        } else {
            checkGameOver();
        }
    }

    private void saveGame() {//שומרת את המצב הנוכחי של המשחק לקובץ בתוך המחשב
        if (_gameLogic._gameInProcess) {
            SaveManager.save(_gameLogic._state, _twoPlayerMode);
        }
    }

    private void handlePromotion(int fR, int fC, int tR, int tC, Color color) {//מקפיצה חלון בחירה כשרגלי מגיע לסוף הלוח ומעדכנת את הלוח בכלי החדש
        String[] opts = {"מלכה", "צריח", "רץ", "פרש"};
        int ch = JOptionPane.showOptionDialog(this, "בחר כלי:", "קידום",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, opts, opts[0]);
        if (ch < 0) ch = 0;
        Piece promo;
        switch (ch) {
            case 1: promo = new Rook(tC, tR, color);   break;
            case 2: promo = new Bishop(tC, tR, color); break;
            case 3: promo = new Knight(tC, tR, color); break;
            default: promo = new Queen(tC, tR, color);
        }
        Move move = new Move(_gameLogic._state.getPiecePlace(fR, fC), fC, fR, tC, tR);
        if (move.CanMove(_gameLogic._state)) {
            move.set_isPromotion(true);
            move.set_promoteTo(promo);
            move.DoThisMove(_gameLogic._state);
        }
    }

    private void loadPossibleMoves(int row, int col) {//מכינה ושומרת את כל המהלכים החוקיים של הכלי שנבחר כדי שנוכל לצייר אותם
        _possibleMoves.clear(); _captureMoves.clear();
        for (int[] m : _gameLogic.highlightPossibleMoves(row, col)) {
            Piece t = _gameLogic._state.getPiecePlace(m[0], m[1]);
            (t != null ? _captureMoves : _possibleMoves).add(new int[]{m[0], m[1]});
        }
    }

    private void clearSelection() {//מנקה את הבחירה הנוכחית של השחקן ומאפסת את ההדגשות שעל המסך
        _selectedRow = _selectedCol = -1;
        _possibleMoves.clear(); _captureMoves.clear();
    }

    private void checkGameOver() {//בודקת אם המשחק נגמר מציגה כפתור של תוצאות ועוצרת את הבוט
        if (_gameLogic._state.is_isCheckmate() || _gameLogic._state.is_isStalemate()) {
            _gameLogic._gameInProcess = false;
            SaveManager.deleteSave();
            SwingUtilities.invokeLater(() -> {
                _viewResultButton.setVisible(true);
                _boardCanvas.repaint();
            });
        }
    }

    private void goToWinScreen() {//עוברת למסך הניצחון או התיקו 
        JFrame f = (JFrame) SwingUtilities.getWindowAncestor(this);
        if (f == null) return;
        f.getContentPane().removeAll();
        f.add(new WinScreen(f, _gameLogic.getWinner(),
                            _gameLogic._state.getEndGameReason()));
        f.revalidate(); f.repaint();
    }

    private void drawSquares(Graphics2D g2) {//מציירת את המשבצות
        for (int r=0; r<BOARD_SIZE; r++)
            for (int c=0; c<BOARD_SIZE; c++) {
                g2.setColor(((r+c)%2==0) ? LIGHT_SQUARE : DARK_SQUARE);
                g2.fillRect(c*SQUARE_SIZE, r*SQUARE_SIZE, SQUARE_SIZE, SQUARE_SIZE);
            }
    }

    private void drawHighlights(Graphics2D g2) {//מציירת את ההדגשות הירוקות והאדומות על המשבצות האפשריות
        if (_selectedRow >= 0) {
            g2.setColor(SELECTED_COLOR);//מסמן את המשבצת הנבחרה
            g2.fillRect(_selectedCol*SQUARE_SIZE, _selectedRow*SQUARE_SIZE,
                        SQUARE_SIZE, SQUARE_SIZE);
        }
        Color turn = _gameLogic._state.get_currentTurn();
        if (_gameLogic._state.isKingInCheck(turn)) {
            java.util.ArrayList<Byte> kings = (turn==Color.white)
                    ? _gameLogic._state.getWhiteKings()
                    : _gameLogic._state.getBlackKings();
            if (!kings.isEmpty()) {
                byte pos = kings.get(0);//יודע שיש מלך אחד
                g2.setColor(CHECK_COLOR);
                g2.fillRect(logic.GameState.getColFromByte(pos)*SQUARE_SIZE,//מייצר ריבוע אדום
                            logic.GameState.getRowFromByte(pos)*SQUARE_SIZE,
                            SQUARE_SIZE, SQUARE_SIZE);
            }
        }
        for (int[] m : _possibleMoves) {
            g2.setColor(POSSIBLE_MOVE_COLOR);
            int d=30;
            g2.fillOval(m[1]*SQUARE_SIZE+(SQUARE_SIZE-d)/2,
                        m[0]*SQUARE_SIZE+(SQUARE_SIZE-d)/2, d, d);//מצייר נקודות
        }
        for (int[] m : _captureMoves) {
            g2.setColor(CAPTURE_COLOR);
            g2.setStroke(new BasicStroke(5));
            g2.drawRect(m[1] * SQUARE_SIZE, m[0] * SQUARE_SIZE, SQUARE_SIZE, SQUARE_SIZE);            
            g2.setStroke(new BasicStroke(1));// החזרת עובי העיפרון לרגיל כדי לא להשפיע על שאר הציורים בלוח
        }
    }

    private void drawPieces(Graphics2D g2) {//עוברת על הלוח ומציירת כל כלי במקום המעודכן שלו
        for (int r=0; r<BOARD_SIZE; r++)
            for (int c=0; c<BOARD_SIZE; c++) {
                Piece p = _gameLogic._state.getPiecePlace(r, c);
                if (p != null) { p.x=c*SQUARE_SIZE; p.y=r*SQUARE_SIZE; p.draw(g2); }
            }
    }
}