# AndroidLibrary


# RecyclerTreeViewAdapter

RecyclerView をツリー表示可能にするためのアダプター

```gradle.gradle
repositories {
    maven { url 'https://github.com/TS-SARUTAHIKO/AndroidLibrary/raw/master/repository/' }
}
dependencies {
    implementation 'com.xxxsarutahikoxxx.android:RecyclerTreeViewAdapter:0.0.3'
}
```

```main.kt
// 対象の RecyclerView を取得する
val recycler : RecyclerView = findViewById<RecyclerView>(R.id.recycler_view)

// RecyclerView をツリーノードとして扱うためのアダプターをセットする
recycler.asTree {
    create("c1") // ルートノードに c1 ノードを追加する
    create("c2"){ // ルートノードに c2 ノードを追加する
        create("c2-1"){ // c2 ノードに c2-1 ノードを追加する
            create("c2-1-1")
        }
        create("c2-2")
    }
    
    expandAll() // 全てのノードを展開する
}.apply {
    // ノードが選択された時の処理を設定する。
    // デフォルトではロングプレスされた時に選択される。
    onSelected = { node : TreeViewNode? ->  }
}
```

