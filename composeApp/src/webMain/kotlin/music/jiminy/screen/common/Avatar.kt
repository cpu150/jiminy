package music.jiminy.screen.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import jiminy.composeapp.generated.resources.Res
import jiminy.composeapp.generated.resources.bass2
import jiminy.composeapp.generated.resources.drums3
import jiminy.composeapp.generated.resources.guitar2
import jiminy.composeapp.generated.resources.mic2
import jiminy.composeapp.generated.resources.raspberry_pi2
import jiminy.composeapp.generated.resources.snd_interface2
import jiminy.composeapp.generated.resources.synth1
import music.jiminy.AvatarIconsEnum
import music.jiminy.JiminyDevice
import org.jetbrains.compose.resources.painterResource

fun AvatarIconsEnum.toResource() = when (this) {
    AvatarIconsEnum.GT_1000 -> Res.drawable.guitar2
    AvatarIconsEnum.QUAD_CORTEX -> Res.drawable.bass2
    AvatarIconsEnum.ROLAND_TD_07 -> Res.drawable.drums3
    AvatarIconsEnum.FLUIDSYNTH -> Res.drawable.synth1
    AvatarIconsEnum.SND_CARD_U_GREEN -> Res.drawable.raspberry_pi2
    AvatarIconsEnum.SND_CARD_RED -> Res.drawable.snd_interface2
    AvatarIconsEnum.SND_CARD_24 -> Res.drawable.snd_interface2
    AvatarIconsEnum.MIC_SHURE_MV88 -> Res.drawable.mic2
    AvatarIconsEnum.Unknown -> null
}

@Composable
fun DeviceAvatar(
    device: () -> JiminyDevice,
    modifier: Modifier = Modifier,
) {
    val device = device()
    val avatarImg = device.avatarIcon.toResource()

    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        val modifierImg = Modifier.fillMaxSize().padding(4.dp)
        if (avatarImg != null) {
            Image(
                painter = painterResource(avatarImg),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = modifierImg,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface.copy(alpha = .75f)),
            )
        } else {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = "Generic Device",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = modifierImg,
            )
        }
    }
}
