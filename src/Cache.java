package generated;

import org.javabip.annotations.*;
import org.javabip.api.PortType;

@Ports({
    @Port(name = "recv_requestedData", type = PortType.enforceable),
    @Port(name = "recv_cacheSize", type = PortType.enforceable),
    @Port(name = "send_foundInCache", type = PortType.enforceable)
})
@ComponentType(name = "cache", initial = "INIT")
public class Cache {

    public Cache() {
        // TODO: initialisation des champs
    }

    // === TRANSITIONS ===

    @Transition(name = "recv_requestedData", source = "INIT", target = "requestedData_RCVD", guard = "")
    public void step1() {
        // TODO: logique de la transition
    }

    @Transition(name = "recv_cacheSize", source = "requestedData_RCVD", target = "cacheSize_RCVD", guard = "")
    public void step2() {
        // TODO: logique de la transition
    }

    @Transition(name = "send_foundInCache", source = "cacheSize_RCVD", target = "foundInCache_SENT", guard = "")
    public void step3() {
        // TODO: logique de la transition
    }

    @Transition(name = "loop_back", source = "foundInCache_SENT", target = "INIT", guard = "")
    public void loopBack() {
        // TODO: logique de la transition
    }

    // === DATA WIRES (getters) ===

    // Importer DataOut pour AccessType.any :
    // import org.javabip.api.DataOut;

    @Data(name = "foundInCache", accessTypePort = DataOut.AccessType.any)
    public Object getFoundInCache() {
        // TODO: retourner la valeur de la donnée
        return null;
    }

}
