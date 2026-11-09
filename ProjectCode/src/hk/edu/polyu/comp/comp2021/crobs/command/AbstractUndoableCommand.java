package hk.edu.polyu.comp.comp2021.crobs.command;

import hk.edu.polyu.comp.comp2021.crobs.controller.CrobsException;
import hk.edu.polyu.comp.comp2021.crobs.model.ReversibleChange;

import java.util.ArrayDeque;
import java.util.Deque;

/** Keeps separate changes when a registered command is executed more than once. */
abstract class AbstractUndoableCommand implements Command {
    private final Deque<ReversibleChange<?>> undoHistory = new ArrayDeque<>();
    private final Deque<ReversibleChange<?>> redoHistory = new ArrayDeque<>();

    /**
     * Records a change only after its execution has succeeded.
     * @param change an already-applied domain change
     * @param <T> the operation's result type
     * @return the operation's result
     */
    protected final <T> T remember(ReversibleChange<T> change) {
        undoHistory.push(change);
        redoHistory.clear();
        return change.getValue();
    }

    /** {@inheritDoc} */
    @Override
    public final void undo() throws CrobsException {
        if (undoHistory.isEmpty()) {
            throw new CrobsException("No " + getName() + " command to undo.");
        }
        ReversibleChange<?> change = undoHistory.peek();
        change.undo();
        undoHistory.pop();
        redoHistory.push(change);
    }

    /** {@inheritDoc} */
    @Override
    public final void redo() throws CrobsException {
        if (redoHistory.isEmpty()) {
            throw new CrobsException("No " + getName() + " command to redo.");
        }
        ReversibleChange<?> change = redoHistory.peek();
        change.redo();
        redoHistory.pop();
        undoHistory.push(change);
    }
}
