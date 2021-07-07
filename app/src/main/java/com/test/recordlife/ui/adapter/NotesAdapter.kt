package com.test.recordlife.ui.adapter

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.test.recordlife.R
import com.test.recordlife.util.DateUtil
import com.test.recordlife.util.inflate

class NotesAdapter(var dataList: List<HashMap<String, Any>>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun getItemCount() = dataList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        LaunchesViewHolder(R.layout.item_note.inflate(parent))

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = dataList[position]
        holder as LaunchesViewHolder
        item.run {
            holder.date.text = DateUtil.getDateAndTime(item["time"] as Long)

            holder.userName.text = item["userName"].toString()

            holder.text.text = item["text"].toString()

            holder.location.text = item["location"].toString()

        }
    }

    inner class LaunchesViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val date = view.findViewById<TextView>(R.id.date)
        val userName = view.findViewById<TextView>(R.id.name)
        val location = view.findViewById<TextView>(R.id.location)
        val text = view.findViewById<TextView>(R.id.note)
        val rootView = view.findViewById<LinearLayout>(R.id.rootView)
    }

    companion object {
        const val TAG = "NotesAdapter"
    }
}