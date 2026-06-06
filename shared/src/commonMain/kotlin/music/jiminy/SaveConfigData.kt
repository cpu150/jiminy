package music.jiminy

import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList

@Stable
data class SaveConfigData(
    val options: SaveConfigOptions,
    val audioLinks: ImmutableList<JiminyLink>,
    val midiLinks: ImmutableList<JiminyLink>,
)
