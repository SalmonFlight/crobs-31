package hk.edu.polyu.comp.comp2021.crobs.model;

import hk.edu.polyu.comp.comp2021.crobs.controller.CrobsException;

import java.util.Objects;

/**
 * Stores the result and reversal of an already successful domain change.
 * @param <T> the result type
 */
public final class ReversibleChange<T> {
    /** A domain operation that can report an expected failure. */
    @FunctionalInterface
    public interface Action {
        /**
         * Performs the operation.
         * @throws CrobsException when the operation cannot be completed
         */
        void run() throws CrobsException;
    }

    private final T value;
    private final Action undoAction;
    private final Action redoAction;
    private boolean applied = true;

    private ReversibleChange(T value, Action undoAction, Action redoAction) {
        this.value = value;
        this.undoAction = Objects.requireNonNull(undoAction, "Undo action is required.");
        this.redoAction = Objects.requireNonNull(redoAction, "Redo action is required.");
    }

    /**
     * Records a change that the caller has already applied; runs neither action.
     * @param value the result, which may be null
     * @param undoAction restores the previous state
     * @param redoAction reapplies the same change with the same IDs
     * @param <T> the result type
     * @return a reversible change
     */
    public static <T> ReversibleChange<T> applied(T value, Action undoAction, Action redoAction) {
        return new ReversibleChange<>(value, undoAction, redoAction);
    }

    /**
     * Gets the original operation's result.
     * @return the result
     */
    public T getValue() {
        return value;
    }

    /**
     * Restores the previous state, once per application.
     * @throws CrobsException when already undone or the reversal fails
     */
    public void undo() throws CrobsException {
        if (!applied) {
            throw new CrobsException("This change has already been undone.");
        }
        undoAction.run();
        applied = false;
    }

    /**
     * Reapplies the change after undo.
     * @throws CrobsException when still applied or the operation fails
     */
    public void redo() throws CrobsException {
        if (applied) {
            throw new CrobsException("Undo this change before redoing it.");
        }
        redoAction.run();
        applied = true;
    }
}
