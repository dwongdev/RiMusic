package it.fast4x.rimusic.ui.screens.localplaylist

import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.navigation.NavController
import com.github.doyaaaaaken.kotlincsv.client.KotlinCsvExperimental
import it.fast4x.compose.persist.persist
import it.fast4x.compose.persist.persistList
import it.fast4x.compose.reordering.animateItemPlacement
import it.fast4x.compose.reordering.draggedItem
import it.fast4x.compose.reordering.rememberReorderingState
import it.fast4x.compose.reordering.reorder
import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.bodies.BrowseBody
import it.fast4x.innertube.models.bodies.NextBody
import it.fast4x.innertube.requests.playlistPage
import it.fast4x.innertube.requests.relatedSongs
import it.fast4x.rimusic.Database
import it.fast4x.rimusic.EXPLICIT_PREFIX
import it.fast4x.rimusic.LocalPlayerServiceBinder
import it.fast4x.rimusic.MONTHLY_PREFIX
import it.fast4x.rimusic.PINNED_PREFIX
import it.fast4x.rimusic.PIPED_PREFIX
import it.fast4x.rimusic.R
import it.fast4x.rimusic.enums.NavRoutes
import it.fast4x.rimusic.enums.NavigationBarPosition
import it.fast4x.rimusic.enums.PlaylistSongSortBy
import it.fast4x.rimusic.enums.PopupType
import it.fast4x.rimusic.enums.RecommendationsNumber
import it.fast4x.rimusic.enums.ThumbnailRoundness
import it.fast4x.rimusic.enums.UiType
import it.fast4x.rimusic.models.PlaylistPreview
import it.fast4x.rimusic.models.Song
import it.fast4x.rimusic.models.SongEntity
import it.fast4x.rimusic.models.SongPlaylistMap
import it.fast4x.rimusic.query
import it.fast4x.rimusic.service.isLocal
import it.fast4x.rimusic.transaction
import it.fast4x.rimusic.ui.components.LocalMenuState
import it.fast4x.rimusic.ui.components.SwipeableQueueItem
import it.fast4x.rimusic.ui.components.themed.FloatingActionsContainerWithScrollToTop
import it.fast4x.rimusic.ui.components.themed.HeaderIconButton
import it.fast4x.rimusic.ui.components.themed.HeaderWithIcon
import it.fast4x.rimusic.ui.components.themed.IconButton
import it.fast4x.rimusic.ui.components.themed.IconInfo
import it.fast4x.rimusic.ui.components.themed.InPlaylistMediaItemMenu
import it.fast4x.rimusic.ui.components.themed.NowPlayingShow
import it.fast4x.rimusic.ui.components.themed.Playlist
import it.fast4x.rimusic.ui.components.themed.PlaylistsItemMenu
import it.fast4x.rimusic.ui.components.themed.SmartMessage
import it.fast4x.rimusic.ui.items.SongItem
import it.fast4x.rimusic.ui.styling.Dimensions
import it.fast4x.rimusic.ui.styling.onOverlay
import it.fast4x.rimusic.ui.styling.overlay
import it.fast4x.rimusic.ui.styling.px
import it.fast4x.rimusic.utils.addNext
import it.fast4x.rimusic.utils.addToPipedPlaylist
import it.fast4x.rimusic.utils.asMediaItem
import it.fast4x.rimusic.utils.autosyncKey
import it.fast4x.rimusic.utils.center
import it.fast4x.rimusic.utils.checkFileExists
import it.fast4x.rimusic.utils.color
import it.fast4x.rimusic.utils.completed
import it.fast4x.rimusic.utils.deleteFileIfExists
import it.fast4x.rimusic.utils.deletePipedPlaylist
import it.fast4x.rimusic.utils.disableScrollingTextKey
import it.fast4x.rimusic.utils.durationTextToMillis
import it.fast4x.rimusic.utils.enqueue
import it.fast4x.rimusic.utils.forcePlay
import it.fast4x.rimusic.utils.forcePlayAtIndex
import it.fast4x.rimusic.utils.forcePlayFromBeginning
import it.fast4x.rimusic.utils.formatAsTime
import it.fast4x.rimusic.utils.getDownloadState
import it.fast4x.rimusic.utils.getPipedSession
import it.fast4x.rimusic.utils.getTitleMonthlyPlaylist
import it.fast4x.rimusic.utils.isDownloadedSong
import it.fast4x.rimusic.utils.isLandscape
import it.fast4x.rimusic.utils.isPipedEnabledKey
import it.fast4x.rimusic.utils.isRecommendationEnabledKey
import it.fast4x.rimusic.utils.manageDownload
import it.fast4x.rimusic.utils.parentalControlEnabledKey
import it.fast4x.rimusic.utils.recommendationsNumberKey
import it.fast4x.rimusic.utils.rememberPreference
import it.fast4x.rimusic.utils.removeFromPipedPlaylist
import it.fast4x.rimusic.utils.renamePipedPlaylist
import it.fast4x.rimusic.utils.reorderInQueueEnabledKey
import it.fast4x.rimusic.utils.resetFormatContentLength
import it.fast4x.rimusic.utils.saveImageToInternalStorage
import it.fast4x.rimusic.utils.semiBold
import it.fast4x.rimusic.utils.showFloatingIconKey
import it.fast4x.rimusic.utils.syncSongsInPipedPlaylist
import it.fast4x.rimusic.utils.thumbnailRoundnessKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import me.knighthat.appContext
import me.knighthat.colorPalette
import me.knighthat.component.AddToFavorite
import me.knighthat.component.Enqueue
import me.knighthat.component.Export
import me.knighthat.component.IDialog
import me.knighthat.component.ItemSelector
import me.knighthat.component.ListenOnYouTube
import me.knighthat.component.PlayNext
import me.knighthat.component.PlaylistsMenu
import me.knighthat.component.ResetThumbnail
import me.knighthat.component.Search
import me.knighthat.component.Synchronize
import me.knighthat.component.ThumbnailPicker
import me.knighthat.component.header.TabToolBar
import me.knighthat.component.screen.PlaylistSongsSort
import me.knighthat.component.screen.PositionLock
import me.knighthat.component.screen.pin
import me.knighthat.component.tab.ExportSongsToCSVDialog
import me.knighthat.component.tab.LocateComponent
import me.knighthat.component.tab.toolbar.Button
import me.knighthat.component.tab.toolbar.ConfirmDialog
import me.knighthat.component.tab.toolbar.DelAllDownloadedDialog
import me.knighthat.component.tab.toolbar.Descriptive
import me.knighthat.component.tab.toolbar.DownloadAllDialog
import me.knighthat.component.tab.toolbar.MenuIcon
import me.knighthat.component.tab.toolbar.SongsShuffle
import me.knighthat.thumbnailShape
import me.knighthat.typography
import timber.log.Timber
import java.util.UUID


@KotlinCsvExperimental
@ExperimentalMaterialApi
@ExperimentalTextApi
@SuppressLint("SuspiciousIndentation")
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@ExperimentalComposeUiApi
@UnstableApi
@Composable
fun LocalPlaylistSongs(
    navController: NavController,
    playlistId: Long,
    onDelete: () -> Unit,
) {
    // Essentials
    val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val uriHandler = LocalUriHandler.current

    var playlistSongs by persistList<SongEntity>("localPlaylist/$playlistId/songs")
    var playlistPreview by persist<PlaylistPreview?>("localPlaylist/playlist")

    // Non-vital
    val parentalControlEnabled by rememberPreference(parentalControlEnabledKey, false)
    val isPipedEnabled by rememberPreference(isPipedEnabledKey, false)
    val disableScrollingText by rememberPreference(disableScrollingTextKey, false)
    val pipedSession = getPipedSession()
    var isRecommendationEnabled by rememberPreference(isRecommendationEnabledKey, false)
    var selectItems by remember { mutableStateOf( false ) }
    // Playlist non-vital
    val playlistName = remember { mutableStateOf( "" ) }
    var listMediaItems = remember { mutableListOf<MediaItem>() }
    val thumbnailUrl = remember { mutableStateOf("") }

    LaunchedEffect( playlistPreview?.playlist?.name ) {
        playlistName.value =
            playlistPreview?.playlist
                ?.name
                ?.let { name ->
                    if( name.startsWith( MONTHLY_PREFIX, true ) )
                        getTitleMonthlyPlaylist(context, name.substringAfter(MONTHLY_PREFIX))
                    else
                        name.substringAfter( PINNED_PREFIX )
                            .substringAfter( PIPED_PREFIX )
                } ?: "Unknown"

        val thumbnailName = "thumbnail/playlist_${playlistId}"
        val presentThumbnailUrl: String? = checkFileExists(context, thumbnailName)
        if (presentThumbnailUrl != null) {
            thumbnailUrl.value = presentThumbnailUrl
        }
    }

    val search = Search.init()

    val sort = PlaylistSongsSort.init()

    val shuffle = SongsShuffle.init {
        flowOf( playlistSongs.map( SongEntity::song ) )
    }
    val renameDialog = object: IDialog, Descriptive, MenuIcon {
        override val messageId: Int = R.string.rename
        override val iconId: Int = R.drawable.title_edit
        override val dialogTitle: String
            @Composable
            get() = stringResource( R.string.enter_the_playlist_name )
        override val menuIconTitle: String
            @Composable
            get() = stringResource( messageId )

        override var value: String = playlistName.value
            set(value) {
                playlistName.value = value
                field = value
            }
        override var isActive: Boolean by rememberSaveable { mutableStateOf( false ) }

        override fun onShortClick() = super.onShortClick()

        override fun onSet(newValue: String) {
            val isPipedPlaylist =
                playlistPreview?.playlist?.name?.startsWith(PIPED_PREFIX) == true
                        && isPipedEnabled
                        && pipedSession.token.isNotEmpty()
            val prefix = if( isPipedPlaylist ) PIPED_PREFIX else ""

            query {
                playlistPreview?.playlist?.copy(name = "$prefix$newValue")?.let(Database::update)
            }

            if ( isPipedPlaylist )
                renamePipedPlaylist(
                    context = context,
                    coroutineScope = coroutineScope,
                    pipedSession = pipedSession.toApiSession(),
                    id = UUID.fromString(playlistPreview?.playlist?.browseId),
                    name = "$PIPED_PREFIX$newValue"
                )
            onDismiss()
        }
    }
    val exportDialog = ExportSongsToCSVDialog.init( playlistName ) {
        listMediaItems.ifEmpty { playlistSongs.map( SongEntity::asMediaItem ) }
    }
    val deleteDialog = object: ConfirmDialog, Descriptive, MenuIcon {
        override val messageId: Int = R.string.delete
        override val iconId: Int = R.drawable.trash
        override val dialogTitle: String
            @Composable
            get() = stringResource( R.string.delete_playlist )
        override val menuIconTitle: String
            @Composable
            get() = stringResource( messageId )

        override var isActive: Boolean by rememberSaveable { mutableStateOf( false ) }

        override fun onShortClick() = super.onShortClick()

        override fun onConfirm() {
            transaction {
                playlistPreview?.playlist?.let(Database::delete)
            }

            if (
                playlistPreview?.playlist?.name?.startsWith(PIPED_PREFIX) == true
                && isPipedEnabled
                && pipedSession.token.isNotEmpty()
            )
                deletePipedPlaylist(
                    context = context,
                    coroutineScope = coroutineScope,
                    pipedSession = pipedSession.toApiSession(),
                    id = UUID.fromString(playlistPreview?.playlist?.browseId)
                )

            onDelete()
            onDismiss()

            if (navController.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED)
                navController.popBackStack()
        }
    }
    val renumberDialog = object: ConfirmDialog, Descriptive, MenuIcon {
        override val messageId: Int = R.string.renumber_songs_positions
        override val iconId: Int = R.drawable.position
        override val dialogTitle: String
            @Composable
            get() = stringResource( R.string.do_you_really_want_to_renumbering_positions_in_this_playlist )
        override val menuIconTitle: String
            @Composable
            get() = stringResource( messageId )

        override var isActive: Boolean by rememberSaveable { mutableStateOf( false ) }

        override fun onShortClick() = super.onShortClick()

        override fun onConfirm() {
            transaction {
                val pId = playlistPreview?.playlist?.id ?: return@transaction

                playlistSongs.map( SongEntity::song )
                    .forEachIndexed { index, song ->
                        Database.updateSongPosition( pId, song.id, index )
                    }
            }

            onDismiss()
        }
    }
    val downloadAllDialog = DownloadAllDialog.init {
        listMediaItems.ifEmpty {
            playlistSongs.map {
                transaction {
                    Database.insert(
                        Song(
                            id = it.asMediaItem.mediaId,
                            title = it.asMediaItem.mediaMetadata.title.toString(),
                            artistsText = it.asMediaItem.mediaMetadata.artist.toString(),
                            thumbnailUrl = it.song.thumbnailUrl,
                            durationText = null
                        )
                    )
                }

                it.asMediaItem
            }
        }
    }
    val deleteDownloadsDialog = DelAllDownloadedDialog.init {
        listMediaItems.ifEmpty { playlistSongs.map( SongEntity::asMediaItem ) }
    }
    val editThumbnailLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        // Callback is invoked after the user selects a media item or closes the
        // photo picker.
        if (uri != null) {
            val thumbnailName = "playlist_${playlistPreview?.playlist?.id}"
            val permaUri = saveImageToInternalStorage(context, uri, "thumbnail", thumbnailName)
            thumbnailUrl.value = permaUri.toString()
        } else {
            SmartMessage(context.resources.getString(R.string.thumbnail_not_selected), context = context)
        }
    }
    val pin = pin( playlistPreview, playlistId )
    val positionLock = PositionLock.init( sort.sortOrder )

    val itemSelector = ItemSelector {
        selectItems = !selectItems
        if ( !selectItems ) {
            listMediaItems.clear()
        }
    }
    val playNext = PlayNext {
        listMediaItems.ifEmpty { playlistSongs.map( SongEntity::asMediaItem ) }
            .let {
                binder?.player?.addNext( it, appContext() )

                if( listMediaItems.isNotEmpty() ) {
                    listMediaItems.clear()
                    selectItems = false
                }
            }
    }
    val enqueue = Enqueue {
        listMediaItems.ifEmpty { playlistSongs.map( SongEntity::asMediaItem ) }
            .let {
                binder?.player?.enqueue( it, context )

                if( listMediaItems.isNotEmpty() ) {
                    listMediaItems.clear()
                    selectItems = false
                }
            }
    }
    val addToFavorite = AddToFavorite {
        transaction {
            listMediaItems.ifEmpty { playlistSongs.map( SongEntity::asMediaItem ) }
                .forEach {
                    Database.like( it.mediaId, System.currentTimeMillis() )
                }
        }
    }

    var position by remember { mutableIntStateOf(0) }
    val addToPlaylist = PlaylistsMenu.init( navController ) { playlistPreview ->
        position = playlistPreview.songCount.minus( 1 )
        if (position > 0) position++ else position = 0

        if( playlistPreview.playlist.name.startsWith(PIPED_PREFIX)
            && isPipedEnabled
            && pipedSession.token.isNotEmpty()
        )
            addToPipedPlaylist(
                context = context,
                coroutineScope = coroutineScope,
                pipedSession = pipedSession.toApiSession(),
                id = UUID.fromString(playlistPreview.playlist.browseId),
                videos = listMediaItems.map( MediaItem::mediaId )
            )

        transaction {
            try {
                listMediaItems.ifEmpty { playlistSongs.map(SongEntity::asMediaItem) }
                    .forEachIndexed { index, mediaItem ->
                        Database.insert(mediaItem)
                        Database.insert(
                            SongPlaylistMap(
                                songId = mediaItem.mediaId,
                                playlistId = playlistPreview.playlist.id,
                                position = position + index
                            )
                        )
                    }
            } catch( _: Exception ) {
            } finally {
                listMediaItems.clear()
                selectItems = false
            }
        }
    }

    fun sync() {
        playlistPreview?.let { playlistPreview ->
            if (!playlistPreview.playlist.name.startsWith(
                    PIPED_PREFIX,
                    0,
                    true
                )
            ) {
                transaction {
                    runBlocking(Dispatchers.IO) {
                        withContext(Dispatchers.IO) {
                            Innertube.playlistPage(
                                BrowseBody(
                                    browseId = playlistPreview.playlist.browseId
                                        ?: ""
                                )
                            )
                                ?.completed()
                        }
                    }?.getOrNull()?.let { remotePlaylist ->
                        Database.clearPlaylist(playlistId)

                        remotePlaylist.songsPage
                            ?.items
                            ?.map(Innertube.SongItem::asMediaItem)
                            ?.onEach(Database::insert)
                            ?.mapIndexed { position, mediaItem ->
                                SongPlaylistMap(
                                    songId = mediaItem.mediaId,
                                    playlistId = playlistId,
                                    position = position
                                )
                            }?.let(Database::insertSongPlaylistMaps)
                    }
                }
            } else {
                syncSongsInPipedPlaylist(
                    context = context,
                    coroutineScope = coroutineScope,
                    pipedSession = pipedSession.toApiSession(),
                    idPipedPlaylist = UUID.fromString(
                        playlistPreview.playlist.browseId
                    ),
                    playlistId = playlistPreview.playlist.id

                )
            }
        }
    }
    val syncComponent = Synchronize { sync() }
    val listenOnYT = ListenOnYouTube {
        val browseId = playlistPreview?.playlist?.browseId?.removePrefix( "VL" )

        binder?.player?.pause()
        uriHandler.openUri( "https://youtube.com/playlist?list=$browseId" )
    }
    val exportMenuItem = Export( exportDialog::onShortClick )

    fun openEditThumbnailPicker() {
        editThumbnailLauncher.launch("image/*")
    }
    val thumbnailPicker = ThumbnailPicker { openEditThumbnailPicker() }

    fun resetThumbnail() {
        if(thumbnailUrl.value == ""){
            SmartMessage(context.resources.getString(R.string.no_thumbnail_present), context = context)
            return
        }
        val thumbnailName = "thumbnail/playlist_${playlistPreview?.playlist?.id}"
        val retVal = deleteFileIfExists(context, thumbnailName)
        if(retVal == true){
            SmartMessage(context.resources.getString(R.string.removed_thumbnail), context = context)
            thumbnailUrl.value = ""
        } else {
            SmartMessage(context.resources.getString(R.string.failed_to_remove_thumbnail), context = context)
        }
    }
    val resetThumbnail = ResetThumbnail { resetThumbnail() }


    val locator = LocateComponent.init( lazyListState ) { playlistSongs }

    LaunchedEffect( sort.sortOrder, sort.sortBy ) {
        Database.songsPlaylist( playlistId, sort.sortBy, sort.sortOrder ).filterNotNull()
            .collect { playlistSongs = if (parentalControlEnabled)
                it.filter { !it.song.title.startsWith(EXPLICIT_PREFIX) } else it }
    }


    if ( search.input.isNotBlank() )
        playlistSongs = playlistSongs.filter { songItem ->
            songItem.song.title.contains(search.input, true)
                    || songItem.song.artistsText?.contains(search.input, true) ?: false
                    || songItem.albumTitle?.contains(search.input, true) ?: false
        }

    LaunchedEffect(Unit) {
        Database.singlePlaylistPreview(playlistId).collect { playlistPreview = it }
    }

    //**** SMART RECOMMENDATION
    val recommendationsNumber by rememberPreference(
        recommendationsNumberKey,
        RecommendationsNumber.`5`
    )
    var relatedSongsRecommendationResult by persist<Result<Innertube.RelatedSongs?>?>(tag = "home/relatedSongsResult")
    var songBaseRecommendation by persist<SongEntity?>("home/songBaseRecommendation")
    var positionsRecommendationList = arrayListOf<Int>()
    var autosync by rememberPreference(autosyncKey, false)

    if (isRecommendationEnabled) {
        LaunchedEffect(Unit, isRecommendationEnabled) {
            Database.songsPlaylist(playlistId, sort.sortBy, sort.sortOrder).distinctUntilChanged()
                .collect { songs ->
                    val song = songs.firstOrNull()
                    if (relatedSongsRecommendationResult == null || songBaseRecommendation?.song?.id != song?.song?.id) {
                        relatedSongsRecommendationResult =
                            Innertube.relatedSongs(NextBody(videoId = (song?.song?.id ?: "HZnNt9nnEhw")))
                    }
                    songBaseRecommendation = song
                }
        }
        if (relatedSongsRecommendationResult != null) {
            for (index in 0..recommendationsNumber.number) {
                positionsRecommendationList.add((0..playlistSongs.size).random())
            }
        }
    }

    var totalPlayTimes = 0L
    playlistSongs.forEach {
        totalPlayTimes += it.song.durationText?.let { it1 ->
            durationTextToMillis(it1)
        }?.toLong() ?: 0
    }


    val thumbnailRoundness by rememberPreference(
        thumbnailRoundnessKey,
        ThumbnailRoundness.Heavy
    )


    val reorderingState = rememberReorderingState(
        lazyListState = lazyListState,
        key = playlistSongs,
        onDragEnd = { fromIndex, toIndex ->
            //Log.d("mediaItem","reoder playlist $playlistId, from $fromIndex, to $toIndex")
            query {
                Database.move(playlistId, fromIndex, toIndex)
            }
        },
        extraItemCount = 1
    )

    renameDialog.Render()
    exportDialog.Render()
    deleteDialog.Render()
    renumberDialog.Render()
    downloadAllDialog.Render()
    deleteDownloadsDialog.Render()

    var isReorderDisabled by rememberPreference(reorderInQueueEnabledKey, defaultValue = true)

    val playlistThumbnailSizeDp = Dimensions.thumbnails.playlist
    val playlistThumbnailSizePx = playlistThumbnailSizeDp.px

    val thumbnailSizeDp = Dimensions.thumbnails.song
    val thumbnailSizePx = thumbnailSizeDp.px

    val rippleIndication = ripple(bounded = false)

    var scrollToNowPlaying by remember {
        mutableStateOf(false)
    }
    var nowPlayingItem by remember {
        mutableStateOf(-1)
    }
    var plistId by remember {
        mutableStateOf(0L)
    }

    val playlistNotMonthlyType =
        playlistPreview?.playlist?.name?.startsWith(MONTHLY_PREFIX, 0, true) == false
    val playlistNotPipedType =
        playlistPreview?.playlist?.name?.startsWith(PIPED_PREFIX, 0, true) == false
    val hapticFeedback = LocalHapticFeedback.current


    Box(
        modifier = Modifier
            .background(colorPalette().background0)
            //.fillMaxSize()
            .fillMaxHeight()
            .fillMaxWidth(
                if (NavigationBarPosition.Right.isCurrent())
                    Dimensions.contentWidthRightBar
                else
                    1f
            )
    ) {
        //LookaheadScope {
            LazyColumn(
                state = reorderingState.lazyListState,
                //contentPadding = LocalPlayerAwareWindowInsets.current
                //    .only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
                //    .asPaddingValues(),
                modifier = Modifier
                    .background(colorPalette().background0)
                    .fillMaxSize()
            ) {
                item(
                    key = "header",
                    contentType = 0
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {

                        HeaderWithIcon(
                            //title = playlistPreview?.playlist?.name?.substringAfter(PINNED_PREFIX) ?: "Unknown",
                            title = playlistName.value,
                            iconId = R.drawable.playlist,
                            enabled = true,
                            showIcon = false,
                            modifier = Modifier
                                .padding(bottom = 8.dp),
                            onClick = {}
                        )

                    }

                    Row(
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            //.background(colorPalette().background4)
                            .fillMaxSize(0.99F)
                            .background(
                                color = colorPalette().background1,
                                shape = thumbnailRoundness.shape()
                            )
                    ) {

                        playlistPreview?.let {
                            Playlist(
                                playlist = it,
                                thumbnailSizeDp = playlistThumbnailSizeDp,
                                thumbnailSizePx = playlistThumbnailSizePx,
                                alternative = true,
                                showName = false,
                                modifier = Modifier
                                    .padding(top = 14.dp),
                                disableScrollingText = disableScrollingText,
                                thumbnailUrl = if (thumbnailUrl.value == "") null else thumbnailUrl.value
                            )
                        }


                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.Start,
                            modifier = Modifier
                                //.fillMaxHeight()
                                .padding(end = 10.dp)
                                .fillMaxWidth(if (isLandscape) 0.90f else 0.80f)
                        ) {
                            Spacer(modifier = Modifier.height(10.dp))
                            IconInfo(
                                title = playlistSongs.size.toString(),
                                icon = painterResource(R.drawable.musical_notes)
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                            IconInfo(
                                title = formatAsTime(totalPlayTimes),
                                icon = painterResource(R.drawable.time)
                            )
                            if (isRecommendationEnabled) {
                                Spacer(modifier = Modifier.height(5.dp))
                                IconInfo(
                                    title = positionsRecommendationList.distinct().size.toString(),
                                    icon = painterResource(R.drawable.smart_shuffle)
                                )
                            }
                            Spacer(modifier = Modifier.height(30.dp))
                        }

                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            HeaderIconButton(
                                icon = R.drawable.smart_shuffle,
                                enabled = true,
                                color = if (isRecommendationEnabled) colorPalette().text else colorPalette().textDisabled,
                                onClick = {},
                                modifier = Modifier
                                    .combinedClickable(
                                        onClick = {
                                            isRecommendationEnabled = !isRecommendationEnabled
                                        },
                                        onLongClick = {
                                            SmartMessage(
                                                context.resources.getString(R.string.info_smart_recommendation),
                                                context = context
                                            )
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            shuffle.ToolBarButton()
                            Spacer(modifier = Modifier.height(10.dp))
                            search.ToolBarButton()
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    TabToolBar.Buttons(
                        mutableListOf<Button>().apply {
                            if (playlistNotMonthlyType)
                                this.add( pin )
                            if ( sort.sortBy == PlaylistSongSortBy.Position )
                                this.add( positionLock )

                            this.add( downloadAllDialog )
                            this.add( deleteDownloadsDialog )
                            this.add( itemSelector )
                            this.add( playNext )
                            this.add( enqueue )
                            this.add( addToFavorite )
                            this.add( addToPlaylist )
                            if( playlistPreview?.playlist?.browseId?.isNotBlank() == true )
                                this.add( syncComponent )
                            if( playlistPreview?.playlist?.browseId?.isNotBlank() == true )
                                this.add( listenOnYT )
                            this.add( renameDialog )
                            this.add( renumberDialog )
                            this.add( deleteDialog )
                            this.add( exportMenuItem )
                            this.add( thumbnailPicker )
                            this.add( resetThumbnail )
                        }
                    )

                    if (autosync && playlistPreview?.let { playlistPreview -> !playlistPreview.playlist.browseId.isNullOrBlank() } == true) {
                        sync()
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    /*        */
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(horizontal = 10.dp)
                            .fillMaxWidth()
                    ) {

                        sort.ToolBarButton()

                        Row(
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) { locator.ToolBarButton() }

                    }

                    Column { search.SearchBar( this ) }
                }

                itemsIndexed(
                    items = playlistSongs ?: emptyList(),
                    key = { _, song -> song.song.id },
                    contentType = { _, song -> song },
                ) { index, song ->

                    if (index in positionsRecommendationList.distinct()) {
                        val songRecommended =
                            relatedSongsRecommendationResult?.getOrNull()?.songs?.shuffled()
                                ?.lastOrNull()
                        songRecommended?.asMediaItem?.let {
                            SongItem(
                                song = it,
                                isRecommended = true,
                                thumbnailSizeDp = thumbnailSizeDp,
                                thumbnailSizePx = thumbnailSizePx,
                                onDownloadClick = {},
                                downloadState = Download.STATE_STOPPED,
                                trailingContent = {},
                                onThumbnailContent = {},
                                modifier = Modifier
                                    .clickable {
                                        binder?.stopRadio()
                                        binder?.player?.forcePlay(it)
                                    },
                                disableScrollingText = disableScrollingText
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .draggedItem(
                                reorderingState = reorderingState,
                                index = index
                            )
                            .zIndex(2f)
                    ) {


                        val isLocal by remember { derivedStateOf { song.asMediaItem.isLocal } }
                        downloadAllDialog.state = getDownloadState( song.asMediaItem.mediaId )
                        val isDownloaded =
                            if (!isLocal) isDownloadedSong(song.asMediaItem.mediaId) else true
                        val checkedState = rememberSaveable { mutableStateOf(false) }
                        val positionInPlaylist: Int = index

                        if ( !positionLock.isLocked() ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .zIndex(3f)
                                    .align(Alignment.TopEnd)
                                    .offset(x = -15.dp)

                            ) {

                                IconButton(
                                    icon = R.drawable.reorder,
                                    color = colorPalette().textDisabled,
                                    indication = rippleIndication,
                                    onClick = {},
                                    modifier = Modifier
                                        .reorder(
                                            reorderingState = reorderingState,
                                            index = index
                                        )
                                )
                            }
                        }

                        SwipeableQueueItem(
                            mediaItem = song.asMediaItem,
                            onSwipeToLeft = {
                                transaction {
                                    Database.move(playlistId, positionInPlaylist, Int.MAX_VALUE)
                                    Database.delete(
                                        SongPlaylistMap(
                                            song.song.id,
                                            playlistId,
                                            Int.MAX_VALUE
                                        )
                                    )
                                }

                                if (playlistPreview?.playlist?.name?.startsWith(PIPED_PREFIX) == true && isPipedEnabled && pipedSession.token.isNotEmpty()) {
                                    Timber.d("MediaItemMenu LocalPlaylistSongs onSwipeToLeft browseId ${playlistPreview!!.playlist.browseId}")
                                    removeFromPipedPlaylist(
                                        context = context,
                                        coroutineScope = coroutineScope,
                                        pipedSession = pipedSession.toApiSession(),
                                        id = UUID.fromString(playlistPreview?.playlist?.browseId),
                                        positionInPlaylist
                                    )
                                }
                                coroutineScope.launch {
                                    SmartMessage(
                                        context.resources.getString(R.string.deleted) + " \"" + song.asMediaItem.mediaMetadata.title.toString() + " - " + song.asMediaItem.mediaMetadata.artist.toString() + "\" ",
                                        type = PopupType.Warning,
                                        context = context,
                                        durationLong = true
                                    )
                                }

                            },
                            onSwipeToRight = {
                                binder?.player?.addNext(song.asMediaItem)
                            },
                            modifier = Modifier.zIndex(2f)
                        ) {
                            SongItem(
                                song = song.song,
                                onDownloadClick = {
                                    binder?.cache?.removeResource(song.asMediaItem.mediaId)

                                    //query {
                                    //    Database.resetFormatContentLength(song.asMediaItem.mediaId)
                                    //}
                                    resetFormatContentLength(song.asMediaItem.mediaId)

                                    if (!isLocal) {
                                        manageDownload(
                                            context = context,
                                            mediaItem = song.asMediaItem,
                                            downloadState = isDownloaded
                                        )
                                    }
                                    //if (isDownloaded) listDownloadedMedia.dropWhile { it.asMediaItem.mediaId == song.asMediaItem.mediaId } else listDownloadedMedia.add(song)
                                    //Log.d("mediaItem", "manageDownload click isDownloaded ${isDownloaded} listDownloadedMedia ${listDownloadedMedia.distinct().size}")
                                },
                                downloadState = downloadAllDialog.state,
                                thumbnailSizePx = thumbnailSizePx,
                                thumbnailSizeDp = thumbnailSizeDp,
                                trailingContent = {
                                    if (selectItems)
                                        Checkbox(
                                            checked = checkedState.value,
                                            onCheckedChange = {
                                                checkedState.value = it
                                                if (it) listMediaItems.add(song.asMediaItem) else
                                                    listMediaItems.remove(song.asMediaItem)
                                            },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = colorPalette().accent,
                                                uncheckedColor = colorPalette().text
                                            ),
                                            modifier = Modifier
                                                .scale(0.7f)
                                        )
                                    else checkedState.value = false
                                },
                                onThumbnailContent = {
                                    if (sort.sortBy == PlaylistSongSortBy.PlayTime) {
                                        BasicText(
                                            text = song.song.formattedTotalPlayTime,
                                            style = typography().xxs.semiBold.center.color(
                                                colorPalette().onOverlay
                                            ),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    brush = Brush.verticalGradient(
                                                        colors = listOf(
                                                            Color.Transparent,
                                                            colorPalette().overlay
                                                        )
                                                    ),
                                                    shape = thumbnailShape()
                                                )
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                                .align(Alignment.BottomCenter)
                                        )
                                    }

                                    /*
                                if (sortBy == PlaylistSongSortBy.Position)
                                    BasicText(
                                        text = (index + 1).toString(),
                                        style = typography().m.semiBold.center.color(colorPalette().onOverlay),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                brush = Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color.Transparent,
                                                        colorPalette().overlay
                                                    )
                                                ),
                                                shape = thumbnailShape
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                            .align(Alignment.Center)
                                    )
                                 */

                                    if (nowPlayingItem > -1)
                                        NowPlayingShow(song.asMediaItem.mediaId)
                                },
                                modifier = Modifier
                                    .combinedClickable(
                                        onLongClick = {
                                            menuState.display {
                                                InPlaylistMediaItemMenu(
                                                    navController = navController,
                                                    playlist = playlistPreview,
                                                    playlistId = playlistId,
                                                    positionInPlaylist = index,
                                                    song = song.song,
                                                    onDismiss = menuState::hide,
                                                    disableScrollingText = disableScrollingText
                                                )
                                            }
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        },
                                        onClick = {
                                            if (!selectItems) {

                                                search.onItemSelected()

                                                playlistSongs
                                                    .map(SongEntity::asMediaItem)
                                                    .let { mediaItems ->
                                                        binder?.stopRadio()
                                                        binder?.player?.forcePlayAtIndex(
                                                            mediaItems,
                                                            index
                                                        )
                                                    }
                                            } else checkedState.value = !checkedState.value
                                        }
                                    )
                                    .animateItemPlacement(reorderingState)
                                    /*
                                    .draggedItem(
                                        reorderingState = reorderingState,
                                        index = index
                                    )

 */
                                    .background(color = colorPalette().background0)
                                    .zIndex(2f),
                                disableScrollingText = disableScrollingText
                            )
                        }
                    }

                }

                item(
                    key = "footer",
                    contentType = 0,
                ) {
                    Spacer(modifier = Modifier.height(Dimensions.bottomSpacer))
                }
            }

            FloatingActionsContainerWithScrollToTop(lazyListState = lazyListState)

            val showFloatingIcon by rememberPreference(showFloatingIconKey, false)
            if ( UiType.ViMusic.isCurrent() && showFloatingIcon )
                FloatingActionsContainerWithScrollToTop(
                    lazyListState = lazyListState,
                    iconId = R.drawable.shuffle,
                    visible = !reorderingState.isDragging,
                    onClick = {
                        playlistSongs.let { songs ->
                            if (songs.isNotEmpty()) {
                                binder?.stopRadio()
                                binder?.player?.forcePlayFromBeginning(
                                    songs.shuffled().map(SongEntity::asMediaItem)
                                )
                            }
                        }
                    }
                )
    }
}
