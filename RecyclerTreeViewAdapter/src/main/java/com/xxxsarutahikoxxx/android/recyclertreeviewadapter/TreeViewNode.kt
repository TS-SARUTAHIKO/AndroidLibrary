package com.xxxsarutahikoxxx.android.recyclertreeviewadapter

import java.io.Serializable


/**
 * 可変 Triple
 * */
data class NodeParams(
    var isExpanded: Boolean,
    var parent: TreeViewNode?,
    var children: MutableList<TreeViewNode>
) : Serializable {
    override fun toString(): String = "($isExpanded, $parent, $children)"
}

/**
 * ツリーを構築するための基本ノード
 * */
interface TreeViewNode {
    val nodeParams : NodeParams
    val defaultNodeParams : NodeParams get() = NodeParams(false, null, mutableListOf())

    var content : Any?

    val isExpanded get() = nodeParams.isExpanded
    fun expand(){
        if( ! isExpanded ){
            nodeParams.isExpanded = true
            (root as? TreeViewRoot)?.let { it.onExpanded(it, this) }
        }
    }
    fun collapse(){
        if( isExpanded ){
            nodeParams.isExpanded = false
            (root as? TreeViewRoot)?.let { it.onCollapsed(it, this) }
        }
    }
    fun toggle() = if( isExpanded ) collapse() else expand()
    fun expandAll(){
        (listOf(this) + allChildren).filter { ! it.isExpanded }.forEach { it.expand() }
    }
    fun collapseAll(){
        (listOf(this) + allChildren).filter { it.isExpanded }.forEach { it.collapse() }
    }

    val parent : TreeViewNode? get() = nodeParams.parent
    val parents : List<TreeViewNode> get(){
        return parent?.run { val ret = mutableListOf<TreeViewNode>() ; ret.addAll(parents) ; ret.add(this) ; ret } ?: listOf()
    }
    val root : TreeViewNode get() = parents.firstOrNull() ?: this
    val layer : Int get() = parents.size

    val children : List<TreeViewNode> get() = nodeParams.children
    fun add(node : TreeViewNode, index : Int = children.size) : TreeViewNode {
        nodeParams.children.add(index, node)
        node.nodeParams.parent = this

        (root as? TreeViewRoot)?.let { it.onAdded(it, node) }

        return this
    }
    fun remove(node : TreeViewNode){
        (root as? TreeViewRoot)?.let { it.onPreRemoved(it, node) }

        nodeParams.children.remove(node)
        node.nodeParams.parent = null

        (root as? TreeViewRoot)?.let { it.onRemoved(it, node) }
    }
    val allChildren : List<TreeViewNode>
        get() = children.map { listOf(it) + it.allChildren }.flatten()
    val expandedChildren : List<TreeViewNode>
        get() = children.map { listOf(it) + if( it.isExpanded ){ it.expandedChildren }else{ listOf() } }.flatten()

    val isLeaf : Boolean get() = children.isEmpty()
    val isBranch : Boolean get() = children.isNotEmpty()
}
/**
 * ツリーの Root となるノード
 * */
interface TreeViewRoot : TreeViewNode {
    /** ノードが開いた場合に呼ばれる */
    var onExpanded : TreeViewRoot.(node : TreeViewNode) -> (Unit)
    /** ノードが閉じた場合に呼ばれる */
    var onCollapsed : TreeViewRoot.(node : TreeViewNode) -> (Unit)
    /** ノードが追加された場合に呼ばれる（除去の処理が実行された後に呼ばれる） */
    var onAdded : TreeViewRoot.(node : TreeViewNode) -> (Unit)
    /** ノードが除去される直前に呼ばれる */
    var onPreRemoved : TreeViewRoot.(node : TreeViewNode) -> (Unit)
    /** ノードが除去された直後に呼ばれる */
    var onRemoved : TreeViewRoot.(node : TreeViewNode) -> (Unit)
}



/**
 * [treeNode] のデフォルト実装を返す
 *
 * [childFactory] が指定されている場合はそれによって子となる [CONTENT] を取得して再帰的に子ノードを作成する
 *
 * [init] が指定されている場合は [init] によって返されるノードの初期化処理が行われる
 * */
fun <CONTENT : Any?> treeRoot(content : CONTENT, childFactory : (CONTENT)->(List<CONTENT>) = { listOf() }, init : TreeViewRoot.()->(Unit) = {} ) : TreeViewRoot {
    return object  : TreeViewRoot {
        override val nodeParams: NodeParams = defaultNodeParams
        override var content : Any? = content

        override var onExpanded: TreeViewRoot.(node: TreeViewNode) -> Unit = {}
        override var onCollapsed: TreeViewRoot.(node: TreeViewNode) -> Unit = {}
        override var onAdded: TreeViewRoot.(node: TreeViewNode) -> Unit = {}
        override var onPreRemoved: TreeViewRoot.(node: TreeViewNode) -> Unit = {}
        override var onRemoved: TreeViewRoot.(node: TreeViewNode) -> Unit = {}
    }.apply {
        childFactory(content).forEach {
            add( treeNode(it, childFactory) )
        }

        init()
    }
}
/**
 * [treeRoot] のデフォルト実装を返す
 *
 * [childFactory] が指定されている場合はそれによって子となる [CONTENT] を取得して再帰的に子ノードを作成する
 *
 * [init] が指定されている場合は [init] によって返されるノードの初期化処理が行われる
 * */
fun <CONTENT : Any?> treeNode(content : CONTENT, childFactory : (CONTENT)->(List<CONTENT>) = { listOf() }, init : TreeViewNode.()->(Unit) = {} ) : TreeViewNode {
    return object : TreeViewNode {
        override val nodeParams: NodeParams = defaultNodeParams
        override var content : Any? = content
    }.apply {
        childFactory(content).forEach {
            add( treeNode(it, childFactory) )
        }

        init()
    }
}
/** 子ノード追加のシンタックス・シュガー */
fun <CONTENT : Any?> TreeViewNode.create(content : CONTENT, childFactory : (CONTENT)->(List<CONTENT>) = { listOf() }, init : TreeViewNode.()->(Unit) = {}){
    add(treeNode(content, childFactory, init))
}


