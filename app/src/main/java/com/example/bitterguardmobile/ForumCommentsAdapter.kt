package com.example.bitterguardmobile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bitterguardmobile.models.ForumComment

class ForumCommentsAdapter(private var items: MutableList<ForumComment>) : RecyclerView.Adapter<ForumCommentsAdapter.CommentVH>() {

    class CommentVH(view: View) : RecyclerView.ViewHolder(view) {
        val tvAuthor: TextView = view.findViewById(R.id.tvAuthor)
        val tvText: TextView = view.findViewById(R.id.tvText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentVH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_forum_comment, parent, false)
        return CommentVH(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: CommentVH, position: Int) {
        val c = items[position]
        holder.tvAuthor.text = c.authorName
        holder.tvText.text = c.text
    }

    fun updateItems(newItems: List<ForumComment>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}


