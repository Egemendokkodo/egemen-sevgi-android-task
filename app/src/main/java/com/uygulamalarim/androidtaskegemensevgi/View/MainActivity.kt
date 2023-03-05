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
import android.os.Message
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


// TASK FINISHED BY EGEMEN SEVGİ https://github.com/Egemendokkodo/egemen-sevgi-android-task
// FORKED THIS PROJECT https://github.com/Egemendokkodo/egemen-sevgi-android-task.
// FROM https://github.com/VERO-Digital-Solutions/android-task

/*
    Thank you for downloading/viewing this project.
    I paid attention to the MVVM archictecture and clean-code principles.
 */


class MainActivity : AppCompatActivity() {


    // we need these two urls to fetch the data from api.
    private val url1 = "https://api.baubuddy.de/index.php/login"
    private val url2 = "https://api.baubuddy.de/dev/index.php/v1/tasks/select"
    private var modelClassItemList = emptyList<ModelClassItem>()
    // The solution for viewing the data offline is Shared Preferences.
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    //We need to filter the data in recyclerview.
    private lateinit var searchView: SearchView




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        operations()

        swipeRefreshAction() // added swipe-2-refresh layout in activity main.xml and gave the functionality



        // created a worker that requests the resources from above every 60 minutes
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
    private fun operations(){
        // Include shared preferences.
        sharedPreferences = applicationContext.getSharedPreferences("sharedPref", Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()
        // the data from when user last online stored here.
        val jsonString = sharedPreferences.getString("jsonString", null)
        if (isNetworkAvailable()) {// checks if internet connection is availabla
            fetchDataFromAPI()

        }else{// when internet connection is not available
            if (jsonString == null) { // if stored data in shared preferences is null,
                // you need to connect at least 1 time to view the data offline
                AlertDialog.Builder(this).setTitle("No Internet Connection")
                    .setMessage("No Internet Connection, You need to connect to the internet at least 1 time")
                    .setIcon(R.drawable.ic_baseline_signal_wifi_off_24)
                    .setPositiveButton("OK"){dialogInterface, which ->
                        this.finish()
                    }.show()
            } else { // if you have no connection but stored the old data from your last time when you were online, this block will run.
                AlertDialog.Builder(this)
                    .setTitle("Warning")
                    .setMessage("No internet connection, but showing the last data from the last time you were online.")
                    .setIcon(R.drawable.ic_baseline_signal_wifi_off_24  )
                    .setPositiveButton("OK"){dialogInterface, which ->

                    }.show()
                val gson = Gson()
                modelClassItemList = gson.fromJson(jsonString, Array<ModelClassItem>::class.java).toList()
                displayData()
            }
        }
    }

    private fun swipeRefreshAction(){
        /*
        Problem:
            "In order to refresh the data, the app should offer:
            a swipe-2-refresh functionality"
        Solution:
           I added the swipe-2-refresh to layout file
                I also gave the feature that checks the internet connection.
                if you have internet, it fetches the data from api, if you dot have internet, it shows a toast message
         */
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
        /*
         Problem:
            The app should offer a menu item that allows scanning for QR-Codes
         Solution:
           when you push the button from the toolbar, scannerview activity opens.
           After switching to ScannerViewActivity, the app requests a permission from the user.
           if user gives the permission camera will be active for scan-searching
         */
        return when (item.itemId) {
            R.id.qrcodebutton -> {
                val i:Intent=Intent(this, ScannerViewActivity::class.java)
                startActivity(i)



                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        /*
        Problem:
            The app should offer a search menu item that allows searching for any of the class properties
            (even those, that are not visible to the user directly)

            The app should offer a menu item that allows scanning for QR-Codes
            Upon successful scan, the search query should be set to the scanned text
        Solution:
            search by typing:
                if you don't want to use qr code scan, you can simply type the search query to searchview.
            search by scanning qr code:
                in this function, i gave the functionality to searchview and search with qr button.
                firstly, qr_scanned_text stores the data from previous intent(ScannerViewActivity) with intent.getStringExtra method
                after storing the data with qr_scanned_text variable, it pastes into searchview as query.
                if you approve the qr code result, you can search it and filter the recyclerview.
         */
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
        /*
        Problem:
            Request the resources located at https://api.baubuddy.de/dev/index.php/v1/tasks/select
        Solution:
            Before starting the explanation for my solution, i want to highlight one thing.
            this function performs a network operation and we can't execute this
                process on main thread. So i used coroutines. Additionally, the code updates the UI using the
                "runOnUiThread" function to ensure that UI updates are performed safely on the UI thread without
                freezing the user interface.
            And also, his function does the pulling the json data from the api when you have internet connection.
            and saves it into the shared preferences for offline using purposes.

         */




        var  progressBar = findViewById<ProgressBar>(R.id.progressBar)
        progressBar.visibility = View.VISIBLE
        GlobalScope.launch {
            val networkTask = NetworkTask(object : NetworkTask.NetworkTaskListener {
                override fun onResult(result: String?) {
                    Log.d("Result", result ?: "")
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
        // displays the data.
        val adapter = RecyclerAdapter(modelClassItemList)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this@MainActivity)
        recyclerView.adapter = adapter
    }

    private fun isNetworkAvailable(): Boolean {
        // checks the network connection.
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }

}