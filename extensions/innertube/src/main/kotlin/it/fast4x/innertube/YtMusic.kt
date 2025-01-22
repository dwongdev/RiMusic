package it.fast4x.innertube

import io.ktor.client.call.body
import it.fast4x.innertube.Innertube.getBestQuality
import it.fast4x.innertube.models.BrowseEndpoint
import it.fast4x.innertube.models.BrowseResponse
import it.fast4x.innertube.models.Context
import it.fast4x.innertube.models.CreatePlaylistResponse
import it.fast4x.innertube.models.MusicCarouselShelfRenderer
import it.fast4x.innertube.models.NavigationEndpoint
import it.fast4x.innertube.models.getContinuation
import it.fast4x.innertube.models.oddElements
import it.fast4x.innertube.requests.AlbumPage
import it.fast4x.innertube.requests.ArtistItemsPage
import it.fast4x.innertube.requests.ArtistPage
import it.fast4x.innertube.requests.HistoryPage
import it.fast4x.innertube.requests.HomePage
import it.fast4x.innertube.requests.NewReleaseAlbumPage
import it.fast4x.innertube.requests.PlaylistContinuationPage
import it.fast4x.innertube.requests.PlaylistPage
import it.fast4x.innertube.utils.from

object YtMusic {

    suspend fun createPlaylist(title: String) = runCatching {
        Innertube.createPlaylist(Context.DefaultWeb.client, title).body<CreatePlaylistResponse>().playlistId
    }.onFailure {
        println("YtMusic createPlaylist error: ${it.stackTraceToString()}")
    }

    suspend fun deletePlaylist(playlistId: String) = runCatching {
        Innertube.deletePlaylist(Context.DefaultWeb.client, playlistId)
    }.onFailure {
        println("YtMusic deletePlaylist error: ${it.stackTraceToString()}")
    }

    suspend fun renamePlaylist(playlistId: String, name: String) = runCatching {
        Innertube.renamePlaylist(Context.DefaultWeb.client, playlistId, name)
    }.onFailure {
        println("YtMusic renamePlaylist error: ${it.stackTraceToString()}")
    }

    suspend fun addToPlaylist(playlistId: String, videoId: String) = runCatching {
        Innertube.addToPlaylist(Context.DefaultWeb.client, playlistId, videoId)
    }.onFailure {
        println("YtMusic addToPlaylist error: ${it.stackTraceToString()}")
    }

    suspend fun removeFromPlaylist(playlistId: String, videoId: String, setVideoId: String? = null) = runCatching {
        Innertube.removeFromPlaylist(Context.DefaultWeb.client, playlistId, videoId, setVideoId)
    }.onFailure {
        println("YtMusic removeFromPlaylist error: ${it.stackTraceToString()}")
    }

    suspend fun getHomePage(setLogin: Boolean = false): Result<HomePage> = runCatching {

        var response = Innertube.browse(browseId = "FEmusic_home", setLogin = setLogin).body<BrowseResponse>()

        println("homePage() response sections: ${response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
            ?.tabRenderer?.content?.sectionListRenderer?.contents}" )


        var continuation = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
            ?.tabRenderer?.content?.sectionListRenderer?.continuations?.getContinuation()

        val sections = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
            ?.tabRenderer?.content?.sectionListRenderer?.contents!!
            .mapNotNull { it.musicCarouselShelfRenderer }
            .mapNotNull {
                HomePage.Section.fromMusicCarouselShelfRenderer(it)
            }.toMutableList()
        while (continuation != null) {
            println("gethomePage() continuation before:  ${continuation}" )
            response = Innertube.browse(continuation = continuation).body<BrowseResponse>()
            continuation = response.continuationContents?.sectionListContinuation?.continuations?.getContinuation()
            println("gethomePage() continuation after:  ${continuation}" )

            sections += response.continuationContents?.sectionListContinuation?.contents
                ?.mapNotNull { it.musicCarouselShelfRenderer }
                ?.mapNotNull {
                    HomePage.Section.fromMusicCarouselShelfRenderer(it)
                }.orEmpty()

        }
        HomePage( sections = sections )
    }

    suspend fun getHistory(setLogin: Boolean = false): Result<HistoryPage> = runCatching {

        val response = Innertube.browse(browseId = "FEmusic_history", setLogin = setLogin)
            .body<BrowseResponse>()

        println("getHistory() response sections: ${response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
            ?.tabRenderer?.content?.sectionListRenderer?.contents}" )

        HistoryPage(
            sections = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
                ?.tabRenderer?.content?.sectionListRenderer?.contents
                ?.mapNotNull {
                    it.musicShelfRenderer?.let { musicShelfRenderer ->
                        HistoryPage.fromMusicShelfRenderer(musicShelfRenderer)
                    }
                }
        )

    }

    suspend fun getArtistPage(browseId: String, setLogin: Boolean = false): Result<ArtistPage> = runCatching {
        val response = Innertube.browse(browseId = browseId, setLogin = setLogin).body<BrowseResponse>()
        val sections = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
            ?.tabRenderer?.content?.sectionListRenderer?.contents
            ?.mapNotNull(ArtistPage::fromSectionListRendererContent)!!

        ArtistPage(
            artist = Innertube.ArtistItem(
                info = Innertube.Info(
                    name = response.header?.musicImmersiveHeaderRenderer?.title?.runs?.firstOrNull()?.text
                        ?: response.header?.musicVisualHeaderRenderer?.title?.runs?.firstOrNull()?.text
                        ?: response.header?.musicHeaderRenderer?.title?.runs?.firstOrNull()?.text!!,
                    endpoint = NavigationEndpoint.Endpoint.Browse(
                        browseId = browseId,
                        params = response.header?.musicImmersiveHeaderRenderer?.title?.runs?.firstOrNull()?.navigationEndpoint?.browseEndpoint?.params
                    )
                ),
                thumbnail = response.header?.musicImmersiveHeaderRenderer?.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.lastOrNull()
                    ?: response.header?.musicVisualHeaderRenderer?.foregroundThumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.lastOrNull()
                    ?: response.header?.musicDetailHeaderRenderer?.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.lastOrNull(),
                subscribersCountText = null,
            ),
            sections = sections,
            description = response.header?.musicImmersiveHeaderRenderer?.description?.runs?.firstOrNull()?.text,
            subscribers = response.header?.musicImmersiveHeaderRenderer?.subscriptionButton?.subscribeButtonRenderer?.subscriberCountText?.text,
            shuffleEndpoint = response.header?.musicImmersiveHeaderRenderer?.playButton?.buttonRenderer?.navigationEndpoint?.watchEndpoint,
            radioEndpoint = response.header?.musicImmersiveHeaderRenderer?.startRadioButton?.buttonRenderer?.navigationEndpoint?.watchEndpoint,
        )
    }

    suspend fun getArtistItemsPage(endpoint: BrowseEndpoint): Result<ArtistItemsPage> = runCatching {
        val response = Innertube.browse(browseId = endpoint.browseId, params = endpoint.params).body<BrowseResponse>()
        val gridRenderer = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
            ?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()
            ?.gridRenderer
        if (gridRenderer != null) {
            ArtistItemsPage(
                title = gridRenderer.header?.gridHeaderRenderer?.title?.runs?.firstOrNull()?.text.orEmpty(),
                items = gridRenderer.items!!.mapNotNull {
                    it.musicTwoRowItemRenderer?.let { renderer ->
                        ArtistItemsPage.fromMusicTwoRowItemRenderer(renderer)
                    }
                },
                continuation = gridRenderer.continuations?.getContinuation()
            )
        } else {
            ArtistItemsPage(
                title = response.header?.musicHeaderRenderer?.title?.runs?.firstOrNull()?.text!!,
                items = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
                    ?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()
                    ?.musicPlaylistShelfRenderer?.contents?.mapNotNull {
                        ArtistItemsPage.fromMusicResponsiveListItemRenderer(it.musicResponsiveListItemRenderer!!)
                    }!!,
                continuation = response.contents.singleColumnBrowseResultsRenderer.tabs.firstOrNull()
                    ?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()
                    ?.musicPlaylistShelfRenderer?.continuations?.getContinuation()
            )
        }
    }

    suspend fun getPlaylist(playlistId: String): Result<PlaylistPage> = runCatching {
        val response = Innertube.browse(
            browseId = playlistId,
            setLogin = true
        ).body<BrowseResponse>()
        val playlistIdChecked = if (playlistId.startsWith("VL")) playlistId else "VL$playlistId"
        if (response.header != null)
            getPlaylistPreviousMode(playlistIdChecked, response)
        else
            getPlaylistNewMode(playlistIdChecked, response)
    }

    private fun getPlaylistPreviousMode(playlistId: String, response: BrowseResponse): PlaylistPage {
        val header = response.header?.musicDetailHeaderRenderer ?:
            response.header?.musicEditablePlaylistDetailHeaderRenderer?.header?.musicDetailHeaderRenderer


        //val editable = response.header?.musicEditablePlaylistDetailHeaderRenderer != null

        return PlaylistPage(
            playlist = Innertube.PlaylistItem(
                info = Innertube.Info(
                    name = header?.title?.runs?.firstOrNull()?.text!!,
                    endpoint = NavigationEndpoint.Endpoint.Browse(
                        browseId = playlistId,
                    )
                ),
                songCount = 0, //header.secondSubtitle.runs?.firstOrNull()?.text,
                thumbnail = header.thumbnail.croppedSquareThumbnailRenderer?.thumbnail?.thumbnails?.getBestQuality(),
                channel = null,
//                playEndpoint = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
//                    ?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()
//                    ?.musicPlaylistShelfRenderer?.contents?.firstOrNull()?.musicResponsiveListItemRenderer
//                    ?.overlay?.musicItemThumbnailOverlayRenderer?.content?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchEndpoint,
//                shuffleEndpoint = header.menu.menuRenderer.topLevelButtons?.firstOrNull()?.buttonRenderer?.navigationEndpoint?.watchPlaylistEndpoint!!,
//                radioEndpoint = header.menu.menuRenderer.items?.find {
//                    it.menuNavigationItemRenderer?.icon?.iconType == "MIX"
//                }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint!!,
//                isEditable = editable
            ),
            songs = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
                ?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()
                ?.musicPlaylistShelfRenderer?.contents?.mapNotNull {
                    it.musicResponsiveListItemRenderer?.let { it1 ->
                        PlaylistPage.fromMusicResponsiveListItemRenderer(
                            it1
                        )
                    }
                }!!,
            songsContinuation = response.contents.singleColumnBrowseResultsRenderer.tabs.firstOrNull()
                ?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()
                ?.musicPlaylistShelfRenderer?.continuations?.getContinuation(),
            continuation = response.contents.singleColumnBrowseResultsRenderer.tabs.firstOrNull()
                ?.tabRenderer?.content?.sectionListRenderer?.continuations?.getContinuation()
        )
    }

    private fun getPlaylistNewMode(playlistId: String, response: BrowseResponse): PlaylistPage {
        val header = response.contents?.twoColumnBrowseResultsRenderer?.tabs?.firstOrNull()
            ?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicResponsiveHeaderRenderer
            ?: response.contents?.twoColumnBrowseResultsRenderer?.tabs?.firstOrNull()
                ?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()
                ?.musicEditablePlaylistDetailHeaderRenderer?.header?.musicResponsiveHeaderRenderer

//        val editable = response.contents?.twoColumnBrowseResultsRenderer?.tabs?.firstOrNull()
//            ?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()
//            ?.musicEditablePlaylistDetailHeaderRenderer != null

        return PlaylistPage(
            playlist = Innertube.PlaylistItem(
                info = Innertube.Info(
                    name = header?.title?.runs?.firstOrNull()?.text!!,
                    endpoint = NavigationEndpoint.Endpoint.Browse(
                        browseId = playlistId,
                    )
                ),
                //TODO: add description IN PLAYLISTPAGE
                songCount = 0,//header.secondSubtitle?.runs?.firstOrNull()?.text,
                thumbnail = response.background?.musicThumbnailRenderer?.thumbnail?.thumbnails?.getBestQuality(),
                channel = null
//                playEndpoint = header.buttons.getOrNull(1)?.musicPlayButtonRenderer
//                    ?.playNavigationEndpoint?.watchEndpoint,
//                shuffleEndpoint = header.buttons.getOrNull(2)?.menuRenderer?.items?.find {
//                    it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE"
//                }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint,
//                radioEndpoint = header.buttons.getOrNull(2)?.menuRenderer?.items?.find {
//                    it.menuNavigationItemRenderer?.icon?.iconType == "MIX"
//                }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint,
//                isEditable = editable
            ),
            songs = response.contents?.twoColumnBrowseResultsRenderer?.secondaryContents?.sectionListRenderer
                ?.contents?.firstOrNull()?.musicPlaylistShelfRenderer?.contents?.mapNotNull {
                    it.musicResponsiveListItemRenderer?.let { it1 ->
                        PlaylistPage.fromMusicResponsiveListItemRenderer(
                            it1
                        )
                    }
                }!!,
            songsContinuation = response.contents.twoColumnBrowseResultsRenderer.secondaryContents.sectionListRenderer
                .contents.firstOrNull()?.musicPlaylistShelfRenderer?.continuations?.getContinuation(),
            continuation = response.contents.twoColumnBrowseResultsRenderer.secondaryContents.sectionListRenderer
                .continuations?.getContinuation()
        )
    }

    suspend fun getPlaylistContinuation(continuation: String) = runCatching {
        val response = Innertube.browse(
            continuation = continuation,
            setLogin = true
        ).body<BrowseResponse>()
        PlaylistContinuationPage(
            songs = response.continuationContents?.musicPlaylistShelfContinuation?.contents?.mapNotNull {
                it.musicResponsiveListItemRenderer?.let { it1 ->
                    PlaylistPage.fromMusicResponsiveListItemRenderer(
                        it1
                    )
                }
            }!!,
            continuation = response.continuationContents.musicPlaylistShelfContinuation.continuations?.getContinuation()
        )
    }

    suspend fun getAlbum(browseId: String, withSongs: Boolean = true): Result<AlbumPage> = runCatching {
        val response = Innertube.browse(browseId = browseId).body<BrowseResponse>()
        val playlistId = response.microformat?.microformatDataRenderer?.urlCanonical?.substringAfterLast('=')!!
//        val otherVersions = response.contents?.twoColumnBrowseResultsRenderer?.secondaryContents?.sectionListRenderer?.contents?.getOrNull(
//            1
//        )?.musicCarouselShelfRenderer?.contents
        //println("mediaItem getAlbum otherVersions: $otherVersions")
//        val description = response.contents?.twoColumnBrowseResultsRenderer?.tabs
//            ?.firstOrNull()
//            ?.tabRenderer
//            ?.content
//            ?.sectionListRenderer
//            ?.contents
//            ?.firstOrNull()
//            ?.musicResponsiveHeaderRenderer
//            ?.description
//            ?.musicDescriptionShelfRenderer
//            ?.description
//        println("mediaItem getAlbum description: $description")

        AlbumPage(
            album = Innertube.AlbumItem(
                info = Innertube.Info(
                    name = response.contents?.twoColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicResponsiveHeaderRenderer?.title?.runs?.firstOrNull()?.text!!,
                    endpoint = NavigationEndpoint.Endpoint.Browse(
                        browseId = browseId,
                    )
                ),
                authors = response.contents.twoColumnBrowseResultsRenderer.tabs.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicResponsiveHeaderRenderer?.straplineTextOne?.runs?.oddElements()
                    ?.map {
                        Innertube.Info(
                            name = it.text,
                            endpoint = NavigationEndpoint.Endpoint.Browse(
                                browseId = it.navigationEndpoint?.browseEndpoint?.browseId
                            ),
                        )
                    }!!,
                year = response.contents.twoColumnBrowseResultsRenderer.tabs.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicResponsiveHeaderRenderer?.subtitle?.runs?.lastOrNull()?.text,
                thumbnail = response.contents.twoColumnBrowseResultsRenderer.tabs.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicResponsiveHeaderRenderer?.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.lastOrNull(),
            ),
            songs = if (withSongs) getAlbumSongs(playlistId).getOrThrow() else emptyList(),
            otherVersions = response.contents.twoColumnBrowseResultsRenderer.secondaryContents?.sectionListRenderer?.contents?.getOrNull(
                1
            )?.musicCarouselShelfRenderer?.contents
//                ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
//                ?.mapNotNull(Innertube.AlbumItem::from)
                ?.mapNotNull { it.musicTwoRowItemRenderer }
                ?.map(NewReleaseAlbumPage::fromMusicTwoRowItemRenderer)
                .orEmpty(),
            url = response.microformat.microformatDataRenderer.urlCanonical,
            //description = response.header?.musicDetailHeaderRenderer?.description?.text,
            description = response.contents.twoColumnBrowseResultsRenderer.tabs
                .firstOrNull()
                ?.tabRenderer
                ?.content
                ?.sectionListRenderer
                ?.contents
                ?.firstOrNull()
                ?.musicResponsiveHeaderRenderer
                ?.description
                ?.musicDescriptionShelfRenderer
                ?.description?.text,
        )
    }

    suspend fun getAlbumSongs(playlistId: String): Result<List<Innertube.SongItem>> = runCatching {
        val response = Innertube.browse(browseId = "VL$playlistId").body<BrowseResponse>()

        val contents =
            response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
                ?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()
                ?.musicPlaylistShelfRenderer?.contents ?:
            response.contents?.twoColumnBrowseResultsRenderer?.secondaryContents?.sectionListRenderer
                ?.contents?.firstOrNull()?.musicPlaylistShelfRenderer?.contents

        val songs = contents?.mapNotNull {
            it.musicResponsiveListItemRenderer?.let { it1 -> AlbumPage.getSong(it1) }
        }
        songs!!
    }

}