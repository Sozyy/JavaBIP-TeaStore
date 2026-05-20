package generated;

import org.javabip.annotations.*;
import org.javabip.api.PortType;

@Ports({
    @Port(name = "recv_requested_time", type = PortType.enforceable),
    @Port(name = "recv_resulting_time", type = PortType.enforceable),
    @Port(name = "recv_enabled", type = PortType.enforceable),
    @Port(name = "send_knob", type = PortType.enforceable)
})
@ComponentType(name = "ctrl", initial = "INIT")
public class Ctrl {

    public Ctrl() {
        // TODO: initialisation des champs
    }

    // === TRANSITIONS ===

    @Transition(name = "recv_requested_time", source = "INIT", target = "requested_time_RCVD", guard = "")
    public void step1() {
        // TODO: logique de la transition
    }

    @Transition(name = "recv_resulting_time", source = "requested_time_RCVD", target = "resulting_time_RCVD", guard = "")
    public void step2() {
        // TODO: logique de la transition
    }

    @Transition(name = "recv_enabled", source = "resulting_time_RCVD", target = "enabled_RCVD", guard = "")
    public void step3() {
        // TODO: logique de la transition
    }

    @Transition(name = "send_knob", source = "enabled_RCVD", target = "knob_SENT", guard = "")
    public void step4() {
        // TODO: logique de la transition
    }

    @Transition(name = "loop_back", source = "knob_SENT", target = "INIT", guard = "")
    public void loopBack() {
        // TODO: logique de la transition
    }

    // === DATA WIRES (getters) ===

    // Importer DataOut pour AccessType.any :
    // import org.javabip.api.DataOut;

    @Data(name = "knob", accessTypePort = DataOut.AccessType.any)
    public Object getKnob() {
        // TODO: retourner la valeur de la donnée
        return null;
    }

}
