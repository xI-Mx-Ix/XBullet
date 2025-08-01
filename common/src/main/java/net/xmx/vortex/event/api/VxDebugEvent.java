package net.xmx.vortex.event.api;

import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import java.util.List;

public class VxDebugEvent {

    /**
     * Fired when the debug screen text is being gathered.
     * Allows adding custom lines to the F3 overlay.
     */
    public static class AddDebugInfo {
        public static final Event<Listener> EVENT = EventFactory.createLoop();
        private final List<String> infoList;

        public AddDebugInfo(List<String> infoList) {
            this.infoList = infoList;
        }

        public List<String> getInfoList() {
            return infoList;
        }

        @FunctionalInterface
        public interface Listener {
            void onAddDebugInfo(AddDebugInfo event);
        }
    }
}