import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge
import spoon.Launcher
import spoon.reflect.code.CtExpression
import spoon.reflect.code.CtNewArray
import spoon.reflect.declaration.*
import spoon.reflect.reference.CtArrayTypeReference
import spoon.reflect.reference.CtPackageReference
import spoon.reflect.reference.CtReference
import spoon.reflect.reference.CtTypeReference

const val SOURCES_PATH = "src/main/resources/spoon/ChildServiceSupport.java"
const val CLASS_NAME = "ChildServiceSupport"

fun main(args: Array<String>) {
    println("Hello World!")

    val launcher = Launcher()
    launcher.addInputResource(SOURCES_PATH) //(sources)
    try {
        launcher.buildModel()
    } catch (e: Exception) {
        println(e.message)
    }
    //return launcher.model


    val model = launcher.model //buildSourcesModel(SOURCES_PATH)
    val fsm = DefaultDirectedGraph<FSMState, FSMEdge>(FSMEdge::class.java)

    populateFSM(fsm)
    getClassSpoon(CLASS_NAME, model)?.let {
        bipalizeClass(it, fsm)
        launcher.createOutputWriter().createJavaFile(it)
    }
}

fun bipalizeClass(ctClass: CtClass<*>, fsm: DefaultDirectedGraph<FSMState, FSMEdge>) {
    addPorts(ctClass, fsm)
    addComponent(ctClass, fsm)
    addTransactions(ctClass, fsm)
}



private fun addPorts(
    ctClass: CtClass<*>,
    fsm: DefaultDirectedGraph<FSMState, FSMEdge>
) {
    val factory = ctClass.factory
    val ports = factory.createAnnotation<Annotation>()
    val annotationType = ports.createAnnotationType("Ports", "org.apache.camel.support")
    ports.setAnnotationType<CtAnnotation<Annotation>>(annotationType)
    ports.setType<CtAnnotation<Annotation>>(annotationType)

    ports.setParent(ctClass)

    val elementValues = factory.createNewArray<CtArrayTypeReference<Annotation>>()

    fsm.edgeSet().forEach {
        val portAnnotation: CtAnnotation<Annotation> = factory.createAnnotation()
        val portAnnotationType = portAnnotation.createAnnotationType("Port", "org.apache.camel.support")
        portAnnotation.setAnnotationType<CtAnnotation<Annotation>>(portAnnotationType)
        portAnnotation.setType<CtAnnotation<Annotation>>(portAnnotationType)
        portAnnotation.setParent(elementValues)

        portAnnotation.addValue<CtAnnotation<Annotation>>("name", it.toString())
        portAnnotation.addValue<CtAnnotation<Annotation>>("type", "PortType.enforceable")
        elementValues.addElement< CtNewArray<CtArrayTypeReference<Annotation>>>(portAnnotation)
    }

    val elementValuesType = factory.createArrayTypeReference<CtArrayTypeReference<Annotation>>()
    val componentType = factory.createTypeReference<CtExpression<*>>()
    componentType.setPackage<CtTypeReference<CtExpression<*>>>(createPackage(componentType, "org.apache.camel.support"))
    componentType.setSimpleName<CtReference>("Port")
    componentType.setParent(elementValuesType)

    elementValuesType.setComponentType<CtArrayTypeReference<CtArrayTypeReference<Annotation>>>(componentType)
    elementValuesType.setSimpleName<CtReference>("")
    elementValuesType.setParent(componentType)
    componentType.setPackage<CtTypeReference<CtExpression<*>>>(createPackage( componentType,"org.apache.camel.support"))

    elementValues.setType<CtTypedElement<*>>(elementValuesType)

    ports.addValue<CtAnnotation<Annotation>>("value", elementValues)


    ctClass.addAnnotation<CtElement>(ports)
}

private fun addTransactions(
    ctClass: CtClass<*>,
    fsm: DefaultDirectedGraph<FSMState, FSMEdge>
) {

    val factory = ctClass.factory

    fsm.edgeSet()
        .groupBy { it.label }
        .forEach { (name, edgesList) ->

            //TODO remove toLowerCase call
            ctClass.methods.find { it.simpleName == name.toLowerCase() }?.let { ctMethod ->

                val transitions = factory.createAnnotation<Annotation>()
                val annotationType = transitions.createAnnotationType("Transitions", "org.apache.camel.support")
                transitions.setAnnotationType<CtAnnotation<Annotation>>(annotationType)
                transitions.setType<CtAnnotation<Annotation>>(annotationType)

                val elementValues = factory.createNewArray<CtArrayTypeReference<Annotation>>()

                transitions.setParent(ctMethod)

                edgesList.forEach { edge ->
                    val transitionAnnotation = factory.createAnnotation<Annotation>()

                    val transitionAnnotationType =
                        transitionAnnotation.createAnnotationType("Transition", "org.apache.camel.support")
                    transitionAnnotation.setAnnotationType<CtAnnotation<Annotation>>(transitionAnnotationType)
                    transitionAnnotation.setType<CtAnnotation<Annotation>>(transitionAnnotationType)

                    transitionAnnotation.setParent(elementValues)
                    transitionAnnotation.addValue<CtAnnotation<Annotation>>("name", edge.label)
                    transitionAnnotation.addValue<CtAnnotation<Annotation>>("source", edge.source)
                    transitionAnnotation.addValue<CtAnnotation<Annotation>>("target", edge.target)
                    transitionAnnotation.addValue<CtAnnotation<Annotation>>("guard", "")

                    elementValues.addElement<CtNewArray<CtArrayTypeReference<Annotation>>>(transitionAnnotation)
                }

                val elementValuesType = factory.createArrayTypeReference<CtArrayTypeReference<Annotation>>()
                val componentType = factory.createTypeReference<CtExpression<*>>()
                componentType.setPackage<CtTypeReference<CtExpression<*>>>(
                    createPackage(
                        componentType,
                        "org.apache.camel.support"
                    )
                )
                componentType.setSimpleName<CtReference>("Transition")
                componentType.setParent(elementValuesType)

                elementValuesType.setComponentType<CtArrayTypeReference<CtArrayTypeReference<Annotation>>>(componentType)
                elementValuesType.setSimpleName<CtReference>("")
                elementValuesType.setParent(componentType)
                componentType.setPackage<CtTypeReference<CtExpression<*>>>(
                    createPackage(
                        componentType,
                        "org.apache.camel.support"
                    )
                )

                elementValues.setType<CtTypedElement<*>>(elementValuesType)

                transitions.addValue<CtAnnotation<Annotation>>("value", elementValues)


                ctMethod.addAnnotation<CtElement>(transitions)
            }
        }
}

private fun addComponent(
    ctClass: CtClass<*>,
    fsm: DefaultDirectedGraph<FSMState, FSMEdge>
) {
    val componentAnnotation = ctClass.factory.createAnnotation<Annotation>()
    val annotationType = componentAnnotation.createAnnotationType("ComponentType", "org.apache.camel.support")

    componentAnnotation.setAnnotationType<CtAnnotation<Annotation>>(annotationType)
    componentAnnotation.setElementValues(fsm)
    componentAnnotation.setType<CtAnnotation<Annotation>>(annotationType)
    componentAnnotation.setParent(ctClass)

    ctClass.addAnnotation<CtElement>(componentAnnotation)
}

private fun CtAnnotation<Annotation>.setElementValues(fsm: DefaultDirectedGraph<FSMState, FSMEdge>) {
    this.addValue<CtAnnotation<Annotation>>("name", CLASS_NAME)

    fsm.vertexSet().find { fsm.incomingEdgesOf(it).isEmpty() }?.let {
        this.addValue<CtAnnotation<Annotation>>("initial", it.name)
    }
}

fun CtAnnotation<Annotation>.createAnnotationType(annotationSimpleName: String, packageSimpleName: String): CtTypeReference<Annotation>? {
    val tr = factory.createTypeReference<Annotation>()
    tr.setPackage<CtTypeReference<Annotation>>(createPackage(tr, packageSimpleName))
    tr.setSimpleName<CtReference>(annotationSimpleName)
    tr.setParent(this)

    return tr
}

fun createPackage(typeReference: CtTypeReference<*>, packageSimpleName: String): CtPackageReference? {
    val pr = typeReference.factory.createPackageReference()
    pr.setSimpleName<CtReference>(packageSimpleName)
    pr.setParent(typeReference)

    return pr
}

class FSMState(val name: String, val obj: Any = ""){
    override fun toString() = name

    override fun equals(other: Any?): Boolean  =
        (other as? FSMState)?.let {
            other.obj == obj && other.name == name
        } ?: false

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + obj.hashCode()
        return result
    }
}

class FSMEdge(val label: String, val source: String, val target: String) : DefaultEdge() {
    override fun toString(): String {
        return label
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FSMEdge

        if (label != other.label) return false
        if (source != other.source) return false
        if (target != other.target) return false

        return true
    }

    override fun hashCode(): Int {
        var result = label.hashCode()
        result = 31 * result + source.hashCode()
        result = 31 * result + target.hashCode()
        return result
    }
}

const val NEW = "NEW"
const val STARTING = "STARTING"
const val STARTED = "STARTED"
const val STOPPING = "STOPPING"
const val STOPPED = "STOPPED"
const val SHUTTING = "SHUTTING"
const val SHUTDOWN = "SHUTDOWN"
const val FAILED = "FAILED"

const val START = "START"
const val STOP = "STOP"
const val SHUT = "SHUT"

enum class FSMStates {
    NEW, STARTED, STARTING, STOPPED, STOPPING, SHUTTING, SHUTDOWN, FAILED
}