package main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class InstructionsScreen extends JPanel {
    private JFrame parentFrame;
    
    public InstructionsScreen(JFrame frame) {
        this.parentFrame = frame;
        setLayout(new BorderLayout());
        setBackground(new Color(49, 46, 43));
        
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(new Color(49, 46, 43));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));
        
        JLabel titleLabel = new JLabel("INSTRUCTIONS");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 50));
        titleLabel.setForeground(new Color(230, 192, 141));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JTextArea instructionsText = new JTextArea();
        instructionsText.setText(
            "כללי המשחק:\n\n" +
            "1. השחמט הוא משחק אסטרטגיה לשני שחקנים\n" +
            "2. מטרת המשחק: להכניס את המלך היריב למצב מט\n" +
            "3. כל כלי נע באופן ייחודי:\n" +
            "   - חייל: צעד אחד קדימה, תוקף באלכסון\n" +
            "   - צריח: אופקי ואנכי ללא הגבלה\n" +
            "   - פרש: תנועת L\n" +
            "   - רץ: באלכסון ללא הגבלה\n" +
            "   - מלכה: לכל הכיוונים ללא הגבלה\n" +
            "   - מלך: צעד אחד לכל כיוון\n\n" +
            "4. לבנים מתחילים\n" +
            "5. שחקנים זזים בתורות\n" +
            "6. לא ניתן לשים את המלך שלך בסכנה"
        );
        instructionsText.setFont(new Font("Arial", Font.PLAIN, 18));
        instructionsText.setForeground(new Color(230, 192, 141));
        instructionsText.setBackground(new Color(49, 46, 43));
        instructionsText.setEditable(false);
        instructionsText.setLineWrap(true);
        instructionsText.setWrapStyleWord(true);
        instructionsText.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JButton backButton = new JButton("BACK");
        backButton.setFont(new Font("Arial", Font.BOLD, 25));
        backButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        backButton.setMaximumSize(new Dimension(150, 50));
        backButton.setBackground(new Color(230, 192, 141));
        backButton.setForeground(new Color(49, 46, 43));
        backButton.setFocusPainted(false);
        backButton.setBorderPainted(false);
        
        centerPanel.add(titleLabel);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 30)));
        centerPanel.add(instructionsText);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 30)));
        centerPanel.add(backButton);
        
        add(centerPanel, BorderLayout.CENTER);
        
        backButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                parentFrame.getContentPane().removeAll();
                parentFrame.add(new StartScreen(parentFrame));
                parentFrame.revalidate();
                parentFrame.repaint();
            }
        });
    }
}