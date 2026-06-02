package music.jiminy

import kotlinx.serialization.Serializable

fun JiminyDeviceNode.getAvatar() = deviceNameToAvatar[deviceName] ?: AvatarIconsEnum.Unknown

@Serializable
enum class JiminyDeviceType {
    Audio,
    Midi,
}

@Serializable
data class JiminyDevice(
    val name: String,
    val type: JiminyDeviceType,
) {
    val alias = deviceNameToAlias[name]
    val displayName = alias ?: name

    private val _speakers = mutableListOf<JiminyDeviceNode>()
    val speakers: List<JiminyDeviceNode>
        get() = _speakers

    private val _instruments = mutableListOf<JiminyDeviceNode>()
    val instruments: List<JiminyDeviceNode>
        get() = _instruments

    private val _volumes = mutableListOf<JiminyVolume>()
    val volumes: List<JiminyVolume>
        get() = _volumes

    fun removeNode(node: JiminyDeviceNode) {
        when (node.type) {
            JiminyDeviceNodeType.Speaker -> _speakers.remove(node)
            JiminyDeviceNodeType.Instrument -> _instruments.remove(node)
            else -> Unit
        }
    }

    fun addNode(node: JiminyDeviceNode) {
        val nodeList = when (node.type) {
            JiminyDeviceNodeType.Speaker -> _speakers
            JiminyDeviceNodeType.Instrument -> _instruments
            else -> null
        }

        nodeList?.add(node)
        nodeList?.sortBy { it.fullName }
    }

    fun addNodes(nodes: List<JiminyDeviceNode>) {
        nodes.forEach { addNode(it) }
    }

    fun nodes(): List<JiminyDeviceNode> = _speakers + _instruments

    fun addVolume(volume: JiminyVolume) = _volumes.add(volume)
        .also { _volumes.sortBy { it.type } }

    operator fun plus(other: JiminyDevice) = JiminyDevice(
        name = name,
        type = type,
    ).also { new ->
        val speakers = _speakers + other._speakers.filter { !_speakers.contains(it) }
        val instruments = _instruments + other._instruments.filter { !_instruments.contains(it) }

        new.addNodes(speakers + instruments)
    }

    override fun toString() =
        "$displayName, ${_instruments.count()} instrument(s), ${_speakers.count()} speaker(s), ${_volumes.count()} volume device(s)"

    override fun equals(other: Any?) = when (other) {
        is JiminyDevice -> {
            name == other.name &&
                    type == other.type &&
                    _speakers == other._speakers &&
                    _instruments == other._instruments
        }

        else -> super.equals(other)
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + _speakers.hashCode()
        result = 31 * result + _instruments.hashCode()
        return result
    }
}

@Serializable
data class JiminyDeviceNode(
    val fullName: String,
    val deviceName: String,
    val portName: String,
    val type: JiminyDeviceNodeType,
) {
    val aliasName = deviceNameToAlias[deviceName]
    val displayName = aliasName ?: deviceName
    val aliasPortName = deviceNameToAlias["$deviceName:$portName"]
    val displayPortName = aliasPortName ?: portName

    override fun toString() = "$type name: $displayName port: $displayPortName fullName: $fullName"
    override fun hashCode() = fullName.hashCode()
    override fun equals(other: Any?) = when (other) {
        is JiminyDeviceNode -> fullName == other.fullName
        else -> super.equals(other)
    }
}

@Serializable
data class JiminyVolume(
    val id: String,
    val volume: Float, // 0.0 - 1.0
    val type: JiminyDeviceNodeType,
    val mute: Boolean,
) {
    override fun toString() = "$type [id: $id - vol: $volume]"

    override fun equals(other: Any?) = when (other) {
        is JiminyVolume -> id == other.id && type == other.type
        else -> super.equals(other)
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }
}

@Serializable
enum class JiminyDeviceNodeType {
    Instrument,
    Speaker,
    Unknown,
}
