package music.jiminy

data class JiminyLink(
    val instrumentDevices: List<JiminyAudioDevice>,
    val speakerDevice: JiminyAudioDevice,
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
                .filter { otherDevice -> !mergedInstrumentDevices.contains(otherDevice) }
                .forEach { add(it) }
        } + mergedInstrumentDevices
    }.let { instrumentDevices ->
        JiminyLink(instrumentDevices.sortedBy { it.name }, speakerDevice + other.speakerDevice)
    }
}

fun JiminyLink.instrumentNodes(
    dev: JiminyAudioDevice,
    node: JiminyDeviceNode? = null,
) = if (dev == speakerDevice) {
    instrumentDevices.flatMap { it.nodes() }
} else if (node != null) {
    listOf(node)
} else {
    dev.nodes()
}

fun JiminyLink.speakerNodes(
    dev: JiminyAudioDevice,
    node: JiminyDeviceNode? = null,
) = if (dev != speakerDevice) {
    speakerDevice.nodes()
} else if (node != null) {
    listOf(node)
} else {
    dev.nodes()
}

fun JiminyLink.disconnectionNodesList(
    dev: JiminyAudioDevice,
    node: JiminyDeviceNode? = null,
) = buildList {
    instrumentNodes(dev, node).forEach { instrument ->
        speakerNodes(dev, node).forEach { speaker ->
            add(instrument to speaker)
        }
    }
}
