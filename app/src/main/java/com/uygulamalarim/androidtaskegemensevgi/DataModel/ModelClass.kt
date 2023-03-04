package com.uygulamalarim.androidtaskegemensevgi.DataModel

import com.google.gson.annotations.SerializedName

data class ModelClass(
    @SerializedName("items")
    val items: List<ModelClassItem>
)