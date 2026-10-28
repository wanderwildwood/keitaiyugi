package com.swordfish.lemuroid.app.utils.android.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alorma.compose.settings.storage.base.SettingValueState
import com.alorma.compose.settings.ui.SettingsMenuLink
import com.alorma.compose.settings.ui.SettingsSlider
import com.alorma.compose.settings.ui.SettingsSwitch
import com.mudita.mmd.components.text.TextMMD
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.PagedColumn
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * A settings screen: each `item` is one group, and the list pages a whole group at a time
 * (PagedColumn) rather than scrolling freely.
 */
@Composable
fun LemuroidSettingsPage(
    modifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit,
) {
    PagedColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content,
    )
}

@Composable
fun LemuroidSettingsSwitch(
    enabled: Boolean = true,
    state: SettingValueState<Boolean>,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable () -> Unit,
    subtitle: @Composable (() -> Unit)? = null,
    onCheckedChange: (Boolean) -> Unit = {},
) {
    SettingsSwitch(
        enabled = enabled,
        state = state.value,
        icon = icon,
        title = title,
        subtitle = subtitle,
        onCheckedChange = {
            state.value = it
            onCheckedChange(it)
        },
        colors = lemuroidSettingsColor(enabled),
    )
}

@Composable
fun LemuroidSettingsMenuLink(
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
    title: @Composable () -> Unit,
    subtitle: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    SettingsMenuLink(
        enabled = enabled,
        icon = icon,
        title = title,
        subtitle = subtitle,
        action = action,
        onClick = onClick,
        colors = lemuroidSettingsColor(enabled),
    )
}

@Composable
fun LemuroidSettingsGroup(
    modifier: Modifier = Modifier,
    title: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface {
        Column(
            modifier = modifier.fillMaxWidth(),
        ) {
            if (title != null) {
                SettingsGroupTitleSmall(title)
            }
            content()
        }
    }
}

@Composable
fun LemuroidCardSettingsGroup(
    modifier: Modifier = Modifier,
    title: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface {
        Column(
            modifier =
                modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp),
        ) {
            OutlinedCard {
                if (title != null) {
                    SettingsGroupTitleSmall(title)
                }
                content()
            }
        }
    }
}

@Composable
fun LemuroidSettingsSlider(
    modifier: Modifier = Modifier,
    state: SettingValueState<Int>,
    steps: Int,
    enabled: Boolean,
    valueRange: ClosedFloatingPointRange<Float>,
    title: @Composable () -> Unit,
    subtitle: @Composable () -> Unit = { },
) {
    val defaultColors = ListItemDefaults.colors()

    SettingsSlider(
        modifier = modifier,
        steps = steps,
        value = state.value.toFloat(),
        onValueChange = { state.value = it.roundToInt() },
        valueRange = valueRange,
        title = title,
        subtitle = subtitle,
        enabled = enabled,
        colors = defaultColors,
    )
}

@Composable
private fun SettingsGroupTitleSmall(title: @Composable () -> Unit) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        val primary = MaterialTheme.colorScheme.primary
        val titleStyle = MaterialTheme.typography.labelLarge.copy(color = primary)
        ProvideTextStyle(value = titleStyle) { title() }
    }
}

/**
 * The same colours enabled or not. A disabled row used to draw in grey, which on this panel is
 * a dither; the row says why it is off in words instead.
 */
@Composable
private fun lemuroidSettingsColor(
    @Suppress("UNUSED_PARAMETER") enabled: Boolean,
): ListItemColors = ListItemDefaults.colors()

/** How long an armed row waits for its second tap before it disarms itself. */
const val ARMED_ROW_MILLIS = 6_000L

/**
 * A row that asks before it acts. The first tap arms it and it says "<label> — tap again"; a
 * second tap within six seconds does it, and after six it goes back to what it was. Six, not
 * fewer: the panel needs a second or two just to show the prompt.
 */
@Composable
fun LemuroidArmedSettingsLink(
    title: String,
    armedTitle: String,
    icon: (@Composable () -> Unit)? = null,
    subtitle: (@Composable () -> Unit)? = null,
    onConfirmed: () -> Unit,
) {
    var armed by remember { mutableStateOf(false) }
    LaunchedEffect(armed) {
        if (armed) {
            delay(ARMED_ROW_MILLIS)
            armed = false
        }
    }
    LemuroidSettingsMenuLink(
        icon = icon,
        title = { TextMMD(text = if (armed) armedTitle else title) },
        subtitle = if (armed) null else subtitle,
        onClick = {
            if (armed) {
                armed = false
                onConfirmed()
            } else {
                armed = true
            }
        },
    )
}
