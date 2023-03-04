package com.uygulamalarim.androidtaskegemensevgi.DataModel

import com.google.gson.annotations.SerializedName

data class ModelClassItem(
    @SerializedName("businessUnit")
    val businessUnit: String,
    @SerializedName("BusinessUnitKey")
    val businessUnitKey: String,
    @SerializedName("colorCode")
    val colorCode: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("isAvailableInTimeTrackingKioskMode")
    val isAvailableInTimeTrackingKioskMode: Boolean,
    @SerializedName("parentTaskID")
    val parentTaskID: String,
    @SerializedName("preplanningBoardQuickSelect")
    val preplanningBoardQuickSelect: Any,
    @SerializedName("sort")
    val sort: String,
    @SerializedName("task")
    val task: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("wageType")
    val wageType: String,
    @SerializedName("workingTime")
    val workingTime: Any
)