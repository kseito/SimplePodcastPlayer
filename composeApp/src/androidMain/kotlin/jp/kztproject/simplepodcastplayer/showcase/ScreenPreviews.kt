package jp.kztproject.simplepodcastplayer.showcase

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import jp.kztproject.simplepodcastplayer.screen.InProgressEpisodesScreenEmptyPreview
import jp.kztproject.simplepodcastplayer.screen.InProgressEpisodesScreenPreview
import jp.kztproject.simplepodcastplayer.screen.PlayerScreenPreview
import jp.kztproject.simplepodcastplayer.screen.PodcastDetailScreenPreview
import jp.kztproject.simplepodcastplayer.screen.PodcastListScreenEmptyPreview
import jp.kztproject.simplepodcastplayer.screen.PodcastListScreenPreview
import jp.kztproject.simplepodcastplayer.screen.PodcastSearchScreenWithResultsPreview

// commonMain の CMP Preview は Showkase の自動検出対象外（androidx の @Preview ではないため）。
// ここで androidx の @Preview + @ShowkaseComposable を付けた薄いラッパーから既存 Preview を呼び出し、
// Showkase の列挙対象に載せて Roborazzi の VRT 対象にする。サンプルデータは既存 Preview を再利用。

private const val GROUP = "Screens"

@ShowkaseComposable(name = "PodcastList", group = GROUP)
@Preview
@Composable
fun PodcastListShowcase() = PodcastListScreenPreview()

@ShowkaseComposable(name = "PodcastListEmpty", group = GROUP)
@Preview
@Composable
fun PodcastListEmptyShowcase() = PodcastListScreenEmptyPreview()

// PodcastSearchScreenPreview（ステートフル版）は koinViewModel() を使い Koin 起動が必要なため
// VRT 対象から除外。検索画面は下のステートレス版（WithResults）でカバーする。

@ShowkaseComposable(name = "PodcastSearchWithResults", group = GROUP)
@Preview
@Composable
fun PodcastSearchWithResultsShowcase() = PodcastSearchScreenWithResultsPreview()

@ShowkaseComposable(name = "PodcastDetail", group = GROUP)
@Preview
@Composable
fun PodcastDetailShowcase() = PodcastDetailScreenPreview()

@ShowkaseComposable(name = "Player", group = GROUP)
@Preview
@Composable
fun PlayerShowcase() = PlayerScreenPreview()

@ShowkaseComposable(name = "InProgressEpisodes", group = GROUP)
@Preview
@Composable
fun InProgressEpisodesShowcase() = InProgressEpisodesScreenPreview()

@ShowkaseComposable(name = "InProgressEpisodesEmpty", group = GROUP)
@Preview
@Composable
fun InProgressEpisodesEmptyShowcase() = InProgressEpisodesScreenEmptyPreview()
