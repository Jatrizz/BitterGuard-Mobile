package com.example.bitterguardmobile

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.TextView

class HelpFaqAdapter(
    private val context: Context,
    private val categories: List<String>,
    private val faqMap: Map<String, List<Pair<String, String>>>
) : BaseExpandableListAdapter() {

    override fun getGroupCount(): Int = categories.size

    override fun getChildrenCount(groupPosition: Int): Int =
        faqMap[categories[groupPosition]]?.size ?: 0

    override fun getGroup(groupPosition: Int): Any = categories[groupPosition]

    override fun getChild(groupPosition: Int, childPosition: Int): Any =
        faqMap[categories[groupPosition]]?.get(childPosition) ?: ("" to "")

    override fun getGroupId(groupPosition: Int): Long = groupPosition.toLong()

    override fun getChildId(groupPosition: Int, childPosition: Int): Long =
        (groupPosition * 1000 + childPosition).toLong()

    override fun hasStableIds(): Boolean = false

    override fun getGroupView(
        groupPosition: Int,
        isExpanded: Boolean,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        val view = LayoutInflater.from(context).inflate(R.layout.item_faq_group, parent, false)
        val tv = view.findViewById<TextView>(R.id.tvGroupTitle)
        tv.text = getGroup(groupPosition) as String
        val arrow = view.findViewById<android.widget.ImageView>(R.id.ivArrow)
        arrow.rotation = if (isExpanded) 180f else 0f
        return view
    }

    override fun getChildView(
        groupPosition: Int,
        childPosition: Int,
        isLastChild: Boolean,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        val view = LayoutInflater.from(context).inflate(R.layout.item_faq_child, parent, false)
        val pair = getChild(groupPosition, childPosition) as Pair<String, String>
        view.findViewById<TextView>(R.id.tvQuestion).text = pair.first
        view.findViewById<TextView>(R.id.tvAnswer).text = pair.second
        return view
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean = false
}


