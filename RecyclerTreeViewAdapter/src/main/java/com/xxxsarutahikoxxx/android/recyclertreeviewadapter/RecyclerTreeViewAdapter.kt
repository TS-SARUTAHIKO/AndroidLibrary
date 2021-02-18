package com.xxxsarutahikoxxx.android.recyclertreeviewadapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.CallSuper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

/**
 * 既存の [RecyclerView] に対して [adapter] としてセットすることでツリー構造を表示することが可能なアダプター
 *
 * [RecyclerView] に対して [asTree] 関数を実行することでセットアップされる
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
    var onLongClickListener = { _ : View, node : TreeViewNode -> selected = node ; false }
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
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : Holder {
        val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)

        return Holder(view)
    }
    override fun getItemViewType(position: Int): Int = getItemViewLayout(treeItems[position])

    @CallSuper
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = treeItems[position]

        holder.view.setOnClickListener { view -> onClickListener(view, item) }
        holder.view.setOnLongClickListener { view -> onLongClickListener(view, item) }
        holder.view.setOnTouchListener { view, event -> onTouchListener(view, event, item) }

        holder.title?.textSize = titleTextSize*displayMetricsDensity

        onBindViewHolder(holder, item)
    }
    protected open fun onBindViewHolder(holder : Holder, node : TreeViewNode){
        // 矢印の画像を更新する
        holder.arrow?.setImageResource(
            when {
                node.isLeaf -> R.drawable.arrow_non
                node.isExpanded -> R.drawable.arrow_down
                else -> R.drawable.arrow_right
            }
        )

        // Icon 画像を更新する
        when( val res = getIconResource(node, selected == node) ){
            null -> holder.icon?.visibility = View.GONE
            else -> {
                holder.icon?.setImageResource(res)
                holder.icon?.visibility = View.VISIBLE
            }
        }

        // タイトルテキストを更新する
        holder.title?.text = getTitleText(node)
        holder.title?.setTextColor( if(node == selected) Color.BLUE else Color.BLACK )

        // インデントを更新する
        holder.margin?.layoutParams?.width = (treeRowIndent * displayMetricsDensity).toInt() * (node.layer-1)
    }

    /**
     * レイアウトリソースの取得関数
     *
     * デフォルトでは常に R.layout.recycler_tree_view_simple_row を返す
     * */
    var getItemViewLayout : (TreeViewNode)->(Int) = { R.layout.recycler_tree_view_simple_row }
    /**
     * タイトル文字列の取得関数
     *
     * デフォルトでは [node.content] が File の時のみ File.name を返し、それ以外は [node.content.toString()] を返す
     * */
    var getTitleText : (node : TreeViewNode) -> String = { node -> node.content?.run { if( this is File ) name else this.toString() } ?: "null" }
    /**
     * アイコン画像の取得関数
     *
     * null ならアイコンは非表示(gone)。それ以外なら ResourceID と解釈する。
     *
     * デフォルトでは常に null を返す
     *  */
    var getIconResource : (node : TreeViewNode, selected : Boolean) -> Int? = { _, _ -> null }

    var titleTextSize : Int = 10
    var treeRowIndent : Int = 15


    inner open class Holder(val view : View) : RecyclerView.ViewHolder(view) {
        open val margin = view.findViewById<View?>(R.id.TreeViewRowMargin)
        open val arrow = view.findViewById<ImageView?>(R.id.TreeViewRowArrow)
        open val icon = view.findViewById<ImageView?>(R.id.TreeViewRowIcon)
        open val title = view.findViewById<TextView?>(R.id.TreeViewRowTitle)
    }
}

fun RecyclerView.asTree(root : TreeViewRoot = treeRoot("Root"), init : (TreeViewRoot).()->(Unit) ) : RecyclerTreeViewAdapter {
    layoutManager = LinearLayoutManager( context )
    adapter = RecyclerTreeViewAdapter( root.apply(init) )

    return adapter as RecyclerTreeViewAdapter
}

/**
 * [adapter] を [RecyclerTreeViewAdapter] として取得する
 *
 * [adapter] が存在しないか型変換できない場合は null を返す
 *  */
val RecyclerView.treeAdapter : RecyclerTreeViewAdapter? get() = adapter as? RecyclerTreeViewAdapter
