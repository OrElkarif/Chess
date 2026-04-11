package logic;

public abstract class ActionNode extends DecisionNode {
    public ActionNode(DecisionNode fallbackNode) {
        super(null, fallbackNode);
    }
    @Override
    public boolean isActionNode() { return true; }
}