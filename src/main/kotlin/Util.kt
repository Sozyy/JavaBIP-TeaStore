import org.jgrapht.graph.DefaultDirectedGraph
import spoon.Launcher
import spoon.reflect.CtModel
import spoon.reflect.declaration.CtClass
import spoon.reflect.visitor.filter.TypeFilter

//ADDED FOR TESTING PURPOSES, TO BE REMOVED LATER
fun populateFSM(fsm: DefaultDirectedGraph<FSMState, FSMEdge>) {
    FSMStates.values().forEach { fsm.addVertex(FSMState(it.toString())) }

    //start edge
    fsm.addEdge(FSMState(NEW), FSMState(STARTED), FSMEdge(START, NEW, STARTED))
    fsm.addEdge(FSMState(NEW), FSMState(FAILED), FSMEdge(START, NEW, FAILED))

    fsm.addEdge(FSMState(STARTED), FSMState(STARTED), FSMEdge(START, STARTED, STARTED))
    fsm.addEdge(FSMState(STARTING), FSMState(STARTING), FSMEdge(START, STARTING, STARTING))

    fsm.addEdge(FSMState(STOPPING), FSMState(FAILED), FSMEdge(START, STOPPING, FAILED))
    fsm.addEdge(FSMState(STOPPED), FSMState(FAILED), FSMEdge(START, STOPPED, FAILED))
    fsm.addEdge(FSMState(SHUTTING), FSMState(FAILED), FSMEdge(START, SHUTTING, FAILED))
    fsm.addEdge(FSMState(SHUTDOWN), FSMState(FAILED), FSMEdge(START, SHUTDOWN, FAILED))

    fsm.addEdge(FSMState(STOPPING), FSMState(STARTED), FSMEdge(START, STOPPING, STARTED))
    fsm.addEdge(FSMState(STOPPED), FSMState(STARTED), FSMEdge(START, STOPPED, FAILED))
    fsm.addEdge(FSMState(SHUTTING), FSMState(STARTED), FSMEdge(START, SHUTTING, STARTED))
    fsm.addEdge(FSMState(SHUTDOWN), FSMState(STARTED), FSMEdge(START, SHUTDOWN, STARTED))


    //stop edge
    fsm.addEdge(FSMState(STOPPING), FSMState(STOPPING), FSMEdge(STOP, STOPPING, STOPPING))
    fsm.addEdge(FSMState(STOPPED), FSMState(STOPPED), FSMEdge(STOP, STOPPED, STOPPED))
    fsm.addEdge(FSMState(SHUTTING), FSMState(SHUTTING), FSMEdge(STOP, SHUTTING, SHUTTING))
    fsm.addEdge(FSMState(SHUTDOWN), FSMState(SHUTDOWN), FSMEdge(STOP, SHUTDOWN, SHUTDOWN))

    fsm.addEdge(FSMState(STARTED), FSMState(FAILED), FSMEdge(STOP, STARTED, FAILED))
    fsm.addEdge(FSMState(STARTING), FSMState(FAILED), FSMEdge(STOP, STARTING, FAILED))
    fsm.addEdge(FSMState(STARTING), FSMState(STOPPED), FSMEdge(STOP, STARTING, STOPPED))
    fsm.addEdge(FSMState(STARTED), FSMState(STOPPED), FSMEdge(STOP, STARTED, STOPPED))


    //shut edge
    fsm.addEdge(FSMState(SHUTTING), FSMState(SHUTTING), FSMEdge(SHUT, SHUTTING, SHUTTING))
    fsm.addEdge(FSMState(SHUTDOWN), FSMState(SHUTDOWN), FSMEdge(SHUT, SHUTDOWN, SHUTDOWN))

    fsm.addEdge(FSMState(STARTED), FSMState(FAILED), FSMEdge(SHUT, STARTED, FAILED))
    fsm.addEdge(FSMState(STARTING), FSMState(FAILED), FSMEdge(SHUT, STARTING, FAILED))
    fsm.addEdge(FSMState(STOPPING), FSMState(FAILED), FSMEdge(SHUT, STOPPING, FAILED))
    fsm.addEdge(FSMState(STOPPED), FSMState(FAILED), FSMEdge(SHUT, STOPPED, FAILED))

    fsm.addEdge(FSMState(STARTING), FSMState(SHUTDOWN), FSMEdge(SHUT, STARTING, SHUTDOWN))
    fsm.addEdge(FSMState(STARTED), FSMState(SHUTDOWN), FSMEdge(SHUT, STARTED, SHUTDOWN))
    fsm.addEdge(FSMState(STOPPING), FSMState(SHUTDOWN), FSMEdge(SHUT, STOPPING, SHUTDOWN))
    fsm.addEdge(FSMState(STOPPED), FSMState(SHUTDOWN), FSMEdge(SHUT, STOPPED, SHUTDOWN))
}


fun buildSourcesModel(sources: String): CtModel {
    val launcher = Launcher()
    launcher.addInputResource(sources)
    try {
        launcher.buildModel()
    } catch (e: Exception) {
        println(e.message)
    }
    return launcher.model
}

fun getClassSpoon(className: String, model: CtModel)
        = model.getElements(TypeFilter(CtClass::class.java)).find { it.simpleName == className }
