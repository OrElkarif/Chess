package logic;

public abstract class QuestionNode extends DecisionNode {
    public QuestionNode(DecisionNode yesNode, DecisionNode noNode) {
        super(yesNode, noNode);
    }
    @Override
    public boolean isActionNode() { return false; }
}