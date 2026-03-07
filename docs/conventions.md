# コーディング規約・実装パターン

## 新しい画面の追加手順

1. **ViewModel 作成**: `screen/FooViewModel.kt` に ViewModel と UiState を定義
2. **Screen 作成**: `screen/FooScreen.kt` に Composable 関数を定義
3. **ナビゲーション追加**: `App.kt` の `NavHost` に `composable("foo") { ... }` を追加
4. **DI 登録**: `di/AppModule.kt` に `viewModel { FooViewModel(...) }` を追加
5. **遷移元の更新**: 遷移元 Screen のコールバックに `navController.navigate("foo")` を追加
6. **テスト作成**: `commonTest/screen/FooViewModelTest.kt` を作成
7. **Fake 作成**: 必要に応じて `commonTest/fake/` に Fake クラスを追加

## UiState の規則

- `data class` で定義し、全フィールドにデフォルト値を設定する
- ViewModel と同じファイル内に定義する
- ネスト型は使わず、フラットな構造を維持する

```kotlin
// FooViewModel.kt に併記する
data class FooUiState(
    val items: List<Item> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)
```

## ViewModel の構造パターン

```kotlin
class FooViewModel(private val repository: IFooRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(FooUiState())
    val uiState: StateFlow<FooUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            repository.getData().collect { data ->
                _uiState.value = _uiState.value.copy(items = data, isLoading = false)
            }
        }
    }

    fun doAction() {
        viewModelScope.launch {
            // 状態更新
            _uiState.value = _uiState.value.copy(isLoading = true)
            // 処理
        }
    }
}
```

- 状態は `MutableStateFlow` + `asStateFlow()` で外部に公開
- 副作用は `viewModelScope.launch` で実行
- `_uiState.value = _uiState.value.copy(...)` でイミュータブルに更新

## Screen の構造パターン

```kotlin
// 最上位関数: ViewModel 取得と状態購読のみ
@Composable
fun FooScreen(onNavigateBack: () -> Unit) {
    val viewModel: FooViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    FooContent(uiState = uiState, onNavigateBack = onNavigateBack, onAction = viewModel::doAction)
}

// Content 関数: 純粋な UI（Preview 可能）
@Composable
private fun FooContent(
    uiState: FooUiState,
    onNavigateBack: () -> Unit,
    onAction: () -> Unit,
) {
    // UI 実装
}

// Preview
@Preview
@Composable
fun FooScreenPreview() {
    MaterialTheme {
        FooContent(
            uiState = FooUiState(items = listOf(...)),
            onNavigateBack = {},
            onAction = {},
        )
    }
}
```

- 最上位 Composable では `koinViewModel()` と `collectAsStateWithLifecycle()` を使用
- UI ロジックは `private fun FooContent(...)` に委譲して Preview 対応
- `@Preview` は `MaterialTheme` でラップする

## テストの書き方

### ViewModel テスト基本構造

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class FooViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeFooRepository
    private lateinit var viewModel: FooViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeFooRepository()
        viewModel = FooViewModel(fakeRepository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun methodName_condition_expectedResult() = runTest {
        // Arrange: Fake にデータをセット
        fakeRepository.setData(TestDataFactory.createItem())

        // Act
        viewModel.doAction()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert: Turbine で Flow をテスト
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(expected, state.someField)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

### テストユーティリティ

| クラス | 役割 |
|---|---|
| `StandardTestDispatcher` | コルーチンを手動制御。`advanceUntilIdle()` で全処理を実行 |
| `Turbine (.test { })` | StateFlow の排出を検証 |
| `TestDataFactory` | テスト用データの一貫した生成 |
| `Fake*` クラス | Repository / DAO / API Client の Fake 実装 |

### Fake クラスのパターン

```kotlin
class FakeFooRepository : IFooRepository {
    private var data: List<Item> = emptyList()
    private var shouldThrowError: Exception? = null

    fun setData(items: List<Item>) { data = items }
    fun setShouldThrowError(e: Exception) { shouldThrowError = e }

    override fun getData(): Flow<List<Item>> {
        shouldThrowError?.let { throw it }
        return flowOf(data)
    }
}
```

## コードスタイル

### Spotless / ktlint 設定

- インデント: **4スペース**（タブ不使用）
- 最大行長: **120文字**
- 改行コード: **LF**
- コミット前に `./gradlew spotlessApply` を実行すると自動修正される

### Detekt 制約

| 制約 | 上限 |
|---|---|
| メソッドの行数 | 120行 |
| クラスの行数 | 600行 |
| パラメータ数 | 8個 |
| 複雑度（Cyclomatic） | 15 |

画面コンポーネントが大きくなる場合は `private fun` で部品に分割する。

### インポート整理

`import` は使用しているものだけを記載する。ワイルドカードインポート (`import foo.*`) は使用しない。
Spotless が自動整理するため、手動で厳密に管理する必要はない。
