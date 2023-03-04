package com.uygulamalarim.androidtaskegemensevgi.Worker

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.uygulamalarim.androidtaskegemensevgi.Adapter.RecyclerAdapter
import com.uygulamalarim.androidtaskegemensevgi.DataModel.ModelClassItem
import com.uygulamalarim.androidtaskegemensevgi.NetworkOperations.NetworkTask
import com.uygulamalarim.androidtaskegemensevgi.R
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class FetchDataWorker(context: Context, workerParameters: WorkerParameters) : Worker(context, workerParameters) {

    private val url1 = "https://api.baubuddy.de/index.php/login"
    private val url2 = "https://api.baubuddy.de/dev/index.php/v1/tasks/select"

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private var modelClassItemList = emptyList<ModelClassItem>()


    override fun doWork(): Result {
        Log.d(applicationContext.toString(),"FETCHDATAWORKERDOWORK ÇALIŞTI.")
        sharedPreferences = applicationContext.getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()

        if (isNetworkAvailable()) {
            fetchDataFromAPI()
        }
        return Result.success()
    }

    private fun fetchDataFromAPI() {

        if(isNetworkAvailable()){

            GlobalScope.launch {

                val networkTask = NetworkTask(object : NetworkTask.NetworkTaskListener {
                    override fun onResult(result: String?) {
                        Log.d("FETCHDATAWORKERRESULT", result ?: "")
                        val jsonString = result

                        val gson = Gson()
                        modelClassItemList = gson.fromJson(jsonString, Array<ModelClassItem>::class.java).toList()

                        editor.putString("jsonString", jsonString)
                        editor.apply()

                        (applicationContext as Activity).runOnUiThread {
                            displayData()
                        }
                    }
                })
                networkTask.execute(url1, url2)
            }

        }else{
            Toast.makeText(applicationContext, "Worker failed.", Toast.LENGTH_SHORT).show()
        }

    }

    private fun displayData() {
        val adapter = RecyclerAdapter(modelClassItemList)
        val recyclerView = (applicationContext as Activity).findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(applicationContext)
        recyclerView.adapter = adapter
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }
}