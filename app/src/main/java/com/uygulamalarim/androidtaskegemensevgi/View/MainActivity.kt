package com.uygulamalarim.androidtaskegemensevgi.View

import android.app.AlertDialog
import android.app.ProgressDialog
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
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.google.gson.Gson
import com.uygulamalarim.androidtaskegemensevgi.Adapter.RecyclerAdapter
import com.uygulamalarim.androidtaskegemensevgi.DataModel.ModelClassItem
import com.uygulamalarim.androidtaskegemensevgi.NetworkOperations.NetworkTask
import com.uygulamalarim.androidtaskegemensevgi.R
import com.uygulamalarim.androidtaskegemensevgi.Worker.FetchDataWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

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

        sharedPreferences = applicationContext.getSharedPreferences("sharedPref", Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()
        val jsonString = sharedPreferences.getString("jsonString", null)


        swipeRefreshAction()

        if (isNetworkAvailable()) {
            fetchDataFromAPI()

        }else{
            if (jsonString == null) {
                Toast.makeText(this, "No Internet Connection, You need to connect to the internet at least 1 time", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No Internet Connection but Showing the Last Data From When You are Last Online ", Toast.LENGTH_SHORT).show()
                val gson = Gson()
                modelClassItemList = gson.fromJson(jsonString, Array<ModelClassItem>::class.java).toList()
                displayData()
            }
        }

        val fetchWorkRequest = PeriodicWorkRequest.Builder(
            FetchDataWorker::class.java,
            60, TimeUnit.MINUTES)
            .setInitialDelay(15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "FETCH_DATA_WORKER",
            ExistingPeriodicWorkPolicy.KEEP,
            fetchWorkRequest)

    }
    private fun swipeRefreshAction(){ // swipe-2-refresh functionality
        val swipeRefreshLayout = findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener {
            if(isNetworkAvailable()){
                fetchDataFromAPI()
                swipeRefreshLayout.isRefreshing = false


            }else{
                Toast.makeText(this, "No internet connection.", Toast.LENGTH_SHORT).show()
                displayData()
                swipeRefreshLayout.isRefreshing = false
            }

        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.qrcodebutton -> {
                val i:Intent=Intent(this, ScannerViewActivity::class.java)
                startActivity(i)
                this.finish()



                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        val inflater = menuInflater
        inflater.inflate(R.menu.toolbar_items, menu)
        val manager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchItem = menu?.findItem(R.id.search)
        searchView = searchItem?.actionView as SearchView
        searchView.setSearchableInfo(manager.getSearchableInfo(componentName))
        searchView.onActionViewExpanded()
        searchView.clearFocus()


        var qr_scanned_text = intent.getStringExtra("scanned_code")


        if (qr_scanned_text!=null){
            searchView.setQuery(qr_scanned_text,true)
            AlertDialog.Builder(this)
                .setTitle("Qr Scan Successful")
                .setMessage("Scanned message is \"$qr_scanned_text\" please submit the value from above if you approve.")
                .setPositiveButton("OK"){dialogInterface, which ->
                    if(isNetworkAvailable()){
                        fetchDataFromAPI()

                    }else{
                        Toast.makeText(this, "No internet connection.", Toast.LENGTH_SHORT).show()
                        displayData()
                    }
                }
                .setIcon(R.drawable.checkicon).show()
        }






        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                (recyclerView.adapter as RecyclerAdapter).filter.filter(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {

                (recyclerView.adapter as RecyclerAdapter).filter.filter(newText)


                return true
            }
        })

        return true
    }
    private fun fetchDataFromAPI() {
        var  progressBar = findViewById<ProgressBar>(R.id.progressBar)
        progressBar.visibility = View.VISIBLE // ProgressBar görünür hale getiriliyor
        GlobalScope.launch {
            val networkTask = NetworkTask(object : NetworkTask.NetworkTaskListener {
                override fun onResult(result: String?) {
                    Log.d("Resultante importante", result ?: "")
                    val gson = Gson()
                    if (result == null) {
                        runOnUiThread {
                            displayData()
                            progressBar.visibility = View.GONE
                        }
                    } else {
                        modelClassItemList = gson.fromJson(result, Array<ModelClassItem>::class.java).toList()
                        editor.putString("jsonString", result)
                        editor.apply()
                        runOnUiThread {
                            displayData()
                            progressBar.visibility = View.GONE
                        }
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