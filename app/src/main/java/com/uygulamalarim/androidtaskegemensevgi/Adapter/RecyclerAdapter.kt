package com.uygulamalarim.androidtaskegemensevgi.Adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.uygulamalarim.androidtaskegemensevgi.DataModel.ModelClassItem
import com.uygulamalarim.androidtaskegemensevgi.R
import java.util.*

class RecyclerAdapter(private val dataSet: List<ModelClassItem>) :
    RecyclerView.Adapter<RecyclerAdapter.ViewHolder>(), Filterable {

    var filteredList: List<ModelClassItem> = dataSet

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tv_task: TextView = view.findViewById(R.id.tv_task)
        val tv_description: TextView = view.findViewById(R.id.tv_description)
        val tv_title: TextView = view.findViewById(R.id.tv_title)
        val tv_colorcode: TextView = view.findViewById(R.id.tv_colorcode)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.recycler_item_style, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val modelClassItem = filteredList[position]
        holder.tv_task.text = modelClassItem.task
        holder.tv_description.text = modelClassItem.description
        holder.tv_title.text = modelClassItem.title
        holder.tv_colorcode.text = modelClassItem.colorCode

        val colorCode = modelClassItem.colorCode
        if (!colorCode.isNullOrEmpty()) {
            holder.itemView.setBackgroundColor(Color.parseColor(colorCode))
        } else {
            holder.itemView.setBackgroundColor(Color.WHITE)
        }
    }

    override fun getItemCount() = filteredList.size

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val searchQuery = constraint.toString().toLowerCase(Locale.ROOT).trim()
                filteredList = if (searchQuery.isEmpty()) {
                    dataSet
                } else {
                    val tempList = ArrayList<ModelClassItem>()
                    for (modelClassItem in dataSet) {
                        if (modelClassItem.title.toLowerCase(Locale.ROOT).contains(searchQuery)) {
                            tempList.add(modelClassItem)
                        }
                        if (modelClassItem.task.toLowerCase(Locale.ROOT).contains(searchQuery)) {
                            tempList.add(modelClassItem)
                        }
                        if (modelClassItem.description.toLowerCase(Locale.ROOT).contains(searchQuery)) {
                            tempList.add(modelClassItem)
                        }
                        if (modelClassItem.colorCode.toLowerCase(Locale.ROOT).contains(searchQuery)) {
                            tempList.add(modelClassItem)
                        }




                    }
                    tempList
                }
                val filterResults = FilterResults()
                filterResults.values = filteredList
                return filterResults
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredList = results?.values as ArrayList<ModelClassItem>

                notifyDataSetChanged()
            }

        }
    }

}