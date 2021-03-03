package com.xxxsarutahikoxxx.android.recyclertreeviewadapter

import android.Manifest
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.CallSuper
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.lang.RuntimeException

/**
 * 既存の [RecyclerView] に対して [adapter] としてセットすることでツリー構造を表示することが可能なアダプター
 *
 * [RecyclerView] に対して [asTree] 関数を実行することでセットアップされる
 *
 * ノードの構造状態が変更されたときは自動的にツリーに変更が通知される
 *
 * ノードの情報が変更されてビューを更新する必要がる場合は [notifyTreeItemChanged] によって通知する
 *
 * --- 動作 ---
 *
 * ・ クリック -> ノードの展開状態の変更
 *
 * ・ 長押し -> ノードの選択
 *
 * ・ タッチ -> 何もしない
 *
 * クリック、長押し、タッチ時の処理は [onClickListener] / [onLongClickListener] / [onTouchListener] を変更することで実装できる
 *
 * [onSelected] によってノード選択時の処理を実装できる
 *
 * --- 項目の表示 ---
 *
 * デフォルトではツリーの各アイテムはシンプルな　R.layout.recycler_tree_view_simple_row によって表示される
 *
 * このレイアウトの表示する要素は Icon (id = TreeViewRow_Icon), Title (id = TreeViewRow_Title), Tips (id = TreeViewRow_Tips), 開閉状態 (id = TreeViewRow_Arrow), マージン (id = TreeViewRow_Margin) である
 *
 * より複雑な独自レイアウトで表示するならば [nodeToLayout] を変更して返り値としてレイアウトIDを返却すること
 *
 * Q. 表示されるタイトルを変更する -> A. [nodeToTitle] を変更する
 *
 * Q. 表示されるTipsを変更する -> A. [nodeToTips] を変更する
 *
 * Q. 表示されるアイコンを変更する -> A. [nodeToIconId] を変更する
 *
 * Q. ノードごとに表示するレイアウトを変更する。
 * A. [nodeToLayout] を変更する
 * E.G. treeAdapter.nodeToLayout = { node : TreeViewNode -> when( node.content ){ else -> R.layout.recycler_tree_view_simple_row } }
 *
 * Q. 独自のレイアウトを使用した際にレイアウトに独自の処理を適用する -> A. [bindViewHolder] を変更する

 *
 * */
open class RecyclerTreeViewAdapter(root : TreeViewRoot) : RecyclerView.Adapter<RecyclerTreeViewAdapter.Holder>(){

    /** ディスプレイ密度。ノードの階層ずらしのために必要な情報。 */
    private var displayMetricsDensity : Float = 0f

    /** 根源ノード */
    var treeRoot : TreeViewRoot = root.apply { init() }
        set(value) {
            field = value.apply { init() }
            treeItems = value.expandedChildren
            notifyDataSetChanged()
        }
    /** 表示するノードのリスト */
    private var treeItems : List<TreeViewNode> = root.expandedChildren

    /** [treeRoot] から [selected] までを表すノードリスト */
    private var currents : List<TreeViewNode> = listOf()
    /** 選択状態のノードを表すノード */
    var selected : TreeViewNode? = null
        set(value) {
            field = value

            val list = currents.toMutableList()
            currents = value?.run { value.parents + value } ?: listOf()

            (listOf<TreeViewNode>() + list.lastOrNull() + value )
                .filterNotNull()
                .forEach { notifyItemChanged(treeItems.indexOf(it)) }

            onSelected(value)
        }

    /** ノードのクリックの処理。デフォルトではノードの展開状態の反転を行う。 */
    var onClickListener = { _ : View, node : TreeViewNode -> node.toggle() }
    /** ノードの長押しの処理。デフォルトではノードを選択する。 */
    var onLongClickListener = { _ : View, node : TreeViewNode -> if( selected == node ){ selected = null }else{ selected = node } ; false }
    /** ノードのタッチの処理。デフォルトでは何もしない。 */
    var onTouchListener = { _ : View, _ : MotionEvent, _ : TreeViewNode -> false }
    /** [selected] が変化した際のコールバック */
    var onSelected : (TreeViewNode?)->(Unit) = {  }


    /** 根源ノードの初期化処理。リスナーを登録して展開・追加・削除の際にそれを [TreeViewAdapter] に通知する。 */
    private fun TreeViewRoot.init(){
        onExpanded = {
            // [treeItems]の更新を行う
            treeItems = treeRoot.expandedChildren

            if( ! it.isLeaf ){
                val range = (treeItems.indexOf(it)+1) to (it.expandedChildren.size)

                // 変更を通知する
                notifyItemRangeInserted(range.first, range.second)
            }

            notifyItemChanged(treeItems.indexOf(it))
        }
        onCollapsed = {
            // [treeItems]の更新を行う
            treeItems = treeRoot.expandedChildren

            if( ! it.isLeaf ){
                val range = (treeItems.indexOf(it)+1) to (it.expandedChildren.size)

                // 変更を通知する
                notifyItemRangeRemoved(range.first, range.second)
            }

            notifyItemChanged(treeItems.indexOf(it))
        }
        onAdded = {
            // [treeItems]の更新を行う
            treeItems = treeRoot.expandedChildren

            val range = treeItems.indexOf(it) to (it.expandedChildren.size+1)

            // 変更を通知する
            notifyItemRangeInserted(range.first, range.second)

            // Leaf <-> Branch が変更されたのでノード情報を更新する
            notifyItemChanged(treeItems.indexOf(it.parent))
        }
        onPreRemoved = {
            val range = treeItems.indexOf(it) to (it.expandedChildren.size+1)

            // [treeItems]の更新を行う
            // ノードが除去される前に呼ばれるために他とは違う処理を行う
            val removed = it
            treeItems = treeRoot.expandedChildren.filter { removed !in it.parents && removed != it }

            // 変更を通知する
            notifyItemRangeInserted(range.first, range.second)
        }
        onRemoved = {
            // Leaf <-> Branch が変更されたのでノード情報を更新する
            notifyItemChanged(treeItems.indexOf(it.parent))
        }
    }
    override fun getItemCount(): Int = treeItems.size
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)

        // Resources から 必要な情報を取得する（DisplayMetrics など）
        displayMetricsDensity = recyclerView.resources.displayMetrics.density

        // layoutManager を設定する
        recyclerView.layoutManager = LinearLayoutManager( recyclerView.context )
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : Holder {
        val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)

        return Holder(view)
    }
    override fun getItemViewType(position: Int): Int = nodeToLayout(treeItems[position])

    @CallSuper
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = treeItems[position]

        holder.view.setOnClickListener { view -> onClickListener(view, item) }
        holder.view.setOnLongClickListener { view -> onLongClickListener(view, item) }
        holder.view.setOnTouchListener { view, event -> onTouchListener(view, event, item) }

        onBindViewHolder(holder, item)
    }
    open fun onBindViewHolder(holder : Holder, node : TreeViewNode){
        // 矢印の画像を更新する
        holder.arrow?.setImageResource(
            when {
                node.isLeaf -> R.drawable.tree_node_state_leaf
                node.isExpanded -> R.drawable.tree_node_state_expanded
                else -> R.drawable.tree_node_state_collapsed
            }
        )

        // Icon 画像を更新する
        when( val res = nodeToIconId(node, selected == node) ){
            null -> holder.icon?.visibility = View.GONE
            else -> {
                holder.icon?.setImageResource(res)
                holder.icon?.visibility = View.VISIBLE
            }
        }

        // タイトルテキストを更新する
        holder.title?.text = nodeToTitle(node)
        holder.title?.setTextColor( if(node == selected) Color.BLUE else Color.BLACK )
        (holder.title?.layoutParams as? LinearLayoutCompat.LayoutParams)?.apply {
            topMargin = (this@RecyclerTreeViewAdapter.topMargin * displayMetricsDensity).toInt()
            bottomMargin = (this@RecyclerTreeViewAdapter.bottomMargin * displayMetricsDensity).toInt()
        }

        // タイトルテキストを更新する
        holder.tips?.text = nodeToTips(node)

        // インデントを更新する
        holder.margin?.layoutParams?.width = (layerIndent * displayMetricsDensity).toInt() * (node.layer-1)

        // Base
        (holder.view.layoutParams as? RecyclerView.LayoutParams)?.width = 3000 // 表示するのに十分な長さ

        // 追加的な効果を適用する
        bindViewHolder(holder, node)
    }

    /**
     * レイアウトリソースの取得関数
     *
     * デフォルトでは常に R.layout.recycler_tree_view_simple_row を返す
     * */
    var nodeToLayout : (TreeViewNode)->(Int) = { R.layout.recycler_tree_view_simple_row }
    /**
     * アイコン画像の取得関数
     *
     * null ならアイコンは非表示(gone)。それ以外なら ResourceID と解釈する。
     *
     * デフォルトでは常に null を返す
     *  */
    var nodeToIconId : (node : TreeViewNode, selected : Boolean) -> (Int?) = { _, _ -> null }
    /**
     * タイトル文字列の取得関数
     *
     * デフォルトでは [node.content] が File の時のみ File.name を返し、それ以外は [node.content.toString()] を返す
     * */
    var nodeToTitle : (node : TreeViewNode) -> (String) = { node -> node.content?.run { if( this is File ) name else this.toString() } ?: "null" }
    /**
     * Tips 文字列の取得関数
     *
     * デフォルトでは空の文字列を返す
     * */
    var nodeToTips : (node : TreeViewNode) -> (String) = { _ -> "" }
    /**
     * ツリーの行([holder])の追加的な情報更新を行う
     * */
    var bindViewHolder : (holder : Holder, node : TreeViewNode) -> (Unit) = { _,_ -> }

    var layerIndent : Int = 15
    var topMargin : Int = 1
    var bottomMargin : Int = 1

    /**
     * ノードのコンテンツとして [content] を持つノードの状態変更を通知する
     * */
    fun notifyTreeItemChanged(content : Any?){
        treeItems.forEach {
            if( it.content == content ) notifyItemChanged(treeItems.indexOf(it))
        }
    }


    inner open class Holder(val view : View) : RecyclerView.ViewHolder(view) {
        open val margin = view.findViewById<View?>(R.id.TreeViewRow_Margin)
        open val arrow = view.findViewById<ImageView?>(R.id.TreeViewRow_Arrow)
        open val icon = view.findViewById<ImageView?>(R.id.TreeViewRow_Icon)
        open val title = view.findViewById<TextView?>(R.id.TreeViewRow_Title)
        open val tips = view.findViewById<TextView?>(R.id.TreeViewRow_Tips)
    }
}

/**
 * [RecyclerView] をツリービューとして扱う
 *
 * [root] をルートとして、続く [root] に対する初期化関数でツリー構造を構築する
 *
 * root 自体はツリーに表示されない
 *
 * root を指定しない場合は自動的に content = "Root" で初期化されたルートが作成される
 *
 * root に指定するオブジェクトを treeRoot( content, childFactory, init ) で childFactory を指定することで再帰的に自動構築できる
 *
 * --- (e.g.) ---
 *
 * // 指定したディレクトリーをルートとする全ファイルを表示するツリービュー
 *
 * val childFactory = { file : File -> if( file.isDirectory ) file.listFiles().toList() else listOf() }
 *
 * val root = treeRoot(rootDirectory, childFactory)
 *
 * Recycler.asTree( root ){  }
 *
 * --- ---
 * */
fun RecyclerView.asTree(root : TreeViewRoot = treeRoot("Root"), init : (TreeViewRoot).()->(Unit) = {} ) : RecyclerTreeViewAdapter {
    adapter = RecyclerTreeViewAdapter( root.apply(init) )

    return adapter as RecyclerTreeViewAdapter
}
/**
 * [adapter] を [RecyclerTreeViewAdapter] として取得する
 *
 * [adapter] が存在しないか型変換できない場合は null を返す
 *  */
val RecyclerView.treeAdapter : RecyclerTreeViewAdapter? get() = adapter as? RecyclerTreeViewAdapter

/**
 * [RecyclerView] を指定したパスをルートとする Directory Explorer として構築する
 *
 * 必要なパーミッション（Manifest.permission.READ_EXTERNAL_STORAGE など）を事前に取得しておくこと
 * */
fun RecyclerView.asExplorer(root : File) : RecyclerTreeViewAdapter {
    return asTree( treeRoot(root, { if( it.isDirectory ) it.listFiles().toList() else listOf() }) )
}


internal var out : Any?
    get() = throw RuntimeException("")
    set(value) { Log.d("標準", "$value") }
