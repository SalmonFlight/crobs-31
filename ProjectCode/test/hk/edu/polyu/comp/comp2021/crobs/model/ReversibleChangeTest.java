package hk.edu.polyu.comp.comp2021.crobs.model;

import hk.edu.polyu.comp.comp2021.crobs.controller.CrobsException;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ReversibleChangeTest {
    @Test
    void recordsAnAlreadyAppliedChangeWithoutRunningCallbacks() throws CrobsException {
        AtomicInteger state = new AtomicInteger(1);
        ReversibleChange<String> change = ReversibleChange.applied("result", state::decrementAndGet, state::incrementAndGet);
        assertEquals("result", change.getValue());
        assertEquals(1, state.get());
        assertThrows(CrobsException.class, change::redo);
        change.undo();
        assertEquals(0, state.get());
        assertThrows(CrobsException.class, change::undo);
        change.redo();
        assertEquals(1, state.get());
        change.undo();
        assertEquals(0, state.get());
    }

    @Test
    void failedUndoCanBeRetriedWithoutChangingHistoryState() throws CrobsException {
        AtomicBoolean fail = new AtomicBoolean(true);
        AtomicInteger state = new AtomicInteger(1);
        ReversibleChange<Void> change = ReversibleChange.applied(null, () -> {
            if (fail.get()) {
                throw new CrobsException("Undo failed.");
            }
            state.set(0);
        }, () -> state.set(1));
        assertThrows(CrobsException.class, change::undo);
        assertEquals(1, state.get());
        fail.set(false);
        change.undo();
        assertEquals(0, state.get());
        change.redo();
        assertEquals(1, state.get());
    }

    @Test
    void failedRedoCanBeRetriedWithoutChangingHistoryState() throws CrobsException {
        AtomicBoolean fail = new AtomicBoolean(true);
        AtomicInteger state = new AtomicInteger(1);
        ReversibleChange<Void> change = ReversibleChange.applied(null, () -> state.set(0), () -> {
            if (fail.get()) {
                throw new CrobsException("Redo failed.");
            }
            state.set(1);
        });
        change.undo();
        assertThrows(CrobsException.class, change::redo);
        assertEquals(0, state.get());
        fail.set(false);
        change.redo();
        assertEquals(1, state.get());
    }

    @Test
    void requiresBothCallbacks() {
        assertThrows(NullPointerException.class, () -> ReversibleChange.applied(null, null, () -> { }));
        assertThrows(NullPointerException.class, () -> ReversibleChange.applied(null, () -> { }, null));
    }
}
