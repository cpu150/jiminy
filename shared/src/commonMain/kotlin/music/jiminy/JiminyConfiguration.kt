package music.jiminy

import kotlinx.serialization.Serializable

@Serializable
data class JiminyConfiguration(
    val name: String,
    val links: List<JiminyCommand.Link>,
)
