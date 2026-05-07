package main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class StartScreen extends JPanel {//המסך הראשון שעולה כשפותחים את המשחק ומציע להתחיל או לראות הוראות
    private JFrame parentFrame;

    public StartScreen(JFrame frame) {//בונה את המסך את הרקע ואת הכפתורים של התפריט הראשי
        this.parentFrame = frame;
        setLayout(new BorderLayout());
        setBackground(new Color(49, 46, 43));

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(new Color(49, 46, 43));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(100, 0, 0, 0));

        JLabel titleLabel = new JLabel("CHESS GAME");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 60));
        titleLabel.setForeground(new Color(230, 192, 141));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton playButton = new JButton("PLAY");
        playButton.setFont(new Font("Arial", Font.BOLD, 30));
        playButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        playButton.setMaximumSize(new Dimension(200, 60));
        playButton.setBackground(new Color(230, 192, 141));
        playButton.setForeground(new Color(49, 46, 43));
        playButton.setFocusPainted(false);
        playButton.setBorderPainted(false);

        JButton instructionsButton = new JButton("INSTRUCTIONS");
        instructionsButton.setFont(new Font("Arial", Font.BOLD, 30));
        instructionsButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        instructionsButton.setMaximumSize(new Dimension(300, 60));
        instructionsButton.setBackground(new Color(230, 192, 141));
        instructionsButton.setForeground(new Color(49, 46, 43));
        instructionsButton.setFocusPainted(false);
        instructionsButton.setBorderPainted(false);

        centerPanel.add(titleLabel);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 50)));
        centerPanel.add(playButton);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        centerPanel.add(instructionsButton);

        add(centerPanel, BorderLayout.CENTER);

        playButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                parentFrame.getContentPane().removeAll();
                parentFrame.add(new ModeSelectScreen(parentFrame));
                parentFrame.revalidate();
                parentFrame.repaint();
            }
        });

        instructionsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                parentFrame.getContentPane().removeAll();
                parentFrame.add(new InstructionsScreen(parentFrame));
                parentFrame.revalidate();
                parentFrame.repaint();
            }
        });
    }
}