package music.jiminy

interface JiminyDeviceI<T> {
    val name: String
    val displayName: String
    val speakers: List<JiminyDeviceNodeI>
    val instruments: List<JiminyDeviceNodeI>
    fun nodes(): List<JiminyDeviceNodeI>
    operator fun plus(other: T): T
}

interface JiminyDeviceNodeI {
    val fullName: String
    val deviceName: String
    val displayName: String
    val displayPortName: String
    val type: JiminyDeviceNodeType
}

data class JiminyLink<T : JiminyDeviceI<T>>(
    val instrumentDevices: List<T>,
    val speakerDevice: T,
) {
    operator fun plus(other: JiminyLink<T>) = instrumentDevices.map { instrumentDevice ->
        var instrument = instrumentDevice
        other.instrumentDevices.forEach { otherInstrument ->
            if (instrument.name == otherInstrument.name) {
                instrument += otherInstrument
            }
        }
        instrument
    }.let { mergedInstrumentDevices ->
        buildList {
            other
                .instrumentDevices
                .filter { otherDevice -> !mergedInstrumentDevices.any { it.name == otherDevice.name } }
                .forEach { add(it) }
        } + mergedInstrumentDevices
    }.let { instrumentDevices ->
        JiminyLink(instrumentDevices.sortedBy { it.name }, speakerDevice + other.speakerDevice)
    }
}

fun <T : JiminyDeviceI<T>> JiminyLink<T>.instrumentNodes(
    dev: T,
    node: JiminyDeviceNodeI? = null,
) = if (dev.name == speakerDevice.name) {
    instrumentDevices.flatMap { it.nodes() }
} else if (node != null) {
    listOf(node)
} else {
    dev.nodes()
}

fun <T : JiminyDeviceI<T>> JiminyLink<T>.speakerNodes(
    dev: T,
    node: JiminyDeviceNodeI? = null,
) = if (dev.name != speakerDevice.name) {
    speakerDevice.nodes()
} else if (node != null) {
    listOf(node)
} else {
    dev.nodes()
}

fun <T : JiminyDeviceI<T>> JiminyLink<T>.disconnectionNodesList(
    dev: T,
    node: JiminyDeviceNodeI? = null,
) = buildList {
    instrumentNodes(dev, node).forEach { instrument ->
        speakerNodes(dev, node).forEach { speaker ->
            add(instrument to speaker)
        }
    }
}
