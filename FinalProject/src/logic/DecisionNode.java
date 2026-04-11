package logic;

public abstract class DecisionNode {
    public DecisionNode left;
    public DecisionNode right;

    public DecisionNode(DecisionNode left, DecisionNode right) {
        this.left = left;
        this.right = right;
    }

    public abstract boolean isActionNode();
    public boolean checkCondition(GameState state) { return false; }
    public Move calculateMove(GameState state) { return null; }
}