package com.uygulamalarim.androidtaskegemensevgi

import android.app.AlertDialog
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.gson.Gson
import com.uygulamalarim.androidtaskegemensevgi.Adapter.RecyclerAdapter
import com.uygulamalarim.androidtaskegemensevgi.DataModel.ModelClassItem
import com.uygulamalarim.androidtaskegemensevgi.NetworkOperations.NetworkTask
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val url1 = "https://api.baubuddy.de/index.php/login"
    private val url2 = "https://api.baubuddy.de/dev/index.php/v1/tasks/select"
    private var modelClassItemList = emptyList<ModelClassItem>()

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    private lateinit var searchView: SearchView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val swipeRefreshLayout = findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout)
        sharedPreferences = applicationContext.getSharedPreferences("sharedPref", Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()
        val jsonString = sharedPreferences.getString("jsonString", null)


        if (isNetworkAvailable()) {
            fetchDataFromAPI()
            swipeRefreshLayout.setOnRefreshListener {
                fetchDataFromAPI()
                swipeRefreshLayout.isRefreshing = false
            }

        }else{
            if (jsonString == null) {
                Toast.makeText(this, "No Internet Connection, You need to connect to the internet at least 1 time", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No Internet Connection but Showing the Last Data From When You are Last Online ", Toast.LENGTH_SHORT).show()
                val gson = Gson()
                modelClassItemList = gson.fromJson(jsonString, Array<ModelClassItem>::class.java).toList()

                displayData()
                swipeRefreshLayout.setOnRefreshListener {
                    Toast.makeText(this, "No internet connection.", Toast.LENGTH_SHORT).show()
                    displayData()
                    swipeRefreshLayout.isRefreshing = false
                }


            }
        }

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.qrcodebutton -> {
                //TODO: create a scannerviewactivity intent


                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        val inflater = menuInflater
        inflater.inflate(R.menu.toolbar_items, menu)
        val manager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchItem = menu?.findItem(R.id.search)
        searchView = searchItem?.actionView as SearchView
        searchView.setSearchableInfo(manager.getSearchableInfo(componentName))
        searchView.onActionViewExpanded()
        searchView.clearFocus()

        return true
    }
    private fun fetchDataFromAPI() {
        GlobalScope.launch {
            val networkTask = NetworkTask(object : NetworkTask.NetworkTaskListener {
                override fun onResult(result: String?) {
                    Log.d("Resultante importante", result ?: "")
                    val jsonString = result

                    val gson = Gson()
                    modelClassItemList = gson.fromJson(jsonString, Array<ModelClassItem>::class.java).toList()

                    editor.putString("jsonString", jsonString)
                    editor.apply()


                    runOnUiThread {
                        displayData()
                    }
                }
            })
            networkTask.execute(url1, url2)
        }
    }
    private fun displayData() {
        val adapter = RecyclerAdapter(modelClassItemList)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this@MainActivity)
        recyclerView.adapter = adapter
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }

}