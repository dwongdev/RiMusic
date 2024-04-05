package it.fast4x.rimusic.ui.screens.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import it.fast4x.compose.persist.PersistMapCleanup
import it.fast4x.compose.routing.RouteHandler
import it.fast4x.rimusic.Database
import it.fast4x.rimusic.R
import it.fast4x.rimusic.enums.SearchType
import it.fast4x.rimusic.models.SearchQuery
import it.fast4x.rimusic.transaction
import it.fast4x.rimusic.ui.components.themed.IconButton
import it.fast4x.rimusic.ui.components.Scaffold
import it.fast4x.rimusic.ui.screens.globalRoutes
import it.fast4x.rimusic.ui.screens.homeRoute
import it.fast4x.rimusic.ui.screens.searchResultRoute
import it.fast4x.rimusic.ui.screens.searchRoute
import it.fast4x.rimusic.ui.screens.searchresult.SearchResultScreen
import it.fast4x.rimusic.ui.styling.LocalAppearance
import it.fast4x.rimusic.ui.styling.favoritesIcon
import it.fast4x.rimusic.utils.pauseSearchHistoryKey
import it.fast4x.rimusic.utils.preferences
import it.fast4x.rimusic.utils.secondary
@ExperimentalMaterialApi
@ExperimentalTextApi
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@UnstableApi
@Composable
fun SearchTypeScreen(
    searchType: SearchType,
) {
    val preferences = LocalContext.current.preferences
    val saveableStateHolder = rememberSaveableStateHolder()
    val (colorPalette) = LocalAppearance.current

    val initialTextInput = ""

    val (tabIndex, onTabChanged) = remember {
        mutableIntStateOf(searchType.index)
    }

    val (textFieldValue, onTextFieldValueChanged) = rememberSaveable(
        initialTextInput,
        stateSaver = TextFieldValue.Saver
    ) {
        mutableStateOf(
            TextFieldValue(
                text = initialTextInput,
                selection = TextRange(initialTextInput.length)
            )
        )
    }

    PersistMapCleanup(tagPrefix = "search/")

    RouteHandler(listenToGlobalEmitter = true) {
        globalRoutes()
        searchResultRoute { query ->
            SearchResultScreen(
                query = query,
                onSearchAgain = {
                    searchRoute(query)
                }
            )
        }

        val onGoToHome = homeRoute::global

        host {
            val decorationBox: @Composable (@Composable () -> Unit) -> Unit = { innerTextField ->
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier
                        // .weight(1f)
                        .padding(horizontal = 10.dp)
                ) {
                    IconButton(
                        onClick = {},
                        icon = R.drawable.search,
                        color = colorPalette.favoritesIcon,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(24.dp)
                    )
                }
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier
                        // .weight(1f)
                        .padding(horizontal = 40.dp)
                ) {
                    AnimatedVisibility(
                        visible = textFieldValue.text.isEmpty(),
                        enter = fadeIn(tween(300)),
                        exit = fadeOut(tween(300)),
                        modifier = Modifier
                            .align(Alignment.Center)
                    ) {
                        BasicText(
                            text = stringResource(R.string.search), //stringResource(R.string.enter_a_name),
                            maxLines = 1,
                            style = LocalAppearance.current.typography.l.secondary,

                        )
                    }

                    innerTextField()
                }
            }

            Scaffold(
                topIconButtonId = R.drawable.chevron_back,
                onTopIconButtonClick = { onGoToHome() },
                topIconButton2Id = R.drawable.chevron_back,
                onTopIconButton2Click = pop,
                showButton2 = false,
                tabIndex = tabIndex,
                onTabChanged = onTabChanged,
                tabColumnContent = { Item ->
                    Item(0, stringResource(R.string.online), R.drawable.globe)
                    Item(1, stringResource(R.string.library), R.drawable.library)
                    Item(2, stringResource(R.string.go_to_link), R.drawable.link)
                }
            ) { currentTabIndex ->
                saveableStateHolder.SaveableStateProvider(currentTabIndex) {
                    when (currentTabIndex) {
                        0 -> OnlineSearch(
                            textFieldValue = textFieldValue,
                            onTextFieldValueChanged = onTextFieldValueChanged,
                            onSearch = { query ->
                                    //pop()
                                    searchResultRoute(query)
                                    if (!preferences.getBoolean(pauseSearchHistoryKey, false)) {
                                        transaction {
                                            Database.insert(SearchQuery(query = query))
                                        }
                                    }
                            },
                            onViewPlaylist = {}, //onViewPlaylist,
                            decorationBox = decorationBox,
                            onAction1 = { onTabChanged(0) },
                            onAction2 = { onTabChanged(1) },
                            onAction3 = { onTabChanged(2) },
                            onAction4 = { onGoToHome() },
                        )

                        1 -> LocalSongSearch(
                            textFieldValue = textFieldValue,
                            onTextFieldValueChanged = onTextFieldValueChanged,
                            decorationBox = decorationBox,
                            onAction1 = { onTabChanged(0) },
                            onAction2 = { onTabChanged(1) },
                            onAction3 = { onTabChanged(2) },
                            onAction4 = { onGoToHome() },
                        )

                        2 -> GoToLink(
                            textFieldValue = textFieldValue,
                            onTextFieldValueChanged = onTextFieldValueChanged,
                            decorationBox = decorationBox,
                            onAction1 = { onTabChanged(0) },
                            onAction2 = { onTabChanged(1) },
                            onAction3 = { onTabChanged(2) },
                            onAction4 = { onGoToHome() },
                        )
                    }
                }
            }
        }
    }
}
