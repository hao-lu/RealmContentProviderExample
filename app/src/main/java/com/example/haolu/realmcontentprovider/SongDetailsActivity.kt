package com.example.haolu.realmcontentprovider

import android.database.Cursor
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.content.CursorLoader
import android.support.v4.content.Loader
import kotlinx.android.synthetic.main.activity_song_details.*

class SongDetailsActivity : AppCompatActivity(), android.support.v4.app.LoaderManager.LoaderCallbacks<Cursor> {

    private lateinit var mUri: Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_song_details)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Needed for CursorLoader to get data
        mUri = intent.data
        // Invokes onCreateLoader()
        supportLoaderManager.initLoader(0, null, this)
    }

    override fun onLoaderReset(loader: Loader<Cursor>?) {
    }

    // Loads the cursor with the data from the intent
    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        return CursorLoader(this, mUri, null, null, null, null)
    }

    // After the cursor has been loaded with the name, the actionBar title can be set and the
    // fragments can get the name and begin to work
    override fun onLoadFinished(loader: Loader<Cursor>?, data: Cursor?) {
        if (data!!.moveToFirst()) {
            val rank = data.getString(data.getColumnIndex(data.getColumnName(0)))
            val title = data.getString(data.getColumnIndex(data.getColumnName(1)))
            val artist = data.getString(data.getColumnIndex(data.getColumnName(2)))

            text_rank.text = rank
            text_title.text = title
            text_artist.text = artist
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }
}
