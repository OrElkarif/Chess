package logic;

public interface TreeTask {
    boolean isAction();                    // האם זו פעולה או שאלה
    boolean checkCondition(GameState state); // מופעל אם זו שאלה
    Move calculateMove(GameState state);     // מופעל אם זו פעולה
}