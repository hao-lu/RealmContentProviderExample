# Using Realm and Kotlin with ContentProvider
I was working on my [app](https://play.google.com/store/apps/details?id=com.lucidity.haolu.duelking) and wanted to implement name suggestion drop down in my SearchView so I was looking into the documentation for custom suggestions and it needed a ContentProvider and some sort of database. I decided on Realm over SQLite to eliminate a lot of the boilerplate code from SQLite. Realm was a lot easier to get up running, although my app won't see many of speed and performance benefits. I didn't find any examples and documentation on using Realm and a ContentProvider so here's how I did it.

## Adding Realm to Your Android Project
In the top level gradle, add

```classpath "io.realm:realm-gradle-plugin:3.5.0"```

Apply the plugin in your project level gradle

```apply plugin: 'realm-android'```


## Importing your CSV file into Realm Browser
Realm Browser 2.1.7 and up exports .realm files in the new file format. However, it seems like the latest version of Realm for Java (3.5.0) does not support this new file format. 

	io.realm.exceptions.RealmFileException: Unable to open a realm at path '/data/data/com.example.haolu.realmcontentprovider/files/billboard.realm': Unsupported Realm file format version. (Unsupported Realm file format version) (/data/data/com.example.haolu.realmcontentprovider/files/billboard.realm) in /home/cc/repo/realm/release/realm/realm-library/src/main/cpp/io_realm_internal_SharedRealm.cpp line 252 Kind: ACCESS_ERROR.
	
If you want to import a CSV file into the Realm Browser, use Realm Browser 2.1.6 to get the old file format to fix the error.

## Creating Your SearchView
### search_menu.xml
    <item
        android:id="@+id/action_search"
        android:title="Search"
        android:icon="@drawable/ic_search_black_24dp"
        app:showAsAction="always"
        app:actionViewClass="android.support.v7.widget.SearchView"/>

### searchable.xml
This is where you can change attributes about your searchView.

***searchSuggestAuthority:*** Required to provide search suggestions

***searchSuggestIntentData:***  The default intent data to be used when a user clicks on a custom search suggestion.

***searchSuggestIntentData:*** The default intent action to be used when a user clicks on a custom search suggestion 

***searchSuggestThreshold:*** The minimum number of characters needed to trigger a suggestion look-up

***searchSuggestSelection:*** This value is passed into your query function as the selection parameter, the ? mark is needed as a placeholder for our query 

	<searchable xmlns:android="http://schemas.android.com/apk/res/android"
	    android:label="@string/app_name"
	    android:hint="Search"
		android:searchSuggestIntentAction="android.intent.action.VIEW"    
		android:searchSuggestAuthority="com.example.haolu.realmcontentprovider.SuggestionContentProvider"
	    android:searchSuggestIntentData="content://com.example.haolu.realmcontentprovider.SuggestionContentProvider/songs"
	    android:searchSuggestThreshold="1"
	    android:searchSuggestSelection=" ?">
	</searchable>
    
### AndroidManifest.xml
Content providers must be declared in the manfiest. 

	 <provider
	      android:name="com.example.haolu.realmcontentprovider.SuggestionContentProvider"
	      android:authorities="com.example.haolu.realmcontentprovider.SuggestionContentProvider"
	      android:exported="false" />
	      

## Create Your Content Provider

### Abstract Methods for Content Provider

	override fun onCreate(): Boolean {}
	override fun query(uri: Uri?, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor {}
 	override fun insert(uri: Uri?, values: ContentValues?): Uri {}
    override fun update(uri: Uri?, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {}
    override fun delete(uri: Uri?, selection: String?, selectionArgs: Array<out String>?): Int {}
    override fun getType(uri: Uri?): String {}
    
#### UriMatcher

    companion object {
        val AUTHORITY = "com.example.haolu.realmcontentprovider.SuggestionContentProvider"
        val CONTENT_URI = Uri.parse("content://$AUTHORITY/songs")
    }

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

For this example, we are only concerned about two cases the actual drop-down search suggestions and when the suggestion is selected. 


#### fun query()

    override fun query(uri: Uri?, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
        when (mUriMatcher.match(uri)) {
            SEARCH_SUGGEST -> if (selectionArgs == null)
                throw IllegalArgumentException("selectionArgs must be provided for the Uri: " + uri)
            else return getSuggestions(selectionArgs[0])
            GET_NAME -> return getName(uri!!)
            else -> throw IllegalArgumentException()
        }
    }

The SearchView will pass a Uri to query method and it will use our UriMatcher to determine whether it will return a list of suggestions or the data on the selected suggestion.

List of suggestions

    private fun getSuggestions(query: String): Cursor {        
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
        // Remember to close 
        mRealm.close()
        return matrixCursor
    }
  
  Selected suggestion
     
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
        // Remember to close 
        mRealm.close()
        return matrixCursor
    }


## Load Your Realm File
The app loads the .realm file from ```'/data/data/com.example.haolu.realmcontentprovider/files/'``` so when your app is initialized, you have to make sure the data is initialized in that path. 

	private fun copyBundledRealmFile(inputStream: InputStream, outFileName: String): String? {
        try {
            val file = File(this.filesDir, outFileName)
            val outputStream = FileOutputStream(file)
            val buf = ByteArray(1024)
            var bytesRead = inputStream.read(buf)
            while (bytesRead > 0) {
                outputStream.write(buf, 0, bytesRead)
                bytesRead = inputStream.read(buf)
            }
            outputStream.close()
            return file.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }