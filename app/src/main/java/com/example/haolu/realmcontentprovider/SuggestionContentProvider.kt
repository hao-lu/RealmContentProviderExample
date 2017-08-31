package com.example.haolu.realmcontentprovider

import android.app.SearchManager
import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import io.realm.Case
import io.realm.Realm

class SuggestionContentProvider : ContentProvider() {

    private val TAG = "SuggestionProvider"

    companion object {
        val AUTHORITY = "com.example.haolu.realmcontentprovider.SuggestionContentProvider"
        val CONTENT_URI = Uri.parse("content://$AUTHORITY/songs")
    }

    // Different ID for each case
    private val SEARCH_SUGGEST = 0
    private val GET_NAME = 1

    private var mUriMatcher = buildUriMatcher()

    private fun buildUriMatcher(): UriMatcher {
        val matcher = UriMatcher(UriMatcher.NO_MATCH)

        // Get suggestions
        matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH_SUGGEST)
        matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH_SUGGEST)

        // Select suggestion
        matcher.addURI(AUTHORITY, "songs/#", GET_NAME)
        return matcher
    }

    override fun onCreate(): Boolean {
        return true
    }

    override fun query(uri: Uri?, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
        when (mUriMatcher.match(uri)) {
            SEARCH_SUGGEST -> if (selectionArgs == null)
                throw IllegalArgumentException("selectionArgs must be provided for the Uri: " + uri)
            else return getSuggestions(selectionArgs[0])
            GET_NAME -> return getName(uri!!)
            else -> throw IllegalArgumentException()
        }
    }

    // Get the cursor with the suggestions
    private fun getSuggestions(query: String): Cursor {
        // Need _id column, the column names have to match (suggest_text_1)
        // Need the suggest_column_intent_data_id to launch intent on click suggestion
        val columns = arrayOf("_ID", SearchManager.SUGGEST_COLUMN_TEXT_1, SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID)
        val lowerCaseQuery = query.toLowerCase()
        val mRealm = Realm.getDefaultInstance()
        val realmQuery = mRealm.where(Song::class.java)
        val results = realmQuery.beginsWith("title", lowerCaseQuery, Case.INSENSITIVE).findAll()
        val matrixCursor = MatrixCursor(columns)
        for (r in results) {
            val rowData = arrayOf(r.rank, r.title, r.rank)
            matrixCursor.addRow(rowData)
        }
        mRealm.close()
        return matrixCursor
    }

    // Get the name of the the suggestion clicked on
    private fun getName(uri: Uri): Cursor {
        val rowId = uri.lastPathSegment.toInt()
        val columns = arrayOf("_ID", SearchManager.SUGGEST_COLUMN_TEXT_1, SearchManager.SUGGEST_COLUMN_TEXT_2)
        val mRealm = Realm.getDefaultInstance()
        val realmQuery = mRealm.where(Song::class.java)
        val result = realmQuery.equalTo("rank", rowId).findFirst()
        val matrixCursor = MatrixCursor(columns)
        val rowData = arrayOf(result.rank, result.title, result.artist)
        matrixCursor.addRow(rowData)
        mRealm.close()
        return matrixCursor
    }

    override fun insert(uri: Uri?, values: ContentValues?): Uri {
        throw UnsupportedOperationException()
    }

    override fun update(uri: Uri?, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
        throw UnsupportedOperationException()
    }

    override fun delete(uri: Uri?, selection: String?, selectionArgs: Array<out String>?): Int {
        throw UnsupportedOperationException()
    }

    override fun getType(uri: Uri?): String {
        throw UnsupportedOperationException()
    }
}