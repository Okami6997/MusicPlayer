package com.musicplayer.auto

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template

/**
 * A simple Car App [Session] that launches the music browser [Screen].
 */
class MusicCarSession : Session() {
    override fun onCreateScreen(intent: android.content.Intent): Screen =
        MusicBrowserScreen(carContext)
}

/**
 * The main browsing screen shown inside Android Auto.
 */
class MusicBrowserScreen(carContext: CarContext) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        val listBuilder = ItemList.Builder()
        listBuilder.addItem(
            Row.Builder()
                .setTitle("Library")
                .addText("Browse your music library")
                .build()
        )
        listBuilder.addItem(
            Row.Builder()
                .setTitle("Recently Played")
                .addText("Jump back in")
                .build()
        )

        return ListTemplate.Builder()
            .setTitle("Music Player")
            .setHeaderAction(Action.APP_ICON)
            .setSingleList(listBuilder.build())
            .build()
    }
}
