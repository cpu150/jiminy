package music.jiminy

data class NodeConnection(
    val instrument: JiminyDeviceNode,
    val speaker: JiminyDeviceNode,
)

data class JiminyLink(
    val instrumentDevices: List<JiminyDevice>,
    val speakerDevice: JiminyDevice,
) {
    operator fun plus(other: JiminyLink) = instrumentDevices.map { instrumentDevice ->
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
        JiminyLink(
            instrumentDevices = instrumentDevices.sortedBy { it.name },
            speakerDevice = speakerDevice + other.speakerDevice,
        )
    }
}

fun JiminyLink.instrumentNodes(
    dev: JiminyDevice,
    node: JiminyDeviceNode? = null,
) = if (dev.name == speakerDevice.name) {
    instrumentDevices.flatMap { it.nodes() }
} else if (node != null) {
    listOf(node)
} else {
    dev.nodes()
}

fun JiminyLink.speakerNodes(
    dev: JiminyDevice,
    node: JiminyDeviceNode? = null,
) = if (dev.name != speakerDevice.name) {
    speakerDevice.nodes()
} else if (node != null) {
    listOf(node)
} else {
    dev.nodes()
}

fun JiminyLink.disconnectionNodesList(
    dev: JiminyDevice,
    node: JiminyDeviceNode? = null,
) = buildList {
    instrumentNodes(dev, node).forEach { instrument ->
        speakerNodes(dev, node).forEach { speaker ->
            add(NodeConnection(instrument, speaker))
        }
    }
}
